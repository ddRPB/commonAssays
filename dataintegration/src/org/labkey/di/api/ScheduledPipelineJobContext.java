/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.di.api;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * Created with IntelliJ IDEA.
 * User: matthewb
 * Date: 2013-03-18
 * Time: 3:52 PM
 */
public class ScheduledPipelineJobContext
{
    String _key;
    private String _containerId = null;
    private int _userId = 0;

    public ScheduledPipelineJobContext(ScheduledPipelineJobDescriptor descriptor, Container container, User user)
    {
        _containerId = container.getId();
        if (null != user)
            _userId = user.getUserId();
        _key = "Container" + container.getRowId() + ":" + descriptor.getId();
    }

    public Container getContainer()
    {
        return ContainerManager.getForId(_containerId);
    }

    public User getUser()
    {
        return UserManager.getUser(_userId);
    }

    public String getKey()
    {
        return _key;
    }


    @Override
    public String toString()
    {
        return _key;
    }



    // JobDataMap helpers

    public static ScheduledPipelineJobContext getFromJobDetail(JobExecutionContext jobExecutionContext)
    {
        JobDataMap map = jobExecutionContext.getTrigger().getJobDataMap();
        Object result = map.get(ScheduledPipelineJobContext.class.getName());
        if (result == null)
        {
            map = jobExecutionContext.getJobDetail().getJobDataMap();
            result = map.get(ScheduledPipelineJobContext.class.getName());
            if (result == null)
                throw new IllegalArgumentException("No ScheduledPipelineJobContext found!");
        }
        return (ScheduledPipelineJobContext) result;
    }

    protected void writeJobDataMap(JobDataMap map)
    {
        map.put(ScheduledPipelineJobContext.class.getName(), this);
    }

    public JobDataMap getJobDataMap()
    {
        JobDataMap map = new JobDataMap();
        writeJobDataMap(map);
        return map;
    }
}