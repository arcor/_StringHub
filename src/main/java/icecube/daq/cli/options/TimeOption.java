package icecube.daq.cli.options;


import picocli.CommandLine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command line options for specifying IceCube time durations and intervals.
 */
public interface TimeOption
{

     final static long TENTH_NANOS_PER_NANO = 10;
     final static long TENTH_NANOS_PER_SEC = 10_000_000_000L;
     final static long TENTH_NANOS_PER_MIN = TENTH_NANOS_PER_SEC * 60;
     final static long TENTH_NANOS_PER_HOUR = TENTH_NANOS_PER_MIN * 60;
     final static long TENTH_NANOS_PER_DAY = TENTH_NANOS_PER_HOUR * 24;

    /**
     * A time duration with fixed precision of 1E-10 seconds.
     */
    public static class TimeDuration
    {

        public final long tenth_nanos;

        public TimeDuration(long tenth_nanos)
        {
            if(tenth_nanos<0)
            {
                throw new IllegalArgumentException("Negative time durations are not supported");
            }
            this.tenth_nanos = tenth_nanos;
        }

        @Override
        public String toString()
        {

            // handle zero
            if(tenth_nanos == 0)
            {
                return "0";
            }

            long days = tenth_nanos / TENTH_NANOS_PER_DAY;
            long hours = (tenth_nanos % TENTH_NANOS_PER_DAY) / TENTH_NANOS_PER_HOUR;
            long minutes = (tenth_nanos % TENTH_NANOS_PER_HOUR) / TENTH_NANOS_PER_MIN;
            long seconds = (tenth_nanos % TENTH_NANOS_PER_MIN) / TENTH_NANOS_PER_SEC;
            long nanos = (tenth_nanos % TENTH_NANOS_PER_SEC) / TENTH_NANOS_PER_NANO;
            long tenthNanos = (tenth_nanos % TENTH_NANOS_PER_NANO);

            StringBuilder sb = new StringBuilder();
            if(days>0){sb.append(days).append("d");}
            if(hours>0){sb.append(hours).append("h");}
            if(minutes>0){sb.append(minutes).append("m");}
            if(seconds>0){sb.append(seconds).append("s");}

            if(nanos>0 || tenthNanos>0) {
                sb.append(nanos).append(".").append(tenthNanos).append("n");
            }

            return sb.toString();
        }
    }

    public static class TimeInterval
    {
        public final long from;
        public final long to;

        public TimeInterval(long from, long to)
        {
            if(from>to)
            {
                throw new IllegalArgumentException("Negative time intervals are not supported");
            }

            this.from = from;
            this.to = to;
        }

        public boolean inRange(long utc)
        {
            return  utc >= from && utc <= to;
        }

        @Override
        public String toString()
        {
            return String.format("[%d:%d]", from, to);
        }
    }

    /**
     * Convert strings of form [3d7h33m22s32892314.6n] to a number of 1E-10 seconds
     */
    public static class DurationParameterConverter implements CommandLine.ITypeConverter<TimeDuration>
    {

