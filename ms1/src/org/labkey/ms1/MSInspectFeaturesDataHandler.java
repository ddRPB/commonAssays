package org.labkey.ms1;

import org.apache.commons.beanutils.ConversionException;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.tools.TabLoader;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This data handler loads msInspect feature files, which use a tsv format.
 * It also handles deleting and moving that data when the experiment run
 * is deleted or moved.
 * @author DaveS
 * User: daves
 * Date: Sept, 2007
 */
public class MSInspectFeaturesDataHandler extends AbstractExperimentDataHandler
{
    public static final String FEATURES_FILE_EXTENSION = ".features.tsv";

    /**
     * This class maps a source column in the features tsv file with its
     * target database column and its jdbc data type. This is used within
     * the MSInspectFeaturesDataHandler class, and enables us to handle
     * feature files with missing or additional (but well-known) columns.
     * @author DaveS
     */
    protected static class ColumnBinding
    {
        public String sourceColumn;    //name of the source column
        public String targetColumn;    //name of the target column
        public int jdbcType;           //jdbc data type of target column
        public boolean isRequired;     //true if this column is required

        public ColumnBinding(String sourceColumn, String targetColumn, int jdbcType, boolean isRequired)
        {
            this.sourceColumn = sourceColumn;
            this.targetColumn = targetColumn;
            this.jdbcType = jdbcType;
            this.isRequired = isRequired;
        } //c-tor

        @Override
        public String toString()
        {
            return sourceColumn + "->" + targetColumn + " (type: " + jdbcType + ")" + (isRequired ? " Required" : "");
        }
    } //class Binding

    /**
     * Helper class for storing a map of ColumnBinding objects, keyed on the source column name
     */
    protected static class ColumnBindingHashMap extends HashMap<String,ColumnBinding>
    {
        public ColumnBinding put(ColumnBinding binding)
        {
            return put(binding.sourceColumn, binding);
        }
    } //class ColumBindingHashMap

    /**
     * Helper class for detecting conversion errors when using
     * the TabLoader class.
     */
    protected static class ConversionError
    {
        private String _columnName = "";

        public ConversionError(String columnName)
        {
            _columnName = columnName;
        }

        public String getColumnName()
        {
            return _columnName;
        }
    }

    //Constants and Static Data Members
    private static final int CHUNK_SIZE = 1000;         //number of insert statements in a batch

