package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.ProteinGroupProteins;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.CaseInsensitiveHashSet;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Sep 25, 2006
 */
public class MS2Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms2";

    public static final String SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME = "SamplePrepRuns";
    public static final String XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "XTandemSearchRuns";
    public static final String MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "MascotSearchRuns";
    public static final String SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "SequestSearchRuns";
    public static final String GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME = "MS2SearchRuns";

    public static final String PEPTIDES_TABLE_NAME = "Peptides";
    public static final String PROTEIN_GROUPS_TABLE_NAME = "ProteinGroups";
    public static final String PROTEIN_GROUPS_FOR_RUN_TABLE_NAME = "ProteinGroupsForRun";
    public static final String PROTEIN_GROUPS_FOR_SEARCH_TABLE_NAME = "ProteinGroupsForSearch";
    public static final String SEQUENCES_TABLE_NAME = "Sequences";
    public static final String COMPARE_PROTEIN_PROPHET_TABLE_NAME = "CompareProteinProphet";
    public static final String COMPARE_PEPTIDES_TABLE_NAME = "ComparePeptides";

    private static final String MASCOT_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Mascot%";
    private static final String SEQUEST_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.Sequest%";
    private static final String XTANDEM_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.XTandem%";
    private static final String SAMPLE_PREP_PROTOCOL_PATTERN = "urn:lsid:%:Protocol.%:MS2.PreSearch.%";

    private static final Set<String> HIDDEN_PEPTIDE_MEMBERSHIPS_COLUMN_NAMES = new CaseInsensitiveHashSet("PeptideId");

    private ProteinGroupProteins _proteinGroupProteins = new ProteinGroupProteins();
    private List<MS2Run> _runs;

    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new MS2Schema(schema.getUser(), schema.getContainer());
            }
        });
    }

    private ExpSchema _expSchema;

    public MS2Schema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, ExperimentService.get().getSchema());
        _expSchema = new ExpSchema(user, container);
    }

    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(
            SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME,
            XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME,
            PEPTIDES_TABLE_NAME,
            PROTEIN_GROUPS_TABLE_NAME,
            SEQUENCES_TABLE_NAME));
    }

    public ProteinGroupProteins getProteinGroupProteins()
    {
        return _proteinGroupProteins;
    }

    public TableInfo getTable(String name, String alias)
    {
        if (SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            ExpRunTable result = _expSchema.createRunsTable(alias);
            result.setProtocolPatterns(SAMPLE_PREP_PROTOCOL_PATTERN);
            return result;
        }
        else if (XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN);
        }
        else if (MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, MASCOT_PROTOCOL_PATTERN);
        }
        else if (SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSearchTable(alias, SEQUEST_PROTOCOL_PATTERN);
        }
        else if (GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createRunsTable(alias);
        }
        else if (PEPTIDES_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createPeptidesTable(alias);
        }
        else if (PROTEIN_GROUPS_TABLE_NAME.equalsIgnoreCase(name))
        {
            ProteinGroupTableInfo result = new ProteinGroupTableInfo(alias, this);
            result.addContainerCondition(getContainer(), getUser(), false);
            return result;
        }
        else if (PROTEIN_GROUPS_FOR_SEARCH_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createProteinGroupsForSearchTable(alias);
        }
        else if (PROTEIN_GROUPS_FOR_RUN_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createProteinGroupsForRunTable(alias, false);
        }
        else if (SEQUENCES_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createSequencesTable(alias);
        }
        else if (COMPARE_PROTEIN_PROPHET_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createProteinProphetCompareTable(alias, null, null);
        }
        else if (COMPARE_PEPTIDES_TABLE_NAME.equalsIgnoreCase(name))
        {
            ComparePeptideTableInfo result = createPeptidesCompareTable(false, null, null);
            result.setAlias(alias);
            return result;
        }
        else
        {
            SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(name);
            if (config != null)
            {
                return createSpectraCountTable(config, null, null);
            }
        }

        return super.getTable(name, alias);
    }

    public ComparePeptideTableInfo createPeptidesCompareTable(boolean forExport, HttpServletRequest request, String peptideViewName)
    {
        return new ComparePeptideTableInfo(this, _runs, forExport, request, peptideViewName);
    }

    public CompareProteinProphetTableInfo createProteinProphetCompareTable(String alias, HttpServletRequest request, String peptideViewName)
    {
        return new CompareProteinProphetTableInfo(alias, this, _runs, false, request, peptideViewName);
    }

    public TableInfo createRunsTable(String alias)
    {
        return createSearchTable(alias, XTANDEM_PROTOCOL_PATTERN, MASCOT_PROTOCOL_PATTERN,SEQUEST_PROTOCOL_PATTERN);
    }

    public SpectraCountTableInfo createSpectraCountTable(SpectraCountConfiguration config, HttpServletRequest request, String peptideViewName)
    {
        return new SpectraCountTableInfo(this, config, request, peptideViewName);
    }

    public ProteinGroupTableInfo createProteinGroupsForSearchTable(String alias)
    {
        ProteinGroupTableInfo result = new ProteinGroupTableInfo(alias, this);
        List<FieldKey> defaultColumns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        defaultColumns.add(0, FieldKey.fromParts("ProteinProphet","Run"));
        defaultColumns.add(0, FieldKey.fromParts("ProteinProphet", "Run", "Folder"));
        result.setDefaultVisibleColumns(defaultColumns);
        return result;
    }

    public ProteinGroupTableInfo createProteinGroupsForRunTable(String alias)
    {
        return createProteinGroupsForRunTable(alias, true);
    }

    public ProteinGroupTableInfo createProteinGroupsForRunTable(String alias, boolean includeFirstProteinColumn)
    {
        ProteinGroupTableInfo result = new ProteinGroupTableInfo(alias, this, includeFirstProteinColumn);
        result.addProteinsColumn();
        List<FieldKey> defaultColumns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "BestGeneName"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "Mass"));
        defaultColumns.add(FieldKey.fromParts("Proteins", "Protein", "Description"));
        result.setDefaultVisibleColumns(defaultColumns);
        return result;
    }

    protected TableInfo createPeptideMembershipsTable(final MS2Run... runs)
    {
        TableInfo info = MS2Manager.getTableInfoPeptideMemberships();
        FilteredTable result = new FilteredTable(info);
        for (ColumnInfo col : info.getColumns())
        {
            ColumnInfo newColumn = result.addWrapColumn(col);
            if (HIDDEN_PEPTIDE_MEMBERSHIPS_COLUMN_NAMES.contains(newColumn.getName()))
            {
                newColumn.setIsHidden(true);
            }
        }
        LookupForeignKey fk = new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                ProteinGroupTableInfo result = new ProteinGroupTableInfo(null, MS2Schema.this);
                result.getColumn("ProteinProphet").setIsHidden(true);
                result.addProteinDetailColumns(runs);

                return result;
            }
        };
        fk.setPrefixColumnCaption(false);
        result.getColumn("ProteinGroupId").setFk(fk);
        return result;
    }

    protected TableInfo createFractionsTable()
    {
        SqlDialect dialect = MS2Manager.getSqlDialect();
        FilteredTable result = new FilteredTable(MS2Manager.getTableInfoFractions());
        result.wrapAllColumns(true);

        SQLFragment fractionNameSQL = new SQLFragment(dialect.getSubstringFunction(ExprColumn.STR_TABLE_ALIAS + ".FileName", "1", dialect.getStringIndexOfFunction("'.'", ExprColumn.STR_TABLE_ALIAS + ".FileName") + "- 1"));

        ColumnInfo fractionName = new ExprColumn(result, "FractionName", fractionNameSQL, Types.VARCHAR);
        fractionName.setCaption("Name");
        fractionName.setWidth("200");
        result.addColumn(fractionName);

        ActionURL url = new ActionURL(MS2Controller.ShowRunAction.class, getContainer());
        result.getColumn("Run").setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                return new RunTableInfo(MS2Schema.this);
            }
        });

        return result;
    }

    public SequencesTableInfo createSequencesTable(String aliasName)
    {
        return new SequencesTableInfo(aliasName, this);
    }

    public TableInfo createPeptidesTable(final String alias)
    {
        PeptidesTableInfo result = new PeptidesTableInfo(this);
        result.setAlias(alias);
        return result;
    }

    private TableInfo createSearchTable(String alias, String... protocolPattern)
    {
        final ExpRunTable result = _expSchema.createRunsTable(alias);
        result.setProtocolPatterns(protocolPattern);

        SQLFragment sql = new SQLFragment("(SELECT MIN(ms2Runs.run)\n" +
                "\nFROM " + MS2Manager.getTableInfoRuns() + " ms2Runs " +
                "\nWHERE ms2Runs.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID)");
        ColumnInfo ms2DetailsColumn = new ExprColumn(result, alias, sql, Types.INTEGER);
        ms2DetailsColumn.setName("MS2Details");
        ActionURL url = new ActionURL(MS2Controller.ShowRunAction.class, getContainer());
        ms2DetailsColumn.setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(MS2Manager.getTableInfoRuns());
                result.addWrapColumn(result.getRealTable().getColumn("Run"));
                result.addWrapColumn(result.getRealTable().getColumn("Description"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("Path"));
                result.addWrapColumn(result.getRealTable().getColumn("SearchEngine"));
                result.addWrapColumn(result.getRealTable().getColumn("MassSpecType"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideCount"));
                result.addWrapColumn(result.getRealTable().getColumn("SpectrumCount"));
                result.addWrapColumn(result.getRealTable().getColumn("SearchEnzyme"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("Status"));
                result.addWrapColumn(result.getRealTable().getColumn("Type"));

                ColumnInfo iconColumn = result.wrapColumn("Links", result.getRealTable().getColumn("Run"));
                iconColumn.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        ActionURL linkURL = new ActionURL(MS2Controller.ShowRunAction.class, getContainer());
                        return new IconDisplayColumn(colInfo, 18, 18, linkURL, "run", AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif");
                    }
                });
                result.addColumn(iconColumn);
                return result;
            }
        });
        result.addColumn(ms2DetailsColumn);

        ms2DetailsColumn.setIsHidden(false);
        FieldKey fieldLinks = FieldKey.fromParts("Links");
        FieldKey fieldMS2Links = FieldKey.fromParts("MS2Details", "Links");
        boolean ms2LinksAdded = false;
        List<FieldKey> columns = new ArrayList<FieldKey>();
        for (FieldKey field : result.getDefaultVisibleColumns())
        {
            columns.add(field);
            if (!ms2LinksAdded && fieldLinks.equals(field))
            {
                columns.add(fieldMS2Links);
                ms2LinksAdded = true;
            }
        }
        columns.remove(FieldKey.fromParts("Name"));
        columns.remove(FieldKey.fromParts("Protocol"));
        columns.remove(FieldKey.fromParts("CreatedBy"));
        if (!ms2LinksAdded)
        {
            columns.add(fieldMS2Links);
        }
        columns.add(FieldKey.fromParts("MS2Details", "Path"));
        columns.add(FieldKey.fromParts("Input", "FASTA"));
        result.setDefaultVisibleColumns(columns);
        return result;
    }

    public void setRuns(MS2Run[] runs)
    {
        _runs = Arrays.asList(runs);
    }

    public void setRuns(List<MS2Run> runs)
    {
        _runs = runs;
    }
    
    public List<MS2Run> getRuns()
    {
        return _runs;
    }

    protected SQLFragment getPeptideSelectSQL(HttpServletRequest request, String viewName, Collection<FieldKey> fieldKeys)
    {
        QueryDefinition queryDef = QueryService.get().createQueryDefForTable(this, MS2Schema.PEPTIDES_TABLE_NAME);
        SimpleFilter filter = new SimpleFilter();
        CustomView view = queryDef.getCustomView(getUser(), request, viewName);
        if (view != null)
        {
            ActionURL url = new ActionURL();
            view.applyFilterAndSortToURL(url, "InternalName");
            filter.addUrlFilters(url, "InternalName");
        }

        TableInfo peptidesTable = createPeptidesTable("PeptidesAlias");

        ColumnInfo[] peptideCols = QueryService.get().getColumns(peptidesTable, fieldKeys).values().toArray(new ColumnInfo[0]);

        return Table.getSelectSQL(peptidesTable, peptideCols, filter, new Sort());
    }
}
