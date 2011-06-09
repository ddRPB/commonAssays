<%
/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
<%@ page import="org.labkey.api.collections.CaseInsensitiveTreeMap" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.Report" %>
<%@ page import="org.labkey.api.reports.ReportService" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.reports.report.ReportIdentifier" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = getViewContext();
    User user = context.getUser();
    Container c = context.getContainer();

    boolean canEdit = c.hasPermission(user, UpdatePermission.class);

    ReportIdentifier id = ((ReportsController.IdForm) HttpView.currentModel()).getReportId();

    ReportService.I svc = ReportService.get();
    Report[] all = svc.getReports(user, c);
    Map<String, Report> reports = new CaseInsensitiveTreeMap<Report>();
    for (Report r : all)
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        reports.put(r.getDescriptor().getReportName(), r);
    }
    %><select onchange="Select_onChange(this.value)"><%
    for (Report r : reports.values())
    {
        if (!r.getType().startsWith("Flow."))
            continue;
        ReportDescriptor d = r.getDescriptor();
        boolean selected = id != null && id.equals(d.getReportId());
        %><option <%=selected?"selected":""%> value="<%=h(r.getRunReportURL(context))%>"><%=h(d.getReportName())%></option><%
    }
    %></select>

    <% if (canEdit && id != null) { %>
    <%=generateButton("Edit", id.getReport().getEditReportURL(context))%>
    <% } %>
    
<script type="text/javascript">
    function Select_onChange(url)
    {
        Ext.getBody().mask();
        window.location=url;
    }
</script>
