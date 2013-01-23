/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;


public class NAbSpecimenTable extends FilteredTable<NabProtocolSchema>
{
    public NAbSpecimenTable(NabProtocolSchema schema)
    {
        super(NabProtocolSchema.getTableInfoNAbSpecimen(), schema);

        wrapAllColumns(false);

        SQLFragment protocolIDFilter = new SQLFragment("ProtocolID = ?");
        protocolIDFilter.add(_userSchema.getProtocol().getRowId());
        addCondition(protocolIDFilter, FieldKey.fromParts("ProtocolID"));
    }
}