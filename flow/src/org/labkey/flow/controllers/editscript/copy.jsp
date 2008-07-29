<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.flow.data.FlowProtocolStep"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<% ScriptController.CopyProtocolForm form = (ScriptController.CopyProtocolForm) this.form; %>
<form action="<%=urlFor(ScriptController.Action.copy)%>" method="POST">
    <p>
        What do you want to call the new script?<br>
        <input type="text" name="name" value="<%=h(form.name)%>">
    </p>
    <p>
        Which sections of the '<%=form.analysisScript.getName()%>' script do you want to copy?<br>
<% if (form.analysisScript.hasStep(FlowProtocolStep.calculateCompensation)) { %>
        <input type="checkbox" name="copyCompensationCalculation" value="true"<%=form.copyCompensationCalculation ? " checked" : ""%>>Compensation Calculation<br>
<% } %>
<% if (form.analysisScript.hasStep(FlowProtocolStep.analysis)) { %>
        <input type="checkbox" name="copyAnalysis" value="true"<%=form.copyAnalysis ? " checked" : ""%>>Analysis<br>
<% } %>
    </p>
    <input type="submit" value="Make Copy">

</form>