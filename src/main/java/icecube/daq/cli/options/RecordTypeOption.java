package icecube.daq.cli.options;

import icecube.daq.cli.stream.RecordType;
import picocli.CommandLine;

import java.nio.ByteBuffer;

/**
 * Encapsulates a syntax to specify record types on the command line.
 */
public interface RecordTypeOption
{
        // keyword for heuristic discovery of data type
        public static String DISCOVER = "discover";

        // teaches picocli the completion values
        public static class CompletionValues extends RecordType.CompletionValues
        {
                CompletionValues(){
                        add(DISCOVER);
                }

        }


        RecordType getRecordType(ByteBuffer bb);

        class Known implements RecordTypeOption
        {
                final RecordType known;

                public Known(RecordType known)
                {
                        this.known = known;
                }

                @Override
                public RecordType getRecordType(ByteBuffer bb)
                {
                        return known;
                }
        }

        class Discover implements RecordTypeOption
        {

                @Override
                public RecordType getRecordType(ByteBuffer bb)
                {
                        if(bb == null)
                        {
                                return RecordType.UNKNOWN;
                        }
                        else {
                                return RecordType.discover(bb);
                        }
                }
        }



        public static class Converter implements CommandLine.ITypeConverter<RecordTypeOption>
        {

                @Override
                public RecordTypeOption convert(String keyword) throws Exception
                {
                        if(DISCOVER.equals(keyword))
                        {
                                return new RecordTypeOption.Discover();
                        }
                        else
                        {
                                RecordType type = RecordType.lookup(keyword);
                                if(type == null)
                                {
                                        throw new CommandLine.TypeConversionException("Unrecognized type name: " + keyword);

                                }
                                return new RecordTypeOption.Known(type);
                        }

                }
        }


}
