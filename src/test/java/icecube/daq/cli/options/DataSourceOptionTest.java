package icecube.daq.cli.options;

import icecube.daq.cli.CLITestData;
import icecube.daq.cli.stream.DataSource;
import icecube.daq.cli.stream.FileScheme;
import icecube.daq.cli.stream.RecordType;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Tests DataSourceOption.java
 *
 * Implemented as a Suite of test classes to encapsulate test data management via CLITestData.java
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        DataSourceOptionTest.TestPdaqHitspoolFile.class,
        DataSourceOptionTest.TestPdaqHitspoolDir.class,
        DataSourceOptionTest.TestPdaqMoni.class,
        DataSourceOptionTest.TestPdaqSN.class,
        DataSourceOptionTest.TestPdaqTCAL.class,
        DataSourceOptionTest.Test2ndbuildMoni.class,
        DataSourceOptionTest.Test2ndbuildTcal.class,
        DataSourceOptionTest.Test2ndbuildSN.class,
        DataSourceOptionTest.TestPhysicsV5.class})
public class DataSourceOptionTest
{

    @CommandLine.Command(name = "TestCommand", description = "to test DataSourceOption",
            mixinStandardHelpOptions = true)
    static class TestCommand
    {
        @CommandLine.Mixin
        DataSourceOption dataSourceOption;
    }

    static class ExpectedDataSourceDetails
    {
        final long expectedByteCount;
        final long expectedRecordCount;
        final long firstUtc;
        final long lastUtc;

        ExpectedDataSourceDetails(long expectedByteCount, long expectedRecordCount, long firstUtc, long lastUtc)
        {
            this.expectedByteCount = expectedByteCount;
            this.expectedRecordCount = expectedRecordCount;
            this.firstUtc = firstUtc;
            this.lastUtc = lastUtc;
        }
    }

    static class BaseDataCaseTest extends CLITestData.Client
    {
        final FileScheme expectedFileScheme;
        final RecordType expectedRecordType;
        final ExpectedDataSourceDetails expectedDetails;

        CLITestData.DataResource testData;

        public BaseDataCaseTest(CLITestData.DataResource testData, FileScheme expectedFileScheme,
                                RecordType expectedRecordType, ExpectedDataSourceDetails expectedDetails)
        {
            super(testData);
            this.expectedFileScheme = expectedFileScheme;
            this.expectedRecordType = expectedRecordType;
            this.expectedDetails = expectedDetails;
            this.testData = testData;
        }

        @Override
        @Before
        public void setup() throws IOException
        {
            BasicConfigurator.resetConfiguration();
            BasicConfigurator.configure();
            super.setup();
        }

        @Test
        public void testDataSouceResolution() throws IOException
        {
            //
            // test automatic discovery of FileScheme and RecordType
            // as well as user-supplied
            //

            String testDataPath = null;

            if(testData instanceof CLITestData.DataFile)
            {
                CLITestData.DataFile narrow = (CLITestData.DataFile) testData;
                testDataPath = dataDirectory.resolve(narrow.getFilename()).toString();
            }
            else if(testData instanceof CLITestData.DataFileSet)
            {
                testDataPath = dataDirectory.toString();
            }
            else {
                throw new Error("Miscode");
            }

            verify(new String[]{"--source", testDataPath});
            verify(new String[]{"--source", testDataPath, "--file-scheme", "discover"});
            verify(new String[]{"--source", testDataPath, "--file-scheme", "discover", "--record-type", "discover"});
            verify(new String[]{"--source", testDataPath, "--file-scheme", expectedFileScheme.keyword});
            verify(new String[]{"--source", testDataPath, "--record-type", expectedRecordType.keyword});
            verify(new String[]{"--source", testDataPath, "--file-scheme", expectedFileScheme.keyword, "--record-type", expectedRecordType.keyword});


        }

