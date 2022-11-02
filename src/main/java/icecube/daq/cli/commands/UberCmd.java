package icecube.daq.cli.commands;

import org.apache.log4j.BasicConfigurator;
import org.sqlite.JDBC;
import picocli.CommandLine;

/**
 * Packages selected CLI utilities as a set of subcommands in
 * a unified CLI application
 */
@CommandLine.Command(name = "pdaq-cli", description = "PDAQ Command Line Utility",
        mixinStandardHelpOptions = true,
        subcommands = {InfoCmd.class,
                RecordCmd.class,
                ProcessCmd.class,
                OmicronCmd.class,
                }
)
public class UberCmd
{

    // force the shade plugin to include sqlite driver
    static
    {
        try {
            Class<JDBC> jdbcClass = JDBC.class;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        BasicConfigurator.configure();

        CommandLine cmd = new CommandLine(new UberCmd());
        cmd.execute(args);

    }

    static class Test
    {
        public static void main(String[] args)
        {
//            UberCmd.main(new String[]{"--help"});
//            UberCmd.main(new String[]{"info", "--help"});
//            UberCmd.main(new String[]{"info", "doms", "--help"});
//            UberCmd.main(new String[]{"info", "domsets", "--help"});
//            UberCmd.main(new String[]{"info", "doms", "--doms", "string:0"});
//            UberCmd.main(new String[]{"info" "doms", "--doms", "hub:203"});
//            UberCmd.main(new String[]{"info" "doms", "--doms", "domset:DMICE_TRIG"});
//            UberCmd.main(new String[]{"info", "domsets"});

//            UberCmd.main(new String[]{"formats", "--help"});
//            UberCmd.main(new String[]{"formats", "list", "--help"});
//            UberCmd.main(new String[]{"formats", "describe", "--help"});
//            UberCmd.main(new String[]{"formats", "list"});
//            UberCmd.main(new String[]{"formats", "describe", "--type", "pdaq-hit-delta"});

//            UberCmd.main(new String[]{"process", "--help"});
            UberCmd.main(new String[]{"process", "extract", "--help"});
//            UberCmd.main(new String[]{"process", "info", "--help"});


//            UberCmd.main(new String[]{"omicron", "--help"});

        }
    }
}
