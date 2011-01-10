<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.flow.controllers.editscript.EditSettingsForm" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>
<% EditSettingsForm form = (EditSettingsForm) this.form;
    boolean canEdit = form.canEdit();
    String contextPath = request.getContextPath();
    Map<FieldKey, String> fieldOptions = form.getFieldOptions();
    Map<String, String> opOptions = form.getOpOptions();
    int clauseCount = Math.max(form.ff_filter_field.length, 3);

    FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
%>
<labkey:errors/>
<form action="<%=form.urlFor(ScriptController.EditSettingsAction.class)%>" method="POST">
    <table>
        <tr class="labkey-wp-header"><td>Filter FCS files by keyword:</td></tr>
        <tr><td>
            Filters may be applied to this analysis script.  The set of keyword and
            value pairs <i>must</i> all match in the FCS header to be included in the analysis.
        <% if (protocol != null) { %>
            Alternatively, you may
            <a href="<%= protocol.urlFor(ProtocolController.EditFCSAnalysisFilterAction.class) %>">create protocol filters</a>
            that will be applied to all analysis scripts in the project folder.
        <% } %>
        </td></tr>
    </table>
    <table>
        <tr><th/><th>Keyword</th><th/><th>Value</th></tr>
        <%
        for (int i = 0; i < clauseCount; i++)
        {
            FieldKey field = null;
            String op = null;
            String value = null;

            if (i < form.ff_filter_field.length)
            {
                field = form.ff_filter_field[i];
                op = form.ff_filter_op[i];
                value = form.ff_filter_value[i];
            }

            if (!canEdit && field == null)
                continue;

            %>
            <tr>
                <td><%= i == 0 ? "" : "and" %></td>
            <% if (canEdit) { %>
                <td><select name="ff_filter_field"><labkey:options value="<%=field%>" map="<%=fieldOptions%>" /></select></td>
                <td><select name="ff_filter_op"><labkey:options value="<%=op%>" map="<%=opOptions%>" /></select></td>
                <td><input type="text" name="ff_filter_value" value="<%=h(value)%>"></td>
            <% } else { %>
                <td><%=fieldOptions.get(field)%></td>
                <td><%=opOptions.get(op)%></td>
                <td><%=h(value)%></td>
            <% } %>
            </tr>
            <%
        }
        %>
    </table>

    <% if (canEdit) { %>
        <labkey:button text="Update" />
        <labkey:button text="Cancel" href="<%=form.urlFor(ScriptController.BeginAction.class)%>" />
    <% } else { %>
        <labkey:button text="Go Back" href="<%=form.urlFor(ScriptController.BeginAction.class)%>" />
    <% } %>

    <p/>
    
    <table>
        <tr class="labkey-wp-header"><td>Edit Minimum Values:</td></tr>
        <tr><td>
        For each parameter, specify the minimum value.  This value will be used when drawing graphs.
        Also, for the purpose of calculating statistics, and applying gates, values will be constrained to be greater than
        or equal to this minimum value.
        </td></tr>
    </table>
    <table>
        <tr><th>Parameter</th><th>Minimum Value</th></tr>
        <% for (int i = 0; i < form.ff_parameter.length; i ++) {
        String parameter = form.ff_parameter[i];
        String minValue = form.ff_minValue[i];
        %>
        <tr><td><%=h(parameter)%><input type="hidden" name="ff_parameter" value="<%=h(parameter)%>"></td>
            <td><% if (canEdit) { %>
                <input type="text" name="ff_minValue" value="<%=h(minValue)%>">
                <% } else { %>
                <%=h(minValue)%>
                <% } %>
                </td>
        </tr>
        <% } %>
    </table>


<% if (canEdit) { %>
    <labkey:button text="Update" />
    <labkey:button text="Cancel" href="<%=form.urlFor(ScriptController.BeginAction.class)%>" />
<% } else { %>
    <labkey:button text="Go Back" href="<%=form.urlFor(ScriptController.BeginAction.class)%>" />
<% } %>
</form>

<script type="text/javascript">
    var contextPath = <%=q(contextPath)%>;
</script>
