<%
/*
 * Copyright (c) 2013 LabKey Corporation
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

/**
* User: jeckels
* Date: August 27, 2013
*/

%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LeveyJenningsForm" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.luminex.LeveyJenningsMenuView" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    LeveyJenningsMenuView me = (LeveyJenningsMenuView) HttpView.currentView();
    LeveyJenningsMenuView.Bean bean = me.getModelBean();
%>

<table>
    <tr>
        <td valign="top">
            <strong>Titrations</strong><br/>
            <% if (bean.getTitrations().isEmpty()) { %>
                <em>No titrations</em> <%
            }
            for (String titrationName : bean.getTitrations())
            {
                ActionURL url = new ActionURL(LuminexController.LeveyJenningsReportAction.class, getViewContext().getContainer());
                url.addParameter("rowId", bean.getProtocol().getRowId());
                url.addParameter("controlName", titrationName);
                url.addParameter("controlType", LeveyJenningsForm.ControlType.Titration.toString());
                %><%= textLink(titrationName, url) %><br/><%
            }%>
        </td>
        <td valign="top">
            <strong>Single Point Controls</strong><br/>
            <% if (bean.getSinglePointControls().isEmpty()) { %>
                <em>No single point controls</em> <%
            }
            for (String singlePointControl : bean.getSinglePointControls())
            {
                ActionURL url = new ActionURL(LuminexController.LeveyJenningsReportAction.class, getViewContext().getContainer());
                url.addParameter("rowId", bean.getProtocol().getRowId());
                url.addParameter("controlName", singlePointControl);
                url.addParameter("controlType", LeveyJenningsForm.ControlType.SinglePoint.toString());
                %><%= textLink(singlePointControl, url) %><br/><%
            }%>
        </td>
    </tr>
</table>