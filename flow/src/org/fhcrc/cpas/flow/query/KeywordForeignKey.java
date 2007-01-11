package org.fhcrc.cpas.flow.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;

public class KeywordForeignKey extends AttributeForeignKey<String>
{
    public KeywordForeignKey(FlowPropertySet fps)
    {
        super(fps.getKeywordProperties().keySet());
    }

    protected String attributeFromString(String field)
    {
        return field;
    }

    protected void initColumn(String attrName, ColumnInfo column)
    {
        column.setSqlTypeName("VARCHAR");
        column.setCaption(attrName);
        if (isHidden(attrName))
        {
            column.setIsHidden(true);
        }
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, String attrName, int attrId)
    {
        SQLFragment ret = new SQLFragment("(SELECT flow.Keyword.Value FROM flow.Keyword WHERE flow.Keyword.ObjectId = ");
        ret.append(objectIdColumn.getValueSql());
        ret.append(" AND flow.Keyword.KeywordId = ");
        ret.append(attrId);
        ret.append(")");
        return ret;
    }

    static public boolean isHidden(String keyword)
    {
        if (keyword.startsWith("$"))
            return true;
        if (keyword.startsWith("P") && keyword.endsWith("DISPLAY"))
            return true;
        return false;
    }
}
