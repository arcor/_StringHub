package icecube.daq.cli.options;

import icecube.daq.performance.binary.buffer.IndexFactory;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.spool.RecordSpool;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

/**
 * Command line options for specifying a record spool configuration.
 *
 * E.G.
 * --spool spooldir:spoolname
 * --spool spooldir:hits:27d
 * --spool spooldir:hits:8h30m
 * --spool spooldir:hits:1024:150000000000:
 * --spool spooldir:hits:1024:15s:
 */
public class SpoolOption
{

    public static long DEFAULT_INTERVAL = 150_000_000_000L; // 15 seconds files
    public static int DEFAULT_FILE_NUM = 1920;             // 8 hours at 15 second files

    public final String spoolDir;
    public final String spoolName;
    public final int numFiles;
    public final long fileInterval;

    public SpoolOption(String spoolDir, String spoolName, int numFiles, long fileInterval)
    {
        this.spoolDir = spoolDir;
        this.spoolName = spoolName;
        this.numFiles = numFiles;
        this.fileInterval = fileInterval;

    }

    public RecordSpool plumbSpool(RecordReader rr) throws IOException
    {
        // spool files
        IndexFactory index = IndexFactory.NO_INDEX;
        RecordSpool spool = new RecordSpool(rr, rr.getOrderingField(), new File(spoolDir), spoolName,
                fileInterval, numFiles, index);

        return spool;

    }

    public static class Converter implements CommandLine.ITypeConverter<SpoolOption>
    {
        TimeOption.DurationParameterConverter durationConverter = new TimeOption.DurationParameterConverter();

        @Override
        public SpoolOption convert(String s)
        {
            String[] tokens = s!=null ? s.split(":") : new String[]{};

            // reasonable restrictions on spool directory and name
            if(tokens.length > 1)
            {
                String spooldir = tokens[0];
                if (spooldir == null ||
                        spooldir.equals("") ||
                        spooldir.matches("\\s*"))
                {
                    throw new CommandLine.TypeConversionException("Cannot convert " + s + " to a spool configuration," +
                            "the spool dir [" + spooldir + "] is not permitted");

                }

                String spoolname = tokens[1];
                if (spoolname == null ||
                        spoolname.equals("") ||
                        spoolname.matches("\\s*"))
                {
                    throw new CommandLine.TypeConversionException("Cannot convert " + s + " to a spool configuration," +
                            "the spool name [" + spooldir + "] is not permitted");

                }
            }

            try {
                switch (tokens.length) {
                    case 2:
                        //use spool dir and name and default the rest
                        return new SpoolOption(tokens[0], tokens[1], DEFAULT_FILE_NUM, DEFAULT_INTERVAL);
                    case 3:
                        //use spool dir and default the second parameter as a target for the overall
                        // timespan DEFAULT_INTERVAL
                        TimeOption.TimeDuration duration = durationConverter.convert(tokens[2]);
                        int numFiles = (int) ((duration.tenth_nanos + DEFAULT_INTERVAL - 1)/DEFAULT_INTERVAL);

                        return new SpoolOption(tokens[0], tokens[1], numFiles, DEFAULT_INTERVAL);

                    case 4:
                        //use spool dir and default the second parameter as a target for the overall
                        // timespan DEFAULT_INTERVAL
                        numFiles = Integer.parseInt(tokens[2]);
                        TimeOption.TimeDuration interval = durationConverter.convert(tokens[3]);

                        return new SpoolOption(tokens[0], tokens[1], numFiles, interval.tenth_nanos);

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
