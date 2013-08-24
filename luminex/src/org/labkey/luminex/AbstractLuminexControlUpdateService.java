package org.labkey.luminex;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.AbstractBeanQueryUpdateService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public abstract class AbstractLuminexControlUpdateService<Type extends AbstractLuminexControlAnalyte> extends AbstractBeanQueryUpdateService<Type, Pair<Integer, Integer>>
{
    protected final LuminexProtocolSchema _userSchema;
    private final Class<Type> _typeClass;
    private final TableInfo _rawTable;

    public AbstractLuminexControlUpdateService(AbstractLuminexTable queryTable, Class<Type> typeClass)
    {
        super(queryTable);
        _typeClass = typeClass;
        _rawTable = queryTable.getRealTable();
        _userSchema = queryTable.getUserSchema();
    }

    protected Analyte getAnalyte(int rowId)
    {
        Analyte analyte = new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes()).getObject(rowId, Analyte.class);
        if (analyte == null)
        {
            throw new IllegalStateException("Unable to find referenced analyte: " + rowId);
        }
        return analyte;
    }

    protected ExpRun getRun(int rowId)
    {
        ExpRun run = ExperimentService.get().getExpRun(rowId);
        if (run == null)
        {
            throw new IllegalStateException("Unable to find referenced run: " + rowId);
        }
        return run;
    }

    protected Map<String, String> getIsotypeAndConjugate(ExpRun run)
    {
        Map<String, String> isotypeConjugate = new HashMap<>();
        isotypeConjugate.put("Isotype", null);
        isotypeConjugate.put("Conjugate", null);
        Map<String, ObjectProperty> runProps = run.getObjectProperties();
        for (ObjectProperty property : runProps.values())
        {
            if (property.getName().equalsIgnoreCase("Isotype"))
            {
                isotypeConjugate.put("Isotype", property.getStringValue());
            }
            if (property.getName().equalsIgnoreCase("Conjugate"))
            {
                isotypeConjugate.put("Conjugate", property.getStringValue());
            }
        }
        return isotypeConjugate;
    }

    protected abstract void updateQCFlags(User user, Type bean) throws SQLException;

    @Override
    public List<Map<String, Object>> updateRows(User user, Container container, List<Map<String, Object>> rows, List<Map<String, Object>> oldKeys, Map<String, Object> extraScriptContext) throws InvalidKeyException, BatchValidationException, QueryUpdateServiceException, SQLException
    {
        List<Map<String, Object>> results = super.updateRows(user, container, rows, oldKeys, extraScriptContext);

        // If any of the updated rows includes a change to the guide set calculation (i.e. has IncludeInGuideSetCalculation as an updated value)
        // then we need to update the QC Flags for all rows that are associated with the given guide set(s)
        Set<Integer> guideSetIds = new HashSet<>();
        Set<Type> forUpdate = new HashSet<>();
        for (Map<String, Object> row : rows)
        {
            // add the current row to the set that need to have their QC Flags updated
            Type t = get(user, container, keyFromMap(row));
            forUpdate.add(t);
            if (row.containsKey("IncludeInGuideSetCalculation"))
            {
                guideSetIds.add(t.getGuideSetId());
            }
        }

        // Add all rows to the update set for the guide sets that have changed
        for (Integer guideSetId : guideSetIds)
        {
            SimpleFilter guideSetFilter = new SimpleFilter("GuideSetId", guideSetId);
            Type[] guideSetBeans = Table.select(_rawTable, Table.ALL_COLUMNS, guideSetFilter, null, _typeClass);
            forUpdate.addAll(Arrays.asList(guideSetBeans));
        }

        for (Type t : forUpdate)
        {
            updateQCFlags(user, t);
        }

        return results;
    }

    @Override
    protected Type update(User user, Container container, Type bean, Pair<Integer, Integer> oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
    {
        Integer newGuideSetId = bean.getGuideSetId();

        if (newGuideSetId != null)
        {
            GuideSet guideSet = new TableSelector(LuminexProtocolSchema.getTableInfoGuideSet()).getObject(newGuideSetId.intValue(), GuideSet.class);
            if (guideSet == null)
            {
                throw new ValidationException("No such guideSetId: " + newGuideSetId);
            }
            if (guideSet.getProtocolId() != _userSchema.getProtocol().getRowId())
            {
                throw new ValidationException("Can't set guideSetId to point to a guide set from another assay definition: " + newGuideSetId);
            }

            Analyte analyte = getAnalyte(bean.getAnalyteId());
            validate(bean, guideSet, analyte);
        }

        Object[] keys = new Object[2];

        boolean analyteFirst = _rawTable.getPkColumnNames().get(0).equalsIgnoreCase("AnalyteId");
        keys[0] = analyteFirst ? oldKey.getKey() : oldKey.getValue();
        keys[1] = analyteFirst ? oldKey.getValue() : oldKey.getKey();

        return Table.update(user, _rawTable, bean, keys);
    }

    protected abstract void validate(Type bean, GuideSet guideSet, Analyte analyte) throws ValidationException;
    @Override
    protected void delete(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException, SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Type insert(User user, Container container, Type bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
    {
        throw new UnsupportedOperationException();
    }
}
