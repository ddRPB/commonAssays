/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.apache.log4j.Logger;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.security.ACL;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.util.HString;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewForm;
import org.labkey.api.view.HttpView;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationSet;
import org.labkey.flow.analysis.model.ScriptComponent;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.*;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowSchema;

import java.io.FileNotFoundException;
import java.util.*;


public class EditScriptForm extends ViewForm
{
    static private Logger _log = Logger.getLogger(EditScriptForm.class);
    private static int MAX_WELLS_TO_POLL = 15;

    public FlowScript analysisScript;
    public ScriptDocument analysisDocument;
    public FlowProtocolStep step;
    private int _runCount;
    private FlowCompensationMatrix _comp;
    private FlowRun _run;

    @Override
    public void setViewContext(ViewContext context)
    {
        super.setViewContext(context);
        reset();
    }

    public void reset()
    {
        try
        {
            HString scriptIdStr = new HString(getRequest().getParameter("scriptId"));
            if (scriptIdStr == null)
                HttpView.throwNotFound("scriptId required");
            int scriptId = 0;
            try
            {
                scriptId = scriptIdStr.parseInt();
            }
            catch (NumberFormatException nfe)
            {
                HttpView.throwNotFound("scriptId must be an integer");
            }
            analysisScript = FlowScript.fromScriptId(scriptId);
            if (analysisScript == null || analysisScript.getExpObject() == null)
                HttpView.throwNotFound("scriptId not found: " + scriptIdStr);
            _runCount = analysisScript.getRunCount();
            step = FlowProtocolStep.fromRequest(getRequest());
            _run = FlowRun.fromURL(getViewContext().getActionURL(), getRequest());
            if (_run != null)
            {
                FlowPreference.editScriptRunId.setValue(getRequest(), Integer.toString(_run.getRunId()));
            }
            _comp = FlowCompensationMatrix.fromURL(getViewContext().getActionURL(), getRequest());
            if (_comp != null)
            {
                FlowPreference.editScriptCompId.setValue(getRequest(), Integer.toString(_comp.getRowId()));
            }
            String strWellId = getRequest().getParameter(FlowParam.wellId.toString());
            if (strWellId != null)
            {
                FlowPreference.editScriptWellId.setValue(getRequest(), strWellId);
            }
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
        try
        {
            analysisDocument = analysisScript.getAnalysisScriptDocument();
        }
        catch (Exception e)
        {

        }
    }
    
    public ScriptComponent getAnalysis() throws Exception
    {
        return analysisScript.getCompensationCalcOrAnalysis(step);
    }

    private void addPopulation(Map<SubsetSpec, Population> map, SubsetSpec parent, Population pop)
    {
        SubsetSpec subset = new SubsetSpec(parent, pop.getName());
        map.put(subset, pop);
        for (Population child : pop.getPopulations())
        {
            addPopulation(map, subset, child);
        }
    }

    public Map<SubsetSpec, Population> getPopulations() throws Exception
    {
        LinkedHashMap<SubsetSpec, Population> ret = new LinkedHashMap<SubsetSpec, Population>();
        PopulationSet popset = getAnalysis();
        for (Population child : popset.getPopulations())
        {
            addPopulation(ret, null, child);
        }
        return ret;
    }

    public FlowRun getRun()
    {
        if (_run != null)
            return _run;
        int runId = FlowPreference.editScriptRunId.getIntValue(getRequest());
        FlowRun run = FlowRun.fromRunId(runId);
        if (run != null && run.getContainer().equals(getContainer()))
        {
            _run = run;
            return _run;
        }
        try
        {
            FlowRun[] available = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
            for (FlowRun runTry : available)
            {
                if (runTry.getPath() != null)
                {
                    _run = runTry;
                    break;
                }
            }
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        return _run;
    }

    public FlowCompensationMatrix getCompensationMatrix()
    {
        if (_comp != null)
            return _comp;
        int compId = FlowPreference.editScriptCompId.getIntValue(getRequest());
        FlowCompensationMatrix comp = FlowCompensationMatrix.fromCompId(compId);
        if (comp != null && comp.getContainer().equals(getContainer()))
        {
            _comp = comp;
            return _comp;
        }
        try
        {
            List matrices = FlowCompensationMatrix.getCompensationMatrices(getContainer());
            if (matrices.size() == 0)
                return null;
            _comp = (FlowCompensationMatrix) matrices.get(0);
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        return _comp;
    }

    public Map<String, String> getParameters()
    {
        try
        {
            String[] compChannels = analysisScript.getCompensationChannels();
            if (compChannels == null)
            {
                if (getRun() != null)
                {
                    FlowCompensationMatrix matrix = getCompensationMatrix();
                    if (matrix != null)
                    {
                        compChannels = matrix.getCompensationMatrix().getChannelNames();
                    }
                }
            }
            if (compChannels == null)
                compChannels = new String[0];
            return getParameterNames(getRun(), compChannels);
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
            return Collections.EMPTY_MAP;
        }
    }

    static public Map<String, String> getParameterNames(FlowRun run, String[] compChannels)
    {
        Map<String, String> ret = new LinkedHashMap<String, String>();
        if (run == null)
            return ret;

        FlowWell[] wells = run.getWells(true);
        int cWellsCounted = 0;
        for (int i = 0; i < wells.length && cWellsCounted < MAX_WELLS_TO_POLL; i ++)
        {
            try
            {
                try
                {
                    Map<String, String> wellParams = FCSAnalyzer.get().getParameterNames(wells[i].getFCSURI(), compChannels);
                    for (Map.Entry<String, String> entry : wellParams.entrySet())
                    {
                        String previous = ret.get(entry.getKey());
                        if (previous == null)
                        {
                            ret.put(entry.getKey(), entry.getValue());
                        }
                        else if (previous.length() < entry.getValue().length())
                        {
                            ret.put(entry.getKey(), entry.getValue());
                        }
                    }
                    cWellsCounted ++;
                }
                catch(FileNotFoundException e)
                {
                    _log.warn("Error opening file " + wells[i].getFCSURI(), e);
                }
            }
            catch(Exception e)
            {
                _log.error("Error", e);
            }
        }
        return ret;
    }

    public ActionURL urlFor(ScriptController.Action action)
    {
        ActionURL url = analysisScript.urlFor(action);
        if (step != null)
        {
            step.addParams(url);
        }

        return url;
    }

    public String[] getAvailableKeywords()
    {
        HashSet<String> keywords = new HashSet<String>();
        try
        {
            FlowRun run = getRun();
            FlowSchema schema = new FlowSchema(getUser(), getContainer());
            if (run != null)
                schema.setRun(run);
            FlowPropertySet fps = new FlowPropertySet(schema.createFCSFileTable("foo"));
            keywords.addAll(fps.getKeywordProperties().keySet());
        }
        catch (Throwable t)
        {
            _log.error("Error", t);
        }
        String[] ret = keywords.toArray(new String[keywords.size()]);
        Arrays.sort(ret);
        return ret;
    }

    public boolean canEdit()
    {
        return getContainer().hasPermission(getUser(), ACL.PERM_UPDATE) && _runCount == 0;
    }

    public Map<Integer, String> getExperimentRuns() throws Exception
    {
        return getExperimentRuns(false);
    }

    public Map<Integer, String> getExperimentRuns(boolean realFiles) throws Exception
    {
        LinkedHashMap<Integer, String> ret = new LinkedHashMap<Integer, String>();
        FlowRun[] runs = realFiles ?
                FlowRun.getRunsWithRealFCSFiles(getContainer(), FlowProtocolStep.keywords) :
                FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
        for (FlowRun run : runs)
        {
            ret.put(run.getRunId(), run.getName());
        }
        return ret;
    }
    
}
