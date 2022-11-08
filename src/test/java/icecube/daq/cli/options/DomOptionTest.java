package icecube.daq.cli.options;

import icecube.daq.util.DOMInfo;
import icecube.daq.util.LocatePDAQ;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import icecube.daq.cli.options.DomOption.*;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DomOptionTest
{
    @Before
    public void setup()
    {
        BasicConfigurator.configure();

        // prior tests may have set the pdaq config directory
        // to a test location
        LocatePDAQ.clearCache();
    }

    @Test
    public void testMbidConversion()
    {
        MbidConverter subject = new MbidConverter();

        class Case
        {
            final String spec;
            final long expected;

            Case(String spec, long expected)
            {
                this.spec = spec;
                this.expected = expected;
            }
        }

        Case[] good = new Case[]
                {
                        new Case("0", 0),
                        new Case("123", 0x123),
                        new Case("0x123", 0x123),
                        new Case("0x12e22b334", 0x12e22b334L),

                        new Case("Doxophobia", 0x69A843A5E181L),
                };

        for(Case c : good)
        {
            Long res = subject.convert(c.spec);
            assertEquals(c.expected, res.longValue());

        }


        String[] bad = new String[]
                {
                        "",
                        "-8",
                        "g",
                        "i_am_not_a_known_dom_name"
                };
        for(String spec : bad)
        {
            try {
                Long res = subject.convert(spec);
                fail(String.format("%s resolved illegal mbid string %s as %x", subject.getClass().getName(), spec, res.longValue()));
            } catch (CommandLine.TypeConversionException e) {
                //desired
            }

        }
    }

    @Test
    public void testMbidListConversion()
    {
        MBIDListConverter subject = new MBIDListConverter();

        class Case
        {
            final String spec;
            final long[] expectedMbids;

            Case(String spec, long[] expectedMbids)
            {
                this.spec = spec;
                this.expectedMbids = expectedMbids;
            }
        }

        Case[] good = new Case[]
                {
                        new Case("0", new long[]{0}),
                        new Case("123", new long[]{0x123}),
                        new Case("0x123", new long[]{0x123}),
                        new Case("Doxophobia", new long[]{0x69A843A5E181L}),
                        new Case("Doxophobia,0x69A843A5E181", new long[]{0x69A843A5E181L}), //de-duplicate

                        new Case("Doxophobia,0x12e22b334", new long[]{0x69A843A5E181L, 0x12e22b334L}),
                        new Case("Doxophobia,hub:201,0xeeeeeeee,0xeeeeeeee",
                                new long[]{0x69A843A5E181L, 0xeeeeeeeeL,
                                0x10344741a2f6L, 0x18b445cafae3L, 0x1c67337a4b44L, 0x1ea49e87099aL,
                                0x21a709d3d347L, 0x220429d7a7b4L, 0x26fc676d9182L, 0x2d19f6709412L,
                                0x3681e9662126L, 0x39368d8f1d7cL, 0x45bb1c8696abL, 0x4fc540c56b6cL,
                                0x53d98fecf007L, 0x5ec891d83b6aL, 0x6540d583f8a7L, 0x700721bb194dL,
                                0x7119578dde4aL, 0x772a80d66795L, 0x787bfc0b3113L, 0x78af15a33c5bL,
                                0x7a372d7364d9L, 0x8c7b03a07541L, 0x93a6a8f49425L, 0x9a227030a18dL,
                                0xa218a7e0a3a4L, 0xa2aed1ac2000L, 0xb4faf7b177c7L, 0xb91a2f8040a7L,
                                0xd271bfaef0c9L, 0xdd326db4fdadL, 0xed51beb261daL, 0xfd580731607fL
                        }),
                        new Case("string:27", new long[]{
                                0x608bf3229caL, 0xcae98d7e7afL, 0x16be296642fbL, 0x1bab7411b904L,
                                0x24c946ac1351L, 0x2518d59e32d1L, 0x25aa1596894bL, 0x33370227f166L,
                                0x3d19d4b790c5L, 0x4029969df885L, 0x4293a966b3acL, 0x45345c32c5e5L,
                                0x483008578a98L, 0x494299d1099fL, 0x4e4332716213L, 0x4faed11485e0L,
                                0x507589a5e5ceL, 0x512e359a643bL, 0x56283d5eef84L, 0x5d70990a445dL,
                                0x5e87fccefb9fL, 0x5ecfdc496107L, 0x617084e31091L, 0x619ccecb98eaL,
                                0x64cb95fa78a9L, 0x65eeebb1c1f2L, 0x6734654db93bL, 0x6d7efbd5e694L,
                                0x833e5212e1e8L, 0x8b4c57c763cfL, 0x8ba1d0f31adaL, 0x8c0aa8139acaL,
                                0x94dedff58331L, 0x9d383152121bL, 0x9d6d1cf2ce29L, 0xa2ad5690d590L,
                                0xa8d62493ff3fL, 0xae4fa5356e28L, 0xaf6f51ac422cL, 0xb0088ef7c8aaL,
                                0xb6cf4c4b1210L, 0xb7859d68e8c1L, 0xba50472909b4L, 0xbbc6dac6c156L,
                                0xbc62125efdc0L, 0xc08647a47bc8L, 0xc4efcc141b73L, 0xc65322ffe974L,
                                0xcbc451cda964L, 0xd04bbe33cae6L, 0xd2db85647366L, 0xdaa1eefbcc54L,
                                0xdd788139c03dL, 0xdf433894c6c6L, 0xdfd3ff66933fL, 0xdfdb80cfeed8L,
                                0xe07086ea806fL, 0xe2e89dee3102L, 0xedc24bfa1009L, 0xef7fc42aaeddL,
                                0xf359b616c6c2L, 0xf996d1619febL, 0xfae52e9ef79bL, 0xff1cca741c04L,

                        }),
                        new Case("domset:DMICE_TRIG", new long[]{
                                0x4f1d6939870L,  0x37793e5448e1L, 0x8c40d705fc4bL, 0x99b591c21d6eL
                        }),

                };

        for(Case c : good)
        {
            List<Long> res = subject.convert(c.spec);

            List<Long> expected = Arrays.stream(c.expectedMbids).boxed().collect(Collectors.toList());
            expected.sort(Long::compare);
            res.sort(Long::compare);

//            StringBuilder sb = new StringBuilder();
//            res.forEach(l -> sb.append(String.format("0x%12xL, ", l.longValue())));
//            System.out.println(sb);

            assertEquals(expected,res);

        }

        String[] bad = new String[]
                {
                        "",
                        "-8",
                        "g",
                        "i_am_not_a_known_dom_name",
                        "Doxophobia,hub:431x,0xeeeeeeee",
                        "stringtypo:55",
                        "string:55x",
                        "hub:",
                        "string:",
                        "domset:",
                };

        for(String spec : bad)
        {
            try {
                List<Long> res = subject.convert(spec);
                fail(String.format("%s resolved illegal mbid string %s as %s", subject.getClass().getName(), spec, res.toString()));
            } catch (CommandLine.TypeConversionException e) {
                //desired
            }

        }

    }


    @Test
    public void testDomInfoConversion()
    {
        DOMInfoConverter subject = new DOMInfoConverter();

        class Case
        {
            final String spec;
            final long expectedMbid;
            final String expectedName;

            Case(String spec, long expected, String expectedName)
            {
                this.spec = spec;
                this.expectedMbid = expected;
                this.expectedName = expectedName;
            }
        }

        Case[] good = new Case[]
                {
                        new Case("0x69A843A5E181", 0x69A843A5E181L, "Doxophobia"),
                        new Case("69A843A5E181", 0x69A843A5E181L, "Doxophobia"),
                        new Case("Doxophobia", 0x69A843A5E181L, "Doxophobia"),
                };

        for(Case c : good)
        {
            DOMInfo res = subject.convert(c.spec);
            assertEquals(c.expectedMbid, res.getNumericMainboardId());
            assertEquals(c.expectedName, res.getName());
        }


        String[] bad = new String[]
                {
                        "",
                        "-8",
                        "g",
                        "i_am_not_a_known_dom_name"
                };
        for(String spec : bad)
        {
            try {
                DOMInfo res = subject.convert(spec);
                fail(String.format("%s resolved illegal mbid string %s as %s", subject.getClass().getName(), spec, res.getMainboardId()));
            } catch (CommandLine.TypeConversionException e) {
                //desired
            }

        }
    }


    @Test
    public void testDomInfoListConversion()
    {
        DOMInfoListConverter subject = new DOMInfoListConverter();

        class Case
        {
            final String spec;
            final long[] expectedMbids;

            Case(String spec, long[] expectedMbids)
            {
                this.spec = spec;
                this.expectedMbids = expectedMbids;
            }
        }

        Case[] good = new Case[]
                {

                        new Case("Doxophobia", new long[]{0x69A843A5E181L}),

                        new Case("Doxophobia,0x69A843A5E181", new long[]{0x69A843A5E181L}), //de-duplicate

                        new Case("Doxophobia,0x65eeebb1c1f2", new long[]{0x69A843A5E181L, 0x65eeebb1c1f2L}),
                        new Case("Doxophobia,hub:201,0x65eeebb1c1f2", new long[]{0x69A843A5E181L, 0x65eeebb1c1f2L,
                                0x10344741a2f6L, 0x18b445cafae3L, 0x1c67337a4b44L, 0x1ea49e87099aL,
                                0x21a709d3d347L, 0x220429d7a7b4L, 0x26fc676d9182L, 0x2d19f6709412L,
                                0x3681e9662126L, 0x39368d8f1d7cL, 0x45bb1c8696abL, 0x4fc540c56b6cL,
                                0x53d98fecf007L, 0x5ec891d83b6aL, 0x6540d583f8a7L, 0x700721bb194dL,
                                0x7119578dde4aL, 0x772a80d66795L, 0x787bfc0b3113L, 0x78af15a33c5bL,
                                0x7a372d7364d9L, 0x8c7b03a07541L, 0x93a6a8f49425L, 0x9a227030a18dL,
                                0xa218a7e0a3a4L, 0xa2aed1ac2000L, 0xb4faf7b177c7L, 0xb91a2f8040a7L,
                                0xd271bfaef0c9L, 0xdd326db4fdadL, 0xed51beb261daL, 0xfd580731607fL
                        }),
                        new Case("string:27", new long[]{
                                0x608bf3229caL, 0xcae98d7e7afL, 0x16be296642fbL, 0x1bab7411b904L,
                                0x24c946ac1351L, 0x2518d59e32d1L, 0x25aa1596894bL, 0x33370227f166L,
                                0x3d19d4b790c5L, 0x4029969df885L, 0x4293a966b3acL, 0x45345c32c5e5L,
                                0x483008578a98L, 0x494299d1099fL, 0x4e4332716213L, 0x4faed11485e0L,
                                0x507589a5e5ceL, 0x512e359a643bL, 0x56283d5eef84L, 0x5d70990a445dL,
                                0x5e87fccefb9fL, 0x5ecfdc496107L, 0x617084e31091L, 0x619ccecb98eaL,
                                0x64cb95fa78a9L, 0x65eeebb1c1f2L, 0x6734654db93bL, 0x6d7efbd5e694L,
                                0x833e5212e1e8L, 0x8b4c57c763cfL, 0x8ba1d0f31adaL, 0x8c0aa8139acaL,
                                0x94dedff58331L, 0x9d383152121bL, 0x9d6d1cf2ce29L, 0xa2ad5690d590L,
                                0xa8d62493ff3fL, 0xae4fa5356e28L, 0xaf6f51ac422cL, 0xb0088ef7c8aaL,
                                0xb6cf4c4b1210L, 0xb7859d68e8c1L, 0xba50472909b4L, 0xbbc6dac6c156L,
                                0xbc62125efdc0L, 0xc08647a47bc8L, 0xc4efcc141b73L, 0xc65322ffe974L,
                                0xcbc451cda964L, 0xd04bbe33cae6L, 0xd2db85647366L, 0xdaa1eefbcc54L,
                                0xdd788139c03dL, 0xdf433894c6c6L, 0xdfd3ff66933fL, 0xdfdb80cfeed8L,
                                0xe07086ea806fL, 0xe2e89dee3102L, 0xedc24bfa1009L, 0xef7fc42aaeddL,
                                0xf359b616c6c2L, 0xf996d1619febL, 0xfae52e9ef79bL, 0xff1cca741c04L,

                        }),
                        new Case("string:27,0x483008578a98,0xc4efcc141b73", new long[]{              //de-duplicate
                                0x608bf3229caL, 0xcae98d7e7afL, 0x16be296642fbL, 0x1bab7411b904L,
                                0x24c946ac1351L, 0x2518d59e32d1L, 0x25aa1596894bL, 0x33370227f166L,
                                0x3d19d4b790c5L, 0x4029969df885L, 0x4293a966b3acL, 0x45345c32c5e5L,
                                0x483008578a98L, 0x494299d1099fL, 0x4e4332716213L, 0x4faed11485e0L,
                                0x507589a5e5ceL, 0x512e359a643bL, 0x56283d5eef84L, 0x5d70990a445dL,
                                0x5e87fccefb9fL, 0x5ecfdc496107L, 0x617084e31091L, 0x619ccecb98eaL,
                                0x64cb95fa78a9L, 0x65eeebb1c1f2L, 0x6734654db93bL, 0x6d7efbd5e694L,
                                0x833e5212e1e8L, 0x8b4c57c763cfL, 0x8ba1d0f31adaL, 0x8c0aa8139acaL,
                                0x94dedff58331L, 0x9d383152121bL, 0x9d6d1cf2ce29L, 0xa2ad5690d590L,
                                0xa8d62493ff3fL, 0xae4fa5356e28L, 0xaf6f51ac422cL, 0xb0088ef7c8aaL,
                                0xb6cf4c4b1210L, 0xb7859d68e8c1L, 0xba50472909b4L, 0xbbc6dac6c156L,
                                0xbc62125efdc0L, 0xc08647a47bc8L, 0xc4efcc141b73L, 0xc65322ffe974L,
                                0xcbc451cda964L, 0xd04bbe33cae6L, 0xd2db85647366L, 0xdaa1eefbcc54L,
                                0xdd788139c03dL, 0xdf433894c6c6L, 0xdfd3ff66933fL, 0xdfdb80cfeed8L,
                                0xe07086ea806fL, 0xe2e89dee3102L, 0xedc24bfa1009L, 0xef7fc42aaeddL,
                                0xf359b616c6c2L, 0xf996d1619febL, 0xfae52e9ef79bL, 0xff1cca741c04L,

                        }),
                        new Case("domset:DMICE_TRIG", new long[]{
                                0x4f1d6939870L,  0x37793e5448e1L, 0x8c40d705fc4bL, 0x99b591c21d6eL
                        }),

                };

        for(Case c : good)
        {
            List<DOMInfo> res = subject.convert(c.spec);

            List<Long> expected = Arrays.stream(c.expectedMbids).boxed().collect(Collectors.toList());
            expected.sort(Long::compare);

            List<Long> out = res.stream().map(domInfo -> (domInfo.getNumericMainboardId())).collect(Collectors.toList());
            out.sort(Long::compare);

            assertEquals(expected, out);

        }


        String[] bad = new String[]
                {
                        "",
                        "0",
                        "123",
                        "0x123",
                        "-8",
                        "g",
                        "i_am_not_a_known_dom_name",
                        "Doxophobia,hub:431x,0xeeeeeeee",
                        "stringtypo:55",
                        "hub:",
                        "string:",
                        "domset:",
                };

        for(String spec : bad)
        {
            try {
                List<DOMInfo> res = subject.convert(spec);

                StringBuilder sb = new StringBuilder();
                res.forEach(domInfo -> sb.append(String.format("0x%s, ", domInfo.getMainboardId())));
                fail(String.format("%s resolved illegal mbid string %s as %s", subject.getClass().getName(), spec, sb.toString()));
            } catch (CommandLine.TypeConversionException e) {
                //desired
            }

        }

    }

}
