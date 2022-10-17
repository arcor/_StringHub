package icecube.daq.cli.options;

import icecube.daq.cli.filter.Filter;
import icecube.daq.cli.filter.TriggerSourceFilter;
import icecube.daq.cli.stream.RecordType;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
 *
 * noop
 * "noop:a"
 * "noop:b"
 */
public class FilterOption implements  CommandLine.ITypeConverter<Filter>
{

    public static class FilterCompletions extends ArrayList<String>
    {
        FilterCompletions() { super(Arrays.stream(FilterType.values()).map(r -> r.keyword).collect(Collectors.toList()));}
    }

    enum FilterType
    {
        LIMIT("limit", "limit:<count | size(b)(kb)(mb)(gb)>") {
            @Override
            Filter resolve(String value)
            {
                Pattern re = Pattern.compile("([0-9]+)(b|kb|mb|gb)?");
                Matcher matcher = re.matcher(value);
                if(matcher.matches())
                {
                    long magnitude = Long.parseLong(matcher.group(1));
                    if (matcher.group(2) != null) {
                        switch (matcher.group(2)) {
                            case "b":
                                return Filter.limitSizeFilter(magnitude);
                            case "kb":
                                return Filter.limitSizeFilter(magnitude * 1024);
                            case "mb":
                                return Filter.limitSizeFilter(magnitude * 1024 * 1024);
                            case "gb":
                                return Filter.limitSizeFilter(magnitude * 1024 * 1024 * 1024);
                            default:
                                throw new Error(String.format("Miscoded? regular expression matched [%s]",
                                        matcher.group(2)));

                        }
                    } else {
                        return Filter.limitCountFilter(magnitude);
                    }
                }
                else
                {
                    throw new CommandLine.TypeConversionException(String.format("Unknown filter type [%s]",
                            value));
                }


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
        MBID("mbid", "mbid:<mbid-spec>") {
            @Override
            Filter resolve(String value)
            {
                DomOption.MBIDListConverter mbidListConverter = new DomOption.MBIDListConverter();
                List<Long> mbids = mbidListConverter.convert(value);
                return Filter.mbidFilter(mbids.stream().mapToLong(l -> l).toArray());
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
        },
        NOOP("noop", "noop:id")
                {
                    @Override
                    Filter resolve(String value)
                    {
                        return new Filter()
                        {
                            @Override
                            public String describe()
                            {
                                return String.format("noop:%s", value);
                            }

                            @Override
                            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
                            {
                                return r -> true;
                            }
                        };
                    }
                };

        final String keyword;
        final String doc;

        static Map<String, FilterType> LOOKUP_MAP;

        static {
            Map<String, FilterType> map = new ConcurrentHashMap<>();
            for (FilterType t : FilterType.values()) {
                map.put(t.keyword, t);
            }
            LOOKUP_MAP = Collections.unmodifiableMap(map);
        }

        FilterType(String keyword, String doc)
        {
            this.keyword = keyword;
            this.doc = doc;
        }

        public static FilterType lookup(String keyword)
        {
            return LOOKUP_MAP.get(keyword);
        }


        abstract Filter resolve(String value);

        static Filter resolve(String keyword, String value)
        {
            FilterType filter = lookup(keyword);
            if(filter != null)
            {
                return filter.resolve(value);
            }
            else
            {
                throw new CommandLine.TypeConversionException(
                        String.format("bad filter argument, no filter type [%s]", keyword));
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
            String keyword = s.split(":")[0];
            String val = s.substring(s.indexOf(":") + 1);

            return FilterType.resolve(keyword, val);
        }
        else
        {
            return FilterType.resolve(s, "");
        }
    }


    public static void main(String[] args) throws Exception
    {
        FilterOption filterOption = new FilterOption();

        System.out.println(filterOption.convert("mbid:hub:27").describe());
        System.out.println(filterOption.convert("time:.:143512412").describe());
        System.out.println(filterOption.convert("!mbid:hub:27").describe());
        System.out.println(filterOption.convert("limit:1234").describe());
        System.out.println(filterOption.convert("limit:1234kb").describe());
    }

}
