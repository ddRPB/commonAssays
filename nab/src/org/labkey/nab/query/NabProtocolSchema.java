/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayDataLinkDisplayColumn;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.RunListDetailsQueryView;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayController;
import org.labkey.nab.NabAssayProvider;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/13/12
 */
public class NabProtocolSchema extends AssayProtocolSchema
{
    /*package*/ static final String DATA_ROW_TABLE_NAME = "Data";

    public NabProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public NabRunDataTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        NabRunDataTable table = new NabRunDataTable(this, getProtocol());

        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, true);
        }
        return table;
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        final ExpRunTable runTable = super.createRunsTable();
        ColumnInfo nameColumn = runTable.getColumn(ExpRunTable.Column.Name);
        // NAb has two detail type views of a run - the filtered results/data grid, and the run details page that
        // shows the graph. Set the run's name to be a link to the grid instead of the default details page.
        nameColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo, runTable.getContainerFilter());
            }
        });
        return runTable;
    }

    @Override
    protected ResultsQueryView createDataQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabResultsQueryView((NabAssayProvider)getProvider(), getProtocol(), context);
    }

    public static class NabResultsQueryView extends ResultsQueryView
    {
        public NabResultsQueryView(NabAssayProvider provider, ExpProtocol protocol, ViewContext context)
        {
            super(protocol, context, getDefaultSettings(provider, protocol, context));
        }

        private static QuerySettings getDefaultSettings(NabAssayProvider provider, ExpProtocol protocol, ViewContext context)
        {
            NabProtocolSchema schema = provider.createProtocolSchema(context.getUser(), context.getContainer(), protocol, null);
            return schema.getSettings(context, AssayProtocolSchema.DATA_TABLE_NAME, AssayProtocolSchema.DATA_TABLE_NAME);
        }

        private void addGraphSubItems(NavTree parent, Domain domain, String dataRegionName, Set<String> excluded)
        {
            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            for (DomainProperty prop : domain.getProperties())
            {
                if (!excluded.contains(prop.getName()))
                {
                    NavTree menuItem = new NavTree(prop.getLabel(), "#");
                    menuItem.setScript("document.forms['" + dataRegionName + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                            "document.forms['" + dataRegionName + "'].captionColumn.value = '" + prop.getName() + "';\n" +
                            "document.forms['" + dataRegionName + "'].chartTitle.value = 'Neutralization by " + prop.getLabel() + "';\n" +
                            "document.forms['" + dataRegionName + "'].method = 'GET';\n" +
                            "document.forms['" + dataRegionName + "'].submit(); return false;");
                    parent.addChild(menuItem);
                }
            }
        }

        public DataView createDataView()
        {
            DataView view = super.createDataView();
            DataRegion rgn = view.getDataRegion();
            rgn.setRecordSelectorValueColumns("ObjectId");
            rgn.addHiddenFormField("protocolId", "" + _protocol.getRowId());
            ButtonBar bbar = view.getDataRegion().getButtonBar(DataRegion.MODE_GRID);

            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            MenuButton graphSelectedButton = new MenuButton("Graph");
            rgn.addHiddenFormField("captionColumn", "");
            rgn.addHiddenFormField("chartTitle", "");

            graphSelectedButton.addMenuItem("Default Graph", "#",
                    "document.forms['" + rgn.getName() + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                    "document.forms['" + rgn.getName() + "'].method = 'GET';\n" +
                    "document.forms['" + rgn.getName() + "'].submit(); return false;");

            Domain sampleDomain = ((NabAssayProvider) _provider).getSampleWellGroupDomain(_protocol);
            NavTree sampleSubMenu = new NavTree("Custom Caption (Sample)");
            Set<String> excluded = new HashSet<String>();
            excluded.add(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME);
            excluded.add(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME);
            excluded.add(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME);
            addGraphSubItems(sampleSubMenu, sampleDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(sampleSubMenu);

            Domain runDomain = _provider.getRunDomain(_protocol);
            NavTree runSubMenu = new NavTree("Custom Caption (Run)");
            excluded.clear();
            excluded.add(NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
            excluded.add(NabAssayProvider.LOCK_AXES_PROPERTY_NAME);
            excluded.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
            addGraphSubItems(runSubMenu, runDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(runSubMenu);
            graphSelectedButton.setRequiresSelection(true);
            bbar.add(graphSelectedButton);

            rgn.addDisplayColumn(0, new SimpleDisplayColumn()
            {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    Object runId = ctx.getRow().get(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runId != null)
                    {
                        ActionURL url = new ActionURL(NabAssayController.DetailsAction.class, ctx.getContainer()).addParameter("rowId", "" + runId);
                        Map<String, String> title = new HashMap<String, String>();
                        title.put("title", "View run details");
                        out.write(PageFlowUtil.textLink("run details", url, "", "", title));
                    }
                }

                @Override
                public void addQueryColumns(Set<ColumnInfo> set)
                {
                    super.addQueryColumns(set);
                    ColumnInfo runIdColumn = getTable().getColumn(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runIdColumn != null)
                        set.add(runIdColumn);
                }
            });
            return view;
        }
    }

    public static class NabRunListQueryView extends RunListDetailsQueryView
    {
        public NabRunListQueryView(ExpProtocol protocol, ViewContext context)
        {
            super(protocol, context, NabAssayController.DetailsAction.class, "rowId", ExpRunTable.Column.RowId.toString());
        }
    }

    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabRunListQueryView(getProtocol(), context);
    }
}
