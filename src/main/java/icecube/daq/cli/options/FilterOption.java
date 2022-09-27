package icecube.daq.cli.options;

import icecube.daq.cli.filter.Filter;
import icecube.daq.cli.filter.TriggerSourceFilter;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * --filter time:0:444
 * --filter mbid:string32
 * --filter mbid:Graphophobia
 */
/**
 * Encapsulates a syntax to specify record stream filters on the command line.
 *
 * Recognizes:
 *
 * time range:
 * "time:1111111:4444444"
 * "time:1111111:+1m"
 * "time:-1m:4444444"
 * "time:1111111:.
 * "time:.:4444444
 *
 * source mbid:
 * "mbid:Graphaphobia"
 * "mbid:hub:27"
 * "mbid:string:33"
 * "mbid:domset:<set name>
 * "mbid:0x69a843a5e181,69a843a5e181,Doxophobia"
 *
 * trigger type
 * "trigger:<trigger type>:<capture window>"
 * "trigger:MB_LED:1000000"
 * "trigger:CPU:0"
 */
public class FilterOption implements  CommandLine.ITypeConverter<Filter>
{

    public static class FilterCompletions extends ArrayList<String>
    {
        FilterCompletions() { super(Arrays.stream(FilterType.values()).map(r -> r.doc).collect(Collectors.toList()));}
    }

    enum FilterType
    {
        MBID("mbid", "mbid:<mbid-spec>") {
            @Override
            Filter resolve(String value)
            {
                DomOption.MBIDListConverter mbidListConverter = new DomOption.MBIDListConverter();
                List<Long> mbids = mbidListConverter.convert(value);
                return Filter.mbidFilter(mbids.stream().mapToLong(l -> l).toArray());
            }
        },
        TIMERANGE("time", "time:<from>:<to>") {
                    @Override
                    Filter resolve(String value)
                    {
                        TimeOption.IntervalParameterConverter intervalParameterConverter = new TimeOption.IntervalParameterConverter();
                        TimeOption.TimeInterval range = intervalParameterConverter.convert(value);
                        return Filter.timeRangeFilter(range);
                    }
                },
                TRIGGER_SOURCE("triggersource", "triggersource:<val>[:<window>]") {
                    @Override
                    Filter resolve(String value)
                    {
                        String[] tokens = value.split(":");
                        short val = Short.parseShort(tokens[0]);
                        long window = 0;
                        if(tokens.length == 2)
                        {
                            TimeOption.DurationParameterConverter durationParameterConverter = new TimeOption.DurationParameterConverter();
                            TimeOption.TimeDuration duration = durationParameterConverter.convert(tokens[1]);
                            window = duration.tenth_nanos;
                        }

                        return new TriggerSourceFilter(val, window);

                    }
                }
        ;

        final String keyword;
        final String doc;

        FilterType(String keyword, String doc)
        {
            this.keyword = keyword;
            this.doc = doc;
        }

        abstract Filter resolve(String value);

        static Filter resolve(String type, String value)
        {
           switch (type)
           {
               case "mbid" : return MBID.resolve(value);
               case "time" : return TIMERANGE.resolve(value);
               case "triggersource" : return TRIGGER_SOURCE.resolve(value);
               default:
                   throw new CommandLine.TypeConversionException(
                           String.format("bad filter argument, no type [%s]", type));
           }
        }
    }

    @Override
    public Filter convert(String s) throws Exception
    {

        if(s.startsWith("!"))
        {
            return Filter.negateFilter(base(s.substring(1)));
        }
        else
        {
        return base(s);
        }


    }

    private Filter base(String s) throws Exception
    {

        if(s.contains(":"))
        {
            String type = s.split(":")[0];
            String val = s.substring(s.indexOf(":") + 1);

            return FilterType.resolve(type, val);
        }
        else
        {
            //todo
            throw new CommandLine.TypeConversionException(
                    String.format("bad data source argument [%s]", s));
        }
    }


    public static void main(String[] args) throws Exception
    {
        FilterOption filterOption = new FilterOption();

        System.out.println(filterOption.convert("mbid:hub:27").describe());
        System.out.println(filterOption.convert("time:.:143512412").describe());
        System.out.println(filterOption.convert("!mbid:hub:27").describe());
    }

}
