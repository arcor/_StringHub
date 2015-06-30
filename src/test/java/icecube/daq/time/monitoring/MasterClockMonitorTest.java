package icecube.daq.time.monitoring;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static icecube.daq.time.monitoring.TestUtilities.*;

/**
 * Tests icecube.daq.time.monitoring.MasterClockMonitor
 */
public class MasterClockMonitorTest
{


    public static final int ZERO_DELTA = 0;


    @BeforeClass
    public static void setupLogging()
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @AfterClass
    public static void tearDownClass()
    {
        BasicConfigurator.resetConfiguration();
    }

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown()
    {
    }


    @Test
    public void testOffsetCalculation()
    {
        //
        // Tests basic offset calculation with a single tcal/gps/ntp
        //

        int card = 3;
        MasterClockMonitor subject = new MasterClockMonitor(card);

        //test initial state
        assertEquals("Intial offset value", 0,
                subject.getMasterClockOffsetMillis(), 0);


        //ntp/gps before tcal
        PointInTime pit = new PointInTime(5,19,33,27);
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, 3000),
                generateGPSSnapshot(card, pit.GPSString, (byte)32, 12345));

        assertEquals("Intial offset value", 0,
                subject.getMasterClockOffsetMillis(), 0);


        // issue a tcal, ntp, gps with a 187 millisecond offset
        long nanoBase = 14837324743L;
        long dorBase = 11333848;
        pit = new PointInTime(11,3,59,3);
        subject.process(generateTCALMeasurement(dorBase, nanoBase, card, "31A"));
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase - millisAsNano(5)),
                generateGPSSnapshot(card, pit.GPSString, (byte)32, dorBase - millisAsDor(5 + 187)));

        assertEquals("Expected a 187.0 offset", -187, subject.getMasterClockOffsetMillis(), ZERO_DELTA);


        //issue new gps/ntp with offset of 1 hour
        long ONE_HOUR_MS = 1 * 60 * 60 * 1000;
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase - millisAsNano(987)),
                generateGPSSnapshot(card, pit.GPSString, (byte)32, dorBase + millisAsDor(ONE_HOUR_MS-987)));

        assertEquals("Expected a 187.0 offset", ONE_HOUR_MS, subject.getMasterClockOffsetMillis(), ZERO_DELTA);


        //issue new gps/ntp with 0 offset
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase - millisAsNano(123123)),
                generateGPSSnapshot(card, pit.GPSString, (byte)32, dorBase - millisAsDor(123123)));

        assertEquals("Expected a zero offset", 0, subject.getMasterClockOffsetMillis(), ZERO_DELTA);
    }

    @Test
    public void testAveraging()
    {
        //
        // Test the MasterClockMonitor averaging of per-dom offsets based
        // on available tcals.
        //

        final int card = 3;
        final MasterClockMonitor subject = new MasterClockMonitor(card);

        final long nanoBase = 14837324743L;
        long dorBase = 11333848;

        final long[] jitter = {
                1000000,2000000,3000000,4500000,1050000,
                8973000,  2005000, 13050000, 17000020, 23004400,
                900400, 15463211, 19000000
        };
        final String[] doms = {
                "00A",
                "01B",
                "13A",
                "13B",
                "60A",
                "61B",
                "70A",
                "71A",
                "72A",
                "73A",
                "74A",
                "75A",
                "31A"
        };

        long sum = 0;
        for (int i = 0; i < jitter.length; i++)
        {
            sum += jitter[i];

        }
        final double monotonicJitterAvgMillis = (sum/1000000d)/jitter.length;
        final double dorJitterAvgIn = (sum/20000d)/jitter.length;

        //issue tcals with jitter on the monotonic time and check that
        // the offset is averaged
        PointInTime pit = new PointInTime(7,19,33,59);

        for (int i = 0; i < doms.length; i++)
        {
            subject.process(generateTCALMeasurement(dorBase, nanoBase+jitter[i], card, doms[i]));
        }
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase),
                generateGPSSnapshot(card, pit.GPSString, (byte) 32, dorBase));

        assertEquals("Offset not averaged", monotonicJitterAvgMillis,
                subject.getMasterClockOffsetMillis(), 0.0001);

        //issue tcals with jitter on the dor time and check that
        // the offset is averaged
        pit = new PointInTime(155,23,1,2);

        for (int i = 0; i < doms.length; i++)
        {
            subject.process(generateTCALMeasurement(dorBase-jitter[i], nanoBase, card, doms[i]));
        }
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase),
                generateGPSSnapshot(card, pit.GPSString, (byte) 32, dorBase));

        assertEquals("Offset not averaged", dorJitterAvgIn,
                subject.getMasterClockOffsetMillis(), 0.0001);


        //issue tcals without jitter
        for (int i = 0; i < doms.length; i++)
        {
            subject.process(generateTCALMeasurement(dorBase, nanoBase, card, doms[i]));
        }
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase),
                generateGPSSnapshot(card, pit.GPSString, (byte) 32, dorBase));

        assertEquals("Offset not averaged", 0,
                subject.getMasterClockOffsetMillis(), ZERO_DELTA);
    }

    @Test
    public void testDays()
    {
        //
        // Test every day of the year
        //

        int card = 3;
        MasterClockMonitor subject = new MasterClockMonitor(card);

        long nanoBase = -14837324743L;
        long dorBase = 11333848;
        for( int day=1; day< 365; day++)
        {
            PointInTime pit = new PointInTime(day,3,15,55);
            subject.process(generateTCALMeasurement(dorBase, nanoBase, card, "31A"));
            subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase ),
                    generateGPSSnapshot(card, pit.GPSString, (byte)32, dorBase - millisAsDor(123)));

            assertEquals("Intial offset value", -123,
                    subject.getMasterClockOffsetMillis(), ZERO_DELTA);
        }
    }


    @Test
    public void testDORClockBoundary()
    {
        //
        // test with DOR clock at max
        //

        //2^48-1
        long MAX_DOR_TICK = 281474976710655L;

        int card = 7;
        MasterClockMonitor subject = new MasterClockMonitor(card);


        // issue a tcal, ntp, gps with a 187 millisecond offset
        long nanoBase = 14837324743L;
        long dorBase = MAX_DOR_TICK - millisAsDor(20);

        PointInTime pit = new PointInTime(1,1,59,3);
        subject.process(generateTCALMeasurement(dorBase, nanoBase, card, "31A"));
        subject.process(generateNTPMeasurement(pit.epochTimeMillis, nanoBase),
                generateGPSSnapshot(card, pit.GPSString, (byte)32, dorBase + millisAsDor(20)));

        assertEquals("Wrong Offset", 20, subject.getMasterClockOffsetMillis(), ZERO_DELTA);
    }




}