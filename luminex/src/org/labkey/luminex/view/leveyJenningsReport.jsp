<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
<%@ page import="org.labkey.luminex.LeveyJenningsForm" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("clientapi/ext3"));
        resources.add(ClientDependency.fromPath("luminex/LeveyJenningsGraphParamsPanel.js"));
        resources.add(ClientDependency.fromPath("luminex/LeveyJenningsGuideSetPanel.js"));
        resources.add(ClientDependency.fromPath("luminex/LeveyJenningsTrendPlotPanel.js"));
        resources.add(ClientDependency.fromPath("luminex/LeveyJenningsTrackingDataPanel.js"));
        resources.add(ClientDependency.fromPath("luminex/ManageGuideSetPanel.js"));
        resources.add(ClientDependency.fromPath("luminex/GuideSetWindow.js"));
        resources.add(ClientDependency.fromPath("luminex/ApplyGuideSetPanel.js"));
        resources.add(ClientDependency.fromPath("Experiment/QCFlagToggleWindow.js"));
        resources.add(ClientDependency.fromPath("luminex/LeveyJenningsReport.css"));
        return resources;
    }
%>
<%
    JspView<LeveyJenningsForm> me = (JspView<LeveyJenningsForm>) HttpView.currentView();
    LeveyJenningsForm bean = me.getModelBean();
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
<div id="trackingDataPanel" style="margin-left:15px"></div>

