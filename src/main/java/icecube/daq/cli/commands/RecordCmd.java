package icecube.daq.cli.commands;

import icecube.daq.cli.options.RecordTypeOption;
import icecube.daq.cli.stream.RecordType;
import org.apache.log4j.BasicConfigurator;
import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(name = "formats", description = "Utility for exploring pdaq records",
        mixinStandardHelpOptions = true,
        subcommands = {RecordCmd.List.class,
                RecordCmd.Describe.class}
)
public class RecordCmd
{


    @CommandLine.Command(name = "describe", description = "Outputs documentation of the record format", mixinStandardHelpOptions = true)
    public static class Describe implements Callable<Integer>
    {

        @CommandLine.Option(names = {"--type"}, required = true,
                description = "The record type to describe%n" +
                "RECORD_TYPE: [${COMPLETION-CANDIDATES}]",
                completionCandidates = RecordType.CompletionValues.class,
                converter = RecordType.Converter.class,
                paramLabel = "RECORD_TYPE")
        RecordType recordType;

        @Override
        public Integer call() throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("type: %s%n",recordType.keyword));
            sb.append(String.format("format:%n"));
            sb.append(recordType.rr.describe());

            System.out.println(sb);

            return 0;
        }
    }


    @CommandLine.Command(name = "list", description = "list known record types", mixinStandardHelpOptions = true)
    public static class List implements Callable<Integer>
    {
        public Integer call()
        {
            StringBuilder sb = new StringBuilder();

            for(RecordType type : RecordType.values())
            {
                sb.append(String.format("%-20s%n", type.keyword));
            }

            System.out.println(sb);

            return 0;
        }
    }


    public static void main(String[] args)
    {
        BasicConfigurator.configure();

        CommandLine cmd = new CommandLine(new RecordCmd());
        cmd.execute(args);

    }


    static class Test
    {
        public static void main(String[] args)
        {
//            RecordCmd.main(new String[]{"--help"});
            RecordCmd.main(new String[]{"list"});
//            RecordCmd.main(new String[]{"describe", "--type", "pdaq-tcal"});
//            RecordCmd.main(new String[]{"describe", "--type", "pdaq-hit-delta"});

//            for(RecordTypeOption.RecordType type : RecordTypeOption.RecordType.values())
//            {
//                            RecordCmd.main(new String[]{"describe", "--type", type.typeName});
//                System.out.println();
//                System.out.println();
//
//            }
        }
    }
}
