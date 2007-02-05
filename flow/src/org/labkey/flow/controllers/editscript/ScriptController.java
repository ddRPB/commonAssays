package org.labkey.flow.controllers.editscript;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apache.xmlbeans.XmlObject;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.*;
import org.labkey.flow.ScriptParser;
import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.model.Polygon;
import org.labkey.flow.analysis.web.*;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.*;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.util.PFUtil;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class ScriptController extends BaseFlowController
{
    public static final int MAX_CHANNELS = 20;
    public static final int MAX_POINTS = 100;
    private static Logger _log = Logger.getLogger(ScriptController.class);

    public enum Action
    {
        begin,
        chooseSample,
        editScript,
        newProtocol,
        editRun,
        editProperties,
        editCompensationCalculation,
        uploadCompensationCalculation,
        editAnalysis,
        editGateTree,
        editGates,
        uploadAnalysis,
        graphWindow,
        graphImage,
        analysisJS,
        saveGate,
        copy,
        delete,
    }

    @Jpf.Action
    protected Forward begin(EditScriptForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("main.jsp", form);
        return renderInTemplate(page, "Choose editor", Action.begin);
    }

    @Jpf.Action
    protected Forward editScript(EditScriptForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        ScriptParser.Error error = null;
        FlowScript script = getScript();
        if (isPost())
        {
            if (safeSetAnalysisScript(script, getRequest().getParameter("script")))
            {
                error = validateScript(script);
                if (error == null)
                {
                    ViewURLHelper forward = form.urlFor(Action.editScript);
                    return new ViewForward(forward);
                }
            }
        }
        else if (getRequest().getParameter("checkSyntax") != null)
        {
            error = validateScript(script);
        }

        EditPage page = (EditPage) getPage("editScript.jsp", form);
        page.scriptParseError = error;
        return renderInTemplate(page, "Source Editor", Action.editScript);
    }

    protected ScriptParser.Error validateScript(FlowScript script) throws SQLException
    {
        ScriptParser parser = new ScriptParser();
        parser.parse(script.getAnalysisScript());
        if (parser.getErrors() != null)
            return parser.getErrors()[0];
        return null;
    }


    @Jpf.Action
    protected Forward newProtocol(NewProtocolForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (getRequest().getMethod().equalsIgnoreCase("post"))
        {
            Forward forward = createScript(form);
            if (forward != null)
                return forward;
        }

        HomeTemplate template = new HomeTemplate(getViewContext(),
                FormPage.getView(ScriptController.class, form, "newProtocol.jsp"),
                getNavTrailConfig(null, "New Analysis Script", Action.newProtocol));
        template.getModel().setFocus("forms[0].ff_name");

        return includeView(template);
    }

    protected boolean isScriptNameUnique(String name) throws Exception
    {
        return ExperimentService.get().getExpData(FlowScript.lsidForName(getContainer(), name)) == null;
    }

    public boolean isEmptyScript(FlowScript analysisScript)
    {
        try
        {
            ScriptDocument doc = analysisScript.getAnalysisScriptDocument();
            ScriptDef script = doc.getScript();
            return script.getRun() == null && script.getCompensationCalculation() == null && script.getAnalysis() == null;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    protected Forward createScript(NewProtocolForm form) throws Exception
    {
        if (form.ff_name == null || form.ff_name.length() == 0)
            addError("The name cannot be blank.");
        boolean errors = false;
        if (!isScriptNameUnique(form.ff_name))
        {
            errors = addError("The name '" + form.ff_name + "' is already in use.  Please choose a unique name.");
        }
        if (errors)
            return null;

        ScriptDocument doc = ScriptDocument.Factory.newInstance();
        doc.addNewScript();

        FlowScript script = FlowScript.create(getUser(), getContainer(), form.ff_name, doc.toString());


        ViewURLHelper forward;
        if (isEmptyScript(script))
        {
            forward = script.urlFor(Action.begin);
        }
        else
        {
            forward = script.urlFor(AnalysisScriptController.Action.begin);
        }

        putParam(forward, FlowParam.scriptId, script.getScriptId());
        return new ViewForward(forward);
    }

    protected String readTemplate(String name) throws Exception
    {
        URI base = new URI("Flow/templates/");
        URI uri = URIUtil.resolve(base, name);
        InputStream stream = getRequest().getSession().getServletContext().getResourceAsStream(uri.toString());

        return PageFlowUtil.getStreamContentsAsString(stream);
    }

    public Page getPage(String name, EditScriptForm form) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setForm(form);
        return ret;
    }

    static public ScriptDocument ensureScript(FlowScript analysisScript) throws ServletException
    {
        try
        {
            return analysisScript.getAnalysisScriptDocument();
        }
        catch (Exception e)
        {
            ViewURLHelper redirect = analysisScript.urlFor(Action.editScript);
            redirect.addParameter("checkSyntax", "1");
            HttpView.throwRedirect(redirect.toString());
            return null;
        }
    }

    protected void setAttribute(XmlObject obj, String attribute, String value)
    {
        Element el = (Element) obj.getDomNode();
        value = StringUtils.trimToNull(value);
        if (value == null)
        {
            el.removeAttribute(attribute);
        }
        else
        {
            el.setAttribute(attribute, value);
        }
    }

    @Jpf.Action
    protected Forward editRun(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        ScriptDocument scriptDoc = ensureScript(form.analysisScript);
        Page page = getPage("editRun.jsp", form);
        if (isPost())
        {
            ScriptDef script = scriptDoc.getScript();
            if (script == null)
            {
                script = scriptDoc.addNewScript();
            }
            RunDef run = script.getRun();
            if (run == null)
            {
                run = script.addNewRun();
            }

            setAttribute(run, "directoryPattern", form.runDirectoryPattern);
            setAttribute(run, "nameKeyword", form.runNameKeyword);
            WellDef well = run.getWell();
            if (well == null)
            {
                well = run.addNewWell();
            }
            setAttribute(well, "nameKeyword", form.wellNameKeyword);
            setAttribute(well, "filePattern", form.fcsFilePattern);
            while(well.getKeywordArray().length > 0)
            {
                well.removeKeyword(0);
            }
            Set<String> keywords = new LinkedHashSet();
            StringTokenizer st = new StringTokenizer(StringUtils.trimToEmpty(form.keywords), "\r\n");
            while (st.hasMoreTokens())
            {
                String token = StringUtils.trimToNull(st.nextToken());

                if (token != null)
                {
                    keywords.add(token);
                }
            }
            for (String keyword : keywords)
            {
                KeywordDef keywordElement = well.addNewKeyword();
                keywordElement.setName(keyword);
            }
            if (!safeSetAnalysisScript(form.analysisScript, scriptDoc.toString()))
            {
                return null;
            }
        }
        return renderInTemplate(page, "Experiment Run Upload Settings Editor", Action.editRun);
    }

    @Jpf.Action
    protected Forward editAnalysis(AnalysisForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("editAnalysis.jsp", form);

        if (isPost())
        {
            Forward forward = updateAnalysis(form);
            if (forward != null)
                return forward;
        }
        return renderInTemplate(page, "Choose statistics and graphs", Action.editAnalysis);
    }

    protected Forward updateAnalysis(AnalysisForm form) throws Exception
    {
        try
        {
            Set<StatisticSpec> stats = new LinkedHashSet();
            StringTokenizer stStats = new StringTokenizer(StringUtils.trimToEmpty(form.statistics), "\n");
            while (stStats.hasMoreElements())
            {
                String strStat = StringUtils.trimToNull(stStats.nextToken());
                if (strStat != null)
                {
                    stats.add(new StatisticSpec(strStat));
                }
            }
            Set<GraphSpec> graphs = new LinkedHashSet();
            StringTokenizer stGraphs = new StringTokenizer(StringUtils.trimToEmpty(form.graphs), "\n");
            while (stGraphs.hasMoreElements())
            {
                String strGraph = StringUtils.trimToNull(stGraphs.nextToken());
                if (strGraph != null)
                {
                    graphs.add(new GraphSpec(strGraph));
                }
            }
            ScriptDocument doc = form.analysisScript.getAnalysisScriptDocument();
            ScriptDef script = doc.getScript();
            AnalysisDef analysis = script.getAnalysis();
            if (analysis == null)
            {
                analysis = script.addNewAnalysis();
            }
            while (analysis.getStatisticArray().length > 0)
            {
                analysis.removeStatistic(0);
            }
            while (analysis.getGraphArray().length > 0)
            {
                analysis.removeGraph(0);
            }
            for (StatisticSpec stat : stats)
            {
                StatisticDef statDef = analysis.addNewStatistic();
                statDef.setName(stat.getStatistic().toString());
                if (stat.getSubset() != null)
                {
                    statDef.setSubset(stat.getSubset().toString());
                }
                if (stat.getParameter() != null)
                    statDef.setParameter(stat.getParameter());
            }
            for (GraphSpec graph : graphs)
            {
                addGraph(analysis, graph);
            }
            if (analysis.getCriteriaArray().length > 0)
            {
                analysis.removeCriteria(analysis.getCriteriaArray().length - 1);
            }
            if (!StringUtils.isEmpty(form.analysisKeywordName))
            {
                CriteriaDef criteria = analysis.addNewCriteria();
                criteria.setKeyword(form.analysisKeywordName);
                criteria.setPattern(form.analysisKeywordPattern);
            }
            if (!safeSetAnalysisScript(form.analysisScript, doc.toString()))
                return null;
            return new ViewForward(form.urlFor(Action.begin));
        }
        catch (FlowException e)
        {
            addError(e.getMessage());
            return null;
        }
    }

    protected Forward renderInTemplate(Page page, String title, Action action) throws Exception
    {
        NavTrailConfig ntc = getFlowNavConfig(getViewContext(), page.getScript(), title, action);
        List<NavTree> children = new ArrayList(Arrays.asList(ntc.getExtraChildren()));
        ViewURLHelper urlBegin = page.form.urlFor(Action.begin);
        if (action != Action.begin)
            children.add(new NavTree("Choose editor", urlBegin));
        ntc.setExtraChildren(children.toArray(new NavTree[0]));
        TemplatePage templatePage = (TemplatePage) getPage("template.jsp", page.form);
        templatePage.body = page;
        templatePage.curAction = action;
        return renderInTemplate(new JspView(templatePage), getContainer(), ntc);
    }

    public ViewURLHelper urlFor(Enum action)
    {
        // Methods in ScriptController should use form.urlFor().
        throw new UnsupportedOperationException();
    }

    public static class RunForm extends EditScriptForm
    {
        public String runDirectoryPattern;
        public String runNameKeyword;
        public String wellNameKeyword;
        public String fcsFilePattern;
        public String keywords;

        public void reset(ActionMapping mapping, HttpServletRequest request)
        {
            super.reset(mapping, request);
            if (analysisDocument == null)
                return;
            ScriptDef script = analysisDocument.getScript();
            if (script == null)
            {
                return;
            }
            RunDef run = script.getRun();
            if (run == null)
                return;
            runDirectoryPattern = run.getDirectoryPattern();
            runNameKeyword = run.getNameKeyword();
            WellDef well = run.getWell();
            if (well != null)
            {
                wellNameKeyword = well.getNameKeyword();
                fcsFilePattern = well.getFilePattern();
                List<String> keywords = new ArrayList();
                for (KeywordDef keyword : well.getKeywordArray())
                {
                    keywords.add(keyword.getName());
                }
                this.keywords = StringUtils.join(keywords.iterator(), "\n");
            }
        }
        public void setRunDirectoryPattern(String runDirectoryPattern)
        {
            this.runDirectoryPattern = runDirectoryPattern;
        }
        public void setRunNameKeyword(String runNameKeyword)
        {
            this.runNameKeyword = runNameKeyword;
        }
        public void setWellNameKeyword(String wellNameKeyword)
        {
            this.wellNameKeyword = wellNameKeyword;
        }
        public void setFcsFilePattern(String fcsFilePattern)
        {
            this.fcsFilePattern = fcsFilePattern;
        }
        public void setKeywords(String keywords)
        {
            this.keywords = keywords;
        }
    }

    public static class AnalysisForm extends EditScriptForm
    {
        public String analysisKeywordName;
        public String analysisKeywordPattern;
        public String statistics;
        public String graphs;

        public void reset(ActionMapping mapping, HttpServletRequest request)
        {
            super.reset(mapping, request);
            if (analysisDocument == null)
                return;
            ScriptDef script = analysisDocument.getScript();
            if (script == null)
                return;
            AnalysisDef analysis = script.getAnalysis();
            if (analysis == null)
                return;
            List<StatisticSpec> stats = new ArrayList();
            for (StatisticDef stat : analysis.getStatisticArray())
            {
                stats.add(FlowAnalyzer.makeStatisticSpec(stat));
            }
            statistics = StringUtils.join(stats.iterator(), "\n");
            List<GraphSpec> graphs = new ArrayList();
            for (GraphDef graph : analysis.getGraphArray())
            {
                graphs.add(FlowAnalyzer.makeGraphSpec(graph));
            }
            this.graphs = StringUtils.join(graphs.iterator(), "\n");
            CriteriaDef[] arrCriteria = analysis.getCriteriaArray();
            if (arrCriteria.length > 0)
            {
                CriteriaDef criteria = arrCriteria[arrCriteria.length - 1];
                analysisKeywordName = criteria.getKeyword();
                analysisKeywordPattern = criteria.getPattern();
            }
        }
        public void setAnalysisKeywordName(String analysisKeywordName)
        {
            this.analysisKeywordName = analysisKeywordName;
        }

        public void setAnalysisKeywordPattern(String analysisKeywordPattern)
        {
            this.analysisKeywordPattern = analysisKeywordPattern;
        }

        public void setGraphs(String graphs)
        {
            this.graphs = graphs;
        }

        public void setStatistics(String statistics)
        {
            this.statistics = statistics;
        }
    }

    abstract static public class EditPage extends Page
    {
        public ScriptParser.Error scriptParseError;
    }

    abstract static public class Page<F extends EditScriptForm> extends FlowPage<ScriptController>
    {
        public F form;

        public String pageHeader(Action action)
        {
            return "";
        }

        public void setForm(F form)
        {
            this.form = form;
        }

        public F getForm()
        {
            return form;
        }

        public FlowScript getScript()
        {
            return form.analysisScript;
        }

        public FlowProtocolStep getStep()
        {
            return form.step;
        }

        public PopulationSet getAnalysis() throws Exception
        {
            return this.form.getAnalysis();
        }

        public String formAction(Action action)
        {
            return urlFor(action).toString();
        }
        public ViewURLHelper urlFor(Action action)
        {
            return form.urlFor(action);
        }
    }

    static abstract public class UploadAnalysisPage extends Page<UploadAnalysisForm>
    {
        public String[] getGroupAnalysisNames()
        {
            if (form.workspaceObject == null)
                return new String[0];
            List<String> ret = new ArrayList();
            for (Analysis analysis : form.workspaceObject.getGroupAnalyses().values())
            {
                if (analysis.getPopulations().size() > 0)
                {
                    ret.add(analysis.getName());
                }
            }
            return ret.toArray(new String[0]);
        }

        public Map<String, String> getSampleAnalysisNames()
        {
            if (form.workspaceObject == null)
                return Collections.EMPTY_MAP;
            Map<String, String> ret = new LinkedHashMap();
            for (FlowJoWorkspace.SampleInfo sample : form.workspaceObject.getSamples())
            {
                Analysis analysis = form.workspaceObject.getSampleAnalysis(sample);
                if (analysis.getPopulations().size() > 0)
                {
                    ret.put(sample.getSampleId(), sample.getLabel());
                }
            }
            return ret;
        }
    }
    @Jpf.Action
    protected Forward uploadAnalysis(UploadAnalysisForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Forward fwd = doUploadAnalysis(form);
            if (fwd != null)
                return fwd;
        }
        UploadAnalysisPage page = (UploadAnalysisPage) getPage("uploadAnalysis.jsp", form);
        page.form = form;
        return renderInTemplate(page, "Upload Flow Jo Analysis", Action.uploadAnalysis);

    }

    protected boolean addError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionError("Error", error));
        return true;
    }

    protected FlowJoWorkspace handleWorkspaceUpload(FormFile file, Set<FlowJoWorkspace.StatisticSet> statisticSets)
    {
        if (file != null && file.getFileSize() > 0)
        {
            try
            {
                return FlowJoWorkspace.readWorkspace(file.getInputStream(), statisticSets);
            }
            catch (Exception e)
            {
                addError("Exception parsing workspace: " + e);
                return null;
            }
        }
        return null;
    }

    protected FlowJoWorkspace handleCompWorkspaceUpload(EditCompensationCalculationForm form)
    {
        if (form.selectedRunId != 0)
        {
            try
            {
                return new FlowRunWorkspace(form.analysisScript, form.step, FlowRun.fromRunId(form.selectedRunId));
            }
            catch (Exception e)
            {
                addError("Exception reading run:" + e);
            }
        }
        return handleWorkspaceUpload(form.workspaceFile, EnumSet.noneOf(FlowJoWorkspace.StatisticSet.class));
    }

    protected Forward doUploadAnalysis(UploadAnalysisForm form) throws Exception
    {
        FlowJoWorkspace newWorkspace = handleWorkspaceUpload(form.workspaceFile, form.ff_statisticSet);
        if (newWorkspace != null)
        {
            form.workspaceObject = newWorkspace;
        }
        if (form.workspaceObject == null)
        {
            addError("No workspace was uploaded.");
            return null;
        }
        FlowJoWorkspace workspace = form.workspaceObject;
        String groupName = form.groupName;
        String sampleId= form.sampleId;
        Analysis analysis = null;
        if (groupName != null)
        {
            analysis = workspace.getGroupAnalyses().get(groupName);
            if (analysis == null)
            {
                addError("No group analysis with the name '" + groupName + "' could be found.");
                return null;
            }
        }
        else if (sampleId != null)
        {
            FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleId);
            if (sample == null)
            {
                addError("Cannot find sample '" + sampleId);
                return null;
            }
            analysis = workspace.getSampleAnalysis(sample);
            if (analysis == null)
            {
                addError("Cannot find analysis for sample " + sampleId);
                return null;
            }
        }
        else
        {
            for (Analysis analysisTry : workspace.getGroupAnalyses().values())
            {
                if (analysisTry.getPopulations().size() > 0)
                {
                    if (analysis != null)
                    {
                        addError("There is more than one group analysis in this workspace.  Please specify which one to use.");
                        return null;
                    }
                    analysis = analysisTry;
                }
            }
            if (analysis == null)
            {
                for (FlowJoWorkspace.SampleInfo sample : workspace.getSamples())
                {
                    Analysis analysisTry = workspace.getSampleAnalysis(sample);
                    if (analysisTry == null)
                        continue;
                    if (analysisTry.getPopulations().size() > 0)
                    {
                        if (analysis != null)
                        {
                            addError("There is more than one sample analysis in this workspace.  Please specify which one to use.");
                            return null;
                        }
                        analysis = analysisTry;
                    }
                }
            }
        }
        if (analysis == null)
        {
            addError("No analyses were found in this workspace.");
            return null;
        }
        FlowScript analysisScript = getScript();
        ScriptDocument doc = analysisScript.getAnalysisScriptDocument();
        AnalysisDef analysisElement = doc.getScript().getAnalysis();
        if (analysisElement == null)
        {
            analysisElement = doc.getScript().addNewAnalysis();
        }
        if (analysisElement.getGraphArray().length == 0)
        {
            addGraphs(analysisElement, analysis);
        }
        if (!form.ff_statisticSet.isEmpty())
        {
            while (analysisElement.getStatisticArray().length > 0)
                analysisElement.removeStatistic(0);
            addStatistics(analysisElement, analysis);
        }

        FlowAnalyzer.makeAnalysis(doc.getScript(), null, makePopulationSet(analysis));
        if (!safeSetAnalysisScript(analysisScript, doc.toString()))
            return null;
        return new ViewForward(form.urlFor(Action.begin));
    }

    /**
     * Turn an Analysis into a PopulationSet so FlowAnalyzer.makeAnalysis doesn't think we've specified to delete
     * the graphs and statistics.
     */
    protected PopulationSet makePopulationSet(PopulationSet popset)
    {
        PopulationSet ret = new PopulationSet();
        ret.setName(popset.getName());
        for (Population pop : popset.getPopulations())
        {
            ret.addPopulation(pop);
        }
        return ret;
    }

    protected void addGraphs(LinkedHashSet<GraphSpec> graphs, SubsetSpec parent, Population population)
    {
        if (population.getGates().size() == 1)
        {
            Gate gate = population.getGates().get(0);
            if (gate instanceof PolygonGate)
            {
                PolygonGate poly = (PolygonGate) gate;
                GraphSpec graph = new GraphSpec(parent, poly.getX(), poly.getY());
                graphs.add(graph);
            }
            else if (gate instanceof IntervalGate)
            {
                IntervalGate interval = (IntervalGate) gate;
                GraphSpec graph = new GraphSpec(parent, interval.getAxis());
                graphs.add(graph);
            }
        }
        SubsetSpec subset = new SubsetSpec(parent, population.getName());
        for (Population child : population.getPopulations())
        {
            addGraphs(graphs, subset, child);
        }

    }

    protected void addGraphs(AnalysisDef analysisElement, Analysis analysis)
    {
        LinkedHashSet<GraphSpec> graphs = new LinkedHashSet();

        for (Population pop : analysis.getPopulations())
        {
            addGraphs(graphs, null, pop);
        }
        for (GraphSpec graph : graphs)
        {
            addGraph(analysisElement, graph);
        }
    }

    protected void addStatistics(AnalysisDef analysisElement, Analysis analysis)
    {
        for (StatisticSpec stat : analysis.getStatistics())
        {
            addStatistic(analysisElement, stat);
        }
    }

    protected StatisticDef addStatistic(AnalysisDef analysis, StatisticSpec stat)
    {
        StatisticDef statElement = analysis.addNewStatistic();
        statElement.setName(stat.getStatistic().toString());
        if (stat.getSubset() != null)
            statElement.setSubset(stat.getSubset().toString());
        if (stat.getParameter() != null)
            statElement.setParameter(stat.getParameter());
        return statElement;
    }

    public GraphDef addGraph(AnalysisDef analysis, GraphSpec graph)
    {
        GraphDef graphDef = analysis.addNewGraph();
        if (graph.getSubset() != null)
        {
            graphDef.setSubset(graph.getSubset().toString());
        }
        graphDef.setXAxis(graph.getParameters()[0]);
        graphDef.setYAxis(graph.getParameters()[1]);
        return graphDef;
    }

    @Jpf.Action
    protected Forward editCompensationCalculation(EditCompensationCalculationForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Forward forward = doEditCompensationCalculation(form);
            if (forward != null)
                return forward;
        }
        String pageName = form.workspace == null ? "showCompensationCalculation.jsp" : "editCompensationCalculation.jsp";
        CompensationCalculationPage page = (CompensationCalculationPage) getPage(pageName, form);
        return renderInTemplate(page, "Compensation Calculation Editor", Action.editCompensationCalculation);
    }
    protected Forward doEditCompensationCalculation(EditCompensationCalculationForm form) throws Exception
    {
        FlowJoWorkspace workspace = handleCompWorkspaceUpload(form);
        if (workspace != null)
        {
            form.setWorkspace(workspace);
            return null;
        }
        if (form.workspace == null)
            return null;
        workspace = form.workspace;
        Map<String, FlowJoWorkspace.CompensationChannelData> dataMap = new HashMap();
        for (int i = 0; i < form.parameters.length; i ++)
        {
            String parameter = form.parameters[i];
            FlowJoWorkspace.CompensationChannelData cd = new FlowJoWorkspace.CompensationChannelData();
            cd.positiveKeywordName = StringUtils.trimToNull(form.positiveKeywordName[i]);
            cd.negativeKeywordName = StringUtils.trimToNull(form.negativeKeywordName[i]);
            if (cd.positiveKeywordName == null)
            {
                continue;
            }
            cd.positiveKeywordValue = StringUtils.trimToNull(form.positiveKeywordValue[i]);
            cd.positiveSubset = StringUtils.trimToNull(form.positiveSubset[i]);
            cd.negativeKeywordValue = StringUtils.trimToNull(form.negativeKeywordValue[i]);
            cd.negativeSubset = StringUtils.trimToNull(form.negativeSubset[i]);
            dataMap.put(parameter, cd);
        }
        List<String> errors = new ArrayList();
        CompensationCalculation calc = workspace.makeCompensationCalculation(dataMap, errors);
        if (errors.size() > 0)
        {
            for (String error : errors)
            {
                addError(error);
            }
            return null;
        }
        ScriptDef script = form.analysisDocument.getScript();
        FlowAnalyzer.makeAnalysis(script, calc, null);
        if (!safeSetAnalysisScript(form.analysisScript, form.analysisDocument.toString()))
            return null;
        return new ViewForward(form.urlFor(Action.editCompensationCalculation));
    }

    protected boolean safeSetAnalysisScript(FlowScript script, String str)
    {
        int runCount = script.getRunCount();
        if (runCount != 0)
        {
            addError("This analysis script cannot be edited because it has been used in the analysis of " + runCount + " runs.");
            return false;
        }
        try
        {
            script.setAnalysisScript(getUser(), str);
        }
        catch (SQLException e)
        {
            addError("An exception occurred: " + e);
            _log.error("Error", e);
            return false;
        }
        return true;
    }

    public static class ChooseSampleForm extends EditScriptForm
    {
        public boolean compRequired;
        public String redirect;
        public String title;
        public int curWellId;
        public int curRunId;
        public int curCompId;
        public void setCurWellId(int wellId)
        {
            this.curWellId = wellId;
        }
        public void setCurCompId(int compensationId)
        {
            this.curCompId = compensationId;
        }
        public void setCompRequired(boolean b)
        {
            this.compRequired = b;
        }
        public void setCurRunId(int runId)
        {
            this.curRunId = runId;
        }
        public void reset(ActionMapping mapping, HttpServletRequest request)
        {
            super.reset(mapping, request);
            if (run != null)
            {
                curRunId = run.getRunId();
            }
            if (well != null)
            {
                curWellId = well.getWellId();
            }
        }
        public void setRedirect(String redirect)
        {
            this.redirect = redirect;
        }
        public void setTitle(String title)
        {
            this.title = title;
        }
    }

    @Jpf.Action
    protected Forward chooseSample(ChooseSampleForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("chooseSample.jsp", form);
        if (isPost())
        {
            FlowRun run = null;
            FlowWell well = null;
            FlowCompensationMatrix comp = null;

            if (form.curRunId != 0)
            {
                run = FlowRun.fromRunId(form.curRunId);
            }
            if (form.curWellId != 0)
            {
                well = FlowWell.fromWellId(form.curWellId);
            }
            if (form.curCompId != 0)
            {
                comp = FlowCompensationMatrix.fromCompId(form.curCompId);
            }

            if (well != null)
            {
                if (run == null)
                {
                    run = well.getRun();
                }
            }
            else
            {
                if (run != null)
                {
                    FlowWell[] wells = run.getWells();
                    if (wells.length == 0)
                    {
                        addError("The run '" + run.getName() + "' has no samples.");
                    }
                    else
                    {
                        well = wells[0];
                    }
                }
            }
            ViewURLHelper redirect = null;
            if (well != null && (!form.compRequired || comp != null))
            {
                if (form.redirect != null)
                {
                    redirect = new ViewURLHelper(form.redirect);
                }
            }

            if (redirect == null)
            {
                redirect = form.analysisScript.urlFor(Action.chooseSample);
            }
            redirect.deleteParameter("runId");
            redirect.deleteParameter("wellId");
            redirect.deleteParameter("compId");
            if (run != null)
            {
                run.addParams(redirect);
            }
            if (well != null)
            {
                well.addParams(redirect);
            }
            if (comp != null)
            {
                comp.addParams(redirect);
            }
            return new ViewForward(redirect);
        }

        return renderInTemplate(page, "Sample Chooser", Action.chooseSample);
    }

    static public class GraphForm extends EditScriptForm
    {
        public String x;
        public String y;
        public String subset;
        public String points;
        public int height = 300;
        public int width = 300;
        public boolean open;
        public void setX(String xAxis)
        {
            this.x = xAxis;
        }
        public void setY(String yAxis)
        {
            this.y = yAxis;
        }
        public void setSubset(String subset)
        {
            this.subset = subset;
        }
        public void setWidth(int width)
        {
            this.width = width;
        }
        public void setHeight(int height)
        {
            this.height = height;
        }
        public void setOpen(boolean open)
        {
            this.open = open;
        }
        // A comma separated list: x1,y1,x2,y2,...
        public void setPoints(String points)
        {
            this.points = points;
        }
    }

    @Jpf.Action
    protected Forward graphWindow(GraphForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        GraphWindowPage page = (GraphWindowPage) getPage("graphWindow.jsp", form);
        page.plotInfo = getPlotInfo(form);
        getView().include(new JspView(page));
        return null;
    }

    protected Forward streamImage(BufferedImage image) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        PageFlowUtil.streamFileBytes(getResponse(), "graph.png", baos.toByteArray(), false);
        return null;
    }

    Point[] decodePoints(String str)
    {
        if (StringUtils.isEmpty(str))
            return new Point[0];
        String[] strPts = StringUtils.splitPreserveAllTokens(str, ',');
        int cPts = strPts.length / 2;
        Point[] ret = new Point[cPts];
        for (int i = 0; i < cPts; i ++)
        {
            ret[i] = new Point(Integer.valueOf(strPts[i * 2]), Integer.valueOf(strPts[i * 2 + 1]));
        }
        return ret;
    }

    @Jpf.Action
    protected Forward graphImage(GraphForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        PlotInfo info = getPlotInfo(form);
        BufferedImage image = info.getImage();
        image = addSelection(image, decodePoints(form.points), !form.open);
        return streamImage(image);
    }

    protected BufferedImage addSelection(BufferedImage imageIn, Point[] pts, boolean closePoly)
    {
        if (pts.length == 0)
            return imageIn;
        BufferedImage image = new BufferedImage(imageIn.getWidth(), imageIn.getHeight(), imageIn.getType());
        imageIn.copyData(image.getRaster());
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        for (int i = 0; i < pts.length - 1; i ++)
        {
            g.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y);
        }
        if (closePoly)
        {
            g.drawLine(pts[pts.length - 1].x, pts[pts.length- 1].y, pts[0].x, pts[0].y);
        }
        g.dispose();
        return image;
    }

    static class GraphCacheKey
    {
        GraphSpec graph;
        int wellId;
        int height;
        int width;

        public GraphCacheKey(GraphSpec graph, FlowWell well, PopulationSet analysis, int width, int height)
        {
            this.graph = graph;
            this.wellId = well.getWellId();
            this.width = width;
            this.height = height;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final GraphCacheKey that = (GraphCacheKey) o;

            if (height != that.height) return false;
            if (wellId != that.wellId) return false;
            if (width != that.width) return false;
            if (!graph.equals(that.graph)) return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = graph.hashCode();
            result = 29 * result + wellId;
            result = 29 * result + height;
            result = 29 * result + width;
            return result;
        }
    }
    transient GraphCacheKey _cacheKey;
    transient PlotInfo _plotCache;

    protected PlotInfo getPlotInfo(GraphForm form) throws Exception
    {
        GraphSpec graphSpec = new GraphSpec(SubsetSpec.fromString(form.subset).getParent(), form.x, form.y);

        PopulationSet group = form.getAnalysis();
        GraphCacheKey key = new GraphCacheKey(graphSpec, form.well, group, form.width, form.height);
        if (key.equals(_cacheKey))
        {
            return _plotCache;
        }
        CompensationMatrix comp = null;
        if (form.getCompensationMatrix() != null)
        {
            comp = form.getCompensationMatrix().getCompensationMatrix();
        }
        PlotInfo ret = FCSAnalyzer.get().generateDesignGraph(FlowAnalyzer.getFCSUri(form.well), comp, group, graphSpec, form.width, form.height);
        if (ret != null)
        {
            _cacheKey = key;
            _plotCache = ret;
        }
        return ret;
    }

    abstract static public class GraphWindowPage extends Page<EditScriptForm>
    {
        public PlotInfo plotInfo;
        public String jscriptRect(Rectangle rc)
        {
            return "{ x : " + rc.x + ",y:" + rc.y + ",width:" + rc.width + ",height:" + rc.height + "}";
        }
        public String jscriptRange(PlotInfo.Range range)
        {
            return "{ min: " + range.min + ",max:" + range.max + ",log:" + range.log + "}";
        }
    }

    @Jpf.Action
    protected Forward analysisJS(EditGatesForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("analysisJS.jsp", form);
        getResponse().setContentType("text/javascript");
        JspView view = new JspView(page);
        view.setFrame(WebPartView.FrameType.NONE);
        getView().include(view);
        return null;
    }

    protected Population findPopulation(PopulationSet root, SubsetSpec subset)
    {
        PopulationSet parent;
        if (subset.getParent() == null)
        {
            parent = root;
        }
        else
        {
            parent = findPopulation(root, subset.getParent());
        }
        if (parent == null)
            return null;
        return parent.getPopulation(subset.getSubset());
    }

    protected void requireWell(EditScriptForm form, String title, Integer actionSequence) throws Exception
    {
        if (form.well != null)
            return;

        FlowRun[] runs = FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords);
        if (runs.length != 0)
        {
            form.run = runs[0];
            form.well = form.run.getWells()[0];
            return;
        }

        ViewURLHelper redirect = form.urlFor(Action.chooseSample);
        redirect.addParameter("redirect", getViewURLHelper().toString());
        redirect.addParameter("title", title);
        if (actionSequence == null)
        {
            redirect.deleteParameter("actionSequence");
        }
        else
        {
            redirect.replaceParameter("actionSequence", Integer.toString(actionSequence));
        }
        HttpView.throwRedirect(redirect.toString());
    }

    @Jpf.Action
    protected Forward editGates(EditGatesForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        requireWell(form, "Edit Gates", null);
        EditGatesPage page = (EditGatesPage) getPage("editGates.jsp", form);
        return renderInTemplate(page, "Gate Editor", Action.editGates);
    }

    @Jpf.Action
    protected Forward saveGate(EditGatesForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        PopulationSet root = form.getAnalysis();
        SubsetSpec subset = SubsetSpec.fromString(form.subset);
        Population pop = findPopulation(root, subset);
        if (pop == null)
        {
            PopulationSet parent;
            if (subset.getParent() == null)
            {
                parent = root;
            }
            else
            {
                parent = findPopulation(root, subset.getParent());
            }
            pop = new Population();
            pop.setName(subset.getSubset());
            parent.addPopulation(pop);
        }
        pop.getGates().clear();
        List<Double> lstX = new ArrayList();
        List<Double> lstY = new ArrayList();
        for (int i = 0; ; i ++)
        {
            String strX = getRequest().getParameter("ptX[" + i + "]");
            String strY = getRequest().getParameter("ptY[" + i + "]");
            if (strX == null)
                break;
            lstX.add(Double.valueOf(strX));
            lstY.add(Double.valueOf(strY));
        }
        PolygonGate poly = new PolygonGate(form.xAxis, form.yAxis, new Polygon(lstX, lstY));
        pop.addGate(poly);
        ScriptDocument doc = getScript().getAnalysisScriptDocument();
        if (root instanceof Analysis)
        {
            FlowAnalyzer.makeAnalysis(doc.getScript(), null, (Analysis) root);
        }
        else if (root instanceof CompensationCalculation)
        {
            FlowAnalyzer.makeAnalysis(doc.getScript(), (CompensationCalculation) root, null);
        }
        if (!safeSetAnalysisScript(form.analysisScript, doc.toString()))
        {
            return null;
        }
        ViewURLHelper forward = form.urlFor(Action.editGates);
        forward.addParameter("subset", subset.toString());
        return new ViewForward(forward);
    }

    String checkValidPopulationName(String name)
    {
        if (StringUtils.indexOfAny(name, "/<'") >= 0)
        {
            return "Population names cannot contain the characters / ' \" or <";
        }
        return null;
    }

    private boolean renamePopulations(Population pop, PopulationSet newParent, SubsetSpec subsetParent, Map<SubsetSpec, String> newNames)
    {
        SubsetSpec subset = new SubsetSpec(subsetParent, pop.getName());
        String newName = newNames.get(subset);
        boolean fSuccess = true;
        if (StringUtils.isEmpty(newName))
        {
            // deleted
            return fSuccess;
        }
        Population newPop = new Population();
        String nameError = checkValidPopulationName(newName);
        if (nameError != null)
        {
            addError(nameError);
            fSuccess = false;
        }
        newPop.setName(newName);
        newPop.getGates().addAll(pop.getGates());
        if (newParent.getPopulation(newName) != null)
        {
            addError("There are two populations called '" + new SubsetSpec(subsetParent, newName) + "'");
            fSuccess = false;
        }

        newParent.addPopulation(newPop);

        for (Population child : pop.getPopulations())
        {
            fSuccess = renamePopulations(child, newPop, subset, newNames) && fSuccess;
        }
        return fSuccess;
    }

    static public class EditGateTreeForm extends EditScriptForm
    {
        public SubsetSpec[] subsets;
        public String[] populationNames;

        public void reset(ActionMapping mapping, HttpServletRequest request)
        {
            super.reset(mapping, request);
            try
            {
                Map<SubsetSpec,Population> pops = getPopulations();
                Map.Entry<SubsetSpec,Population>[] entries = pops.entrySet().toArray(new Map.Entry[0]);
                populationNames = new String[entries.length];
                subsets = new SubsetSpec[entries.length];
                for (int i = 0; i < entries.length; i ++)
                {
                    populationNames[i] = entries[i].getValue().getName();
                    subsets[i] = entries[i].getKey();
                }
            }
            catch (Exception e)
            {
                UnexpectedException.rethrow(e);
            }
        }
        public String[] getPopulationNames()
        {
            return populationNames;
        }
        public void setPopulationNames(String[] names)
        {
            // This method just needs to be here so struts knows populationNames is not read only.
            throw new UnsupportedOperationException();
        }
    }
    @Jpf.Action
    protected Forward editGateTree(EditGateTreeForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("editGateTree.jsp", form);
        if (isPost())
        {
            Map<SubsetSpec, String> newNames = new HashMap();
            for (int i = 0; i < form.populationNames.length; i ++)
            {
                newNames.put(form.subsets[i], form.populationNames[i]);
            }
            PopulationSet oldAnalysis = form.getAnalysis();
            PopulationSet newAnalysis = form.getAnalysis();
            // form.getAnalysis should create a new copy each time it's called.
            assert oldAnalysis != newAnalysis;
            newAnalysis.getPopulations().clear();
            boolean fSuccess = true;
            for (Population pop : oldAnalysis.getPopulations())
            {
                fSuccess = renamePopulations(pop, newAnalysis, null, newNames);
            }
            if (fSuccess)
            {
                ScriptDocument doc = form.analysisScript.getAnalysisScriptDocument();
                fSuccess = saveAnalysisOrComp(form.analysisScript, doc, newAnalysis);
            }
            if (fSuccess)
            {
                return new ViewForward(form.urlFor(Action.editGateTree));
            }
        }
        return renderInTemplate(page, "Population Names Editor", Action.editGateTree);
    }

    protected boolean saveAnalysisOrComp(FlowScript analysisScript, ScriptDocument doc, PopulationSet popset) throws SQLException
    {
        if (popset instanceof CompensationCalculation)
        {
            FlowAnalyzer.makeAnalysis(doc.getScript(), (CompensationCalculation) popset, null);
        }
        else
        {
            FlowAnalyzer.makeAnalysis(doc.getScript(), null, popset);
        }
        return safeSetAnalysisScript(analysisScript, doc.toString());
    }

    static public class CopyProtocolForm extends EditScriptForm
    {
        public String name;
        public boolean copyRunDefinition;
        public boolean copyCompensationCalculation;
        public boolean copyAnalysis;

        public void setName(String name)
        {
            this.name = name;
        }
        public void setCopyRunDefinition(boolean b)
        {
            this.copyRunDefinition = b;
        }
        public void setCopyCompensationCalculation(boolean b)
        {
            this.copyCompensationCalculation = b;
        }
        public void setCopyAnalysis(boolean b)
        {
            this.copyAnalysis = b;
        }
    }

    @Jpf.Action
    protected Forward copy(CopyProtocolForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        Page page = getPage("copy.jsp", form);
        if (isPost())
        {
            Forward fwd = doCopy(form);
            if (fwd != null)
                return fwd;
        }
        return renderInTemplate(page, "Make a copy of '" + form.analysisScript.getName() + "'", Action.copy);
    }

    protected void addChild(XmlObject parent, XmlObject child)
    {
        if (child == null)
            return;
        parent.getDomNode().appendChild(parent.getDomNode().getOwnerDocument().importNode(child.getDomNode(), true));
    }

    protected Forward doCopy(CopyProtocolForm form) throws Exception
    {
        if (StringUtils.isEmpty(form.name))
        {
            addError("The name cannot be blank.");
            return null;
        }
        if (!isScriptNameUnique(form.name))
        {
            addError("There is already a protocol named '" + form.name);
            return null;
        }
        ScriptDef src = form.analysisScript.getAnalysisScriptDocument().getScript();
        ScriptDocument doc = ScriptDocument.Factory.newInstance();
        ScriptDef script = doc.addNewScript();
        if (form.copyRunDefinition)
        {
            addChild(script, src.getRun());
        }
        if (form.copyCompensationCalculation)
        {
            addChild(script, src.getCompensationCalculation());
        }
        if (form.copyAnalysis)
        {
            addChild(script, src.getAnalysis());
        }
        FlowScript newAnalysisScript = FlowScript.create(getUser(), getContainer(), form.name, doc.toString());
        ViewURLHelper forward = newAnalysisScript.urlFor(Action.begin);
        return new ViewForward(forward);
    }

    @Jpf.Action
    protected Forward editProperties(EditPropertiesForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            ExpData protocol = form.analysisScript.getExpObject();
            protocol.setComment(getUser(), form.ff_description);
            return new ViewForward(form.urlFor(Action.begin));
        }
        return renderInTemplate(getPage("editProperties.jsp", form), "Edit Properties", Action.editProperties);
    }

    @Jpf.Action
    protected Forward delete(EditScriptForm form) throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);
        if (isPost())
        {
            ExpData protocol = form.analysisScript.getExpObject();
            protocol.delete(getUser());
            return new ViewForward(PFUtil.urlFor(FlowController.Action.begin, getContainer()));
        }
        return renderInTemplate(getPage("delete.jsp", form), "Confirm Delete", Action.delete);
    }
}
