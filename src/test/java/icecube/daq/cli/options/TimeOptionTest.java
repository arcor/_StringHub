package icecube.daq.cli.options;

import icecube.daq.cli.options.TimeOption.*;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.*;

public class TimeOptionTest
{

    @Test
    public void testDurationConversion()
    {
        DurationParameterConverter subject = new DurationParameterConverter();

        class Case
        {
            final String input;
            final long expectedVal;
            final String expectedRoundTrip;

            Case(String input, long expectedVal, String expectedRoundTrip)
            {
                this.input = input;
                this.expectedVal = expectedVal;
                this.expectedRoundTrip = expectedRoundTrip;
            }
        }
        Case[] cases = new Case[]
                {
                        new Case("0", 0, "0"),
                        new Case("1", 1, "0.1n"),
                        new Case("10", 10, "1.0n"),
                        new Case("11", 11, "1.1n"),
                        new Case("1n", 10, "1.0n"),
                        new Case("1N", 10, "1.0n"),
                        new Case("1.3n", 13, "1.3n"),
                        new Case("999999999n", 9999999990L, "999999999.0n"),
                        new Case("1000000000n", 10000000000L, "1s"),
                        new Case("1000000000.1n", 10000000001L, "1s0.1n"),
                        new Case("1s", 10000000000L, "1s"),
                        new Case("1S", 10000000000L, "1s"),
                        new Case("60s", 600000000000L, "1m"),
                        new Case("61s", 610000000000L, "1m1s"),
                        new Case("1m", 600000000000L, "1m"),
                        new Case("1M", 600000000000L, "1m"),
                        new Case("60m", 36000000000000L, "1h"),
                        new Case("1h", 36000000000000L, "1h"),
                        new Case("1H", 36000000000000L, "1h"),
                        new Case("24h", 864000000000000L, "1d"),
                        new Case("1d", 864000000000000L, "1d"),
                        new Case("1D", 864000000000000L, "1d"),
                        new Case("365d25H", 316260000000000000L, "366d1h"),

                        new Case("1d4h33m22s", 1028020000000000L, "1d4h33m22s"),

                        new Case("922337203685477580.7n", 9223372036854775807L, "10675d4h46m43s685477580.7n"),

                };

        for(Case c: cases)
        {
//            System.out.printf("case [%s]...%n", c.input);
            TimeDuration convert = subject.convert(c.input);
            assertEquals(c.expectedVal, convert.tenth_nanos);
            assertEquals(c.expectedRoundTrip, convert.toString());

        }


        //####################################
        // test illegal values
        //####################################
        String[] baddies = new String[]
                {
                        null,
                        "",
                        " ",
                        "    ",
                        "0.0",
                        ".0",
                        "3.0",
                        "5.4",
                        "-33m4s",
                        "+33m4s",
                        "1.10n",
                        "922337203685477580.71n",
                        "d",
                        "m",
                        "h",
                        "s",
                        "y",
                        "2h1d"

                };
        for (String bad : baddies) {
            try
            {
                TimeDuration convert = subject.convert(bad);
                fail(String.format("String [%s] should have been rejected but parsed to [[%d],[%s]]",
                        bad, convert.tenth_nanos, convert.toString()));
            }
            catch (CommandLine.TypeConversionException tce)
            {
                //desired
            }
        }

    }


