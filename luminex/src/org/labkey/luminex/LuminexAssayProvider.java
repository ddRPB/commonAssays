/*
 * Copyright (c) 2007-2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.luminex;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends AbstractAssayProvider
{
    public static final String ASSAY_DOMAIN_ANALYTE = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Analyte";
    public static final String ASSAY_DOMAIN_EXCEL_RUN = ExpProtocol.ASSAY_DOMAIN_PREFIX + "ExcelRun";

    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", LuminexExcelDataHandler.LUMINEX_DATA_TYPE);
    }

    public String getName()
    {
        return "Luminex";
    }

    protected void registerLsidHandler()
    {
        super.registerLsidHandler();
        LsidManager.get().registerHandler("LuminexDataRow", new LsidManager.LsidHandler()
        {
            public ExpData getObject(Lsid lsid)
            {
                return getDataForDataRow(lsid.getObjectId());
            }

            public String getDisplayURL(Lsid lsid)
            {
                ExpData data = getDataForDataRow(lsid.getObjectId());
                if (data == null)
                    return null;
                ExpRun expRun = data.getRun();
                if (expRun == null)
                    return null;
                ExpProtocol protocol = expRun.getProtocol();
                if (protocol == null)
                    return null;
                ActionURL dataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(expRun.getContainer(), protocol, expRun.getRowId());
                return dataURL.getLocalURIString();
            }

            public Container getContainer(Lsid lsid)
            {
                ExpData data = getObject(lsid);
                return data == null ? null : data.getContainer();
            }
        });
    }

    @Override
    public List<PropertyDescriptor> getRunTableColumns(ExpProtocol protocol)
    {
        List<PropertyDescriptor> cols = super.getRunTableColumns(protocol);
        Collections.addAll(cols, getPropertyDescriptors(getDomainByPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN)));
        return cols;        
    }

    public TableInfo createDataTable(UserSchema schema, ExpProtocol protocol)
    {
        FilteredTable table = new LuminexSchema(schema.getUser(), schema.getContainer(), protocol).createDataRowTable();
        addCopiedToStudyColumns(table, protocol, schema.getUser(), "rowId", true);
        return table;
    }

    public Domain getRunDataDomain(ExpProtocol protocol)
    {
        return null;
    }


    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createBatchDomain(c, user);
        Domain domain = result.getKey();

        addProperty(domain, "Species", PropertyType.STRING);
        addProperty(domain, "LabID", "Lab ID", PropertyType.STRING);
        addProperty(domain, "AnalysisSoftware", "Analysis Software", PropertyType.STRING);

        return result;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        Domain runDomain = result.getKey();
        addProperty(runDomain, "ReplacesPreviousFile", "Replaces Previous File", PropertyType.BOOLEAN);
        addProperty(runDomain, "DateModified", "Date file was modified", PropertyType.DATE_TIME);
        addProperty(runDomain, "SpecimenType", "Specimen Type", PropertyType.STRING);
        addProperty(runDomain, "Additive", PropertyType.STRING);
        addProperty(runDomain, "Derivative", PropertyType.STRING);

        return result;
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Container lookupContainer = c.getProject();
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);

        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_ANALYTE + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":${AssayName}", "Analyte Properties");
        analyteDomain.setDescription("The user will be prompted to enter these properties for each of the analytes in the file they upload. This is the third and final step of the upload process.");
        addProperty(analyteDomain, "StandardName", "Standard Name", PropertyType.STRING);

        addProperty(analyteDomain, "UnitsOfConcentration", "Units of Concentration", PropertyType.STRING);

        ListDefinition isotypeList = lists.get("LuminexIsotypes");
        if (isotypeList == null)
        {
            isotypeList = ListService.get().createList(lookupContainer, "LuminexIsotypes");
            DomainProperty nameProperty = addProperty(isotypeList.getDomain(), "Name", PropertyType.STRING);
            nameProperty.setPropertyURI(isotypeList.getDomain().getTypeURI() + "#Name");
            isotypeList.setKeyName("IsotypeID");
            isotypeList.setTitleColumn(nameProperty.getName());
            isotypeList.setKeyType(ListDefinition.KeyType.Varchar);
            isotypeList.setDescription("Isotypes for Luminex assays");
            try
            {
                isotypeList.save(user);

                ListItem agItem = isotypeList.createListItem();
                agItem.setKey("Ag");
                agItem.setProperty(nameProperty, "Antigen");
                agItem.save(user);

                ListItem abItem = isotypeList.createListItem();
                abItem.setKey("Ab");
                abItem.setProperty(nameProperty, "Antibody");
                abItem.save(user);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        addProperty(analyteDomain, "Isotype", PropertyType.STRING).setLookup(new Lookup(lookupContainer, "lists", isotypeList.getName()));

        addProperty(analyteDomain, "AnalyteType", "Analyte Type", PropertyType.STRING);
        addProperty(analyteDomain, "WeightingMethod", "Weighting Method", PropertyType.STRING);
        
        addProperty(analyteDomain, "BeadManufacturer", "Bead Manufacturer", PropertyType.STRING);
        addProperty(analyteDomain, "BeadDist", "Bead Dist", PropertyType.STRING);
        addProperty(analyteDomain, "BeadCatalogNumber", "Bead Catalog Number", PropertyType.STRING);
        
        result.add(new Pair<Domain, Map<DomainProperty, Object>>(analyteDomain, Collections.<DomainProperty, Object>emptyMap()));

        Domain excelRunDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_EXCEL_RUN + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":${AssayName}", "Excel File Run Properties");
        excelRunDomain.setDescription("When the user uploads a Luminex data file, the server will try to find these properties in the header and footer of the spreadsheet, and does not prompt the user to enter them. This is part of the second step of the upload process.");
        addProperty(excelRunDomain, "FileName", "File Name", PropertyType.STRING);
        addProperty(excelRunDomain, "AcquisitionDate", "Acquisition Date", PropertyType.DATE_TIME);
        addProperty(excelRunDomain, "ReaderSerialNumber", "Reader Serial Number", PropertyType.STRING);
        addProperty(excelRunDomain, "PlateID", "Plate ID", PropertyType.STRING);
        addProperty(excelRunDomain, "RP1PMTvolts", "RP1 PMT (Volts)", PropertyType.DOUBLE);
        addProperty(excelRunDomain, "RP1Target", "RP1 Target", PropertyType.STRING);
        result.add(new Pair<Domain, Map<DomainProperty, Object>>(excelRunDomain, Collections.<DomainProperty, Object>emptyMap()));

        return result;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("Currently the only supported file type is the multi-sheet BioPlex Excel file format.");
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, LuminexUploadWizardAction.class);
    }
    
    public ExpData getDataForDataRow(Object dataRowId)
    {
        // on Postgres 8.3, we must pass in an integer row ID; passing a string that happens to be all digits isn't
        // sufficient, since 8.3 no longer does implicit type casting in this situation.
        Integer dataRowIdInt = null;
        if (dataRowId instanceof Integer)
            dataRowIdInt = ((Integer) dataRowId).intValue();
        else if (dataRowId instanceof String)
        {
            try
            {
                dataRowIdInt = Integer.parseInt((String) dataRowId);
            }
            catch (NumberFormatException e)
            {
                // we'll error out below...
            }
        }
        if (dataRowIdInt == null)
            throw new IllegalArgumentException("Luminex data rows must have integer primary keys.  PK provided: " + dataRowId);
        LuminexDataRow dataRow = Table.selectObject(LuminexSchema.getTableInfoDataRow(), dataRowIdInt, LuminexDataRow.class);
        if (dataRow == null)
        {
            return null;
        }
        return ExperimentService.get().getExpData(dataRow.getDataId());
    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("Data", "Run", "RowId");
    }

    public FieldKey getParticipantIDFieldKey()
    {
        return FieldKey.fromParts(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
    }

    public FieldKey getVisitIDFieldKey(Container container)
    {
        if (AssayPublishService.get().getTimepointType(container) == TimepointType.VISIT)
            return FieldKey.fromParts(AbstractAssayProvider.VISITID_PROPERTY_NAME);
        else
            return FieldKey.fromParts(AbstractAssayProvider.DATE_PROPERTY_NAME);
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("RowId");
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        return FieldKey.fromParts("Description");
    }

    public ActionURL copyToStudy(User user, ExpProtocol protocol, Container study, Map<Integer, AssayPublishKey> dataKeys, List<String> errors)
    {
        try
        {
            SimpleFilter filter = new SimpleFilter();
            filter.addInClause("RowId", dataKeys.keySet());
            LuminexDataRow[] luminexDataRows = Table.select(LuminexSchema.getTableInfoDataRow(), Table.ALL_COLUMNS, filter, null, LuminexDataRow.class);

            List<Map<String, Object>> dataMaps = new ArrayList<Map<String, Object>>(luminexDataRows.length);

            Map<Integer, Pair<Analyte, Map<String, ObjectProperty>>> analytes = new HashMap<Integer, Pair<Analyte, Map<String, ObjectProperty>>>();

            PropertyDescriptor[] excelRunPDs = getPropertyDescriptors(getDomainByPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN));
            CopyToStudyContext context = new CopyToStudyContext(protocol, excelRunPDs);

            TimepointType timepointType = AssayPublishService.get().getTimepointType(study);

            Container sourceContainer = null;

            List<PropertyDescriptor> types = new ArrayList<PropertyDescriptor>();
            // little hack here: since the property descriptors created by the 'addProperty' calls below are not in the database,
            // they have no RowId, and such are never equal to each other.  Since the loop below is run once for each row of data,
            // this will produce a types set that contains rowCount*columnCount property descriptors unless we prevent additions
            // to the map after the first row.  This is done by nulling out the 'tempTypes' object after the first iteration:
            List<PropertyDescriptor> tempTypes = types;
            for (LuminexDataRow luminexDataRow : luminexDataRows)
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                addProperty(study, "RowId", luminexDataRow.getRowId(), dataMap, tempTypes);
                addProperty(study, "ConcInRangeString", luminexDataRow.getConcInRangeString(), dataMap, tempTypes);
                addProperty(study, "ConcInRange", luminexDataRow.getConcInRange(), dataMap, tempTypes);
                addProperty(study, "ConcInRangeOORIndicator", luminexDataRow.getConcInRangeOORIndicator(), dataMap, tempTypes);
                addProperty(study, "ExpConc", luminexDataRow.getExpConc(), dataMap, tempTypes);
                addProperty(study, "FI", luminexDataRow.getFi(), dataMap, tempTypes);
                addProperty(study, "FIString", luminexDataRow.getFiString(), dataMap, tempTypes);
                addProperty(study, "FIOORIndicator", luminexDataRow.getFiOORIndicator(), dataMap, tempTypes);
                addProperty(study, "FIBackground", luminexDataRow.getFiBackground(), dataMap, tempTypes);
                addProperty(study, "FIBackgroundString", luminexDataRow.getFiBackgroundString(), dataMap, tempTypes);
                addProperty(study, "FIBackgroundOORIndicator", luminexDataRow.getFiBackgroundOORIndicator(), dataMap, tempTypes);
                addProperty(study, "ObsConcString", luminexDataRow.getObsConcString(), dataMap, tempTypes);
                addProperty(study, "ObsConc", luminexDataRow.getObsConc(), dataMap, tempTypes);
                addProperty(study, "ObsConcOORIndicator", luminexDataRow.getObsConcOORIndicator(), dataMap, tempTypes);
                addProperty(study, "ObsOverExp", luminexDataRow.getObsOverExp(), dataMap, tempTypes);
                addProperty(study, "StdDev", luminexDataRow.getStdDev(), dataMap, tempTypes);
                addProperty(study, "StdDevString", luminexDataRow.getStdDevString(), dataMap, tempTypes);
                addProperty(study, "StdDevOORIndicator", luminexDataRow.getStdDevOORIndicator(), dataMap, tempTypes);
                addProperty(study, "Type", luminexDataRow.getType(), dataMap, tempTypes);
                addProperty(study, "Well", luminexDataRow.getWell(), dataMap, tempTypes);
                addProperty(study, "Dilution", luminexDataRow.getDilution(), dataMap, tempTypes);
                addProperty(study, "DataRowGroup", luminexDataRow.getDataRowGroup(), dataMap, tempTypes);
                addProperty(study, "Ratio", luminexDataRow.getRatio(), dataMap, tempTypes);
                addProperty(study, "SamplingErrors", luminexDataRow.getSamplingErrors(), dataMap, tempTypes);
                addProperty(study, "Outlier", luminexDataRow.isOutlier(), dataMap, tempTypes);
                addProperty(study, "Description", luminexDataRow.getDescription(), dataMap, tempTypes);
                addProperty(study, "ExtraSpecimenInfo", luminexDataRow.getExtraSpecimenInfo(), dataMap, tempTypes);
                addProperty(study, "SourceLSID", new Lsid("LuminexDataRow", Integer.toString(luminexDataRow.getRowId())).toString(), dataMap, tempTypes);

                ExpRun run = copyRunProperties(user, study, tempTypes, luminexDataRow, dataMap, context);
                sourceContainer = run.getContainer();
                copyAnalyteProperties(study, analytes, tempTypes, luminexDataRow, dataMap, sourceContainer, protocol);

                AssayPublishKey dataKey = dataKeys.get(luminexDataRow.getRowId());
                addProperty(sourceContainer, AssayPublishService.PARTICIPANTID_PROPERTY_NAME, dataKey.getParticipantId(), dataMap, tempTypes);
                if (timepointType == TimepointType.VISIT)
                {
                    addProperty(sourceContainer, AssayPublishService.SEQUENCENUM_PROPERTY_NAME, (double)dataKey.getVisitId(), dataMap, tempTypes);
                    addProperty(sourceContainer, AssayPublishService.DATE_PROPERTY_NAME, luminexDataRow.getDate(), dataMap, tempTypes);
                }
                else
                {
                    addProperty(sourceContainer, AssayPublishService.SEQUENCENUM_PROPERTY_NAME, luminexDataRow.getVisitID(), dataMap, tempTypes);
                    addProperty(sourceContainer, AssayPublishService.DATE_PROPERTY_NAME, dataKey.getDate(), dataMap, tempTypes);
                }

                dataMaps.add(dataMap);
                tempTypes = null;
            }
            return AssayPublishService.get().publishAssayData(user, sourceContainer, study, protocol.getName(), protocol, dataMaps, types, getDataRowIdFieldKey().toString(), errors);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private ExpRun copyRunProperties(User user, Container study, List<PropertyDescriptor> tempTypes, LuminexDataRow luminexDataRow, Map<String, Object> dataMap, CopyToStudyContext context) throws SQLException
    {
        ExpData data = context.getData(luminexDataRow.getDataId());
        ExpRun run = context.getRun(data);
        addStandardRunPublishProperties(user, study, tempTypes, dataMap, run, context);
        return run;
    }

    private void copyAnalyteProperties(Container study, Map<Integer, Pair<Analyte, Map<String, ObjectProperty>>> analytes, List<PropertyDescriptor> tempTypes, LuminexDataRow luminexDataRow, Map<String, Object> dataMap, Container container, ExpProtocol protocol)
        throws SQLException
    {
        // Look up the analyte info so we can copy it
        Pair<Analyte, Map<String, ObjectProperty>> analyteInfo = analytes.get(luminexDataRow.getAnalyteId());
        Analyte analyte;
        Map<String, ObjectProperty> analyteProps;
        // Check if we've already seen it and put in our cache
        if (analyteInfo == null)
        {
            analyte = Table.selectObject(LuminexSchema.getTableInfoAnalytes(), luminexDataRow.getAnalyteId(), Analyte.class);
            analyteProps = OntologyManager.getPropertyObjects(container, analyte.getLsid());
            analytes.put(analyte.getRowId(), new Pair<Analyte, Map<String, ObjectProperty>>(analyte, analyteProps));
        }
        else
        {
            analyte = analyteInfo.first;
            analyteProps = analyteInfo.second;
        }

        // Handle the hard-coded properties
        addProperty(study, "Analyte Name", analyte.getName(), dataMap, tempTypes);
        addProperty(study, "Analyte FitProb", analyte.getFitProb(), dataMap, tempTypes);
        addProperty(study, "Analyte RegressionType", analyte.getRegressionType(), dataMap, tempTypes);
        addProperty(study, "Analyte ResVar", analyte.getResVar(), dataMap, tempTypes);
        addProperty(study, "Analyte StdCurve", analyte.getStdCurve(), dataMap, tempTypes);
        addProperty(study, "Analyte MinStandardRecovery", analyte.getMinStandardRecovery(), dataMap, tempTypes);
        addProperty(study, "Analyte MaxStandardRecovery", analyte.getMaxStandardRecovery(), dataMap, tempTypes);

        // Handle the configurable properties
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(protocol, ASSAY_DOMAIN_ANALYTE);
        for (DomainProperty dp : analyteDomain.getProperties())
        {
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            ObjectProperty prop = analyteProps.get(pd.getPropertyURI());
            PropertyDescriptor publishPD = pd.clone();
            publishPD.setName("Analyte" + pd.getName());
            publishPD.setLabel("Analyte " + pd.getLabel());
            addProperty(publishPD, prop, dataMap, tempTypes);
        }
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    public Set<String> getReservedPropertyNames(ExpProtocol protocol, Domain domain)
    {
        Set<String> result = super.getReservedPropertyNames(protocol, domain);

        if (isDomainType(domain, protocol, ASSAY_DOMAIN_ANALYTE))
        {
            result.add("Name");
            result.add("FitProb");
            result.add("Fit Prob");
            result.add("RegressionType");
            result.add("Regression Type");
            result.add("ResVar");
            result.add("Res Var");
            result.add("StdCurve");
            result.add("Std Curve");
            result.add("MinStandardRecovery");
            result.add("Min Standard Recovery");
            result.add("MaxStandardRecovery");
            result.add("Max Standard Recovery");
        }

        return result;
    }

    public String getDescription()
    {
        return "Imports data in the multi-sheet BioPlex Excel file format.";
    }
}