        @Override
        public TimeDuration convert(String value)
        {

            if(value == null || value.equals(""))
            {
                throw new CommandLine.TypeConversionException(errMsg(value));
            }

            // unit-less values are 1/10 nanos
            // e.g. 13000 --> 130000n
            if(value.matches("^\\d+$"))
            {
                return new TimeDuration(Long.parseLong(value));
            }


            // e.g. 1d4h13m44s
            Pattern re = Pattern.compile("^(?:(\\d+)[dD])?(?:(\\d+)[hH])?(?:(\\d+)[mM])?(?:(\\d+)[sS])?(?:(\\d*)\\.?(\\d?)[nN])?$");
            Matcher matcher = re.matcher(value);

            if(matcher.matches())
            {
//                for(int d=1; d<= matcher.groupCount(); d++)
//                {
//                    System.out.printf("group[%d] --> [%s]%n", d, matcher.group(d));
//                }

                long days = (matcher.group(1) != null) ? Long.parseLong(matcher.group(1)) : 0;
                long hours = (matcher.group(2) != null) ? Long.parseLong(matcher.group(2)) : 0;
                long minute = (matcher.group(3) != null) ? Long.parseLong(matcher.group(3)) : 0;
                long seconds = (matcher.group(4) != null) ? Long.parseLong(matcher.group(4)) : 0;
                long nanos = (matcher.group(5) != null&& !matcher.group(5).equals("")) ?
                        Long.parseLong(matcher.group(5)) : 0;
                long tenthNanos = (matcher.group(6) != null && !matcher.group(6).equals("")) ?
                        Long.parseLong(matcher.group(6)) : 0;


                try {
                    tenthNanos = Math.addExact(tenthNanos, Math.multiplyExact(days, TENTH_NANOS_PER_DAY));
                    tenthNanos = Math.addExact(tenthNanos, Math.multiplyExact(hours, TENTH_NANOS_PER_HOUR));
                    tenthNanos = Math.addExact(tenthNanos, Math.multiplyExact(minute, TENTH_NANOS_PER_MIN));
                    tenthNanos = Math.addExact(tenthNanos, Math.multiplyExact(seconds, TENTH_NANOS_PER_SEC));
                    tenthNanos = Math.addExact(tenthNanos, Math.multiplyExact(nanos, TENTH_NANOS_PER_NANO));
                } catch (ArithmeticException ae) {
                    throw new CommandLine.TypeConversionException(errMsg(value) + ":" + ae.getMessage());
                }

                return new TimeDuration(tenthNanos);
            }

            throw new CommandLine.TypeConversionException(errMsg(value));

        }

        private static String errMsg(String value)
        {
            return "cannot convert [" + value + "] to a time duration";
        }

    }

    // Convert strings of form [from:to]
    // accepts:
    //
    // fully specified:
    // 5:199
    //34123412351235:341234179961491
    //
    // open ended:
    // .:32112351235
    // 32112351235:.
    // .:.
    //
    // relative
    // 154544235:+10m
    // -10m:154544235
    //
    // 154544235:+99999
    // -999999:154544235
    //
    public static class IntervalParameterConverter implements CommandLine.ITypeConverter<TimeInterval>
    {


        @Override
        public TimeInterval convert(String value)
        {

            if(value == null || value.equals(""))
            {
                throw new CommandLine.TypeConversionException(errMsg(value));
            }

            String[] tokens = value.split(":");
            if(tokens.length != 2)
            {
                throw new CommandLine.TypeConversionException(errMsg(value));
            }

            try {

                Long relativeFrom = null;
                Long relativeTo = null;
                long from = -1;
                long to = -1;

                //from is relative
                if(tokens[0].equals("."))
                {
                    from = Long.MIN_VALUE;
                }
                else if(tokens[0].startsWith("-"))
                {
                    DurationParameterConverter con = new DurationParameterConverter();
                    relativeFrom = con.convert(tokens[0].substring(1)).tenth_nanos;
                }
                //NOTE: rejects "+55" which parseLong tolerates
                else if(tokens[0].matches("\\d+"))
                {
                    from = Long.parseLong(tokens[0]);
                }
                else
                {
                    throw new CommandLine.TypeConversionException(errMsg(value));
                }

                //to is relative
                if(tokens[1].equals("."))
                {
                    to = Long.MAX_VALUE;
                }
                else if(tokens[1].startsWith("+"))
                {
                    DurationParameterConverter con = new DurationParameterConverter();
                    relativeTo = con.convert(tokens[1].substring(1)).tenth_nanos;
                }
                //NOTE: rejects "-55" which parseLong tolerates
                else if(tokens[1].matches("\\d+"))
                {
                    to = Long.parseLong(tokens[1]);
                }
                else
                {
                    throw new CommandLine.TypeConversionException(errMsg(value));
                }

                if(relativeFrom != null && relativeTo != null)
                {
                    throw new CommandLine.TypeConversionException(errMsg(value));
                }

                if(relativeFrom != null)
                {
                    from = Math.subtractExact(to, relativeFrom.longValue());
                }

                if(relativeTo != null)
                {
                    to = Math.addExact(from, relativeTo.longValue());
                }

                if(from > to)
                {
                    throw new CommandLine.TypeConversionException(errMsg(value));
                }
                return new TimeInterval(from, to);

            } catch (NumberFormatException | ArithmeticException e){
                throw new CommandLine.TypeConversionException(errMsg(value));
            }

        }

        private static String errMsg(String value)
        {
            return "cannot convert [" + value + "] to a time interval";
        }

    }


}
