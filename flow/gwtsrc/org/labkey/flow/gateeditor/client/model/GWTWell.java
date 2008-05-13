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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTWell implements IsSerializable, Serializable
{
    private int wellId;
    private String name;
    private String label;
    private String[] parameters;
    private GWTScript script;
    private GWTRun run;
    private GWTCompensationMatrix compensationMatrix;

    public GWTRun getRun()
    {
        return run;
    }

    public void setRun(GWTRun run)
    {
        this.run = run;
    }

    public String[] getParameters()
    {
        return parameters;
    }

    public void setParameters(String[] parameters)
    {
        this.parameters = parameters;
    }

    public int getWellId()
    {
        return wellId;
    }

    public void setWellId(int wellId)
    {
        this.wellId = wellId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }


    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }


    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTWell)) return false;

        GWTWell gwtWell = (GWTWell) o;

        if (wellId != gwtWell.wellId) return false;

        return true;
    }

    public int hashCode()
    {
        return wellId;
    }

    public GWTScript getScript()
    {
        return script;
    }

    public void setScript(GWTScript script)
    {
        this.script = script;
    }


    public GWTCompensationMatrix getCompensationMatrix()
    {
        return compensationMatrix;
    }

    public void setCompensationMatrix(GWTCompensationMatrix compensationMatrix)
    {
        this.compensationMatrix = compensationMatrix;
    }
}
