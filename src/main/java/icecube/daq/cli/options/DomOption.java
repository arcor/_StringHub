package icecube.daq.cli.options;

import icecube.daq.cli.util.DomResolver;
import icecube.daq.cli.util.DomSetResolver;
import icecube.daq.util.*;
import picocli.CommandLine;

import java.util.*;

/**
 * Encapsulates a syntax to specify doms on the command line.
 *
 * Recognizes:
 *
 * mbids:
 * "0xe3a2239b"
 * "e3a2239b"
 *
 * names:
 * "Graphaphobia"
 *
 * topology classes:
 * "all"
 * "hub:27"
 * "string:33"
 * "sps"
 * "domset:<set name>
 *
 * combinations:
 * "0x69a843a5e181,69a843a5e181,Doxophobia"
 */
public class DomOption
{

    /**
     * Convert strings to mbid values.
     * String can be:
     *
     * dom_name
     * base16 number
     * 0x prefixed base16 number
     */
    public static class MbidConverter implements CommandLine.ITypeConverter<Long>
    {

        private DOMInfoConverter knownDOMS = new DOMInfoConverter();

        @Override
        public Long convert(String value)
        {
            //try mbid
            if(value.matches("[0-9a-fA-F]+"))
            {
                return Long.parseLong(value, 16);

            }

            //try 0xmbid
            if(value.matches("0x[0-9a-fA-F]+"))
            {
                return  Long.parseLong(value.substring(2), 16);

            }

            // try known dom lookup
            DOMInfo real = knownDOMS.convert(value);
            if(real != null)
            {
                return real.getNumericMainboardId();
            }

            throw new CommandLine.TypeConversionException("Cannot convert " + value + " to a long mbid value");
        }
    }

    /**
     * List form for converting strings to mbid values
     * Adds topology classes: all, sps, spts, hub:21
     */
    public static class MBIDListConverter implements CommandLine.ITypeConverter<List<Long>>
    {
        MbidConverter singleMbidConvert;
        public MBIDListConverter()
        {
            singleMbidConvert = new MbidConverter();
        }

        @Override
        public List<Long> convert(String value)
        {
            // use map to prevent duplicates, e.g "sps,hub:21"
            Set<Long> doms = new HashSet<>();

            IDOMRegistry db = DomResolver.instance();

            String[] items = value.split(",");
            for (int i = 0; i < items.length; i++) {
                String item = items[i];

                if(TopologyGroups.isGroupSpec(item))
                {
                    List<DOMInfo> resolved = TopologyGroups.resolve(db, item);
                    resolved.forEach(domInfo -> doms.add(domInfo.getNumericMainboardId()));
                }
                else
                {
                    doms.add(singleMbidConvert.convert(item));
                }
            }

            return new ArrayList<>(doms);

        }

    }



    /**
     * converts strings to known DOMS
     */
    public static class DOMInfoConverter implements CommandLine.ITypeConverter<DOMInfo>
    {
        @Override
        public DOMInfo convert(String value) {

            IDOMRegistry db = null;
            try {
                db = DomResolver.instance();


                //try mbid
                if(value.matches("[0-9a-fA-F]+"))
                {
                    DOMInfo dom = db.getDom(Long.parseLong(value, 16));
                    if(dom != null){return dom;}

                }


                //try 0xmbid
                if(value.matches("0x[0-9a-fA-F]+"))
                {
                    DOMInfo dom = db.getDom(Long.parseLong(value.substring(2), 16));
                    if(dom != null){return dom;}

                }

                //try by name
                for(DOMInfo info : db.allDOMs())
                {
                    if(info.getName().equals(value))
                    {
                        return info;
                    }
                }

                // fail
                throw new CommandLine.TypeConversionException(String.format("Dom specifier [%s] does not match any known DOM", value));
            } catch (DOMRegistryException e) {
                throw new Error(e);
            }

        }
    }

    /**
     * List form for converting strings to known DOMs.
     * Adds topology classes: all, sps, spts, hub:21
     */
    public static class DOMInfoListConverter implements CommandLine.ITypeConverter<List<DOMInfo>>
    {
        DOMInfoConverter single = new DOMInfoConverter();