    @Test
    public void testIntervalConversion()
    {
        IntervalParameterConverter subject = new IntervalParameterConverter();

        class Case
        {
            final String input;
            final long expectedFrom;
            final long expectedTo;
            final String expectedRoundTrip;


            Case(String input, long expectedFrom, long expectedTo, String expectedRoundTrip)
            {
                this.input = input;
                this.expectedFrom = expectedFrom;
                this.expectedTo = expectedTo;
                this.expectedRoundTrip = expectedRoundTrip;
            }
        }

        Case[] cases = new Case[]
                {
                        // open ended
                        new Case(".:.", Long.MIN_VALUE, Long.MAX_VALUE,"[-9223372036854775808:9223372036854775807]"),
                        new Case(".:1234", Long.MIN_VALUE, 1234,"[-9223372036854775808:1234]"),
                        new Case("1234:.", 1234,Long.MAX_VALUE ,"[1234:9223372036854775807]"),

                        // relative
                        new Case("0:+4", 0, 4 ,"[0:4]"),
                        new Case("0:+4n", 0, 40 ,"[0:40]"),
                        new Case("0:+4.0n", 0, 40 ,"[0:40]"),
                        new Case("0:+4.7n", 0, 47 ,"[0:47]"),
                        new Case("0:+4s", 0, 40000000000L ,"[0:40000000000]"),
                        new Case("0:+4s0.3n", 0, 40000000003L ,"[0:40000000003]"),

                        new Case("-1:315360000000000000", 315359999999999999L, 315360000000000000L ,"[315359999999999999:315360000000000000]"),
                        new Case("-1s:315360000000000000", 315359990000000000L, 315360000000000000L ,"[315359990000000000:315360000000000000]"),
                        new Case("-3m:315360000000000000", 315358200000000000L, 315360000000000000L ,"[315358200000000000:315360000000000000]"),


                        // absolute
                        new Case("1234:12345", 1234, 12345 ,"[1234:12345]"),
                        new Case("0:315360000000000000", 0, 315360000000000000L ,"[0:315360000000000000]"),

                };

        for(Case c : cases)
        {
//            System.out.printf("CASE [%s]...%n", c.input);
            TimeInterval res = subject.convert(c.input);
            assertEquals(c.expectedFrom, res.from);
            assertEquals(c.expectedTo, res.to);
            assertEquals(c.expectedRoundTrip, res.toString());

        }

        //####################################
        // test illegal values
        //####################################
        String[] baddies = new String[]
                {
                        null,
                        "",
                        " ",
                        "    ",
                        ":",
                        "33:",
                        ":44",
                        ":.",
                        ".:",

                        "2:1",

                        "1:-1",
                        "+1:55",

                };
        for (String bad : baddies) {
            try
            {
//                System.out.printf("BADDIE [%s]...%n", bad);
                TimeInterval convert = subject.convert(bad);
                fail(String.format("String [%s] should have been rejected but parsed to [[%d] - [%d]]",
                        bad, convert.from, convert.to));
            }
            catch (CommandLine.TypeConversionException tce)
            {
                //desired
            }
        }

    }

    @Test
    public void testDurationConstructor()
    {
        long[] legal = {0,1,2, 999999, Long.MAX_VALUE};
        long[] illegal = {-1,-2, -999999, Long.MIN_VALUE};

        for(long ok : legal)
        {
            TimeDuration subject = new TimeDuration(ok);
            assertEquals(ok, subject.tenth_nanos);
        }

        for(long bad : illegal)
        {
            try {
                TimeDuration subject = new TimeDuration(bad);
                fail(String.format("TimeDuration constructor accepted bad value [%d]", bad));
            } catch (IllegalArgumentException e) {
                //desired
            }
        }
    }

    @Test
    public void testIntervalConstructor()
    {
       long[][] legal = new long[][]
            {
                    {0,0},
                    {0,1},
                    {1,1},
                    {Long.MIN_VALUE, Long.MAX_VALUE},
                    {Long.MAX_VALUE, Long.MAX_VALUE},
                    {Long.MIN_VALUE, Long.MIN_VALUE},
                    {-1,0},
                    {-99, -3},
                    {132412341234L, 9999999999999L}
            };

        long[][] illegal = new long[][]
                {
                        {1,0},
                        {0,-1},
                        {-5,-8},
                        {-5, Long.MIN_VALUE},
                        {Long.MAX_VALUE, 0},
                        {Long.MAX_VALUE, Long.MIN_VALUE}
                };

        for(long[] ok : legal)
        {
            TimeInterval t = new TimeInterval(ok[0], ok[1]);
            assertEquals(ok[0], t.from);
            assertEquals(ok[1], t.to);
        }

        for(long[] bad : illegal)
        {
            try {
                TimeInterval t = new TimeInterval(bad[0], bad[1]);
                fail(String.format("TimeInterval constructor accepted bad range [%d - %d]", bad[0], bad[1]));
            } catch (IllegalArgumentException e) {
                //desired
            }

        }
    }

    @Test
    public void testIntervalInRange()
    {
        // Test TimeInterval.inRange()
        class Case
        {
            final long from;
            final long to;
            final long[] inRange;
            final long[] outRange;

            Case(long from, long to, long[] inRange, long[] outRange)
            {
                this.from = from;
                this.to = to;
                this.inRange = inRange;
                this.outRange = outRange;
            }
        }

        Case[] cases = new Case[]
                {
                        new Case(0, 0, new long[]{0}, new long[]{Long.MIN_VALUE, -1, 1, Long.MAX_VALUE}),
                        new Case(Long.MIN_VALUE, Long.MAX_VALUE, new long[]{Long.MIN_VALUE, -234124, 0, 132451235, Long.MAX_VALUE}, new long[]{}),
                };

        for(Case c : cases)
        {
            TimeInterval subject = new TimeInterval(c.from, c.to);
            for(long val : c.inRange)
            {
                assertTrue(subject.inRange(val));
            }
            for(long val : c.outRange)
            {
                assertFalse(subject.inRange(val));
            }
        }
    }
}
