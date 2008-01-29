/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms1.view;

import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.CrosstabView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.DataView;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.query.FeaturesTableInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 22, 2008
 * Time: 3:56:33 PM
 */
public class CompareRunsView extends CrosstabView
{
    private MS1Schema _schema = null;
    private List<Integer> _runIds = null;

    public CompareRunsView(MS1Schema schema, List<Integer> runIds)
    {
        super(schema);
        _schema = schema;
        _runIds = runIds;

        QuerySettings settings = new QuerySettings(getViewContext().getActionURL(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setSchemaName(schema.getSchemaName());
        settings.setQueryName(MS1Schema.TABLE_COMPARE_PEP);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(false);
        setSettings(settings);

        setShowCustomizeViewLinkInButtonBar(false);
        setShowRecordSelectors(false);
    }

    protected TableInfo createTable()
    {
        return _schema.getComparePeptideTableInfo(_runIds);
    }

    protected DataView createDataView()
    {
        DataView view = super.createDataView();
        String sortString = CrosstabTableInfo.getDefaultSortString() + ",Peptide";
        view.getRenderContext().setBaseSort(new Sort(sortString));
        return view;
    }
}
