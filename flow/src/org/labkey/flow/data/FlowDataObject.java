/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.AttrObject;
import org.apache.commons.lang.ObjectUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;

abstract public class FlowDataObject extends FlowObject<ExpData>
{
    static public List<FlowDataObject> fromDatas(ExpData[] datas)
    {
        HashMap<Integer,ExpData> flowDatas = new HashMap<Integer,ExpData>(2*datas.length);
        for (ExpData data : datas)
        {
            if (data.getDataType() instanceof FlowDataType)
                flowDatas.put(data.getRowId(), data);
        }
        List<AttrObject> attrs = FlowManager.get().getAttrObjects(flowDatas.values());
        List<FlowDataObject> ret = new ArrayList<FlowDataObject>(attrs.size());
        for (AttrObject a : attrs)
        {
            ExpData data = flowDatas.get(a.getDataId());
            ret.add(((FlowDataType) data.getDataType()).newInstance(data));
        }
        return ret;
    }

    static public FlowDataObject fromData(ExpData data)
    {
        if (data == null)
            return null;
        DataType type = data.getDataType();
        if (!(type instanceof FlowDataType))
            return null;
        AttrObject obj = FlowManager.get().getAttrObject(data);
        if (obj == null)
            return null;
        return ((FlowDataType) type).newInstance(data);
    }

    static public FlowObject fromRowId(int id)
    {
        return fromData(ExperimentService.get().getExpData(id));
    }

    static public FlowObject fromLSID(String lsid)
    {
        return fromData(ExperimentService.get().getExpData(lsid));
    }

    static public FlowObject fromAttrObjectId(int id)
    {
        AttrObject obj = FlowManager.get().getAttrObjectFromRowId(id);
        if (obj == null)
            return null;
        return fromRowId(obj.getDataId());
    }

    static public void addDataOfType(List<ExpData> datas, FlowDataType typeFilter, List list)
    {
        for (ExpData data : datas)
        {
            DataType type = data.getDataType();
            if (!(type instanceof FlowDataType))
                continue;
            if (typeFilter != null && typeFilter != type)
                continue;
            FlowDataObject obj = fromData(data);
            if (obj != null)
                list.add(obj);
        }
    }

    public FlowDataObject(ExpData data)
    {
        super(data);
    }

    public ExpData getData()
    {
        return getExpObject();
    }
    
    public FlowRun getRun()
    {
        ExpRun run = getData().getRun();
        if (run == null)
            return null;
        return new FlowRun(run);
    }

    public int getRowId()
    {
        return getData().getRowId();
    }

    public ExpProtocolApplication getProtocolApplication()
    {
        return getExpObject().getSourceApplication();
    }

    public int getActionSequence()
    {
        return getExpObject().getSourceApplication().getActionSequence();
    }

    public FlowObject getParent()
    {
        return getRun();
    }

    public String getOwnerObjectLSID()
    {
        return getLSID();
    }

    static public String generateDataLSID(Container container, FlowDataType type)
    {
        return ExperimentService.get().generateGuidLSID(container, type);
    }

    static private FlowDataType dataTypeFromLSID(String lsid)
    {
        Lsid LSID = new Lsid(lsid);
        return FlowDataType.ofNamespace(LSID.getNamespacePrefix());
    }

    public FlowDataType getDataType()
    {
        return (FlowDataType) _expObject.getDataType();
    }

    public String getExperimentLSID()
    {
        FlowRun run = getRun();
        if (run == null)
            return null;
        FlowExperiment experiment = getRun().getExperiment();
        if (experiment == null)
            return null;
        return experiment.getLSID();
    }

    static public List<FlowDataObject> getForContainer(Container container, FlowDataType type)
    {
        return fromDatas(ExperimentService.get().getExpDatas(container, type));
    }

    /**
     * Returns true if all objs are in the same experiment.
     */
    static public boolean sameExperiment(List<? extends FlowDataObject> objs)
    {
        if (objs.size() < 2)
            return true;
        String lsidCompare = objs.get(0).getExperimentLSID();
        for (int i = 1; i < objs.size(); i ++)
        {
            if (!ObjectUtils.equals(lsidCompare, objs.get(i).getExperimentLSID()))
                return false;
        }
        return true;
    }

    public FlowRun[] getTargetRuns()
    {
        return FlowRun.fromRuns(getData().getTargetRuns());
    }

    public AttributeSet getAttributeSet()
    {
        return AttributeSet.fromData(getData());
    }
}
