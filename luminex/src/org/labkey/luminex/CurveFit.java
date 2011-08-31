/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class CurveFit
{
    private int _rowId;
    private int _titrationId;
    private int _analyteId;
    private String _curveType;
    private Double _ec50;
    private Double _auc;
    private Double _minAsymptote;
    private Double _maxAsymptote;
    private Double _asymmetry;
    private Double _slope;
    private Double _inflection;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getTitrationId()
    {
        return _titrationId;
    }

    public void setTitrationId(int titrationId)
    {
        _titrationId = titrationId;
    }

    public int getAnalyteId()
    {
        return _analyteId;
    }

    public void setAnalyteId(int analyteId)
    {
        _analyteId = analyteId;
    }

    public String getCurveType()
    {
        return _curveType;
    }

    public void setCurveType(String curveType)
    {
        _curveType = curveType;
    }

    public Double getEC50()
    {
        return _ec50;
    }

    public void setEC50(Double ec50)
    {
        _ec50 = ec50;
    }

    public Double getAUC()
    {
        return _auc;
    }

    public void setAUC(Double auc)
    {
        _auc = auc;
    }

    public Double getMinAsymptote()
    {
        return _minAsymptote;
    }

    public void setMinAsymptote(Double minAsymptote)
    {
        _minAsymptote = minAsymptote;
    }

    public Double getMaxAsymptote()
    {
        return _maxAsymptote;
    }

    public void setMaxAsymptote(Double maxAsymptote)
    {
        _maxAsymptote = maxAsymptote;
    }

    public Double getAsymmetry()
    {
        return _asymmetry;
    }

    public void setAsymmetry(Double asymmetry)
    {
        _asymmetry = asymmetry;
    }

    public Double getSlope()
    {
        return _slope;
    }

    public void setSlope(Double slope)
    {
        _slope = slope;
    }

    public Double getInflection()
    {
        return _inflection;
    }

    public void setInflection(Double inflection)
    {
        _inflection = inflection;
    }
}
