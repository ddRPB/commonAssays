<%
/*
 * Copyright (c) 2015 LabKey Corporation
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
<%@ page import="org.labkey.authentication.duo.DuoController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.authentication.duo.DuoController.DuoForm" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    String message = "REPLACE ME!!!!";
    String sig_request = ((DuoForm) HttpView.currentView().getModelBean()).getSig_request();
%>
<p><%=h(message)%></p>
<p>Is that really you?</p>
<p><%=h(sig_request)%></p>
<labkey:form method="POST">
    <table>
        <tr>
            <td valign="top"><input type="radio" name="valid" value="1"></td>
            <td>Yes!</td>
        </tr>
        <tr>
            <td valign="top"><input type="radio" name="valid" value="0" checked></td>
            <td>No</td>
        </tr>
    </table>
    <input type="submit" value="Validate">
</labkey:form>