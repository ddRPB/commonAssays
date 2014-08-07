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
package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Created by cnathe on 7/21/14.
 */
public class NegativeBeadDisplayColumnFactory implements DisplayColumnFactory
{
    private String _analyteName;
    private String _inputName;
    private String _displayName;
    private Set<String> _initNegativeControlAnalytes;

    public NegativeBeadDisplayColumnFactory(String analyteName, String inputName, Set<String> initNegativeControlAnalytes)
    {
        _analyteName = analyteName;
        _inputName = inputName;
        _displayName = LuminexDataHandler.NEGATIVE_BEAD_DISPLAY_NAME;
        _initNegativeControlAnalytes = initNegativeControlAnalytes;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public String getFormFieldName(RenderContext ctx)
            {
                return _inputName;
            }

            @Override
            public void renderTitle(RenderContext ctx, Writer out) throws IOException
            {
                out.write("<script type='text/javascript'>"
                        + "   LABKEY.requiresScript('luminex/NegativeBeadPopulation.js');"
                        + "</script>");
                out.write(_displayName);
            }

            @Override
            public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
            {
                out.write("<td class='labkey-form-label'>");
                renderTitle(ctx, out);
                StringBuilder sb = new StringBuilder();
                sb.append("The analyte to use in the FI-Bkgd-Neg transform script calculation. Available options are " +
                        "those selected as Negative Control analytes.\n\n");
                sb.append("Type: ").append(getBoundColumn().getFriendlyTypeName()).append("\n");
                out.write(PageFlowUtil.helpPopup(_displayName, sb.toString()));
                out.write("</td>");
            }

            @Override
            public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
            {
                boolean hidden = _initNegativeControlAnalytes.contains(_analyteName);

                out.write("<select name=\"" + _inputName + "\" " +
                        "class=\"negative-bead-input\" " + // used by NegativeBeadPopulation.js
                        "analytename=\"" + _analyteName + "\" " + // used by NegativeBeadPopulation.js
                        "width=\"200\" style=\"width:200px;" +
                        (hidden ? "display:none;" : "display:inline-block;") + "\">");

                if (!hidden)
                {
                    out.write("<option value=\"\"></option>");
                    for (String negControlAnalyte : _initNegativeControlAnalytes)
                    {
                        out.write("<option value=\"" + negControlAnalyte + "\"");
                        if (value != null && value.equals(negControlAnalyte))
                        {
                            out.write(" SELECTED");
                        }
                        out.write(">");
                        out.write(negControlAnalyte);
                        out.write("</option>");
                    }
                }
                out.write("</select>");
            }
        };
    }
}
