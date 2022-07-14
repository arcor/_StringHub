package icecube.daq.cli.options;

import icecube.daq.cli.options.FlasherConfigOption.FlasherConfigCLIConverter;
import icecube.daq.util.FlasherboardConfiguration;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FlasherConfigOptionTest
{
    FlasherConfigCLIConverter subject = new FlasherConfigCLIConverter();

    @Test
    public void testConstruction()
    {

        class Case
        {
            final String spec;
            final String expectedMbid;
            final int expectedBrigthness;
            final int expectedWidth;
            final int expectedDelay;
            final int expectedMask;
            final int expectedRate;

            Case(String spec, String expectedMbid, int expectedBrigthness, int expectedWidth,
                 int expectedDelay, int expectedMask, int expectedRate)
            {
                this.spec = spec;
                this.expectedMbid = expectedMbid;
                this.expectedBrigthness = expectedBrigthness;
                this.expectedWidth = expectedWidth;
                this.expectedDelay = expectedDelay;
                this.expectedMask = expectedMask;
                this.expectedRate = expectedRate;
            }
        }

        Case[] good = new Case[]
                {
                        new Case("dom:a1a1a1a1,brightness:1,width:2,delay:3,mask:4,rate:5",
                                "a1a1a1a1", 1, 2, 3, 4, 5),
                        new Case("dom:a1a1a1a1,mask:4,width:2,rate:5,delay:3,brightness:1",
                                "a1a1a1a1", 1, 2, 3, 4, 5),
                        new Case("dom:a1a1a1a1,mask:0x0F,width:2,rate:5,delay:3,brightness:1",
                                "a1a1a1a1", 1, 2, 3, 0x0F, 5),
                        new Case("dom:a1a1a1a1,mask:32767,width:32767,rate:32767,delay:32767,brightness:32767",
                                "a1a1a1a1", 32767, 32767, 32767, 32767, 32767),
                        new Case("dom:Cxo,mask:32767,width:32767,rate:32767,delay:32767,brightness:32767",
                                "c01a534234dd", 32767, 32767, 32767, 32767, 32767),
                };

        for(Case c : good)
        {
            FlasherboardConfiguration res = subject.convert(c.spec);
            assertEquals(c.expectedMbid, res.getMainboardID());
            assertEquals(c.expectedBrigthness, res.getBrightness());
            assertEquals(c.expectedWidth, res.getWidth());
            assertEquals(c.expectedDelay, res.getDelay());
            assertEquals(c.expectedMask, res.getMask());
            assertEquals(c.expectedRate, res.getRate());

        }

        String[] bad = new String[]
                {
                        "dom:doesnt_exist,brightness:1,width:2,delay:3,mask:4,rate:5",
                        "dom:a1a1a1a1,zzzbrightness:1,width:2,delay:3,mask:4,rate:5",
                        "dom:a1a1a1a1,mask:4,rate:5,delay:3,brightness:1",
                        "dom:a1a1a1a1,mask:0xDDDD,width:2,rate:5,delay:3,brightness:1",
                        "dom:a1a1a1a1,mask:1,width:2,rate:32768,delay:3,brightness:1",
                };

        for(String spec : bad)
        {
            try {
                FlasherboardConfiguration res = subject.convert(spec);
                fail(String.format("%s accepted bad flasher spec [%s]", FlasherConfigCLIConverter.class.getName(),
                        spec));
            } catch (CommandLine.TypeConversionException e) {
                //desired
            }
        }




    }
}
