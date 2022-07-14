package icecube.daq.cli.options;

import icecube.daq.util.FlasherboardConfiguration;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

/**
 * Command line option for a flasher config
 */
public class FlasherConfigOption
{


    public static class FlasherConfigCLIConverter implements CommandLine.ITypeConverter<FlasherboardConfiguration>
    {

        @Override
        /**
         * parse:
         * --flasher dom:abcd,brightness:uint16,width:uint16,delay:uint16,mask:uint16,rate:uint16
         * --flasher flasher-config.xml
         *
         * into a FlasherboardConfiguration instance
         */
        public FlasherboardConfiguration convert(String value)
        {
            String[] tokens = value.split(",");
            if (tokens.length < 2) {
                // todo: treat argument as a file and load from there
                throw new CommandLine.TypeConversionException("cannot convert" + value +
                        " to a flasher configuration");
            }


            String mbid = null;
            Map<String, Short> elements = new HashMap<>(5);
            for(String item : tokens)
            {
                String[] nameval = item.split(":");
                if(nameval.length != 2 )
                {
                    throw new CommandLine.TypeConversionException("cannot convert" + value +
                            " to flasher configuration string element: " + item);
                }


                if(nameval[0].equals("dom"))
                {
                    DomOption.MbidConverter mbidResolve = new DomOption.MbidConverter();
                    mbid = String.format("%x", mbidResolve.convert(nameval[1]));

                }
                else
                {
                    try {
                        elements.put(nameval[0], Short.decode(nameval[1]));
                    }
                    catch (NumberFormatException nfe)
                    {
                        throw new CommandLine.TypeConversionException("cannot convert" + value +
                                " to flasher configuration string element: " + item);

                    }
                }

            }


            if(mbid != null && elements.containsKey("brightness") &&
                    elements.containsKey("width") && elements.containsKey("delay") &&
                    elements.containsKey("mask") && elements.containsKey("rate") )
            {
                return new FlasherboardConfiguration(mbid, elements.get("brightness"), elements.get("width"),
                        elements.get("delay"), elements.get("mask"), elements.get("rate"));
            }
            else
            {
                throw new CommandLine.TypeConversionException("cannot convert" + value +
                        " to a flasher configuration");
            }

        }
    }


}
