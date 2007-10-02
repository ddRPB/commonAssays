/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protein.uniprot;

import java.util.*;
import java.sql.*;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_organism extends ParseActions
{

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        _accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }

        clearCurItems();
        tables.put("Organism", this);
    }

    public void endElement(Connection c, Map<String,ParseActions> tables)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }
        if (getCurItem().get("species") == null || ((String) getCurItem().get("species")).equalsIgnoreCase("sp"))
        {
            getCurItem().put("species", "sp.");
        }
        String uniqKey =
                ((String) getCurItem().get("genus")).toUpperCase() +
                        " " +
                        ((String) getCurItem().get("species")).toUpperCase();
        getAllItems().put(uniqKey, getCurItem());
        setItemCount(getItemCount() + 1);
        Map s = tables.get("ProtSequences").getCurItem();
        s.put("genus", getCurItem().get("genus"));
        s.put("species", getCurItem().get("species"));
    }
}