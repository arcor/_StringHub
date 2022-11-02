package icecube.daq.cli.commands;

import icecube.daq.cli.options.DomOption;
import icecube.daq.cli.util.DomResolver;
import icecube.daq.cli.util.DomSetResolver;
import icecube.daq.util.DOMInfo;
import org.apache.log4j.BasicConfigurator;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "info", description = "List configuration info, I.E. detector geometry, domsets...",
        mixinStandardHelpOptions = true,
subcommands = {InfoCmd.ListDoms.class,
               InfoCmd.ListDOMSets.class}
)
public class InfoCmd
{
    @CommandLine.Command(name = "doms", description = "list dom info", mixinStandardHelpOptions = true)
    public static class ListDoms implements Callable<Integer>
    {
        @CommandLine.Option(names = {"--doms"}, required = true,
                description = "Select only specific DOMs from the config%n" +
                        "   examples:%n" +
                        "    \"64a8ac5299b1,e5c34e1fca8f\"%n" +
                        "    \"Larry_Talbot,Egg_Nebula\"%n" +
                        "    \"string:27\"%n" +
                        "    \"hub:2029\"%n" +
                        "    \"domset:DEEPCORE3\"",
                paramLabel = "DOM_LIST",
                converter = DomOption.DOMInfoListConverter.class)
        List<DOMInfo> doms;

        public Integer call()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%n --- Geometry database sourced from [%s] ---%n%n", DomResolver.sourceLocation));

            prettyPrint(sb, doms);
            System.out.println(sb);

            return 0;
        }
    }


    @CommandLine.Command(name = "domsets", description = "list domset names", mixinStandardHelpOptions = true)
    public static class ListDOMSets implements Callable<Integer>
    {

        public Integer call()
        {
            StringBuilder sb = new StringBuilder();


            List<String> setNames = DomSetResolver.instance().list();
            sb.append(String.format("%n --- Geometry database sourced from [%s] ---%n%n", DomResolver.sourceLocation));
            sb.append(String.format("%n --- Dom sets sourced from [%s] ---%n%n", DomSetResolver.sourceLocation));

            for(String setName : setNames)
            {
                sb.append(String.format("%s%n", setName));
            }

            System.out.println(sb);

            return 0;
        }
    }



    public static StringBuilder prettyPrint(StringBuilder sb, List<DOMInfo> doms)
    {
        //DomInfo.toString() does:
        //TL9H6037[415f1d4e6f90]ch#5475 'Laxticka' at (85, 36)
        // which is ugly

        String header = String.format("#%-15s %-15s %-30s %-10s %-10s %-10s%n",
                "mbid",
                "prod-id",
                "name",
                "hub-id",
                "string-pos",
                "channel"
        );
        sb.append(header);
        for(int i=0; i<header.length(); i++)
        {
            sb.append("-");
        }
        sb.append(String.format("%n"));

        for (int i = 0; i < doms.size(); i++) {
            DOMInfo dom = doms.get(i);

            String line = String.format(" %-15s %-15s %-30s %-10d %-10s %-10d%n",
                    dom.getMainboardId(),
                    dom.getProductionId(),
                    dom.getName(),
                    dom.getHubId(),
                    String.format("%d-%d",dom.getStringMajor(),dom.getStringMinor()),
                    dom.getChannelId()
            );

            sb.append(line);

        }

        return sb;

    }

    public static void main(String[] args)
    {
        BasicConfigurator.configure();

        CommandLine cmd = new CommandLine(new InfoCmd());
        cmd.execute(args);

    }

    static class Test
    {
        public static void main(String[] args)
        {
//            InfoCmd.main(new String[]{"--help"});
//            InfoCmd.main(new String[]{"doms", " --help"});
//            InfoCmd.main(new String[]{"domsets", " --help"});
//            InfoCmd.main(new String[]{"doms", "--doms", "string:0"});
//            InfoCmd.main(new String[]{"doms", "--doms", "hub:203"});
            InfoCmd.main(new String[]{"doms", "--doms", "domset:DMICE_TRIG"});
//            InfoCmd.main(new String[]{"domsets"});
        }
    }
}
