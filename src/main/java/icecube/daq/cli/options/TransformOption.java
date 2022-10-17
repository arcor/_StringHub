package icecube.daq.cli.options;

import icecube.daq.cli.transform.Transform;
import icecube.daq.cli.stream.RecordType;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Encapsulates a syntax to specify record transforms on the command line.
 *
 * Recognizes:
 *
 * 2ndbuild2pdaq
 *
 * noop
 * "noop:a"
 * "noop:b"
 *
 */
public class TransformOption implements  CommandLine.ITypeConverter<Transform>
{

    public static class TransformerCompletions extends ArrayList<String>
    {
        TransformerCompletions() { super(Arrays.stream(TransformType.values()).map(r -> r.keyword).collect(Collectors.toList()));}
    }

    enum TransformType
    {
        SECONDBUILD_TO_PDAQ("2ndbuild2pdaq","")
                {
                    @Override
                    Transform resolve(String value)
                    {
                        return Transform.secondBuildToPdaq();
                    }
                },
        NOOP("noop", "noop:id")
                {
                    @Override
                    Transform resolve(String value)
                    {
                        return new Transform()
                        {
                            @Override
                            public String describe()
                            {
                                return String.format("noop:value");
                            }

                            @Override
                            public Function<ByteBuffer, ByteBuffer> asMapper(RecordType inputType)
                            {
                                return bb -> bb;
                            }

                            @Override
                            public RecordType outputType(RecordType inputType)
                            {
                                return inputType;
                            }

                        };
                    }
                };

        final String keyword;
        final String doc;

        static Map<String, TransformType> LOOKUP_MAP;

        static {
            Map<String, TransformType> map = new ConcurrentHashMap<>();
            for (TransformType t : TransformType.values()) {
                map.put(t.keyword, t);
            }
            LOOKUP_MAP = Collections.unmodifiableMap(map);
        }


        TransformType(String keyword, String doc)
        {
            this.keyword = keyword;
            this.doc = doc;
        }

        public static TransformType lookup(String keyword)
        {
            return LOOKUP_MAP.get(keyword);
        }


        abstract Transform resolve(String value);

        static Transform resolve(String keyword, String value)
        {
            TransformType transform = lookup(keyword);
            if(transform != null)
            {
                return transform.resolve(value);
            }
            else
            {
                throw new CommandLine.TypeConversionException(
                        String.format("bad transform argument, no transform type [%s]", keyword));
            }
        }
    }


    @Override
    public Transform convert(String s) throws Exception
    {
        if(s.contains(":"))
        {
            String keyword = s.split(":")[0];
            String val = s.substring(s.indexOf(":") + 1);

            return TransformType.resolve(keyword, val);
        }
        else
        {
            return TransformType.resolve(s, "");
        }
    }



    public static void main(String[] args) throws Exception
    {
        TransformOption option = new TransformOption();

        System.out.println(option.convert("2ndbuild2pdaq").describe());
    }

}
