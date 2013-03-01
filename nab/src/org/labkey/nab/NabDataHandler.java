/*
 * Copyright (c) 2009-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nab;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.OORDisplayColumnFactory;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 15, 2009
 */
public abstract class NabDataHandler extends AbstractExperimentDataHandler
{
    public static final String NAB_PROPERTY_LSID_PREFIX = "NabProperty";
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";

    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    public static final String FIT_ERROR_PROPERTY = "Fit Error";
    public static final String CURVE_IC_PREFIX = "Curve IC";
    public static final String POINT_IC_PREFIX = "Point IC";
    public static final String AUC_PREFIX = "AUC";
    public static final String pAUC_PREFIX = "PositiveAUC";
    public static final String DATA_ROW_LSID_PROPERTY = "Data Row LSID";
    public static final String AUC_PROPERTY_FORMAT = "0.000";

    protected class NabDataFileParser
    {
        private ExpData _data;
        private File _dataFile;
        private ViewBackgroundInfo _info;

        public NabDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info)
        {
            _data = data;
            _dataFile = dataFile;
            _info = info;
        }

        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            try
            {
                ExpRun run = _data.getRun();
                NabAssayRun assayResults = getAssayResults(run, _info.getUser(), _dataFile, null);
                List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

                for (int summaryIndex = 0; summaryIndex < assayResults.getSummaries().length; summaryIndex++)
                {
                    DilutionSummary dilution = assayResults.getSummaries()[summaryIndex];
                    WellGroup group = dilution.getFirstWellGroup();
                    ExpMaterial sampleInput = assayResults.getMaterial(group);

                    Map<String, Object> props = new HashMap<String, Object>();
                    results.add(props);

                    // generate curve ICs and AUCs for each curve fit type
                    for (DilutionCurve.FitType type : DilutionCurve.FitType.values())
                    {
                        for (Integer cutoff : assayResults.getCutoffs())
                        {
                            double value = dilution.getCutoffDilution(cutoff / 100.0, type);
                            saveICValue(getPropertyName(CURVE_IC_PREFIX, cutoff, type), value,
                                    dilution, props, type);

                            if (type == assayResults.getRenderedCurveFitType())
                            {
                                saveICValue(CURVE_IC_PREFIX + cutoff, value,
                                        dilution, props, type);
                            }
                        }
                        // compute both normal and positive AUC values
                        double auc = dilution.getAUC(type, DilutionCurve.AUCType.NORMAL);
                        if (!Double.isNaN(auc))
                        {
                            props.put(getPropertyName(AUC_PREFIX, type), auc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(AUC_PREFIX, auc);
                        }

                        double pAuc = dilution.getAUC(type, DilutionCurve.AUCType.POSITIVE);
                        if (!Double.isNaN(pAuc))
                        {
                            props.put(getPropertyName(pAUC_PREFIX, type), pAuc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(pAUC_PREFIX, pAuc);
                        }
                    }

                    // only need one set of interpolated ICs as they would be identical for all fit types
                    for (Integer cutoff : assayResults.getCutoffs())
                    {
                        saveICValue(POINT_IC_PREFIX + cutoff, dilution.getInterpolatedCutoffDilution(cutoff / 100.0, assayResults.getRenderedCurveFitType()),
                            dilution, props, assayResults.getRenderedCurveFitType());
                    }
                    props.put(FIT_ERROR_PROPERTY, dilution.getFitError());
                    props.put(NAB_INPUT_MATERIAL_DATA_PROPERTY, sampleInput.getLSID());
                    props.put(WELLGROUP_NAME_PROPERTY, group.getName());
                }
                return results;
            }
            catch (DilutionCurve.FitFailedException e)
            {
                throw new ExperimentException(e.getMessage(), e);
            }
        }

        protected void saveICValue(String name, double icValue, DilutionSummary dilution,
                                   Map<String, Object> results, DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
        {
            String outOfRange = null;
            if (Double.NEGATIVE_INFINITY == icValue)
            {
                outOfRange = "<";
                icValue = dilution.getMinDilution(type);
            }
            else if (Double.POSITIVE_INFINITY == icValue)
            {
                outOfRange = ">";
                icValue = dilution.getMaxDilution(type);
            }

            // Issue 15590: don't attempt to store values that are NaN
            if (Double.isNaN(icValue))
                return;
            
            results.put(name, icValue);
            results.put(name + OORDisplayColumnFactory.OORINDICATOR_COLUMN_SUFFIX, outOfRange);
        }
    }

    protected void throwParseError(File dataFile, String msg) throws ExperimentException
    {
        throwParseError(dataFile, msg, null);
    }

    protected void throwParseError(File dataFile, String msg, @Nullable Exception cause) throws ExperimentException
    {
        StringBuilder fullMessage = new StringBuilder("There was an error parsing ");
        fullMessage.append(dataFile.getName()).append(".\n");
        if (!dataFile.getName().toLowerCase().endsWith(getPreferredDataFileExtension().toLowerCase()))
        {
            fullMessage.append("Your data file may not be in ").append(getPreferredDataFileExtension()).append(" format.\nError details: ");
        }
        if (msg != null)
        {
            fullMessage.append(msg);
            fullMessage.append("\n");
        }
        if (cause != null)
            throw new ExperimentException(fullMessage.toString(), cause);
        else
            throw new ExperimentException(fullMessage.toString());
    }

    protected abstract String getPreferredDataFileExtension();

    public static class MissingDataFileException extends ExperimentException
    {
        public MissingDataFileException(String message)
        {
            super(message);
        }
    }

    public NabAssayRun getAssayResults(ExpRun run, User user) throws ExperimentException
    {
        return getAssayResults(run, user, null);
    }

    public NabAssayRun getAssayResults(ExpRun run, User user, @Nullable DilutionCurve.FitType fit) throws ExperimentException
    {
        File dataFile = getDataFile(run);
        if (dataFile == null)
            throw new MissingDataFileException("Nab data file could not be found for run " + run.getName() + ".  Deleted from file system?");
        return getAssayResults(run, user, dataFile, fit);
    }

    public File getDataFile(ExpRun run)
    {
        if (run == null)
            return null;
        ExpData[] outputDatas = run.getOutputDatas(getDataType());
        if (outputDatas == null || outputDatas.length != 1)
            throw new IllegalStateException("Nab runs should have a single data output.");
        File dataFile = outputDatas[0].getFile();
        if (!dataFile.exists())
            return null;
        return dataFile;
    }

    protected abstract List<Plate> createPlates(File dataFile, PlateTemplate template) throws ExperimentException;

    protected NabAssayRun getAssayResults(ExpRun run, User user, File dataFile, @Nullable DilutionCurve.FitType fit) throws ExperimentException
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = run.getContainer();
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
        PlateTemplate nabTemplate = provider.getPlateTemplate(container, protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getBatchDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = getCutoffFormats(protocol, run);
        List<Integer> sortedCutoffs = new ArrayList<Integer>(cutoffs.keySet());
        Collections.sort(sortedCutoffs);

        List<Plate> plates = createPlates(dataFile, nabTemplate);

        // Copy all properties from the input materials on the appropriate sample wellgroups; the NAb data processing
        // code uses well-group properties internally.
        Collection<ExpMaterial> sampleInputs = run.getMaterialInputs().keySet();
        Map<ExpMaterial, List<WellGroup>> inputs = getMaterialWellGroupMapping(provider, plates, sampleInputs);

        DomainProperty[] sampleProperties = provider.getSampleWellGroupDomain(protocol).getProperties();
        Map<String, DomainProperty> samplePropertyMap = new HashMap<String, DomainProperty>();
        for (DomainProperty sampleProperty : sampleProperties)
            samplePropertyMap.put(sampleProperty.getName(), sampleProperty);
        for (Map.Entry<ExpMaterial, List<WellGroup>> entry : inputs.entrySet())
            prepareWellGroups(entry.getValue(), entry.getKey(), samplePropertyMap);

        DomainProperty curveFitPd = runProperties.get(NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
        if (fit == null)
        {
            fit = DilutionCurve.FitType.FIVE_PARAMETER;
            if (curveFitPd != null)
            {
                Object value = run.getProperty(curveFitPd);
                if (value != null)
                    fit = DilutionCurve.FitType.fromLabel((String) value);
            }
        }
        boolean lockAxes = false;
        DomainProperty lockAxesProperty = runProperties.get(NabAssayProvider.LOCK_AXES_PROPERTY_NAME);
        if (lockAxesProperty != null)
        {
            Boolean lock = (Boolean) run.getProperty(lockAxesProperty);
            if (lock != null)
                lockAxes = lock;
        }

        NabAssayRun assay = createNabAssayRun(provider, run, plates, user, sortedCutoffs, fit);
        assay.setCutoffFormats(cutoffs);
        assay.setMaterialWellGroupMapping(inputs);
        assay.setDataFile(dataFile);
        assay.setLockAxes(lockAxes);
        return assay;
    }

   protected abstract boolean isDilutionDownOrRight();

    protected void applyDilution(List<? extends WellData> wells, ExpMaterial sampleInput, Map<String, DomainProperty> sampleProperties, boolean reverseDirection)
    {
        boolean first = true;
        Double dilution = (Double) sampleInput.getProperty(sampleProperties.get(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME));
        Double factor = (Double) sampleInput.getProperty(sampleProperties.get(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME));
        String methodString = (String) sampleInput.getProperty(sampleProperties.get(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME));
        SampleInfo.Method method = SampleInfo.Method.valueOf(methodString);
        // Single plate NAb run specimens get more dilute as you move up or left on the plate, while
        // high-throughput layouts get more dilute as you move down through the plates:
        boolean diluteDown = isDilutionDownOrRight();
        // The plate template may override the data handler's default dilution direction on a per-well group basis:
        if (reverseDirection)
            diluteDown = !diluteDown;
        // If we're diluting up, we start at the end of our list.  If down, we start at the beginning.
        int firstGroup = diluteDown ? 0 : wells.size() - 1;
        int incrementor = diluteDown ? 1 : -1;
        for (int wellIndex = firstGroup; wellIndex >= 0 && wellIndex < wells.size(); wellIndex = wellIndex + incrementor)
        {
            WellData well = wells.get(wellIndex);
            if (!first)
            {
                if (method == SampleInfo.Method.Dilution)
                    dilution *= factor;
                else if (method == SampleInfo.Method.Concentration)
                    dilution /= factor;
            }
            else
                first = false;
            well.setDilution(dilution);
        }
    }

    protected abstract void prepareWellGroups(List<WellGroup> wellgroups, ExpMaterial material, Map<String, DomainProperty> samplePropertyMap);

    protected abstract Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(NabAssayProvider provider, List<Plate> plates, Collection<ExpMaterial> sampleInputs) throws ExperimentException;

    protected abstract NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, List<Plate> plates, User user, List<Integer> sortedCutoffs, DilutionCurve.FitType fit);

    public Map<DilutionSummary, NabAssayRun> getDilutionSummaries(User user, DilutionCurve.FitType fit, int... dataObjectIds) throws ExperimentException, SQLException
    {
        Map<DilutionSummary, NabAssayRun> summaries = new LinkedHashMap<DilutionSummary, NabAssayRun>();
        if (dataObjectIds == null || dataObjectIds.length == 0)
            return summaries;

    if (!NabManager.useNewNab)
    {
        Map<String, NabAssayRun> dataToAssay = new HashMap<String, NabAssayRun>();
        for (int dataObjectId : dataObjectIds)
        {
            OntologyObject dataRow = OntologyManager.getOntologyObject(dataObjectId);
            if (dataRow == null || dataRow.getOwnerObjectId() == null)
                continue;
            Map<String, ObjectProperty> properties = OntologyManager.getPropertyObjects(dataRow.getContainer(), dataRow.getObjectURI());
            String wellgroupName = null;
            for (ObjectProperty property : properties.values())
            {
                if (WELLGROUP_NAME_PROPERTY.equals(property.getName()))
                {
                    wellgroupName = property.getStringValue();
                    break;
                }
            }
            if (wellgroupName == null)
                continue;

            OntologyObject dataParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId());
            if (dataParent == null)
                continue;
            String dataLsid = dataParent.getObjectURI();
            NabAssayRun assay = dataToAssay.get(dataLsid);
            if (assay == null)
            {
                ExpData dataObject = ExperimentService.get().getExpData(dataLsid);
                if (dataObject == null)
                    continue;
                assay = getAssayResults(dataObject.getRun(), user, fit);
                if (assay == null)
                    continue;
                dataToAssay.put(dataLsid, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getFirstWellGroup().getName()))
                {
                    summaries.put(summary, assay);
                    break;
                }
            }
        }
    }
    else
    {
        Map<Integer, NabAssayRun> dataToAssay = new HashMap<Integer, NabAssayRun>();
        List<Integer> nabSpecimenIds = new ArrayList<Integer>(dataObjectIds.length);
        for (int nabSpecimenId : dataObjectIds)
            nabSpecimenIds.add(nabSpecimenId);
        List<NabSpecimen> nabSpecimens = NabManager.get().getNabSpecimens(nabSpecimenIds);
        for (NabSpecimen nabSpecimen : nabSpecimens)
        {
            String wellgroupName = nabSpecimen.getWellgroupName();
            if (null == wellgroupName)
                continue;

            int runId = nabSpecimen.getRunId();
            NabAssayRun assay = dataToAssay.get(runId);
            if (assay == null)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (null == run)
                    continue;
                assay = getAssayResults(run, user, fit);
                if (null == assay)
                    continue;
                dataToAssay.put(runId, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getFirstWellGroup().getName()))
                {
                    summaries.put(summary, assay);
                    break;
                }
            }
        }
    }
        return summaries;
    }

    protected NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info)
    {
        return new NabDataFileParser(data, dataFile, info);
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        NabDataFileParser parser = getDataFileParser(data, dataFile, info);

        importRows(data, run, protocol, parser.getResults());
    }

    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData) throws ExperimentException
    {
        final String polySuffix = "_poly";
        final String oorSuffix = "OORIndicator";
        final String pl4Suffix = "_4pl";
        final String pl5Suffix = "_5pl";

        try
        {
            Container container = run.getContainer();
            OntologyManager.ensureObject(container, data.getLSID());
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);

            Map<String, ExpMaterial> inputMaterialMap = new HashMap<String, ExpMaterial>();

            for (ExpMaterial material : run.getMaterialInputs().keySet())
                inputMaterialMap.put(material.getLSID(), material);

            for (Map<String, Object> group : rawData)
            {
                if (!group.containsKey(WELLGROUP_NAME_PROPERTY))
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                if (group.get(WELLGROUP_NAME_PROPERTY) == null)
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                if (group.get(NAB_INPUT_MATERIAL_DATA_PROPERTY) == null)
                    throw new ExperimentException("The row must contain a value for the specimen lsid : " + NAB_INPUT_MATERIAL_DATA_PROPERTY);

                String groupName = group.get(WELLGROUP_NAME_PROPERTY).toString();
                String specimenLsid = group.get(NAB_INPUT_MATERIAL_DATA_PROPERTY).toString();

                ExpMaterial material = inputMaterialMap.get(specimenLsid);

                if (material == null)
                    throw new ExperimentException("The row must contain a value for the specimen lsid : " + NAB_INPUT_MATERIAL_DATA_PROPERTY);

                String dataRowLsid = getDataRowLSID(data, groupName, material.getPropertyValues()).toString();

                // TODO ***************** begin section that will go away when nab table transfer is complete
                OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();

                for (Map.Entry<String, Object> prop : group.entrySet())
                {
                    if (prop.getKey().equals(DATA_ROW_LSID_PROPERTY))
                        continue;

                    ObjectProperty objProp = getObjectProperty(container, protocol, dataRowLsid, prop.getKey(), prop.getValue(), cutoffFormats);
                    if (objProp != null)
                        results.add(objProp);
                }
                OntologyManager.insertProperties(container, dataRowLsid, results.toArray(new ObjectProperty[results.size()]));
                int objectId = results.size() > 0 ? results.get(0).getObjectId() : 0;
                // TODO ***************** end section

                // New code to insert into NAbSpecimen and CutoffValue tables instead of Ontology properties
                Map<String, Object> nabSpecimenEntries = new HashMap<String, Object>();
                nabSpecimenEntries.put(WELLGROUP_NAME_PROPERTY, groupName);
                nabSpecimenEntries.put("ObjectId", objectId);                       // TODO: this will go away  when nab table transfer is complete
                nabSpecimenEntries.put("ObjectUri", dataRowLsid);
                nabSpecimenEntries.put("ProtocolId", protocol.getRowId());
                nabSpecimenEntries.put("DataId", data.getRowId());
                nabSpecimenEntries.put("RunId", run.getRowId());
                nabSpecimenEntries.put("SpecimenLsid", group.get("SpecimenLsid"));
                nabSpecimenEntries.put("FitError", group.get(FIT_ERROR_PROPERTY));
                nabSpecimenEntries.put("Auc_Poly", group.get(AUC_PREFIX + polySuffix));
                nabSpecimenEntries.put("PositiveAuc_Poly", group.get(pAUC_PREFIX + polySuffix));
                nabSpecimenEntries.put("Auc_4pl", group.get(AUC_PREFIX + pl4Suffix));
                nabSpecimenEntries.put("PositiveAuc_4pl", group.get(pAUC_PREFIX + pl4Suffix));
                nabSpecimenEntries.put("Auc_5pl", group.get(AUC_PREFIX + pl5Suffix));
                nabSpecimenEntries.put("PositiveAuc_5pl", group.get(pAUC_PREFIX + pl5Suffix));
                int nabRowid = NabManager.get().insertNabSpecimenRow(null, nabSpecimenEntries);

                for (Integer cutoffValue : cutoffFormats.keySet())
                {
                    Map<String, Object> cutoffEntries = new HashMap<String, Object>();
                    cutoffEntries.put("NabSpecimenId", nabRowid);
                    cutoffEntries.put("Cutoff", (double)cutoffValue);

                    String cutoffStr = cutoffValue.toString();
                    String icKey = POINT_IC_PREFIX + cutoffStr;
                    cutoffEntries.put("Point", group.get(icKey));
                    icKey = POINT_IC_PREFIX + cutoffStr + oorSuffix;
                    cutoffEntries.put("PointOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + polySuffix;
                    cutoffEntries.put("IC_Poly", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + polySuffix + oorSuffix;
                    cutoffEntries.put("IC_PolyOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + pl4Suffix;
                    cutoffEntries.put("IC_4pl", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + pl4Suffix + oorSuffix;
                    cutoffEntries.put("IC_4plOORIndicator", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + pl5Suffix;
                    cutoffEntries.put("IC_5pl", group.get(icKey));
                    icKey = CURVE_IC_PREFIX + cutoffStr + pl5Suffix + oorSuffix;
                    cutoffEntries.put("IC_5plOORIndicator", group.get(icKey));
                    NabManager.get().insertCutoffValueRow(null, cutoffEntries);
                }

            }
        }
        catch (ValidationException ve)
        {
            throw new ExperimentException(ve.getMessage(), ve);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected ObjectProperty getObjectProperty(Container container, ExpProtocol protocol, String objectURI, String propertyName, Object value, Map<Integer, String> cutoffFormats)
    {
        if (isValidDataProperty(propertyName))
        {
            PropertyType type = PropertyType.STRING;
            String format = null;

            if (propertyName.equals(FIT_ERROR_PROPERTY))
            {
                type = PropertyType.DOUBLE;
                format = "0.0";
            }
            else if (propertyName.startsWith(AUC_PREFIX) || propertyName.startsWith(pAUC_PREFIX))
            {
                type = PropertyType.DOUBLE;
                format = AUC_PROPERTY_FORMAT;
            }
            else if (propertyName.startsWith(CURVE_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            else if (propertyName.startsWith(POINT_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, format);
        }
        return null;
    }

    protected boolean isValidDataProperty(String propertyName)
    {
        return DATA_ROW_LSID_PROPERTY.equals(propertyName) ||
                NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(propertyName) ||
                WELLGROUP_NAME_PROPERTY.equals(propertyName) ||
                FIT_ERROR_PROPERTY.equals(propertyName) ||
                propertyName.startsWith(AUC_PREFIX) ||
                propertyName.startsWith(pAUC_PREFIX) ||
                propertyName.startsWith(CURVE_IC_PREFIX) ||
                propertyName.startsWith(POINT_IC_PREFIX);
    }

    public Lsid getDataRowLSID(ExpData data, String wellGroupName, Map<PropertyDescriptor, Object> sampleProperties)
    {
        Lsid dataRowLsid = new Lsid(data.getLSID());
        dataRowLsid.setNamespacePrefix(NAB_DATA_ROW_LSID_PREFIX);
        dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + wellGroupName);
        return dataRowLsid;
    }

    protected ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(NAB_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    protected Map<Integer, String> getCutoffFormats(ExpProtocol protocol, ExpRun run)
    {
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getBatchDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = new HashMap<Integer, String>();
        for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
        {
            DomainProperty cutoffProp = runProperties.get(cutoffPropName);
            if (cutoffProp != null)
            {
                Integer cutoff = (Integer) run.getProperty(cutoffProp);
                if (cutoff != null)
                    cutoffs.put(cutoff, cutoffProp.getPropertyDescriptor().getFormat());
            }
        }

        if (cutoffs.isEmpty())
        {
            cutoffs.put(50, "0.000");
            cutoffs.put(80, "0.000");
        }
        return cutoffs;
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(container, p, run.getRowId());
        }
        return null;
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        try
        {
            NabManager.get().deleteRunData(datas);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        OntologyManager.deleteOntologyObject(data.getLSID(), container, true);
    }

    public String getPropertyName(String prefix, int cutoff, DilutionCurve.FitType type)
    {
        return getPropertyName(prefix + cutoff, type);
    }

    public String getPropertyName(String prefix, DilutionCurve.FitType type)
    {
        return prefix + "_" + type.getColSuffix();
    }

    public Integer getCutoffFromPropertyName(String propertyName)
    {
        if (propertyName.startsWith(CURVE_IC_PREFIX) && !propertyName.endsWith(OORDisplayColumnFactory.OORINDICATOR_COLUMN_SUFFIX))
        {
            // parse out the cutoff number
            int idx = propertyName.indexOf('_');
            String num;
            if (idx != -1)
                num = propertyName.substring(CURVE_IC_PREFIX.length(), propertyName.indexOf('_'));
            else
                num = propertyName.substring(CURVE_IC_PREFIX.length());

            return Integer.valueOf(num);
        }
        else if (propertyName.startsWith(POINT_IC_PREFIX) && !propertyName.endsWith(OORDisplayColumnFactory.OORINDICATOR_COLUMN_SUFFIX))
        {
            return Integer.valueOf(propertyName.substring(POINT_IC_PREFIX.length()));
        }
        return null;
    }
}
