/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.synonym;

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DatabaseTableType;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.TableInfo;

/**
 * Created by adam on 8/14/2015.
 */
public class SynonymTestCase extends Assert
{
    @Test
    public void testSynonyms()
    {
        if (CoreSchema.getInstance().getSqlDialect().isSqlServer())
        {
            DbSchema schema = DbSchema.get("test", DbSchemaType.Bare);

            TableInfo containerAliases2 = schema.getTable("ContainerAliases2");
            assertNotNull("Failed to find synonym ContainerAliases2", containerAliases2);
            assertEquals(containerAliases2.getTableType(), DatabaseTableType.TABLE);
            ColumnInfo containerId = containerAliases2.getColumn("ContainerId");
            assertNotNull("Failed to find column ContainerId", containerId);
            ForeignKey containerIdFk = containerId.getFk();
            assertEquals("test", containerIdFk.getLookupSchemaName());
            assertEquals("Containers2", containerIdFk.getLookupTableName());

            TableInfo containers2 = schema.getTable("Containers2");
            assertNotNull("Failed to find synonym Container2", containers2);
            assertEquals(containers2.getTableType(), DatabaseTableType.TABLE);
            ColumnInfo parent = containers2.getColumn("Parent");
            ForeignKey parentFk = parent.getFk();
            assertEquals("test", parentFk.getLookupSchemaName());
            assertEquals("Containers2", parentFk.getLookupTableName());
            assertEquals(containers2.getColumn("Searchable").getJdbcType(), JdbcType.BOOLEAN);
            assertEquals(containers2.getColumn("Type").getJdbcType(), JdbcType.VARCHAR);
            assertFalse(containers2.getColumn("Searchable").isNullable());
            assertFalse(containers2.getColumn("Type").isNullable());

            TableInfo testTable3 = schema.getTable("TestTable3");
            assertNotNull("Failed to find synonym TestTable3", testTable3);
            assertEquals(testTable3.getTableType(), DatabaseTableType.TABLE);
            assertEquals(1, testTable3.getPkColumns().size());
            assertEquals("RowId", testTable3.getPkColumns().get(0).getName());
            assertEquals(testTable3.getColumn("IntNull").getJdbcType(), JdbcType.INTEGER);
            assertEquals(testTable3.getColumn("IntNotNull").getJdbcType(), JdbcType.INTEGER);
            assertTrue(testTable3.getColumn("IntNull").isNullable());
            assertFalse(testTable3.getColumn("IntNotNull").isNullable());
            assertEquals(testTable3.getColumn("BitNull").getJdbcType(), JdbcType.BOOLEAN);
            assertEquals(testTable3.getColumn("BitNotNull").getJdbcType(), JdbcType.BOOLEAN);
            assertTrue(testTable3.getColumn("BitNull").isNullable());
            assertFalse(testTable3.getColumn("BitNotNull").isNullable());

            TableInfo users2 = schema.getTable("Users2");
            assertNotNull("Failed to find synonym Users2", users2);
            assertEquals(users2.getTableType(), DatabaseTableType.VIEW);
            assertEquals(users2.getColumn("Active").getJdbcType(), JdbcType.BOOLEAN);
            assertEquals(users2.getColumn("UserId").getJdbcType(), JdbcType.INTEGER);
            assertEquals(users2.getColumn("Mobile").getJdbcType(), JdbcType.VARCHAR);
            assertFalse(users2.getColumn("UserId").isNullable());
            assertFalse(users2.getColumn("DisplayName").isNullable());
            assertFalse(users2.getColumn("Active").isNullable());

            // Make sure the bare schema doesn't apply test.xml
            assertNull(testTable3.getDescription());

            // Make sure the module schema applies test.xml
            DbSchema moduleSchema = DbSchema.get("test", DbSchemaType.Module);
            assertEquals("This is used to test synonyms on SQL Server", moduleSchema.getTable("TestTable3").getDescription());
        }
    }
}