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

/**
* User: cnathe
* Date: Sept 19, 2011
*/

%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.TitrationForm" %>

<%
    JspView<TitrationForm> me = (JspView<TitrationForm>) HttpView.currentView();
    TitrationForm bean = me.getModelBean();
%>

<table cellpadding="0" cellspacing="15">
    <tr>
        <td rowspan="2"><div id="graphParamsPanel"></div></td>
        <td><div id="guideSetOverviewPanel"></div></td>
    </tr>
    <tr>
        <td><div id="rPlotPanel"></div></td>
    </tr>
</table>
<div id="trackingDataPanelTitle" style="margin-left:15px"></div>
<div id="trackingDataPanel" style="margin-left:15px"></div>

<script type="text/javascript">
    LABKEY.requiresScript("LeveyJenningsGraphParamsPanel.js");
    LABKEY.requiresScript("LeveyJenningsGuideSetPanel.js");
    LABKEY.requiresScript("LeveyJenningsTrendPlotPanel.js");
    LABKEY.requiresScript("LeveyJenningsTrackingDataPanel.js");
    LABKEY.requiresScript("ManageGuideSetPanel.js");
    LABKEY.requiresScript("ApplyGuideSetPanel.js");
    LABKEY.requiresCss("LeveyJenningsReport.css");

    var $h = Ext.util.Format.htmlEncode;

    // local variables for storing the selected graph parameters
    var _analyte, _isotype, _conjugate;

    Ext.onReady(init);
    function init()
    {
        if ("null" == "<%= bean.getTitration() %>")
        {
            Ext.get('graphParamsPanel').update("Error: no titration specified.");
            return;
        }
        if ("null" == "<%= bean.getProtocol() %>")
        {
            Ext.get('graphParamsPanel').update("Error: no protocol specified.");
            return;
        }

        // set the nav trail page title to include the tiration name
        LABKEY.NavTrail.setTrail('<%= bean.getTitration() %> Levey-Jennings Plots');

        // initialize the graph parameters selection panel
        var graphParamsPanel = new LABKEY.LeveyJenningsGraphParamsPanel({
            renderTo: 'graphParamsPanel',
            cls: 'extContainer',
            titration: '<%= bean.getTitration() %>',
            assayName: '<%= bean.getProtocol() %>',
            listeners: {
                'resetGraphBtnClicked': function(analyte, isotype, conjugate){
                    _analyte = analyte;
                    _isotype = isotype;
                    _conjugate = conjugate;

                    guideSetPanel.graphParamsSelected(analyte, isotype, conjugate);
                    trendPlotPanel.graphParamsSelected(analyte, isotype, conjugate);
                    trackingDataPanel.graphParamsSelected(analyte, isotype, conjugate);
                }
            }
        });

        // initialize the panel for user to interact with the current guide set (edit and create new)
        var guideSetPanel = new LABKEY.LeveyJenningsGuideSetPanel({
            renderTo: 'guideSetOverviewPanel',
            cls: 'extContainer',
            titration: '<%= bean.getTitration() %>',
            assayName: '<%= bean.getProtocol() %>',
            listeners: {
                'currentGuideSetUpdated': function() {
                    trendPlotPanel.displayTrendPlot();
                    trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate);
                }
            }
        });

        // initialize the panel that displays the R plot for the trend plotting of EC50, AUC, and High MFI
        var trendPlotPanel = new LABKEY.LeveyJenningsTrendPlotPanel({
            renderTo: 'rPlotPanel',
            cls: 'extContainer',
            titration: '<%= bean.getTitration() %>',
            assayName: '<%= bean.getProtocol() %>'
        });

        // initialize the grid panel to display the tracking data
        var trackingDataPanel = new LABKEY.LeveyJenningsTrackingDataPanel({
            renderTo: 'trackingDataPanel',
            cls: 'extContainer',
            titration: '<%= bean.getTitration() %>',
            assayName: '<%= bean.getProtocol() %>',
            listeners: {
                'appliedGuideSetUpdated': function() {
                    trendPlotPanel.displayTrendPlot();
                    trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate);
                }
            }
        });
    }
</script>
