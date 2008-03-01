package org.labkey.ms2;

import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.SearchClient;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.security.User;
import org.labkey.api.query.FieldKey;
import org.labkey.ms2.pipeline.mascot.MascotClientImpl;
import org.labkey.ms2.pipeline.sequest.SequestClientImpl;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.labkey.ms2.query.MS2Schema;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jan 9, 2007
 */
public class MS2ServiceImpl implements MS2Service.Service
{
    public String getRunsTableName()
    {
        return MS2Manager.getTableInfoRuns().toString();
    }

    public SearchClient createSearchClient(String server, String url, Logger instanceLogger, String userAccount, String userPassword)
    {
        if(server.equalsIgnoreCase("mascot"))
            return new MascotClientImpl(url, instanceLogger, userAccount, userPassword);
        if(server.equalsIgnoreCase("sequest"))
            return new SequestClientImpl(url, instanceLogger);
        return null;
    }

    public TableInfo createPeptidesTableInfo(User user, Container container)
    {
        return createPeptidesTableInfo(user, container, true, true, null, null);
    }

    public TableInfo createPeptidesTableInfo(User user, Container container, boolean includeFeatureFk, boolean restrictContainer, SimpleFilter filter, Iterable<FieldKey> defaultColumns)
    {
        PeptidesTableInfo table = new PeptidesTableInfo(new MS2Schema(user, container), includeFeatureFk, restrictContainer);
        if(null != filter)
            table.addCondition(filter);
        if(null != defaultColumns)
            table.setDefaultVisibleColumns(defaultColumns);
        return table;
    }

}
