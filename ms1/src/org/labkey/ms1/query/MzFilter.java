/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms1.query;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.ms1.MS1Controller;

import java.util.ArrayList;

/**
 * Features filter for mz values
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 16, 2008
 * Time: 2:43:03 PM
 */
public class MzFilter implements FeaturesFilter
{
    private double _mzLow = 0;
    private double _mzHigh = 0;

    public MzFilter(double mz, double offset, MS1Controller.SimilarSearchForm.MzOffsetUnits units)
    {
        double adjustedOffset = offset;

        if(MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm == units)
            adjustedOffset = mz * (offset / 1000000);

        _mzLow = mz - adjustedOffset;
        _mzHigh = mz + adjustedOffset;
    }

    public MzFilter(double mzLow, double mzHigh)
    {
        assert mzLow <= mzHigh : "mzLow is > mzHigh!";
        _mzLow = mzLow;
        _mzHigh = mzHigh;
    }

    public void setFilters(FeaturesTableInfo tinfo)
    {
        tinfo.addCondition(new SQLFragment("MZ BETWEEN " + _mzLow + " AND " + _mzHigh), "MZ");
        
        //change the default visible columnset
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(tinfo.getDefaultVisibleColumns());
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run","Name"));
        visibleColumns.remove(FieldKey.fromParts("AccurateMz"));
        visibleColumns.remove(FieldKey.fromParts("Mass"));
        visibleColumns.remove(FieldKey.fromParts("Charge"));
        visibleColumns.remove(FieldKey.fromParts("Peaks"));
        visibleColumns.remove(FieldKey.fromParts("TotalIntensity"));
        tinfo.setDefaultVisibleColumns(visibleColumns);
    }
}
