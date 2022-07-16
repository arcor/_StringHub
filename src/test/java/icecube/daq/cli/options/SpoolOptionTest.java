package icecube.daq.cli.options;


import icecube.daq.cli.options.SpoolOption.*;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.*;

public class SpoolOptionTest
{

    @Test
    public void testSpoolOptionConverter()
    {
        SpoolOptionsConverter subject = new SpoolOptionsConverter();


        class Case
        {
            final String input;
            final String expectedSpoolDir;
            final int expectedNumFiles;
            final long expectedFileInterval;

            Case(String input, String expectedSpoolDir, int expectedNumFiles, long expectedFileInterval)
            {
                this.input = input;
                this.expectedSpoolDir = expectedSpoolDir;
                this.expectedNumFiles = expectedNumFiles;
                this.expectedFileInterval = expectedFileInterval;
            }
        }

        Case[] cases = new Case[]
                {
                        new Case("myspool", "myspool", SpoolOption.DEFAULT_FILE_NUM, SpoolOption.DEFAULT_INTERVAL),
                        new Case("myspool:", "myspool", SpoolOption.DEFAULT_FILE_NUM, SpoolOption.DEFAULT_INTERVAL),
                        new Case("myspool::", "myspool", SpoolOption.DEFAULT_FILE_NUM, SpoolOption.DEFAULT_INTERVAL),
                        new Case("myspool:4h", "myspool", 960, SpoolOption.DEFAULT_INTERVAL),
                        new Case("foo:337:6s", "foo", 337, 60000000000L),
                        new Case("/var/data/bar:16s", "/var/data/bar", 2, SpoolOption.DEFAULT_INTERVAL),
                };

        for(Case c: cases)
        {
            SpoolOption out = subject.convert(c.input);
            assertEquals(c.expectedSpoolDir, out.spoolDir);
            assertEquals(c.expectedNumFiles, out.numFiles);
            assertEquals(c.expectedFileInterval, out.fileInterval);
        }

        //####################################
        // test illegal values
        //####################################
        String[] baddies = new String[]
                {
                        null,
                        "",
                        " ",
                        "    ",
                        ":5h",
                        "hitspool: ",
                        "hitspool: : ",
                        "hitspool:44typo:9999999:",
                        "hitspool:44:9999999typo:",

                };
        for (String bad : baddies) {
            try
            {
                SpoolOption out = subject.convert(bad);
                fail(String.format("String [%s] should have been rejected but parsed to [[%s], [%d], [%d]",
                        bad, out.spoolDir, out.numFiles, out.fileInterval));
            }
            catch (CommandLine.TypeConversionException tce)
            {
                //desired
            }
        }
    }
}
