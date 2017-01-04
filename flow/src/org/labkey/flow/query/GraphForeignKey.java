/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.flow.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.view.GraphColumn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GraphForeignKey extends AttributeForeignKey<GraphSpec>
{
    FlowPropertySet _fps;

    public GraphForeignKey(Container c, FlowPropertySet fps)
    {
        super(c);
        _fps = fps;
    }

    @Override
    protected AttributeType type()
    {
        return AttributeType.graph;
    }

    protected Collection<AttributeCache.GraphEntry> getAttributes()
    {
        return _fps.getGraphProperties();
    }

    protected GraphSpec attributeFromString(String field)
    {
        try
        {
            return new GraphSpec(field);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    protected void initColumn(final GraphSpec spec, String preferredName, ColumnInfo column)
    {
        column.setSqlTypeName("VARCHAR");
        SubsetSpec subset = _fps.simplifySubset(spec.getSubset());
        GraphSpec captionSpec = new GraphSpec(subset, spec.getParameters());
        column.setLabel(captionSpec.toString());
        column.setDisplayColumnFactory(GraphColumn::new);

        // Set the DetailsURL for the column using ContainerContext.
        // By explicitly setting the ContainerContext, the URL's container will point to the original flow assay data
        // instead of the current container (which may be a different container for flow copied-to-study datasets.)
        // If we ever make FlowProtocol work in multiple containers, we will need to pull the Container from the ResultSet instead.
        ActionURL baseURL = new ActionURL(WellController.ShowGraphAction.class, null);
        Map<String, FieldKey> graphParams = new HashMap<>();
        graphParams.put(FlowParam.objectId_graph.toString(), column.getFieldKey());
        DetailsURL urlGraph = new DetailsURL(baseURL, graphParams);
        urlGraph.setContainerContext(_container);
        column.setURL(urlGraph);

        column.setMeasure(false);
        column.setDimension(false);
    }

    // Select the string concatenated value of objectId+'~~~'+graphSpec
    // When rendering the image URL, we will split the values apart again.
    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, GraphSpec attrName, int attrId)
    {
        final SqlDialect dialect = objectIdColumn.getSqlDialect();
        final SQLFragment sepAndGraphSpec =
            dialect.concatenate(new SQLFragment("'" + GraphColumn.SEP + "'"), new SQLFragment("?").add(attrName.toString()));

        SQLFragment sql = new SQLFragment("(SELECT CASE WHEN COUNT(flow.Graph.ObjectId) = 1");
        sql.append("\nTHEN ");
        sql.append(dialect.concatenate(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS), sepAndGraphSpec));
        sql.append("\nELSE ");
        sql.append(sepAndGraphSpec);
        sql.append(" END");
        sql.append("\nFROM flow.Graph WHERE flow.Graph.GraphId = ");
        sql.append(attrId);
        sql.append("\nAND flow.Graph.ObjectId = ");
        sql.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        sql.append(")");

        return sql;
    }
}
