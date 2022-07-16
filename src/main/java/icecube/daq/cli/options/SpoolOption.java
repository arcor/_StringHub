package icecube.daq.cli.options;

import picocli.CommandLine;

/**
 * Command line options for specifying a record spool configuration.
 *
 * E.G.
 * --spool hitspool
 * --spool tcalspool:27d
 * --spool tcalspool:8h30m
 * --spool monispool:1024:150000000000:
 * --spool monispool:1024:15s:
 */
public class SpoolOption
{

    public static long DEFAULT_INTERVAL = 150_000_000_000L; // 15 seconds files
    public static int DEFAULT_FILE_NUM = 1920;             // 8 hours at 15 second files

    public final String spoolDir;
    public final int numFiles;
    public final long fileInterval;

    public SpoolOption(String spoolDir, int numFiles, long fileInterval)
    {
        this.spoolDir = spoolDir;
        this.numFiles = numFiles;
        this.fileInterval = fileInterval;
    }

    public static class SpoolOptionsConverter implements CommandLine.ITypeConverter<SpoolOption>
    {
        TimeOption.DurationParameterConverter durationConverter = new TimeOption.DurationParameterConverter();

        @Override
        public SpoolOption convert(String s)
        {
            String[] tokens = s!=null ? s.split(":") : new String[]{};

            // reasonable restrictions on spool name
            if(tokens.length > 0)
            {
                String spoolname = tokens[0];
                if (spoolname == null ||
                        spoolname.equals("") ||
                        spoolname.matches("\\s*"))
                {
                    throw new CommandLine.TypeConversionException("Cannot convert " + s + " to a spool configuration," +
                            "the spoolname [" + spoolname + "] is not permitted");

                }
            }

            try {
                switch (tokens.length) {
                    case 1:
                        //use spoolname and default the rest
                        return new SpoolOption(tokens[0], DEFAULT_FILE_NUM, DEFAULT_INTERVAL);
                    case 2:
                        //use spoolname and default the second parameter as a target for the overall
                        // timespan DEFAULT_INTERVAL
                        TimeOption.TimeDuration duration = durationConverter.convert(tokens[1]);
                        int numFiles = (int) ((duration.tenth_nanos + DEFAULT_INTERVAL - 1)/DEFAULT_INTERVAL);

                        return new SpoolOption(tokens[0], numFiles, DEFAULT_INTERVAL);

                    case 3:
                        //use spoolname and default the second parameter as a target for the overall
                        // timespan DEFAULT_INTERVAL
                        numFiles = Integer.parseInt(tokens[1]);
                        TimeOption.TimeDuration interval = durationConverter.convert(tokens[2]);

                        return new SpoolOption(tokens[0], numFiles, interval.tenth_nanos);

                    default:
                        throw new CommandLine.TypeConversionException("Cannot convert " + s + " to a spool configuration");

                }
            } catch (NumberFormatException e) {
                throw new CommandLine.TypeConversionException("Cannot convert " + s + " to a spool configuration: "
                        + e.getMessage());
            }
        }

    }


}
