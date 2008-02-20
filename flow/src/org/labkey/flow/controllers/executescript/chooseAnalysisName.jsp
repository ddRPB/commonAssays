<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController.Action"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ page import="org.labkey.flow.controllers.executescript.ChooseRunsToAnalyzeForm" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% ChooseRunsToAnalyzeForm form = (ChooseRunsToAnalyzeForm) __form; %>
<form method="POST" action="analyzeSelectedRuns.post">
    <%=errors()%>
    <p>What do you want to call the new analysis folder?<br>
        <% String name = form.ff_analysisName;
            if (StringUtils.isEmpty(name))
            {
                Set<String> namesInUse = new HashSet<String>();
                for (FlowExperiment experiment : FlowExperiment.getExperiments(getContainer()))
                {
                    namesInUse.add(experiment.getName().toLowerCase());
                }
                String baseName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
                name = baseName;
                int i = 0;
                while (namesInUse.contains(name.toLowerCase()))
                {
                    i ++;
                    name = baseName + i;
                }
            }
        %>

        <input type="text" name="ff_analysisName" value="<%=h(name)%>">
    </p>

    <labkey:button text="Analyze runs" action="<%=Action.analyzeSelectedRuns%>"/>
    <labkey:button text="Go back" action="<%=Action.chooseRunsToAnalyze%>"/>
    <input type="hidden" name="dataRegionSelectionKey" value="<%=form.getDataRegionSelectionKey()%>">
    <input type="hidden" name="scriptId" value="<%=form.getProtocol().getScriptId()%>">
    <input type="hidden" name="actionSequence" value="<%=form.getProtocolStep().getDefaultActionSequence()%>">
    <input type="hidden" name="ff_compensationMatrixOption" value="<%=h(form.ff_compensationMatrixOption)%>">
</form>
