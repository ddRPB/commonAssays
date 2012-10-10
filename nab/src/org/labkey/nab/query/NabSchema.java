/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.data.*;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.SinglePlateNabDataHandler;
import org.labkey.nab.SampleInfo;

import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Oct 3, 2007
 * Time: 4:22:26 PM
 */
public class NabSchema extends AssaySchema
{
    public static final String SCHEMA_NAME = "Nab";

    public static final String SAMPLE_PREPARATION_METHOD_TABLE_NAME = "SamplePreparationMethod";
    public static final String CURVE_FIT_METHOD_TABLE_NAME = "CurveFitMethod";
    private static final String DATA_ROW_TABLE_NAME = "Data";
    private List<ExpProtocol> _protocols;

    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new NabSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public NabSchema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, ExperimentService.get().getSchema(), null);
    }

    public Set<String> getTableNames()
    {
        Set<String> names = new TreeSet<String>(new Comparator<String>()
        {
            public int compare(String o1, String o2)
            {
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (ExpProtocol protocol : getProtocols())
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof NabAssayProvider)
                names.add(getTableName(protocol));
        }
        names.add(SAMPLE_PREPARATION_METHOD_TABLE_NAME);
        names.add(CURVE_FIT_METHOD_TABLE_NAME);
        return names;
    }

    private static String getTableName(ExpProtocol protocol)
    {
        return protocol.getName() + " " + DATA_ROW_TABLE_NAME;
    }

    public TableInfo createTable(String name)
    {
        if (SAMPLE_PREPARATION_METHOD_TABLE_NAME.equalsIgnoreCase(name))
        {
            EnumTableInfo<SampleInfo.Method> result = new EnumTableInfo<SampleInfo.Method>(SampleInfo.Method.class, getDbSchema(), "List of possible sample preparation methods for the NAb assay.", false);
            result.setPublicSchemaName(SCHEMA_NAME);
            result.setPublicName(SAMPLE_PREPARATION_METHOD_TABLE_NAME);
            return result;
        }
        if (CURVE_FIT_METHOD_TABLE_NAME.equalsIgnoreCase(name))
        {
            EnumTableInfo<DilutionCurve.FitType> result = new EnumTableInfo<DilutionCurve.FitType>(DilutionCurve.FitType.class, getDbSchema(), new EnumTableInfo.EnumValueGetter<DilutionCurve.FitType>()
            {
                public String getValue(DilutionCurve.FitType e)
                {
                    return e.getLabel();
                }
            }, false, "List of possible curve fitting methods for the NAb assay.");
            result.setPublicSchemaName(SCHEMA_NAME);
            result.setPublicName(CURVE_FIT_METHOD_TABLE_NAME);
            return result;
        }

        for (ExpProtocol protocol : getProtocols())
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof NabAssayProvider)
            {
                if (getTableName(protocol).equalsIgnoreCase(name))
                {
                    return createDataRowTable(protocol);
                }
            }
        }
        return null;
    }

    private List<ExpProtocol> getProtocols()
    {
        if (_protocols == null)
        {
            _protocols = AssayService.get().getAssayProtocols(getContainer());
        }
        return _protocols;
    }

    public NabRunDataTable createDataRowTable(ExpProtocol protocol)
    {
        return new NabRunDataTable(this, protocol);        
    }

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        String propPrefix = new Lsid(SinglePlateNabDataHandler.NAB_PROPERTY_LSID_PREFIX, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);
        propertyFilter.addCondition("Project", protocol.getContainer().getProject());
        return Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);
    }
}
