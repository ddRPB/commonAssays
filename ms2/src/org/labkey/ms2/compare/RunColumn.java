package org.labkey.ms2.compare;

/**
 * User: jeckels
 * Date: Oct 6, 2006
 */
public class RunColumn
{
    private final String _label;
    private final String _name;
    private final String _aggregate;
    private String _formatString;

    public RunColumn(String label, String name, String aggregate, String formatString)
    {
        _label = label;
        _name = name;
        _aggregate = aggregate;
        _formatString = formatString;
    }

    public RunColumn(String label, String name, String aggregate)
    {
        this(label, name, aggregate, null);
    }
    
    public String getLabel()
    {
        return _label;
    }

    public String getName()
    {
        return _name;
    }

    public String getAggregate()
    {
        return _aggregate;
    }

    public String getFormatString()
    {
        return _formatString;
    }
}
