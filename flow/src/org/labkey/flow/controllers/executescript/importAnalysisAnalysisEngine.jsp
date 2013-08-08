<%
    /*
    * Copyright (c) 2011-2013 LabKey Corporation
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
%>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.WorkspaceData" %>
<%@ page import="org.labkey.flow.analysis.model.PCWorkspace" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisEngine" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    WorkspaceData workspaceData = form.getWorkspace();
    String workspaceName = workspaceData.getPath() != null ? workspaceData.getPath() : workspaceData.getName();
    assert !(workspaceData.getWorkspaceObject() instanceof PCWorkspace) : "R Engine can only be used on Mac FlowJo workspaces";

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isSiteAdmin() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
<input type="hidden" name="resolving" value="<%=form.isResolving()%>">

<p>Select analysis engine for workspace <em>'<%=h(workspaceName)%>'</em>.
</p>
<hr/>
<input type="radio" name="selectAnalysisEngine" id="<%=AnalysisEngine.FlowJoWorkspace%>" value="<%=AnalysisEngine.FlowJoWorkspace%>" <%=text(AnalysisEngine.FlowJoWorkspace == form.getSelectAnalysisEngine() ? "checked" : "")%> />
<label for="<%=AnalysisEngine.FlowJoWorkspace%>">FlowJo statistics with <%=h(FlowModule.getLongProductName())%> graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    Statistics from the FlowJo workspace are imported and
    graphs will be generated by <%=h(FlowModule.getLongProductName())%>.
</div>

<%--
<input type="radio" name="selectAnalysisEngine" id="labkeyEngine" value="labkeyEngine" <%="labkeyEngine".equals(form.getSelectAnalysisEngine()) ? "checked" : ""%> />
<label for="labkeyEngine"><%=FlowModule.getLongProductName()%> statistics and graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    The analysis defined in the FlowJo workspace will be used
    by the <%=FlowModule.getLongProductName()%> engine to generate stats and graphs.
</div>
--%>

<input type="radio" name="selectAnalysisEngine" id="<%=AnalysisEngine.R%>" value="<%=AnalysisEngine.R%>" <%=text(AnalysisEngine.R == form.getSelectAnalysisEngine() ? "checked" : "")%> />
<label for="<%=AnalysisEngine.R%>">R statistics and graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    The analysis defined in the FlowJo workspace will be used
    by the R engine to generate statistics and graphs.<br>
    R must be configured and the flowWorkspace library must be installed to use this engine.
</div>


