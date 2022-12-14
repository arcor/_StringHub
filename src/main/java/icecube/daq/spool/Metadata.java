package icecube.daq.spool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Metadata
{
    /** metadata filename */
    public static final String FILENAME = "hitspool.db";

    /** Logging instance */
    private static final Logger LOG = Logger.getLogger(Metadata.class);

    /** <tt>true</tt> if the SQLite JDBC driver is loaded */
    private static boolean loadedSQLite;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            loadedSQLite = true;
        } catch (Throwable thr) {
            LOG.error("SQLite driver could not be loaded", thr);
            loadedSQLite = false;
        }
    }

    private Connection conn;
    private PreparedStatement insertStmt;
    private PreparedStatement updateStmt;
    private PreparedStatement rangeQueryStmt;
    private PreparedStatement contentQueryStmt;

    // hitspool_cfg table
    // tags the singular configuration record
    public static final String CONFIG_RECORD_KEY = "2021_UPGRADE";
    private static final String CONFIG_TABLE_CREATE_SQL = "create table hitspool_cfg(" +
            "id text text primary key," +
            "interval integer, num_files integer)";
    private static final String CONFIG_TABLE_QUERY_SQL = "select * from hitspool_cfg where id=?";
    private static final String CONFIG_TABLE_INSERT_SQL = "insert into hitspool_cfg(id, interval, num_files)" +
            " values (?,?,?)";


    //default auto-create is protected until it is not the default
     Metadata(File directory)
            throws SQLException
    {
        this(directory, FILENAME);
    }

    public Metadata(File directory, boolean autocreate)
            throws SQLException
    {
        this(directory, FILENAME, autocreate);
    }

    //default auto-create is protected until it is not the default
     Metadata(final File directory, final String databaseFile)
            throws SQLException
    {
        this(directory, databaseFile, true);
    }

    public Metadata(final File directory, final String databaseFile, final boolean autoCreate)
            throws SQLException
    {
        if (!loadedSQLite) {
            LOG.error("SQLite driver is unavailable, not updating metadata");
        }


        final File dbFile = new File(directory, databaseFile);

        if(!autoCreate && !Files.exists(dbFile.toPath()))
        {
            throw new SQLException(String.format("Spool database [%s] does not exist", dbFile.getAbsolutePath()));
        }

        final String jdbcURL = "jdbc:sqlite:" + dbFile;
        conn = DriverManager.getConnection(jdbcURL);

        // make sure the 'hitspool' table exists
        createTable(conn);

        // prepare the standard INSERT statement
        final String isql =
            "replace into hitspool(filename, start_tick, stop_tick)" +
            " values (?,?,?)";
        insertStmt = conn.prepareStatement(isql);

        // prepare the standard UPDATE statement
        final String usql = "update hitspool set stop_tick=? where filename=?";
        updateStmt = conn.prepareStatement(usql);

        // prepare the standard range QUERY statement
        final String rqsql = "select * from hitspool" +
                " where start_tick <= ?" +
                " and stop_tick >= ?" +
                " order by start_tick asc";
        rangeQueryStmt = conn.prepareStatement(rqsql);

        // prepare the standard content QUERY statement
        final String cqsql = "select * from hitspool" +
                " order by start_tick asc";
        contentQueryStmt = conn.prepareStatement(cqsql);
    }

    public synchronized void close()
    {
        // don't bother if we never loaded the SQLite driver
        if (!loadedSQLite) {
            return;
        }

        try {
            insertStmt.close();
        } catch (SQLException se) {
            LOG.error("Failed to close insert statement", se);
        }

        try {
            updateStmt.close();
        } catch (SQLException se) {
            LOG.error("Failed to close update statement", se);
        }

        try {
            rangeQueryStmt.close();
        } catch (SQLException se) {
            LOG.error("Failed to close select statement", se);
        }

        try {
            contentQueryStmt.close();
        } catch (SQLException se) {
            LOG.error("Failed to close select statement", se);
        }

        try {
            conn.close();
        } catch (SQLException se) {
            LOG.error("Failed to close connection", se);
        }
    }

    private static void createTable(Connection conn)
        throws SQLException
    {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("create table if not exists hitspool(" +
                           "filename text primary key not null," +
                           "start_tick integer, stop_tick integer)");
        stmt.close();
    }

    public void createConfigTable()
            throws SQLException
    {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(CONFIG_TABLE_CREATE_SQL);
        stmt.close();
    }

    public boolean hasConfigTable() throws SQLException
    {
        PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM sqlite_master WHERE type=? AND name=?");
        ps.setString(1,"table");
        ps.setString(2,"hitspool_cfg");

        ResultSet rs = ps.executeQuery();
        rs.next();
        boolean found = (rs.getInt(1) == 1);

        rs.close();
        ps.close();

        return found;

    }

    public void setConfig(long interval, int numFiles) throws SQLException
    {
        PreparedStatement ps = conn.prepareStatement(CONFIG_TABLE_INSERT_SQL);
        try {
            ps.setString(1, CONFIG_RECORD_KEY);
            ps.setLong(2, interval);
            ps.setInt(3, numFiles);
            ps.executeUpdate();
        } catch (SQLException se) {
            LOG.error("Cannot insert config (interval: " + interval +
                    " num_files: " + numFiles + ")", se);
        }
        finally {
            if(ps != null)
            {
                try
                {
                    ps.close();
                }
                catch (SQLException se)
                {
                    LOG.error("Error closing PreparedStatement", se);
                }
            }
        }
    }

    public ConfigRecord getConfig() throws SQLException
    {
        return this.getConfig(CONFIG_RECORD_KEY);
    }

    private ConfigRecord getConfig(final String id) throws SQLException
    {
        PreparedStatement ps = conn.prepareStatement(CONFIG_TABLE_QUERY_SQL);
        ResultSet resultSet = null;
            try
            {
                ps.setString(1, id);
                resultSet = ps.executeQuery();

                if(resultSet.next())
                {
                    return new ConfigRecord(resultSet.getString("id"),
                            resultSet.getLong("interval"),
                            resultSet.getInt("num_files"));
                }
                else
                {
                    return null;
                }
            }
            finally
            {
                if(resultSet != null)
                {
                    try
                    {
                        resultSet.close();
                    }
                    catch (SQLException se)
                    {
                        LOG.error("Error closing ResultSet", se);
                    }
                }
                if(ps != null)
                {
                    try
                    {
                        ps.close();
                    }
                    catch (SQLException se)
                    {
                        LOG.error("Error closing PreparedStatement", se);
                    }
                }
            }
    }

    public synchronized void updateStop(String filename, long stop_tick)
    {
        // don't bother if we never loaded the SQLite driver
        if (!loadedSQLite) {
            return;
        }

        synchronized (updateStmt) {
            try {
                updateStmt.setLong(1, stop_tick);
                updateStmt.setString(2, filename);
                int num = updateStmt.executeUpdate();
                if (num <= 0) {
                    final String errMsg =
                        String.format("Did not update filename %s stop %d",
                                      filename, stop_tick);
                    LOG.error(errMsg);
                }
            } catch (SQLException se) {
                LOG.error("Cannot update metadata (filename " + filename +
                          " stop " + stop_tick + ")", se);
            }
        }
    }

    public synchronized void write(String filename, long start_tick,
                                   long interval)
    {
        // don't bother if we never loaded the SQLite driver
        if (!loadedSQLite) {
            return;
        }

        synchronized (insertStmt) {
            final long stop_tick = start_tick + (interval - 1);
            try {
                insertStmt.setString(1, filename);
                insertStmt.setLong(2, start_tick);
                insertStmt.setLong(3, stop_tick);
                int num = insertStmt.executeUpdate();
                if (num <= 0) {
                    final String errMsg =
                        String.format("Did not insert filename %s [%d-%d]",
                                      filename, start_tick, stop_tick);
                    LOG.error(errMsg);
                }
            } catch (SQLException se) {
                LOG.error("Cannot insert metadata (filename " + filename +
                          " [" + start_tick + "-" + stop_tick + "])", se);
            }
        }
    }

    /**
     * @param from_tick The start of the data range.
     * @param to_tick The end of the data range.
     * @return An ordered list (time ascending) of files that enclose
     *         the range.
     * @throws IOException An error accessing the database.
     */
    public List<HitSpoolRecord> listRecords(long from_tick, long to_tick) throws IOException
    {
        synchronized (rangeQueryStmt) {
            ResultSet resultSet = null;
            try
            {
                rangeQueryStmt.setLong(1, to_tick);
                rangeQueryStmt.setLong(2, from_tick);
                resultSet = rangeQueryStmt.executeQuery();

                List<HitSpoolRecord> records = new LinkedList<>();
                while(resultSet.next())
                {
                    records.add( new HitSpoolRecord(resultSet.getString("filename"),
                            resultSet.getLong("start_tick"),
                            resultSet.getLong("stop_tick")) );
                }

                return records;

            }
            catch (SQLException se) {
                throw new IOException("Cannot query metadata" +
                        " [" + from_tick + "-" + to_tick + "])", se);
            }
            finally
            {
                if(resultSet != null)
                {
                    try
                    {
                        resultSet.close();
                    }
                    catch (SQLException se)
                    {
                        LOG.error("Error closing ResultSet", se);
                    }
                }
            }
        }
    }

    /**
     * @return All records in the database in ascending time order.
     * @throws IOException
     */
    public List<HitSpoolRecord> listRecords() throws IOException
    {
        synchronized (contentQueryStmt) {
            ResultSet resultSet = null;
            try
            {
                resultSet = contentQueryStmt.executeQuery();

                List<HitSpoolRecord> spans = new LinkedList<HitSpoolRecord>();

                while(resultSet.next())
                {
                    spans.add( new HitSpoolRecord(resultSet.getString("filename"),
                            resultSet.getLong("start_tick"),
                            resultSet.getLong("stop_tick")) );
                }
                return spans;
            }
            catch (SQLException se) {
                throw new IOException("Cannot query metadata", se);
            }
            finally
            {
                if(resultSet != null)
                {
                    try
                    {
                        resultSet.close();
                    }
                    catch (SQLException se)
                    {
                        LOG.error("Error closing ResultSet", se);
                    }
                }
            }
        }
    }

    /**
     * Models a spool file record.
     */
    public static class HitSpoolRecord
    {
        public final String filename;
        public final long startTick;
        public final long stopTick;

        public HitSpoolRecord(final String filename,
                              final long startTick, final long stopTick)
        {
            this.filename = filename;
            this.startTick = startTick;
            this.stopTick = stopTick;
        }
    }

    /**
     * Models a config record.
     */
    public static class ConfigRecord
    {
        public final String id;
        public final long interval;
        public final long num_files;


        public ConfigRecord(final String id, final long interval, final long num_files)
        {
            this.id = id;
            this.interval = interval;
            this.num_files = num_files;
        }
    }

}