    //Master map of all possible column bindings.
    //The code below will select the appropriate bindings after the
    //tsv has been loaded based on the column descriptors.
    //To handle a new column in the features file, add a new put statement here.
    //The format is:
    // _bindingMap.put(new ColumnBinding(<tsv column name>, <db column name>, <jdbc type>));
    protected static ColumnBindingHashMap _bindingMap = new ColumnBindingHashMap();
    static
    {
        _bindingMap.put(new ColumnBinding("scan", "Scan", java.sql.Types.INTEGER, true));
        _bindingMap.put(new ColumnBinding("time", "Time", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("mz", "MZ", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("accurateMZ", "AccurateMZ", java.sql.Types.BOOLEAN, false));
        _bindingMap.put(new ColumnBinding("mass", "Mass", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("intensity", "Intensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("charge", "Charge", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("chargeStates", "ChargeStates", java.sql.Types.TINYINT, false));
        _bindingMap.put(new ColumnBinding("kl", "KL", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("background", "Background", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("median", "Median", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("peaks", "Peaks", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanFirst", "ScanFirst", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanLast", "ScanLast", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("scanCount", "ScanCount", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("totalIntensity", "TotalIntensity", java.sql.Types.REAL, false));
        _bindingMap.put(new ColumnBinding("description", "Description", java.sql.Types.VARCHAR, false));

        //columns added by Ceaders-Sinai to their post-processed features files
        _bindingMap.put(new ColumnBinding("MS2scan", "MS2Scan", java.sql.Types.INTEGER, false));
        _bindingMap.put(new ColumnBinding("probability", "MS2ConnectivityProbability", java.sql.Types.REAL, false));
    } //static init for _bindingMap

    /**
     * The experiment loader calls this to load the data file.
     * @param data The experiment data file
     * @param dataFile The data file to load
     * @param info Background info
     * @param log Log to write to
     * @param context The XarContext
     * @throws ExperimentException
     */
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if(null == data || null == dataFile || null == info || null == log || null == context)
            return;

        int numRows = 0;
        try
        {
            //if this file has already been imported before, just return
            if(MS1Manager.get().isAlreadyImported(dataFile, data))
            {
                log.info("Already imported features file " + dataFile.toURI() + " for this experiment into this container.");
                return;
            }
        }
        catch(SQLException e)
        {
            log.warn("Problem checking if this file has already been imported!");
            throw new ExperimentException(MS1Manager.get().getAllErrors(e));
        }

        //NOTE: I'm using the highly-efficient technique of prepared statements and batch execution here,
        //but that also means I'm not using the Table layer and benefiting from its functionality.
        // This may need to change in the future.
        Connection cn = null;
        PreparedStatement pstmt = null;

        //get the ms1 schema and scope
        DbSchema schema = DbSchema.get("ms1");
        DbScope scope = schema.getScope();

        try
        {
            //begin a transaction
            scope.beginTransaction();
            cn = schema.getScope().getConnection();
            long startMs = System.currentTimeMillis();

            //insert the feature files row
            int idFile = insertFeaturesFile(info.getUser(), schema, data);

            //open the tsv file using TabLoader for automatic parsing
            TabLoader tsvloader = new TabLoader(dataFile);
            TabLoader.TabLoaderIterator iter = tsvloader.iterator();
            TabLoader.ColumnDescriptor[] coldescrs = tsvloader.getColumns();

            //set the error value for each column descriptor so that we can
            //detect conversion errors as we process the rows
            for(TabLoader.ColumnDescriptor coldescr : coldescrs)
                coldescr.errorValues = new ConversionError(coldescr.name);

            //insert information about the software used to produce the file
            insertSoftwareInfo(tsvloader.getComments(), idFile, info.getUser(), schema);

            //select the appropriate bindings for this tsv file
            ArrayList<ColumnBinding> bindings = selectBindings(coldescrs, log);

            //if there are no bindings, there is nothing in the file we know how
            //to import, so just return
            if(bindings.isEmpty())
            {
                log.warn("The file " + dataFile.toURI() + " did not contain any columns this system knows how to import.");
                return;
            }

            //build the approrpriate insert sql for the features table
            //and prepare it
            pstmt = cn.prepareStatement(genInsertSQL(bindings));

            Map row;

            //iterate over the rows
            while(iter.hasNext())
            {
                //get a row
                row = (Map)iter.next();
                ++numRows;

                //set parameter values
                pstmt.clearParameters();
                pstmt.setInt(1, idFile); //jdbc params are 1-based!

                for(int idx = 0; idx < bindings.size(); ++idx)
                    setParam(pstmt, idx + 2, numRows, bindings.get(idx), row);

                //add a batch
                pstmt.addBatch();

                //execute if we've reached our chunk limit
                if((numRows % CHUNK_SIZE) == 0)
                {
                    pstmt.executeBatch();
                    log.info("Uploaded " + CHUNK_SIZE + " feature rows to the database.");
                }
            } //while reading rows

            //execute any remaining in the batch
            if(numRows % CHUNK_SIZE != 0)
                pstmt.executeBatch();

            //commit the transaction
            scope.commitTransaction();

            log.info("Finished loading " + numRows + " features in " + (System.currentTimeMillis() - startMs) + " milliseconds.");
        }
        catch(ConversionException ex)
        {
            log.error("Error while converting data in row " + (numRows + 1) + " : " + ex);
            scope.rollbackTransaction();
            throw new ExperimentException(ex);
        }
        catch(IOException ex)
        {
            scope.rollbackTransaction();
            throw new ExperimentException(ex);
        }
        catch(SQLException ex)
        {
            scope.rollbackTransaction();
            throw new ExperimentException(MS1Manager.get().getAllErrors(ex));
        }
        finally
        {
            //final cleanup
            try{if(null != pstmt) pstmt.close();}catch(SQLException ignore){}
            try{if(null != cn) scope.releaseConnection(cn);}catch(SQLException ignore){}
        } //finally

    } //importFile()

    protected int insertFeaturesFile(User user, DbSchema schema, ExpData data) throws SQLException, ExperimentException
    {
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("FileId",null);
        map.put("ExpDataFileId", new Integer(data.getRowId()));
        map.put("Type", new Integer(MS1Manager.FILETYPE_FEATURES));
        map.put("MzXmlURL", getMzXmlFilePath(data));
        map.put("Imported", Boolean.TRUE);

        map = Table.insert(user, schema.getTable(MS1Manager.TABLE_FILES), map);
        if(null == map.get("FileId"))
            throw new ExperimentException("Unable to get new id for features file.");
        
        return ((Integer) (map.get("FileId"))).intValue();
    } //insertFeaturesFile()

    protected void insertSoftwareInfo(Map comments, int idFile, User user, DbSchema schema) throws SQLException
    {
        HashMap<String,Object> software = new HashMap<String,Object>();
        software.put("SoftwareId", null);
        software.put("FileId", idFile);
        software.put("Name", "msInspect");
        software.put("Author", "Fred Hutchinson Cancer Research Center");

        software = Table.insert(user, schema.getTable(MS1Manager.TABLE_SOFTWARE), software);

        //now try to get the algorithm from the comments
        //if we can get it, add that as a named parameter
        String algorithm = (String)comments.get("algorithm");
        if(null != algorithm && algorithm.length() > 0)
        {
            HashMap<String,Object> softwareParam = new HashMap<String,Object>();
            softwareParam.put("SoftwareId", software.get("SoftwareId"));
            softwareParam.put("Name", "algorithm");
            softwareParam.put("Value", algorithm);

            Table.insert(user, schema.getTable(MS1Manager.TABLE_SOFTWARE_PARAMS), softwareParam);
        }
    } //insertSoftwareInfo()

    /**
     * Returns the master mzXML file path for the data file
     * @param data  Experiment data object
     * @return      Path to the mzXML File
     */
    protected String getMzXmlFilePath(ExpData data)
    {
        //by convention, the mzXML has the same base name as the data file (minus the ".features.tsv")
        //and is located three directories above the data file
        File dataFile = data.getDataFile();
        String dataFileName = dataFile.getName();
        String baseName = dataFileName.substring(0, dataFileName.length() - FEATURES_FILE_EXTENSION.length());
        File mzxmlFile = new File(dataFile.getParentFile().getParentFile().getParentFile(), baseName + ".mzXML");
        return mzxmlFile.toURI().toString();
    } //getMzXmlFilePath()

    /**
     * Selects the appropriate column bindings based on the passed column descriptors
     * @param coldescrs The set of column descriptors for the tsv file
     * @param log       Log file
     * @return          The appropriate set of column bindings
     */
    protected ArrayList<ColumnBinding> selectBindings(TabLoader.ColumnDescriptor[] coldescrs, Logger log)
    {
        ArrayList<ColumnBinding> ret = new ArrayList<ColumnBinding>(coldescrs.length);
        ColumnBinding binding;
        for(TabLoader.ColumnDescriptor coldescr : coldescrs)
        {
            binding = _bindingMap.get(coldescr.name);
            if(null != binding)
                ret.add(binding);
            else
                log.warn("The msInspect Features importer does not recognize the column '" + coldescr.name + "' in this file. Its contents will be ignored.");
        }
        return ret;
    } //selectBindings()

    /**
     * Generates the insert SQL statement for the Features table, with the correct column
     * names and number of parameter markers.
     * @param bindings  The column Bindings
     * @return          A properly constructed SQL INSERT statement for the given column bindings
     */
    protected String genInsertSQL(ArrayList<ColumnBinding> bindings)
    {
        StringBuilder sbCols = new StringBuilder("INSERT INTO ");
        sbCols.append(MS1Manager.get().getSQLTableName(MS1Manager.TABLE_FEATURES));
        sbCols.append(" (FileId");

        StringBuilder sbParams = new StringBuilder("(?");

        for(ColumnBinding binding : bindings)
        {
            //if binding is null, we don't know how to import this
            //column, so don't include it in the sql
            if(null != binding)
            {
                sbCols.append(",").append(binding.targetColumn);
                sbParams.append(",?");
            }
        } //for each binding

        //close both with an end paren
        sbCols.append(")");
        sbParams.append(")");
        
        //return the complete SQL
        return sbCols.toString() + " VALUES " + sbParams.toString();
    } //genInsertSQL()

    /**
     * Sets the JDBC parameter with the row/column value from the tsv, performing the appropriate type casting
     * @param pstmt         The prepared statement
     * @param paramIndex    The parameter index to set
     * @param rowNum        The row number (used for error messages)
     * @param binding       The appropriate column bindings for this paramter
     * @param row           The row Map from the TabLoader
     * @throws ExperimentException Thrown if required value is not present
     * @throws SQLException Thrown if there is a database exception
     */
    protected void setParam(PreparedStatement pstmt, int paramIndex, int rowNum, ColumnBinding binding, Map row) throws ExperimentException, SQLException
    {
        if(null == binding)
            return;

        Object val = row.get(binding.sourceColumn);

        if(val instanceof ConversionError)
            throw new ExperimentException("Error converting the value in column '" + ((ConversionError)val).getColumnName() + "' at row " + rowNum);

        try
        {
            //check for null and required
            if(null == val)
            {
                if(binding.isRequired)
                    throw new ExperimentException("The value in the required column '" + binding.sourceColumn +
                                                    "' at row " + rowNum + " was empty.");
                else
                    pstmt.setNull(paramIndex, binding.jdbcType);
            }
            else
            {
                //switch on target column jdbc type
                switch(binding.jdbcType)
                {
                    case Types.BIT:
                    case Types.BOOLEAN:
                        pstmt.setBoolean(paramIndex, objToBoolean(val, binding, rowNum));
                        break;

                    case Types.DATE:
                        pstmt.setDate(paramIndex, objToDate(val, binding, rowNum));
                        break;

                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                        pstmt.setString(paramIndex, val.toString());
                        break;

                    case Types.INTEGER:
                    case Types.SMALLINT:
                    case Types.TINYINT:
                        pstmt.setInt(paramIndex, objToInt(val, binding, rowNum));
                        break;

                    case Types.REAL:
                    case Types.NUMERIC:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                        pstmt.setDouble(paramIndex, objToDouble(val, binding, rowNum));
                        break;

                    default:
                        assert false : "Unsupported JDBC type."; //if you get this, add support above
                }
            } //not null
        }
        catch(SQLException e)
        {
            throw new ExperimentException("Problem setting the value for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to the value " + val + ": " + e.toString());
        }
    } //setParam()

    protected int objToInt(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Number)
                return ((Number)val).intValue();
            else if(val instanceof String)
                return Integer.parseInt((String)val);
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to an integer as required by the database.");
        }
        catch (NumberFormatException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to an integer for the following reason: " + e);
        }
    }

    protected double objToDouble(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Number)
                return ((Number)val).doubleValue();
            else if(val instanceof String)
                return Double.parseDouble((String)val);
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a double-precision decimal number as required by the database.");
        }
        catch (NumberFormatException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a double-precision decimal number for the following reason: " + e);
        }
    }

    protected boolean objToBoolean(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Boolean)
                return ((Boolean)val).booleanValue();
            if(val instanceof Number)
                return ((Number)val).intValue() != 0;
            else if(val instanceof String)
            {
                BooleanFormat parser = BooleanFormat.getInstance();
                return parser.parseObject((String)val).booleanValue();
            }
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a boolean as required by the database.");
        }
        catch (ParseException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a boolean for the following reason: " + e);
        }
    }

    protected java.sql.Date objToDate(Object val, ColumnBinding binding, int rowNum) throws ExperimentException
    {
        try
        {
            if(val instanceof Date)
                return new java.sql.Date(((Date)val).getTime());
            else if(val instanceof String)
            {
                SimpleDateFormat parser = new SimpleDateFormat();
                return new java.sql.Date(parser.parse((String)val).getTime());
            }
            else
                throw new ExperimentException("The value '" + val + "' in row " + rowNum + ", column '" + binding.sourceColumn
                                                + "' cannot be converted to a date/time as required by the database.");
        }
        catch (ParseException e)
        {
            throw new ExperimentException("Unable to convert the value '" + val + "' for column '" + binding.sourceColumn +
                                            "' in row " + rowNum + " to a date/time for the following reason: " + e);
        }
    }

    /**
     * Returns the content URL for files imported through this class. This is called by the Experiment module
     * @param request       The HTTP request object
     * @param container     The current container
     * @param data          The experiment data object
     * @return              The URL the user should be redirected to
     * @throws ExperimentException Thrown if there's a problem
     */
    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ViewURLHelper url = new ViewURLHelper(request, MS1Module.CONTROLLER_NAME, "showFeatures.view", container);
        url.addParameter("runId", Integer.toString(data.getRun().getRowId()));
        return url;
    }

    /**
     * Deletes data rows imported by this class when the experiment run is deleted
     * @param data          The experiment data file being deleted
     * @param container     The container in which it lives
     * @param user          The user deleting it
     * @throws ExperimentException  Thrown if something goes wrong
     */
    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        // Delete the database records for this features file
        if(null == data || null == user)
                return;

        //Although it's not terribly obvious, the caller will have already begun a transaction
        //and the DbScope code will generate an exception if you call beginTrans() more than once
        //so don't use a transaction here because it's already transacted in the caller.
        try
        {
            MS1Manager.get().deleteFeaturesData(data);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(MS1Manager.get().getAllErrors(e));
        }
    } //deleteData()

    /**
     * Moves the container for the given data file uploaded through this class
     * @param newData           The new experiment data object
     * @param container         The old container
     * @param targetContainer   The the container
     * @param oldRunLSID        The old run LSID
     * @param newRunLSID        The new run LSID
     * @param user              The user moving the data
     * @param oldDataRowId      The old data file row id
     * @throws ExperimentException  Thrown if something goes wrong
     */
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowId) throws ExperimentException
    {
        if(null == newData || null == user)
                return;

        //update the database records to reflect the new data file row id
        try
        {
            MS1Manager.get().moveFileData(oldDataRowId, newData.getRowId());
        }
        catch(SQLException e)
        {
            throw new ExperimentException(MS1Manager.get().getAllErrors(e));
        }
    } //runMoved()

    /**
     * Returns the priority if the passed data file is one this class knows how to import, otherwise null.
     * @param data  The data file to import
     * @return      Priority if this file can import it, otherwise null.
     */
    public Priority getPriority(ExpData data)
    {
        //we handle only *.features.tvt files
        String fileUrl = data.getDataFileUrl();
        if(null != fileUrl && fileUrl.endsWith(FEATURES_FILE_EXTENSION))
            return Priority.MEDIUM;
        else
            return null;
    }
} //class MSInspectFeaturesDataHandler
