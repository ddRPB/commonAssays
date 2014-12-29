/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.elispot.plate;

import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.plate.TextPlateReader;

/**
 * Created by klum on 12/14/14.
 */
public class AIDPlateReader extends TextPlateReader
{
    public static final String TYPE = "aid_txt";

    public String getType()
    {
        return TYPE;
    }

    @Override
    protected double convertWellValue(String token) throws ValidationException
    {
        if ("TNTC".equalsIgnoreCase(token))
        {
            return WELL_NOT_COUNTED;
        }
        return super.convertWellValue(token);
    }
}