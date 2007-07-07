package org.labkey.flow.gateeditor.client.model;

public class GWTPopulation extends GWTPopulationSet
{
    String name;
    String fullName;
    GWTGate gate;

    public GWTGate getGate()
    {
        return gate;
    }

    public void setGate(GWTGate gate)
    {
        this.gate = gate;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getParentFullName()
    {
        int ichLastSlash = getFullName().lastIndexOf("/");
        if (ichLastSlash < 0)
            return null;
        return getFullName().substring(0, ichLastSlash);
    }


    public GWTPopulation findPopulation(String fullName)
    {
        if (this.fullName.equals(fullName))
            return this;
        return super.findPopulation(fullName);
    }

    public GWTPopulation duplicate()
    {
        GWTPopulation that = new GWTPopulation();
        that.name = this.name;
        that.fullName = this.fullName;
        that.gate = this.gate;
        that.populations = clonePopulations();
        return that;
    }

    public boolean isIncomplete()
    {
        if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polyGate = (GWTPolygonGate) gate;
            return polyGate.length() == 0;
        }
        return false;
    }
}
