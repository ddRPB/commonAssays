package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnGroup;
import org.labkey.api.data.RenderContext;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Created by cnathe on 8/7/14.
 */
public class NegativeBeadDisplayColumnGroup extends DisplayColumnGroup
{
    private String _inputName;

    public NegativeBeadDisplayColumnGroup(List<DisplayColumn> columns, String inputName)
    {
        super(columns, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME, true);
        _inputName = inputName;
    }

    @Override
    public void writeSameCheckboxCell(RenderContext ctx, Writer out) throws IOException
    {
        out.write("<td>");
        if (isCopyable())
        {
            String inputName = ColumnInfo.propNameFromName(_inputName);
            out.write("<input type=checkbox name='" + inputName + "CheckBox' id='" + inputName + "CheckBox' onchange=\"");
            out.write("b = this.checked;\n" );
            for (int i = 1; i < getColumns().size(); i++)
            {
                DisplayColumn col = getColumns().get(i);
                if (col.getColumnInfo() != null)
                {
                    out.write("s = document.getElementsByName('" + col.getFormFieldName(ctx) + "')[0].options.length;\n");
                    out.write("document.getElementsByName('" + col.getFormFieldName(ctx) + "')[0].style.display = b || s == 0 ? 'none' : 'block';\n");
                }
            }
            out.write(" if (b) { " + inputName + "Updated(); }\">");
        }
        out.write("</td>");
    }
}
