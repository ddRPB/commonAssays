/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.nab;

import org.apache.log4j.Logger;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.StudyService;

import java.util.ArrayList;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: davebradlee
 * Date: 3/8/13
 * Time: 3:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class NabUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(NabUpgradeCode.class);

    // invoked by nab-12.21-12.22.sql
    @SuppressWarnings({"UnusedDeclaration"})
    public void migrateToNabSpecimen(ModuleContext moduleContext)
    {
        User user = moduleContext.getUpgradeUser();
//        User user = userIn;

        DbSchema nabSchema = NabManager.getSchema();
        DbScope scope = nabSchema.getScope();
        try
        {
            scope.beginTransaction();
            SQLFragment sqlProtocolIds = new SQLFragment("SELECT DISTINCT nab.NabSpecimen.ProtocolId FROM study.DataSet, nab.NabSpecimen WHERE study.DataSet.ProtocolId = nab.NabSpecimen.ProtocolId");
            ArrayList<Integer> protocolIds = new SqlSelector(nabSchema.getScope(), sqlProtocolIds).getArrayList(Integer.class);

            for (Integer protocolId : protocolIds)
            {
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(protocolId);
                Map<? extends DataSet, String> dataSets = StudyService.get().getDatasetsAndSelectNameForAssayProtocol(protocol);
                for (Map.Entry<? extends DataSet, String> entry : dataSets.entrySet())
                {
                    // Add RowId column
                    String tableName = entry.getValue();

                    Domain domain = entry.getKey().getDomain();
                    DomainProperty pd = domain.getPropertyByName("ObjectId");
                    if (null != pd)
                    {
                        pd.setName("RowId");
                        domain.save(user);
                    }

                    // Populate RowId
                    SQLFragment sqlSet = new SQLFragment("UPDATE " + tableName + " SET _key = (SELECT RowId FROM nab.NabSpecimen WHERE nab.NabSpecimen.ObjectId = " + tableName + ".RowId)," +
                            " RowId = (SELECT RowId FROM nab.NabSpecimen WHERE nab.NabSpecimen.ObjectId = " + tableName + ".RowId)");
                    new SqlExecutor(nabSchema.getScope()).execute(sqlSet);

                    // Key property name is updated to RowId back in the SQL script
                }
            }
            scope.commitTransaction();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            scope.closeConnection();
        }
    }
}