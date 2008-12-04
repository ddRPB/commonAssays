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

package org.labkey.ms2;

import org.labkey.api.data.*;
import org.labkey.api.view.GridView;
import org.labkey.api.view.WebPartView;

import java.io.PrintWriter;
import java.util.List;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2WebPart extends WebPartView
{
    public MS2WebPart()
    {
    }


    @Override
    public void renderView(Object model, PrintWriter out) throws Exception
    {
        Container c = hasAccess(getViewContext(), "MS2 Runs");
        if (c == null)
        {
            return;
        }

        DataRegion rgn = getGridRegionWebPart();
        rgn.getDisplayColumn(0).setURL(MS2Controller.getShowRunSubstitutionURL(c));

        GridView gridView = new GridView(rgn);
        gridView.setCustomizeLinks(getCustomizeLinks());
        gridView.setTitle("MS2 Runs");
        gridView.setTitleHref(MS2Controller.getShowListURL(c));
        gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
        gridView.setSort(MS2Manager.getRunsBaseSort());

        include(gridView);
    }


    private DataRegion getGridRegionWebPart()
    {
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
        TableInfo ti = MS2Manager.getTableInfoExperimentRuns();
        List<ColumnInfo> cols = ti.getColumns("Description", "Path", "Created", "Run", "ExperimentRunLSID", "ProtocolName", "ExperimentRunRowId");
        rgn.setColumns(cols);
        rgn.getDisplayColumn(3).setVisible(false);
        rgn.getDisplayColumn(4).setVisible(false);
        rgn.getDisplayColumn(5).setVisible(false);
        rgn.getDisplayColumn(6).setVisible(false);

        rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
        return rgn;
    }
}