        void verify(String[] commandArgs) throws IOException
        {
            TestCommand command = new TestCommand();
            CommandLine cmd = new CommandLine(command);
            CommandLine.ParseResult parseResult = cmd.parseArgs(commandArgs);



            DataSource ds = command.dataSourceOption.getDataSource();


            assertEquals(expectedFileScheme, command.dataSourceOption.resolvedFileScheme);
            assertEquals(expectedRecordType, command.dataSourceOption.resolvedRecordType);
            assertEquals(expectedRecordType, ds.getRecordType());


            String expectedDescription = "";
            if(testData instanceof CLITestData.DataFile)
            {
                CLITestData.DataFile narrow = (CLITestData.DataFile) testData;
                expectedDescription = String.format("%s:%s",expectedRecordType.keyword, 
                        dataDirectory.resolve(narrow.getFilename()).toString());
            }
            else if(testData instanceof CLITestData.DataFileSet)
            {
                expectedDescription = String.format("%s:%s:%s", expectedFileScheme.keyword,
                        expectedRecordType.keyword, dataDirectory.toString());
            }
            else
            {
                fail("Unknown test data type");
            }
            assertEquals(expectedDescription, ds.describe());

            //capture details of the stream
            class Capture implements Consumer<ByteBuffer>
            {
                long firstUTC = Long.MAX_VALUE;
                long lastUTC = Long.MIN_VALUE;
                boolean first=true;

                long byteCount = 0;

                @Override
                public void accept(ByteBuffer byteBuffer)
                {
                    long utc = ds.getRecordType().rr.getOrderingField().value(byteBuffer, 0);
                    if(first)
                    {
                        firstUTC = utc;
                        first = false;
                    }
                    lastUTC = utc;

                    byteCount += byteBuffer.remaining();
                }
            }

            Capture capture = new Capture();

            long recordCount = ds.stream().peek(capture).count();

//            System.out.println("capture.byteCount = " + capture.byteCount);
//            System.out.println("recordCount = " + recordCount);
//            System.out.println("capture.firstUTC = " + capture.firstUTC);
//            System.out.println("capture.lastUTC = " + capture.lastUTC);

            assertEquals(expectedDetails.expectedByteCount, capture.byteCount);
            assertEquals(expectedDetails.expectedRecordCount, recordCount);
            assertEquals(expectedDetails.firstUtc, capture.firstUTC);
            assertEquals(expectedDetails.lastUtc, capture.lastUTC);
        }


    }


    public static class TestPdaqHitspoolFile extends BaseDataCaseTest
    {

        public TestPdaqHitspoolFile()
        {
            super(CLITestData.DataFile.PDAQ_HITSPOOL_DATA,
                    FileScheme.NONE, RecordType.PDAQ_DELTA_COMPRESSED_HIT,
                    new ExpectedDataSourceDetails(1010005, 17648, 279144015145580758L, 279144165096718566L));
        }

    }

    public static class TestPdaqHitspoolDir extends BaseDataCaseTest
    {

        public TestPdaqHitspoolDir()
        {
            super(CLITestData.DataFileSet.PDAQ_HITSPOOL_DATA,
                    FileScheme.HITSPOOL, RecordType.PDAQ_DELTA_COMPRESSED_HIT,
                    new ExpectedDataSourceDetails(3023572, 52560, 279144015145580758L, 279144465138239346L));
        }

    }

    public static class TestPdaqMoni extends BaseDataCaseTest
    {

        public TestPdaqMoni()
        {
            super(CLITestData.DataFile.PDAQ_MONI_DATA,
                    FileScheme.NONE, RecordType.PDAQ_MONI,
                    new ExpectedDataSourceDetails(11196, 121, 278925231796306254L, 278925242333786365L));
        }

    }

    public static class TestPdaqSN extends BaseDataCaseTest
    {

        public TestPdaqSN()
        {
            super(CLITestData.DataFile.PDAQ_SN_DATA,
                    FileScheme.NONE, RecordType.PDAQ_SN,
                    new ExpectedDataSourceDetails(10100, 14, 278926065455186262L, 278926065539894072L));
        }

    }

    public static class TestPdaqTCAL extends BaseDataCaseTest
    {

        public TestPdaqTCAL()
        {
            super(CLITestData.DataFile.PDAQ_TCAL_DATA,
                    FileScheme.NONE, RecordType.PDAQ_TCAL,
                    new ExpectedDataSourceDetails(10380,30, 278926077234975768L, 278926123544090079L));
        }

    }

    public static class Test2ndbuildMoni extends BaseDataCaseTest
    {

        public Test2ndbuildMoni()
        {
            super(CLITestData.DataFileSet.SECONDBUILD_MONI_DATA,
                    FileScheme.SECONDBUILD_MONI, RecordType.SECONDBUILD_MONI,
                    new ExpectedDataSourceDetails(30686, 350, 288644928705268484L, 288645291289052715L));
        }

    }


    public static class Test2ndbuildTcal extends BaseDataCaseTest
    {

        public Test2ndbuildTcal()
        {
            super(CLITestData.DataFileSet.SECONDBUILD_TCAL_DATA,
                    FileScheme.SECONDBUILD_TCAL, RecordType.SECONDBUILD_TCAL,
                    new ExpectedDataSourceDetails(341042, 1009, 1222848464672047L, 1258030150602053L));
        }

    }

    public static class Test2ndbuildSN extends BaseDataCaseTest
    {

        public Test2ndbuildSN()
        {
            super(CLITestData.DataFileSet.SECONDBUILD_SN_DATA,
                    FileScheme.SECONDBUILD_SN, RecordType.SECONDBUILD_SN,
                    new ExpectedDataSourceDetails(332904, 482, 281955000394464982L, 281956724912838517L));
        }

    }

    public static class TestPhysicsV5 extends BaseDataCaseTest
    {

        public TestPhysicsV5()
        {
            super(CLITestData.DataFileSet.PHYSICS_V5_DATA,
                    FileScheme.EVBUILD_PHYSICS, RecordType.EVENT_V5,
                    new ExpectedDataSourceDetails(61236, 20, 135450749844161206L, 135450769286165493L));
        }

    }


}