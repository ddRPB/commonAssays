<%
    /*
    * Copyright (c) 2011 LabKey Corporation
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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    ViewContext context = getViewContext();
    Container container = context.getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);

    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
    boolean hasPipelineRoot = pipeRoot != null;
    boolean canSetPipelineRoot = context.getUser().isAdministrator() && (pipeRoot == null || container.equals(pipeRoot.getContainer()));
%>

<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<input type="hidden" name="runFilePathRoot" id="runFilePathRoot" value="<%=h(form.getRunFilePathRoot())%>">

<p>Select analysis engine for workspace <em>'<%=h("need workspace name")%>'</em>.
</p>
<hr/>
<input type="radio" name="selectAnalysisEngine" id="noEngine" value="noEngine" <%="noEngine".equals(form.getSelectAnalysisEngine()) ? "checked" : ""%> />
<label for="noEngine">FlowJo statistics with <%=FlowModule.getLongProductName()%> graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    Statistics from the FlowJo workspace are imported and
    graphs will be generated by <%=FlowModule.getLongProductName()%>.
</div>

<%--
<input type="radio" name="selectAnalysisEngine" id="labkeyEngine" value="labkeyEngine" <%="labkeyEngine".equals(form.getSelectAnalysisEngine()) ? "checked" : ""%> />
<label for="labkeyEngine"><%=FlowModule.getLongProductName()%> statistics and graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    The analysis defined in the FlowJo workspace will be used
    by the <%=FlowModule.getLongProductName()%> engine to generate stats and graphs.
</div>
--%>

<input type="radio" name="selectAnalysisEngine" id="rEngine" value="rEngine" <%="rEngine".equals(form.getSelectAnalysisEngine()) ? "checked" : ""%> />
<label for="rEngine">R statistics and graphs.</label>
<div style="padding-left: 2em; padding-bottom: 1em;">
    The analysis defined in the FlowJo workspace will be used
    by the R engine to generate statistics and graphs.<br>
    R must be configured and the flowWorkspace library must be installed to use this engine.
</div>


