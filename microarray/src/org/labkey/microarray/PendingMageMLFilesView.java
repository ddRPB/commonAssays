/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.exp.api.*;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.reports.ReportService;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Jan 9, 2008
 */
public class PendingMageMLFilesView extends QueryView
{
    public PendingMageMLFilesView(ViewContext context)
    {
        super(new ExpSchema(context.getUser(), context.getContainer()));
        setSettings(createSettings(context));
        setShowExportButtons(false);
        setShowRecordSelectors(true);
        setShowDetailsColumn(false);
        setViewItemFilter(ReportService.EMPTY_ITEM_LIST);
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
    }

    private QuerySettings createSettings(ViewContext context)
    {
        QuerySettings result = new QuerySettings(context.getActionURL(), "PendingMageMLFiles");
        result.setSchemaName(getSchema().getSchemaName());
        result.setAllowChooseQuery(false);
        result.setQueryName(ExpSchema.TableType.Datas.name());
        return result;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
//        super.populateButtonBar(view, bar);

        List<ExpProtocol> protocols = AssayService.get().getAssayProtocols(getContainer());
        List<ExpProtocol> microarrayProtocols = new ArrayList<ExpProtocol>();
        for (ExpProtocol protocol : protocols)
        {
            if (AssayService.get().getProvider(protocol) instanceof MicroarrayAssayProvider)
            {
                microarrayProtocols.add(protocol);
            }
        }

        PipeRoot root;
        try
        {
            root = PipelineService.get().findPipelineRoot(getContainer());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        ActionButton deleteButton = new ActionButton("placeholder", "Delete");
        ActionURL deleteURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getDeleteDatasURL(view.getViewContext().getContainer(), view.getViewContext().getActionURL());
        deleteButton.setScript("if (verifySelected(" + view.getDataRegion().getJavascriptFormReference(true) + ", \"" + deleteURL + "\", \"post\", \"MageML files\")) {" + view.getDataRegion().getJavascriptFormReference(true) + ".submit();} return false;");
        deleteButton.setDisplayPermission(ACL.PERM_DELETE);
        bar.add(deleteButton);

        if (root == null)
        {
            SimpleTextDisplayElement element = new SimpleTextDisplayElement("Unable to import because pipeline has not been configured. [<a href=\"" + PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(getContainer()) + "\">setup pipeline</a>]", true);
            element.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(element);
        }
        else
        {
            if (microarrayProtocols.size() == 0)
            {
                SimpleTextDisplayElement element = new SimpleTextDisplayElement("Unable to import, no microarray assay definitions found", false);
                element.setDisplayPermission(ACL.PERM_INSERT);
                bar.add(element);
            }
            else if (microarrayProtocols.size() == 1)
            {
                ExpProtocol protocol = protocols.get(0);
                ActionURL url = MicroarrayController.getUploadRedirectAction(getContainer(), protocol);
                ActionButton button = new ActionButton(url, "Import selected using " + protocol.getName());
                button.setDisplayPermission(ACL.PERM_INSERT);
                button.setScript("if (verifySelected(" + view.getDataRegion().getJavascriptFormReference(true) + ", \"" + url.getLocalURIString() + "\", \"POST\", \"files\")) { " + view.getDataRegion().getJavascriptFormReference(true) + ".submit(); } return false;");
                bar.add(button);
            }
            else
            {
                MenuButton menu = new MenuButton("Import selected using...");
                menu.setDisplayPermission(ACL.PERM_INSERT);
                bar.add(menu);
                for (ExpProtocol protocol : microarrayProtocols)
                {
                    ActionURL url = MicroarrayController.getUploadRedirectAction(getContainer(), protocol);
                    menu.addMenuItem("Import using " + protocol.getName(), "javascript: if (verifySelected(" + view.getDataRegion().getJavascriptFormReference(false) + ", \"" + url.getLocalURIString() + "\", \"POST\", \"files\")) { " + view.getDataRegion().getJavascriptFormReference(false) + ".submit(); }");
                }
            }
            ActionURL browseURL = PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(), getViewContext().getActionURL().toString());
            ActionButton browseButton = new ActionButton(browseURL, "Browse for MageML Files");
            browseButton.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(browseButton);
        }
    }

    protected TableInfo createTable()
    {
        ExpDataTable table = ExperimentService.get().createDataTable("pendingFile");
        table.setRun(null);
        table.setDataType(MicroarrayModule.MAGE_ML_DATA_TYPE);
        table.populate(getSchema());
        return table;
    }

    public ExpSchema getSchema()
    {
        return (ExpSchema)super.getSchema();
    }
}
