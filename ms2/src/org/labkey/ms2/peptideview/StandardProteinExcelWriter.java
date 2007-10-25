package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.Protein;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class StandardProteinExcelWriter extends AbstractProteinExcelWriter
{
    protected int _peptideIndex = -1;

    public StandardProteinExcelWriter()
    {
        super();
    }

    @Override
    protected void renderGridRow(WritableSheet sheet, RenderContext ctx, List<ExcelColumn> columns) throws SQLException, WriteException, MaxRowsExceededException
    {
        Protein protein = new Protein();

        protein.setSequence((String) ctx.get("Sequence"));

        List<String> peptides = new ArrayList<String>();
        ResultSet nestedRS = _groupedRS.getNextResultSet();

        while (nestedRS.next())
            peptides.add(nestedRS.getString(getPeptideIndex()));

        // If expanded view, back up to the first peptide in this group
        if (_expanded)
            nestedRS.beforeFirst();

        String[] peptideArray = new String[peptides.size()];
        protein.setPeptides(peptides.toArray(peptideArray));

        // Calculate amino acid coverage and add to the rowMap for AACoverageColumn to see
        ctx.put("AACoverage", protein.getAAPercent());

        super.renderGridRow(sheet, ctx, columns);

        // If expanded, output the peptides
        if (_expanded)
        {
            _nestedExcelWriter.setCurrentRow(getCurrentRow());
            _nestedExcelWriter.renderGrid(sheet, nestedRS);
            setCurrentRow(_nestedExcelWriter.getCurrentRow());
        }
        else
            nestedRS.close();
    }

    private int getPeptideIndex() throws SQLException
    {
        if (_peptideIndex == -1)
        {
            _peptideIndex = _groupedRS.findColumn("Peptide");
        }
        return _peptideIndex;
    }
}