        @Override
        public List<DOMInfo> convert(String value) {

            // use map to prevent duplicates, e.g "sps,hub:21"
            Map<String, DOMInfo> doms = new HashMap<>();

            IDOMRegistry db =DomResolver.instance();

            String[] items = value.split(",");
            for (int i = 0; i < items.length; i++) {
                String item = items[i];

                if(TopologyGroups.isGroupSpec(item))
                {
                    List<DOMInfo> resolved = TopologyGroups.resolve(db, item);
                    resolved.forEach(domInfo -> doms.put(domInfo.getMainboardId(), domInfo));
                }
                else
                {
                    DOMInfo dom = single.convert(item);
                    if(dom != null)
                    {
                        doms.put(dom.getMainboardId(), dom);

                    }
                    else
                    {
                        // todo consider
                        //allow non-dom selections
                    }
                }
            }


            ArrayList<DOMInfo> ret = new ArrayList<>(doms.size());
            ret.addAll(doms.values());
            ret.sort(new DomSort());
            return ret;

        }
    }

    /**
     * Encapsulate topology extraction.
     * Understands: all, sps, hub:#, domset:#
     */
    public static class TopologyGroups
    {
        public static boolean isGroupSpec(String token)
        {
            return token.equals("all") ||
                    token.equals("sps") ||
                    token.contains(":");
        }

        public static List<DOMInfo> resolve(IDOMRegistry db, String group)
        {
            try {
                List<DOMInfo> doms = new ArrayList<>();

                if("all".equals(group))
                {
                    for(DOMInfo dom: db.allDOMs())
                    {
                        doms.add(dom);
                    }
                }
                else if("sps".equals(group))
                {
                    for(DOMInfo dom: db.allDOMs())
                    {
                        int hubId = dom.getHubId();

                        if( (hubId >= 1 && hubId <= 86) || (hubId >= 200 && hubId <= 211) || hubId == 300)
                        {
                            doms.add(dom);
                        }
                    }
                }

                else if(group.contains(":"))
                {
                    String[] subitems = group.split(":");
                    if(subitems.length != 2)
                    {
                        throw new CommandLine.TypeConversionException("malformed dom group specifier: [" + group + "]");
                    }
                    switch (subitems[0])
                    {
                        case "hub":

                            try {
                                doms.addAll(db.getDomsOnHub(Integer.parseInt(subitems[1])));
                            } catch (NumberFormatException e) {
                                throw new CommandLine.TypeConversionException(
                                        String.format("bad hub number [%s] in dom group specifier: [%s]",
                                                subitems[1], group));

                            }

                            break;
                        case "string":

                            try {
                                doms.addAll(db.getDomsOnString(Integer.parseInt(subitems[1])));
                            } catch (NumberFormatException e) {
                                throw new CommandLine.TypeConversionException(
                                        String.format("bad string number [%s] in dom group specifier: [%s]",
                                                subitems[1], group));

                            }

                            break;
                        case "domset":

                            try {
                                List<DOMInfo> fromSet = DomSetResolver.instance().resolve(subitems[1]);
                                doms.addAll(fromSet);
                            } catch (NumberFormatException e) {
                                throw new CommandLine.TypeConversionException(
                                        String.format("bad string number [%s] in dom group specifier: [%s]",
                                                subitems[1], group));

                            }

                            break;
                        default:
                            throw new CommandLine.TypeConversionException("Unknown dom group specifier: [" + group + "]");
                    }
                }

                return doms;
            } catch (DOMRegistryException e) {
                throw new Error(e);
            }
        }

    }

    static class DomSort implements Comparator<DOMInfo>
    {
        @Override
        public int compare(DOMInfo o1, DOMInfo o2)
        {
            int hub = Integer.compare(o1.getHubId(), o2.getHubId());
            if(hub == 0)
            {
                return Integer.compare(o1.getStringMinor(), o2.getStringMinor());
            }
            else
            {
                return hub;
            }
        }
    }


}
