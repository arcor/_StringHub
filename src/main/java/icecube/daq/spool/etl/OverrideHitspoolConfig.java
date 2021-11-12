package icecube.daq.spool.etl;

import icecube.daq.sender.SenderSubsystem;
import icecube.daq.spool.Metadata;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Hook for overriding the requested hitspool configuration based on the state of the hub.
 *
 * This will be used during the 2021 OS upgrade to migrate the hitspool configuration from
 * 36000 file look-back to 72000 files precisely at the moment after installing the new OS
 * at which the 6-day look-back was deleted.
 *
 * The general behavior is that if the hitspool database is empty, the requested configuration
 * will be replaced with the desired override and a flag record is written to the database.
 *
 * the presence of the flag record in the database will preserve the override configuration for
 * this hitspool instance going forward.
 *
 */
public class OverrideHitspoolConfig
{

    // The current SPS configuration
    public static final double REQUESTED_INTERVAL = 15.0;
    public static final int REQUESTED_NUM_FILES = 36000;

    // The desired configuration, but apply only if no hitspool exists,
    // of hitspool has override flag record present
    public static final double OVERRIDE_INTERVAL = 15.0;
    public static final int OVERRIDE_NUM_FILES = 72000;

    public static Logger logger = Logger.getLogger("2021_OVERRIDE_HITSPOOL");

    private static int hubId = -1;
    static
    {
        try {
            hubId = Integer.getInteger("icecube.daq.stringhub.componentId");
        } catch (Exception e) {
            // OK for unit tests
        }
    }

    public static SenderSubsystem.HitSpoolConfig overrideHook(final SenderSubsystem.HitSpoolConfig requested)
            throws IOException, SQLException
    {
        if( requested.fileInterval != REQUESTED_INTERVAL ||
            requested.numFiles != REQUESTED_NUM_FILES)
        {
            String msg = String.format("[hub %d] Not applying hitspool cfg override to numFiles: %d, fileInterval: %f",
                    hubId, requested.numFiles, requested.fileInterval);
            logger.warn(msg);
            return requested;
        }
        else
        {
            SenderSubsystem.HitSpoolConfig override = applyOverride(requested);

            if(override.fileInterval == REQUESTED_INTERVAL &&
                    override.numFiles == REQUESTED_NUM_FILES)
            {
                String msg = String.format("[hub %d] Retaining hitspool cfg numFiles: %d, fileInterval: %f",
                        hubId, requested.numFiles, requested.fileInterval);
                logger.warn(msg);
            }
            else
            {
                String msg = String.format("[hub %d] Override hitspool cfg numFiles: %d, fileInterval: %f " +
                                " with numFiles: %d, fileInterval: %f", hubId, requested.numFiles, requested.fileInterval,
                        override.numFiles, override.fileInterval);
                logger.warn(msg);
            }

            return override;

        }

    }

    private static SenderSubsystem.HitSpoolConfig applyOverride(final SenderSubsystem.HitSpoolConfig requested)
            throws SQLException
    {

        // Note: Hitspool applies "/hitspool" to the configured directory:
        //
        // requested.directory = "/mnt/data/pdaqlocal"
        // database_path       = "/mnt/data/pdaqlocal/hitspool/hitspool.db"
        Path databaseDir = requested.directory.toPath().resolve("hitspool");
        Path databaseFile = databaseDir.resolve(Metadata.FILENAME);

        // if we override, this is the config
        final SenderSubsystem.HitSpoolConfig OVERRIDE_CONFIG =
                new SenderSubsystem.HitSpoolConfig(requested.directory, OVERRIDE_INTERVAL, OVERRIDE_NUM_FILES);

        if(!Files.exists(databaseFile)){
            logger.warn(String.format("[hub %d] file %s does not exist", hubId, databaseFile.toString()));

            // double check, this is important
            try {
                new Metadata(databaseDir.toFile(), false);
                throw new Error("database file exists at " + databaseFile.toString());
            } catch (SQLException throwables) {
                // desired, verifies that there is no hitspool database
            }

            // new instance of hitspool, override the config and set the flag
            logger.warn(String.format("[hub %d] creating hitspool database at %s", hubId, databaseFile.toString()));

            // initialize the hitspool database with the override config flag record
            databaseDir.toFile().mkdirs();
            Metadata db = new Metadata(databaseDir.toFile(), true);
            db.createConfigTable();
            db.setConfig( (long)(OVERRIDE_CONFIG.fileInterval * 1E10),
                    OVERRIDE_CONFIG.numFiles);

            db.close();

            return OVERRIDE_CONFIG;
        }
        else
        {
            // database exists passively search for the flag and use it to decide
            // override
            logger.warn(String.format("[hub %d] database exists at %s, querying for flag", hubId, databaseFile.toString()));


            Metadata db = new Metadata(databaseDir.toFile(), true);

            if(!db.hasConfigTable())
            {
                // no flag present
                return requested;
            }
            else {
                Metadata.ConfigRecord config = db.getConfig();
                if (config != null &&
                        (config.interval / 1E10 == OVERRIDE_CONFIG.fileInterval) &&
                        (config.num_files == OVERRIDE_CONFIG.numFiles)) {
                    return OVERRIDE_CONFIG;
                } else {
                    // this is an unplanned state, bail out
                    String msg = config == null ?
                            String.format("[hub %d] Unplanned state, config table exists, but no config record exists", hubId)
                            :
                            String.format("\"[hub %d] Unplanned state, config (%s, %d, %d) not expected",
                                    hubId, config.id, config.interval, config.num_files);
                    throw new Error(msg);
                }
            }

        }


    }



}
