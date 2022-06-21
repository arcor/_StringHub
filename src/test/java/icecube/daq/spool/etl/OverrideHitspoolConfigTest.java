package icecube.daq.spool.etl;

import icecube.daq.sender.SenderSubsystem;
import icecube.daq.spool.Metadata;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests OverrideHitspoolConfig.java
 *
 * Implemented as a Suite of test classes that provide coverage to OverrideHitspoolConfig.java
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({OverrideHitspoolConfigTest.TestStandardSPSDatabase.class,
        OverrideHitspoolConfigTest.TestOverriddenDataBase.class,
        OverrideHitspoolConfigTest.TestEmptyDatabase.class
})
public class OverrideHitspoolConfigTest
{

    // The current SPS configuration
    public static final double SPS_STANDARD_INTERVAL = OverrideHitspoolConfig.REQUESTED_INTERVAL;
    public static final int SPS_STANDARD_NUM_FILES = OverrideHitspoolConfig.REQUESTED_NUM_FILES;

    // The override configuration
    public static final double OVERRIDE_INTERVAL = OverrideHitspoolConfig.OVERRIDE_INTERVAL;
    public static final int OVERRIDE_NUM_FILES = OverrideHitspoolConfig.OVERRIDE_NUM_FILES;

    // an unplanned configuration
    public static final double UNPLANNED_INTERVAL = OverrideHitspoolConfig.OVERRIDE_INTERVAL;
    public static final int UNPLANNED_NUM_FILES = OverrideHitspoolConfig.OVERRIDE_NUM_FILES;


    // The base of all test classes
    private static class BaseDatabaseInstanceTest extends HitspoolTestData.Client
    {
        // config instances, populated in setup when the test database path is known.
        SenderSubsystem.HitSpoolConfig SPS_STANDARD;
        SenderSubsystem.HitSpoolConfig SPS_2021_OVERRIDE;
        SenderSubsystem.HitSpoolConfig SPS_UNPLANNED;

        public BaseDatabaseInstanceTest(HitspoolTestData.TestSpool testData)
        {
            super(testData);
        }

        @BeforeClass
        public static void setupLogging()
        {
            BasicConfigurator.resetConfiguration();
            // exercise logging calls, but output to nowhere
//        BasicConfigurator.configure(new NullAppender());
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.ALL);
        }

        @AfterClass
        public static void tearDownLogging()
        {
            BasicConfigurator.resetConfiguration();
        }

        @Before
        public void setupConfigs() throws IOException
        {
            SPS_STANDARD =new SenderSubsystem.HitSpoolConfig(super.parentDir.toFile(),
                    SPS_STANDARD_INTERVAL, SPS_STANDARD_NUM_FILES);

            SPS_2021_OVERRIDE =new SenderSubsystem.HitSpoolConfig(super.parentDir.toFile(),
                    OVERRIDE_INTERVAL, OVERRIDE_NUM_FILES);

            SPS_UNPLANNED =new SenderSubsystem.HitSpoolConfig(super.parentDir.toFile(),
                    UNPLANNED_INTERVAL, UNPLANNED_NUM_FILES);
        }

        @Test
        public void preserveUnplannedConfigTest() throws IOException, SQLException
        {
            // In all cases, If an unplanned configuration was requested, it should be preserved

            SenderSubsystem.HitSpoolConfig result = OverrideHitspoolConfig.overrideHook(SPS_UNPLANNED);

            assertEquals(SPS_UNPLANNED.directory, result.directory);
            assertEquals(SPS_UNPLANNED.fileInterval, result.fileInterval, 0.0d);
            assertEquals(SPS_UNPLANNED.numFiles, result.numFiles);
        }

    }

    // Tests run with the initial state a standard (15, 36000) hitspool database
    public static class TestStandardSPSDatabase extends BaseDatabaseInstanceTest
    {
        public TestStandardSPSDatabase()
        {
            super(HitspoolTestData.TestSpool.SPS_15E10_36000_SPOOL_DB_ONLY);
        }

        @Test
        public void preserveStandardConfigTest() throws IOException, SQLException
        {
            // If a hitspool exists in the standard configuration it should be preserved

            SenderSubsystem.HitSpoolConfig result = OverrideHitspoolConfig.overrideHook(SPS_STANDARD);

            assertEquals( SPS_STANDARD.directory, result.directory);
            assertEquals(SPS_STANDARD.fileInterval, result.fileInterval, 0.0d);
            assertEquals(SPS_STANDARD.numFiles, result.numFiles);
        }

    }



    // Tests run with the initial state an override (15, 72000) hitspool database
    public static class TestOverriddenDataBase extends BaseDatabaseInstanceTest
    {
        public TestOverriddenDataBase()
        {
            super(HitspoolTestData.TestSpool._15E10_72000_SPOOL_DB_ONLY);
        }

        @Test
        public void preserveOverriddenConfigTest() throws IOException, SQLException
        {
            // If a hitspool exists with the override flag record, it should be preserved

            SenderSubsystem.HitSpoolConfig result = OverrideHitspoolConfig.overrideHook(SPS_STANDARD);

            assertEquals(SPS_2021_OVERRIDE.directory, result.directory);
            assertEquals(SPS_2021_OVERRIDE.fileInterval, result.fileInterval, 0.0d);
            assertEquals(SPS_2021_OVERRIDE.numFiles, result.numFiles);
        }

    }

    // Tests run with the initial state with no hitspool database
    public static class TestEmptyDatabase extends BaseDatabaseInstanceTest
    {
        public TestEmptyDatabase()
        {
            super(HitspoolTestData.TestSpool.EMPTY_SPOOL);
        }


        @Test
        public void overrideStandardConfigTest() throws IOException, SQLException
        {
            // If a hitspool does not exists the standard configuration should be overridden

            SenderSubsystem.HitSpoolConfig result = OverrideHitspoolConfig.overrideHook(SPS_STANDARD);

            assertEquals(SPS_2021_OVERRIDE.directory, result.directory);
            assertEquals(SPS_2021_OVERRIDE.fileInterval, result.fileInterval, 0.0d);
            assertEquals(SPS_2021_OVERRIDE.numFiles, result.numFiles);


            // furthermore, the new database should have the override flag set
            Metadata db = new Metadata(super.databaseDir.toFile(), false);
            assertTrue(db.hasConfigTable());

            Metadata.ConfigRecord cfgRec = db.getConfig();
            assertEquals(Metadata.CONFIG_RECORD_KEY, cfgRec.id);
            assertEquals(SPS_2021_OVERRIDE.fileInterval * 1E10, cfgRec.interval, 0.0);
            assertEquals(SPS_2021_OVERRIDE.numFiles, cfgRec.num_files);

            db.close();
        }

    }

}
