/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.view;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.PanelButton;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.assay.AssayPublishService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTrailConfig;
import org.labkey.api.view.NavTree;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.webparts.FlowFolderType;
import org.springframework.validation.Errors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowQueryView extends QueryView
{
    List<DisplayColumn> _displayColumns;
    boolean __hasGraphs;

    public FlowQueryView(FlowQueryForm form)
    {
        this((FlowSchema) form.getSchema(), (FlowQuerySettings) form.getQuerySettings(), null);
    }

    public FlowQueryView(FlowSchema schema, FlowQuerySettings settings, Errors errors)
    {
        super(schema, settings, errors);
        setShadeAlternatingRows(true);
        setShowBorders(true);

        // CONSIDER: Only show selectors if user can export, delete, or publish/copy-to-study
        setShowRecordSelectors(true);
    }

//    protected MenuButton createExportButton(boolean exportAsWebPage)
//    {
//        MenuButton button = super.createExportButton(exportAsWebPage);
//
//        // XXX: only add menu item for queries based on FCSAnalyses
//        ActionURL url = getViewContext().cloneActionURL();
//        url.setAction(RunController.ExportToSpiceAction.class);
//        button.addMenuItem("Export All to Spice (.spd)", url);
//        return button;
//    }


    @Override
    protected boolean canDelete()
    {
        TableInfo mainTable = null;
        try
        {
            mainTable = getQueryDef().getMainTable();
        }
        catch (java.lang.IllegalArgumentException x)
        {
            // see bug 9119
        }
        if (mainTable instanceof ExpRunTable)
        {
            return true;
        }
        return false;
    }

    @Override
    protected boolean canInsert()
    {
        return false;
    }

    @Override
    protected boolean canUpdate()
    {
        return false;
    }

    public DataView createDataView()
    {
        DataView ret = super.createDataView();

//        if (hasGraphs() && showGraphs())
//        {
//            ret = new GraphView(ret);
//        }
//        ret.getDataRegion().setShowPaginationCount(false);
        return ret;
    }

    @Override
    protected DataRegion createDataRegion()
    {
        if (hasGraphs() && showGraphs())
        {
            DataRegion rgn = new GraphDataRegion();
            configureDataRegion(rgn);
            return rgn;
        }
        else
        {
            return super.createDataRegion();
        }
    }

    @Override
    protected void configureDataRegion(DataRegion rgn)
    {
        super.configureDataRegion(rgn);
        rgn.setShowPaginationCount(false);
    }

    protected User getUser()
    {
        return getViewContext().getUser();
    }

    protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        if (!isPrintView())
        {
            PrintWriter out = response.getWriter();
            if (hasGraphs())
            {
                if (showGraphs())
                {
                    ActionURL urlHide = getViewContext().cloneActionURL();
                    urlHide.deleteParameter(param("showGraphs"));
                    out.write(textLink("Hide Graphs", urlHide));
                    JspView view = new JspView(JspLoader.createPage(FlowQueryView.class, "setGraphSize.jsp"));
                    view.setFrame(FrameType.NONE);
                    HttpView.currentView().include(view, out);
                }
                else
                {
                    ActionURL urlShow = getViewContext().cloneActionURL();
                    urlShow.addParameter(param("showGraphs"), "true");
                    out.write(textLink("Show Graphs", urlShow));
                }
                out.write("<br>");
            }
        }
        super.renderView(model, request, response);
    }

    public FlowQuerySettings getSettings()
    {
        return (FlowQuerySettings) super.getSettings();
    }

    protected boolean showGraphs()
    {
        return getSettings().getShowGraphs();
    }

    protected boolean subtractBackground()
    {
        return getSettings().getSubtractBackground();
    }

    protected URLHelper urlChangeView()
    {
        URLHelper ret = super.urlChangeView();
        ret.deleteParameter(FlowParam.experimentId.toString());
        return ret;
    }

    public FlowSchema getSchema()
    {
        return (FlowSchema) super.getSchema();
    }

    protected void populateButtonBar(DataView view, ButtonBar bar,  boolean exportAsWebPage)
    {
        if (getSchema().getRun() == null /*&& getSchema().getScript() == null*/)
        {
            FlowExperiment[] experiments = FlowExperiment.getAnalysesAndWorkspace(getContainer());
            URLHelper target = urlChangeView();
//            if (getSchema().getScript() != null)
//                getSchema().getScript().addParams(target);
            MenuButton button = new MenuButton("Analysis Folder");

            Map<Integer, String> availableExperiments = new LinkedHashMap();
            availableExperiments.put(0, "All Analysis Folders");

            for (FlowExperiment experiment : experiments)
                availableExperiments.put(experiment.getExperimentId(), experiment.getName());

            FlowExperiment current = getSchema().getExperiment();
            int currentId = current == null ? 0 : current.getExperimentId();

            if (!availableExperiments.containsKey(currentId))
                availableExperiments.put(current.getExperimentId(), current.getName());

            for (Map.Entry<Integer, String> entry : availableExperiments.entrySet())
            {
                URLHelper url = target.clone();
                if (entry.getKey().intValue() != 0)
                    url.replaceParameter(FlowParam.experimentId.name(), String.valueOf(entry.getKey()));
                button.addMenuItem(entry.getValue(),
                        url.toString(),
                        null,
                        currentId == entry.getKey());
            }
            bar.add(button);
        }

        super.populateButtonBar(view, bar, exportAsWebPage);

        // UNDONE: refactor ResultsQueryView create "Copy to Study" button code so it can be re-used here
        FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        if (protocol != null && !AssayPublishService.get().getValidPublishTargets(getUser(), InsertPermission.class).isEmpty())
        {
            ExpProtocol expProtocol = protocol.getProtocol();
            ActionURL publishURL = PageFlowUtil.urlProvider(AssayUrls.class).getCopyToStudyURL(getContainer(), expProtocol);
            /*
            for (Pair<String, String> param : publishURL.getParameters())
            {
                if (!"rowId".equalsIgnoreCase(param.getKey()))
                    view.getDataRegion().addHiddenFormField(param.getKey(), param.getValue());
            }
            publishURL.deleteParameters();
            */

            if (getTable().getContainerFilter() != null)
                publishURL.addParameter("containerFilterName", getTable().getContainerFilter().getType().name());

            ActionButton publishButton = new ActionButton(publishURL,
                    "Copy to Study", DataRegion.MODE_GRID, ActionButton.Action.POST);
            publishButton.setDisplayPermission(InsertPermission.class);
            publishButton.setRequiresSelection(true);

            bar.add(publishButton);
        }
    }

    @Override
    public PanelButton createExportButton(boolean exportAsWebPage)
    {
        PanelButton panelButton = super.createExportButton(exportAsWebPage);

        User user = getUser();
        if (user != null && !user.isGuest() && getSettings() != null)
        {
            String queryName = getSettings().getQueryName();
            if (queryName.equals(FlowTableType.Runs.toString()))
            {
                ExportAnalysisForm form = new ExportAnalysisForm();
                form.setSelectionType("runs");
                HttpView analysisExportView = new JspView<ExportAnalysisForm>("/org/labkey/flow/view/exportAnalysis.jsp", form);
                panelButton.addSubPanel("Analysis", analysisExportView);
            }
            else if (queryName.equals(FlowTableType.FCSFiles.toString()) || queryName.equals(FlowTableType.FCSAnalyses.toString()) || queryName.equals(FlowTableType.CompensationControls.toString()))
            {
                ExportAnalysisForm form = new ExportAnalysisForm();
                form.setSelectionType("wells");
                HttpView analysisExportView = new JspView<ExportAnalysisForm>("/org/labkey/flow/view/exportAnalysis.jsp", form);
                panelButton.addSubPanel("Analysis", analysisExportView);
            }
        }

        return panelButton;
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        if (_displayColumns != null)
            return _displayColumns;
        _displayColumns = super.getDisplayColumns();
        __hasGraphs = false;
        boolean showGraphs = showGraphs();
        for (Iterator<DisplayColumn> it = _displayColumns.iterator(); it.hasNext();)
        {
            DisplayColumn dc = it.next();
            if (dc instanceof GraphColumn)
            {
                __hasGraphs = true;
                if (showGraphs)
                {
                    return _displayColumns;
                }
                it.remove();
            }
        }
        return _displayColumns;
    }

    protected boolean hasGraphs()
    {
        getDisplayColumns();
        return __hasGraphs;
    }

    public NavTrailConfig getNavTrailConfig()
    {
        NavTrailConfig ntc = super.getNavTrailConfig();
        FlowSchema schema = getSchema();
        FlowRun run = schema.getRun();
        List<NavTree> children = new ArrayList<NavTree>();
        if (getContainer().getFolderType() instanceof FlowFolderType)
        {
            children.add(0, new NavTree("Flow Dashboard", PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(getContainer())));
        }
        else
        {
            children.add(0, new NavTree(FlowModule.getShortProductName(), new ActionURL(FlowController.BeginAction.class, getContainer())));
        }
        if (run != null)
        {
            FlowExperiment experiment = run.getExperiment();
            if (experiment != null)
            {
                children.add(new NavTree(experiment.getLabel(), experiment.urlShow()));
            }
            children.add(new NavTree(run.getLabel(), run.urlShow()));
            ntc.setExtraChildren(children.toArray(new NavTree[0]));
        }
        else if (schema.getExperiment() != null)
        {
            children.add(new NavTree(schema.getExperiment().getLabel(), schema.getExperiment().urlShow()));
            ntc.setExtraChildren(children.toArray(new NavTree[0]));
        }
        ntc.setModuleOwner(ModuleLoader.getInstance().getModule(FlowModule.NAME));
        return ntc;
    }

    protected TSVGridWriter.ColumnHeaderType getColumnHeaderType()
    {
        return TSVGridWriter.ColumnHeaderType.queryColumnName;
    }
}
