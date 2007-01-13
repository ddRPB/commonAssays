package org.fhcrc.cpas.flow.data;

import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.*;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.query.FlowTableType;
import org.labkey.api.security.User;
import org.labkey.api.query.QueryAction;
import org.apache.log4j.Logger;
import org.apache.commons.lang.ObjectUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;
import java.io.File;

import Flow.FlowParam;

public class FlowExperiment extends FlowObject<ExpExperiment>
{
    static private final Logger _log = Logger.getLogger(FlowExperiment.class);
    static public String FlowExperimentRunExperimentName = "Flow Experiment Runs";
    static public final String PROP_PRIMARY_ANALYSIS = "PrimaryAnalysis";

    public FlowExperiment(ExpExperiment experiment)
    {
        super(experiment);
    }

    static public FlowExperiment fromLSID(String lsid)
    {
        if (lsid == null)
            return null;
        ExpExperiment exp = ExperimentService.get().getExperiment(lsid);
        if (exp == null)
            return null;
        return new FlowExperiment(exp);
    }

    static public FlowExperiment fromExperimentId(int id)
    {
        ExpExperiment experiment = ExperimentService.get().getExperiment(id);
        if (experiment == null)
            return null;
        return new FlowExperiment(experiment);
    }

    static public FlowExperiment[] getExperiments(Container container)
    {
        ExperimentService.Interface svc = ExperimentService.get();
        ExpExperiment[] experiments = svc.getExperiments(container);
        FlowExperiment[] ret = new FlowExperiment[experiments.length];
        for (int i = 0; i < experiments.length; i ++)
        {
            ret[i] = new FlowExperiment(experiments[i]);
        }
        return ret;
    }

    static public FlowExperiment[] getAnalyses(Container container)
    {
        FlowExperiment[] all = getExperiments(container);
        List<FlowExperiment> ret = new ArrayList();
        for (FlowExperiment exp : all)
        {
            if (exp.getRunCount(FlowProtocolStep.analysis) != 0 || exp.getRunCount(FlowProtocolStep.calculateCompensation) != 0)
                ret.add(exp);
        }
        return ret.toArray(new FlowExperiment[0]);
    }

    static public FlowExperiment getDefaultAnalysis(Container container)
    {
        FlowExperiment[] experiments = getAnalyses(container);
        if (experiments.length == 0)
            return null;
        return experiments[0];
    }

    static public FlowExperiment getPrimaryAnalysis(User user, Container container)
    {
        Map<String, Object> props = PropertyManager.getProperties(user.getUserId(), container.getId(), PROP_CATEGORY, false);
        if (props != null)
        {
            Object lsid = props.get(PROP_PRIMARY_ANALYSIS);
            FlowExperiment ret = fromLSID(ObjectUtils.toString(lsid));
            if (ret != null)
                return ret;
        }
        return getDefaultAnalysis(container);
    }

    static public String getExperimentRunExperimentLSID(Container container)
    {
        FlowExperiment exp = getExperimentRunExperiment(container);
        if (exp != null)
            return exp.getLSID();
        return FlowObject.generateLSID(container, "Experiment", FlowExperimentRunExperimentName);
    }

    static public String getExperimentRunExperimentName(Container container)
    {
        FlowExperiment exp = getExperimentRunExperiment(container);
        if (exp != null)
            return exp.getName();
        return FlowExperimentRunExperimentName;
    }

    static public FlowExperiment getExperimentRunExperiment(Container container)
    {
        String lsidDefault = FlowObject.generateLSID(container, "Experiment", FlowExperimentRunExperimentName);
        ExpExperiment exp = ExperimentService.get().getExperiment(lsidDefault);
        if (exp != null)
            return new FlowExperiment(exp);

        for (FlowExperiment experiment : getExperiments(container))
        {
            if (experiment.getRunCount(FlowProtocolStep.keywords) != 0)
            {
                return experiment;
            }
        }
        return null;
    }

    static public FlowExperiment fromURL(ViewURLHelper url) throws ServletException
    {
        return fromURL(url, null);
    }

    static public FlowExperiment fromURL(ViewURLHelper url, HttpServletRequest request) throws ServletException
    {
        FlowExperiment ret = fromExperimentId(getIntParam(url, request, FlowParam.experimentId));
        if (ret == null)
        {
            return null;
        }
        ret.checkContainer(url);
        return ret;
    }

    public ExpExperiment getExperiment()
    {
        return getExpObject();
    }

    public int getExperimentId()
    {
        return getExperiment().getRowId();
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.experimentId, getExperimentId());
    }

    public FlowObject getParent()
    {
        return null;
    }

    public FlowSchema getFlowSchema(User user)
    {
        FlowSchema ret = new FlowSchema(user, getContainerObject());
        ret.setExperiment(this);
        return ret;
    }

    public ViewURLHelper urlShow()
    {
        ViewURLHelper ret = getFlowSchema(null).urlFor(QueryAction.executeQuery, FlowTableType.Runs);
        addParams(ret);
        return ret;
    }

    public String[] getAnalyzedRunPaths(User user, FlowProtocolStep step) throws Exception
    {
        FlowRun[] runs = getRuns(step);
        String[] ret = new String[runs.length];
        for (int i = 0; i < runs.length; i ++)
        {
            ret[i] = runs[i].getExpObject().getFilePathRoot();
        }
        return ret;
    }

    public FlowRun[] findRun(File filePath, FlowProtocolStep step) throws SQLException
    {
        List<FlowRun> ret = new ArrayList();
        FlowRun[] runs = getRuns(step);
        for (FlowRun run : runs)
        {
            if (filePath.toString().equals(run.getExperimentRun().getFilePathRoot()))
            {
                ret.add(run);
            }
        }
        return ret.toArray(new FlowRun[0]);
    }

    public int[] getRunIds(FlowProtocolStep step)
    {
        FlowRun[] runs = getRuns(step);
        int[] ret = new int[runs.length];
        for (int i = 0; i < runs.length; i ++)
        {
            ret[i] = runs[i].getRunId();
        }
        return ret;
    }

    public int getRunCount(FlowProtocolStep step)
    {
        return getRuns(step).length;
    }

    public FlowRun[] getRuns(FlowProtocolStep step)
    {
        ExpProtocol protocol = null;
        if (step != null)
        {
            protocol = ExperimentService.get().getProtocol(step.getLSID(getContainer()));
            if (protocol == null)
                return new FlowRun[0];
        }
        return FlowRun.fromRuns(getExperiment().getRuns(null, protocol));
    }

    public void setPrimaryAnalysis(User user) throws SQLException
    {
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(user.getUserId(), getContainer().getId(), PROP_CATEGORY, true);
        map.put(PROP_PRIMARY_ANALYSIS, getLSID());
    }
}
