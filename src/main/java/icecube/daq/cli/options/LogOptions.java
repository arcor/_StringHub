package icecube.daq.cli.options;

import org.apache.log4j.*;
import org.apache.log4j.varia.NullAppender;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Command line options for configuring log4j
 */
public class LogOptions
{


    @Option(names = {"--log"}, required = false, description = "log file [filename | stdout | stderr | off]")
    String logfile = "stdout";

    @Option(names = {"--log-level"}, required = false,
            description = "log level [${COMPLETION-CANDIDATES}] default: ${DEFAULT-VALUE}",
    defaultValue = "WARN")
    LevelOpts level;

    @Option(names = {"--log-props"}, required = false, description = "load log settings from a log4j properties file")
    String props = null;


    /**
     * peer to log4j levels
     */
    public static enum LevelOpts
    {
        OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL;

        Level asLevel()
        {
            return Level.toLevel(this.toString());
        }
    }

    public void configure() throws IOException
    {
        BasicConfigurator.resetConfiguration();

        if(props != null && Files.exists(new File(props).toPath()))
        {
            PropertyConfigurator.configure(props);
        }
        else
        {
            Layout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
            switch (logfile)
            {
                case "stdout":
                    BasicConfigurator.configure(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));
                    break;
                case "stderr":
                    BasicConfigurator.configure(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_ERR));
                    break;
                case "off":
                    BasicConfigurator.configure(new NullAppender());
                    break;
                default:
                    BasicConfigurator.configure(new FileAppender(layout, logfile));
            }

            Logger.getRootLogger().setLevel(level.asLevel());

        }

    }
}
