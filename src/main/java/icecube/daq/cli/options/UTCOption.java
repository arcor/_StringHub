package icecube.daq.cli.options;

import icecube.daq.cli.util.UTCResolver;
import picocli.CommandLine;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Defines a command line syntax for controlling UTC time resolution
 */
public class UTCOption
{
    @CommandLine.Option(names = {"--resolve-times"}, required = false,
            defaultValue = "False",
            description = "Resolve ICL numerical times to UTC timestamps for display%n" +
                          "DEFAULT:${DEFAULT-VALUE} ")
    public boolean resolveUtcTime;

    @CommandLine.Option(names = {"--data-year"}, required = false,
            description = "data year used to resolve ICL numerical times to UTC timestamps")
    public int dataYear = discoverCurrentYear();



    public UTCResolver getResolver()
    {
        if(resolveUtcTime)
        {
            return UTCResolver.create(dataYear);
        }
        else
        {
            return UTCResolver.NIL_RESOLVER;
        }
    }


    /**
     * Discovers the current year.
     * @return The current year.
     */
    static int discoverCurrentYear()
    {
        TimeZone utc_zone = TimeZone.getTimeZone("GMT");
        Calendar now = Calendar.getInstance(utc_zone);
        int year = now.get(Calendar.YEAR);
        return year;
    }


}