<script type="text/javascript">

        var $h = Ext.util.Format.htmlEncode;

        // the default number of records to return for the report when no start and end date are provided
        var defaultRowSize = 30;

        // local variables for storing the selected graph parameters
        var _protocolName, _controlName, _controlType, _analyte, _isotype, _conjugate, _protocolExists = false, _networkExists = false;

        function init()
        {
            _controlName = <%=PageFlowUtil.jsString(bean.getControlName())%>;
            _controlType = <%=PageFlowUtil.jsString(bean.getControlType().toString())%>;
            _protocolName = <%=PageFlowUtil.jsString(bean.getProtocol().getName())%>;

            if ("" == _controlType || "" == _controlName)
            {
                Ext.get('graphParamsPanel').update("Error: no control name specified.");
                return;
            }
            if ('SinglePoint' != _controlType && 'Titration' != _controlType)
            {
                Ext.get('graphParamsPanel').update("Error: unsupported control type: '" + _controlType + "'");
                return;
            }
            if ("" == _protocolName)
            {
                Ext.get('graphParamsPanel').update("Error: no protocol specified.");
                return;
            }

            var getByNameQueryComplete = false, executeSqlQueryComplete = false;
            var loader = function() {
                if (getByNameQueryComplete && executeSqlQueryComplete) {
                    initializeReportPanels();
                }
            };

            // Query the assay design to check for the required columns for the L-J report and the existance of Network and Protocol columns
            LABKEY.Assay.getByName({
                name: _protocolName,
                success: function(data) {

                    var missingColumns = ['isotype', 'conjugate', 'acquisitiondate'];
                    var runFields = data[0].domains[_protocolName + ' Run Fields'];
                    runFields = runFields.concat(data[0].domains[_protocolName + ' Excel File Run Properties']);
                    for (var i=0; i<runFields.length; i++)
                    {
                        var index = missingColumns.indexOf(runFields[i].name.toLowerCase());
                        if (index != -1) {
                            missingColumns.splice(index, 1);
                        }
                    }
                    if (missingColumns.length > 0)
                    {
                        Ext.get('graphParamsPanel').update("Error: one or more of the required properties (" + missingColumns.join(',') + ") for the report do not exist in '" + $h(_protocolName) + "'.");
                        return;
                    }

                    var batchFields = data[0].domains[_protocolName + ' Batch Fields'];
                    for (var i=0; i<batchFields.length; i++) {
                        if (batchFields[i].fieldKey.toLowerCase() == "network") {
                            _networkExists = true;
                        }
                        if (batchFields[i].fieldKey.toLowerCase() == "customprotocol") {
                            _protocolExists = true;
                        }
                    }

                    getByNameQueryComplete = true;
                    loader();
                }
            });

            // verify that the given titration/singlepointcontrol exists and has run's associated with it as a Standard or QC Control
            var sql;
            if ('Titration' == _controlType) {
                sql = "SELECT COUNT(*) AS RunCount FROM Titration WHERE Name='" + _controlName + "' AND IncludeInQcReport=true";
            }
            else {
                sql = "SELECT COUNT(*) AS RunCount FROM SinglePointControl WHERE Name='" + _controlName + "'";
            }
            LABKEY.Query.executeSql({
                containerFilter: LABKEY.Query.containerFilter.allFolders,
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(_protocolName),
                sql: sql,
                success: function(data) {
                    if (data.rows.length == 0 || data.rows[0]['RunCount'] == 0)
                    {
                        Ext.get('graphParamsPanel').update("Error: there were no records found in '" + $h(_protocolName) + "' for '" + $h(_controlName) + "'.");
                    }
                    else
                    {
                        executeSqlQueryComplete = true;
                        loader();
                    }
                },
                failure: function(response) {
                    Ext.get('graphParamsPanel').update("Error: " + response.exception);
                }
            });
        }

        function initializeReportPanels()
        {
            // initialize the graph parameters selection panel
            var graphParamsPanel = new LABKEY.LeveyJenningsGraphParamsPanel({
                renderTo: 'graphParamsPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                listeners: {
                    'applyGraphBtnClicked': function(analyte, isotype, conjugate){
                        _analyte = analyte;
                        _isotype = isotype;
                        _conjugate = conjugate;

                        guideSetPanel.graphParamsSelected(analyte, isotype, conjugate);
                        trendPlotPanel.graphParamsSelected(analyte, isotype, conjugate);
                        trackingDataPanel.graphParamsSelected(analyte, isotype, conjugate, null, null);
                    },
                    'graphParamsChanged': function(){
                        guideSetPanel.disable();
                        trendPlotPanel.disable();
                        trackingDataPanel.disable();
                    }
                }
            });

            var resizer = new Ext.Resizable('graphParamsPanel', {
                handles: 'e',
                minWidth: 225
            });
            resizer.on('resize', function(rez, width, height){
                graphParamsPanel.setWidth(width);
                graphParamsPanel.doLayout();
            });

            // initialize the panel for user to interact with the current guide set (edit and create new)
            var guideSetPanel = new LABKEY.LeveyJenningsGuideSetPanel({
                renderTo: 'guideSetOverviewPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                listeners: {
                    'currentGuideSetUpdated': function() {
                        trendPlotPanel.setTabsToRender();
                        trendPlotPanel.displayTrendPlot();
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate());
                    },
                    'exportPdfBtnClicked': function() {
                        if (trendPlotPanel.getPdfHref())
                        {
                            window.location = trendPlotPanel.getPdfHref();
                        }
                    },
                    'guideSetMetricsUpdated': function() {
                        trackingDataPanel.fireEvent('appliedGuideSetUpdated');
                    }
                }
            });

            // initialize the panel that displays the R plot for the trend plotting of EC50, AUC, and High MFI
            var trendPlotPanel = new LABKEY.LeveyJenningsTrendPlotPanel({
                renderTo: 'rPlotPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                defaultRowSize: defaultRowSize,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                listeners: {
                    'reportFilterApplied': function(startDate, endDate, network, networkAny, protocol, protocolAny) {
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, startDate, endDate, network, networkAny, protocol, protocolAny);
                    },
                    'togglePdfBtn': function(toEnable) {
                        guideSetPanel.toggleExportBtn(toEnable);
                    }
                }
            });

            // initialize the grid panel to display the tracking data
            var trackingDataPanel = new LABKEY.LeveyJenningsTrackingDataPanel({
                renderTo: 'trackingDataPanel',
                cls: 'extContainer',
                controlName: _controlName,
                controlType: _controlType,
                assayName: _protocolName,
                defaultRowSize: defaultRowSize,
                networkExists: _networkExists,
                protocolExists: _protocolExists,
                listeners: {
                    'appliedGuideSetUpdated': function() {
                        trendPlotPanel.setTabsToRender();
                        trendPlotPanel.displayTrendPlot();
                        trackingDataPanel.graphParamsSelected(_analyte, _isotype, _conjugate, trendPlotPanel.getStartDate(), trendPlotPanel.getEndDate(),
                                trendPlotPanel.network, trendPlotPanel.networkAny, trendPlotPanel.protocol, trendPlotPanel.protocolAny);
                    },
                    'loadNetworkAndProtocol': function(networks, protocols) {
                        trendPlotPanel.loadNetworkAndProtocol(networks, protocols);
                    }
                }
            });
        }

        Ext.onReady(init);
</script>
