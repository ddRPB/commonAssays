package org.labkey.ms2.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareResult implements IsSerializable
{
    private String[] _proteinNames;
    private String[] _runNames;
    private boolean[][] _hits;

    public CompareResult()
    {
    }

    public CompareResult(String[] proteinNames, String[] runNames, boolean[][] hits)
    {
        _proteinNames = proteinNames;
        _runNames = runNames;
        _hits = hits;
    }

    public String[] getProteinNames()
    {
        return _proteinNames;
    }

    public boolean[][] getHits()
    {
        return _hits;
    }

    public String[] getRunNames()
    {
        return _runNames;
    }
}
