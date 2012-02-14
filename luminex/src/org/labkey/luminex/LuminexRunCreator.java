package org.labkey.luminex;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.Table;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 13, 2012
 */
public class LuminexRunCreator extends DefaultAssayRunCreator<LuminexAssayProvider>
{
    public LuminexRunCreator(LuminexAssayProvider provider)
    {
        super(provider);
    }

    @Override
    public ExpExperiment saveExperimentRun(AssayRunUploadContext uploadContext, @Nullable ExpExperiment batch, ExpRun run, boolean forceSaveBatchProps) throws ExperimentException, ValidationException
    {
        LuminexRunContext context = (LuminexRunContext)uploadContext;

        batch = super.saveExperimentRun(context, batch, run, forceSaveBatchProps);

        Container container = context.getContainer();

        // Save the analyte properties
        List<ExpData> outputs = run.getDataOutputs();
        for (ExpData output : outputs)
        {
            int dataId = output.getRowId();

            for (Analyte analyte : getAnalytes(dataId))
            {
                Map<DomainProperty, String> properties = context.getAnalyteProperties(analyte.getName());

                ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                int i = 0;
                for (Map.Entry<DomainProperty, String> entry : properties.entrySet())
                {
                    ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                            container, entry.getKey().getPropertyURI(),
                            entry.getValue(), entry.getKey().getPropertyDescriptor().getPropertyType());
                    objProperties[i++] = property;
                }
                OntologyManager.insertProperties(container, analyte.getLsid(), objProperties);
            }
        }

        return batch;
    }

    private Analyte[] getAnalytes(int dataRowId)
    {
        try
        {
            return Table.select(LuminexSchema.getTableInfoAnalytes(), Table.ALL_COLUMNS, new SimpleFilter("DataId", dataRowId), new Sort("RowId"), Analyte.class);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }


}
