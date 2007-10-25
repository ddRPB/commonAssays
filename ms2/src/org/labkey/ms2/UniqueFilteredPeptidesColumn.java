package org.labkey.ms2;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;

/**
 * User: jeckels
 * Date: Oct 25, 2007
 */
public class UniqueFilteredPeptidesColumn extends SimpleDisplayColumn
{
    public static final String NAME = "UniqueFilteredPeptides";

    public UniqueFilteredPeptidesColumn()
    {
        super();
        setCaption("Unique Filtered Peptides");
        setWidth("30");
        setTextAlign("right");
    }

    public Object getValue(RenderContext ctx)
    {
        return ctx.get(NAME);
    }

    public Class getValueClass()
    {
        return Integer.class;
    }

    public String getName()
    {
        return NAME;
    }
}