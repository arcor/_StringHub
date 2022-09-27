package icecube.daq.cli.options;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.pdaq.*;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates a syntax to specify record types on the command line.
 */
public class RecordTypeOption
{

        // teaches picocli the completion values
        public static class CompletionValues extends ArrayList<String>
        {
                CompletionValues(){
                        super(Arrays.stream(RecordType.values()).map(r -> r.typeName).collect(Collectors.toList()));

                }

        }

        public enum RecordType
        {

                DISCOVER("discover", NullReader.instance),

                // pdaq internal products
                PDAQ("pdaq-generic", DaqBufferRecordReader.instance),
                PDAQ_HITS("pdaq-hit", DomHitRecordReader.instance),
                PDAQ_ENGR_HIT("pdaq-hit-engr", EngineeringHitRecordReader.instance),
                PDAQ_DELTA_COMPRESSED_HIT("pdaq-hit-delta", DeltaCompressedHitRecordReader.instance),

                PDAQ_TCAL("pdaq-tcal", TCalRecordReader.PDAQ_TCalRecordReader.instance),
                PDAQ_MONI("pdaq-moni", MonitoringRecordReader.PDAQ_MonitoringRecordReader.instance),
                PDAQ_SN("pdaq-sn", SNDAQRecordReader.PDAQ_SNDAQRecordReader.instance),

                PDAQ_SIMPLER_HIT("simpler-hit", SimplerHitRecordReader.instance),

                // 2ndBuild products
                SECOND_BUILD("2ndBuild-generic", SecondBuildRecordReader.instance),
                SECOND_BUILD_TCAL("2ndBuild-tcal", TCalRecordReader.SECONDBUILD_TCalRecordReader.instance),
                SECOND_BUILD_MONI("2ndBuild-moni", MonitoringRecordReader.SECONDBUILD_MonitoringRecordReader.instance),
                SECOND_BUILD_SN("2ndBuild-sn",SNDAQRecordReader.SECONDBUILD_SNDAQRecordReader.instance),

                // Events
                EVENT_V5("eventV5", EventV5RecordReader.instance),

                UNKNOWN("unknown", NullReader.instance);


                public final String typeName;
                public final RecordReader rr;

                static Map<String, RecordType> lookup;

                RecordType(String typeName, RecordReader rr)
                {
                        this.typeName = typeName;
                        this.rr = rr;

                }

                static RecordType lookup(String typename)
                {
                        synchronized (RecordType.class)
                        {
                                if (lookup == null)
                                {
                                        RecordType[] all = RecordType.values();
                                        lookup = new HashMap<>(all.length);
                                        for (RecordType t : all) {
                                                lookup.put(t.typeName, t);
                                        }
                                }

                        }

                        return lookup.get(typename);
                }

                static class NullReader implements RecordReader
                {
                        public static NullReader instance = new NullReader();

                        private NullReader()
                        {
                        }

                        @Override
                        public int getLength(ByteBuffer buffer)
                        {
                                throw new Error("Unknown record type");
                        }

                        @Override
                        public int getLength(ByteBuffer buffer, int offset)
                        {
                                throw new Error("Unknown record type");
                        }

                        @Override
                        public int getLength(RecordBuffer buffer, int offset)
                        {
                                throw new Error("Unknown record type");
                        }

                        @Override
                        public String describe()
                        {
                                return "Unknown Record Type";
                        }
                }
        }

        /**
         * indirection to support "discover" option that
         * examines a sample record(s) to guess the type
         */
        public interface RecordTypeProvider
        {
                RecordType getRecordType(ByteBuffer bb);

                class Known implements RecordTypeProvider
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

                class Discover implements RecordTypeProvider
                {

                        @Override
                        public RecordType getRecordType(ByteBuffer bb)
                        {
                                if(bb == null)
                                {
                                        return RecordType.UNKNOWN;
                                }
                                else {
                                        return guess(bb);
                                }
                        }
                }
        }

        public static class RecordTypeConverter implements CommandLine.ITypeConverter<RecordTypeProvider>
        {

                @Override
                public RecordTypeProvider convert(String typeName) throws Exception
                {

                        RecordType type = RecordType.lookup(typeName);

                        if(type == null)
                        {
                                throw new CommandLine.TypeConversionException("Unrecognized type name: " + typeName);

                        }
                        else
                        {
                               // shoehorn alert:
                                if(type == RecordType.DISCOVER)
                                {
                                        return new RecordTypeProvider.Discover();
                                }
                                else
                                {
                                        return new RecordTypeProvider.Known(type);
                                }
                        }


                }
        }



        public static RecordType guess(ByteBuffer bb)
        {
                if(bb.remaining() < 32)
                {
                        return RecordType.UNKNOWN;
                }

                // what is it?
                /*
                 * A base definition of DAQ Buffer records.
                 *
                 * ----------------------------------------------------------------------
                 * | length [uint4] |  type (uint4)  |         mbid[uint8]              |
                 * ----------------------------------------------------------------------
                 * |          padding [unit8]        |         utc[uint8]               |
                 * ----------------------------------------------------------------------
                 * |              ...                                                   |
                 * ----------------------------------------------------------------------
                 *
                 *
                 *
                 *  * A base definition of SecondBuild records.
                 *  *
                 *  * ----------------------------------------------------------------------
                 *  * | length [uint4] |  type (uint4)  |         utc[uint8]               |
                 *  * ----------------------------------------------------------------------
                 *  * |          mbid [unit8]          |           payload...              |
                 *  * ----------------------------------------------------------------------
                 *  * |              ...                                                   |
                 *  * ----------------------------------------------------------------------
                 *
                 *
                 */
                int length = TypeCodeRecordReader.instance.getLength(bb);
                int typeId = TypeCodeRecordReader.instance.getTypeId(bb);
                long a = bb.getLong(8);
                long b = bb.getLong(16);
                long c = bb.getLong(24);

//        System.out.printf("file: %s%n", file);
//        System.out.printf("len: %d%n", length);
//        System.out.printf("type: %d%n", typeId);
//        System.out.printf("a: %d%n", a);
//        System.out.printf("b: %d%n", b);
//        System.out.printf("c: %d%n", c);
//        System.out.println();

                // 1: go by type
                switch (typeId)
                {
                        case 2:
                                return RecordType.PDAQ_ENGR_HIT;
                        case 3:
                                return RecordType.PDAQ_DELTA_COMPRESSED_HIT;
                        case 102:
                                return RecordType.PDAQ_MONI;

                        case 202:
                                return RecordType.PDAQ_TCAL;

                        case 302:
                                return RecordType.PDAQ_SN;

                        case 4:
                                return RecordType.SECOND_BUILD_TCAL;
                        case 5:
                                return RecordType.SECOND_BUILD_MONI;
                        case 16:
                                return RecordType.SECOND_BUILD_SN;
                        case 21:
                                return RecordType.EVENT_V5;

                        case 24:
                                return RecordType.PDAQ_SIMPLER_HIT;
                }


                // 1: go by pdaq/2ndbuild
                // verify length-type is traversable then go by
                // whether padding generic headers
                boolean traversable = true;
                while(bb.remaining() > 0)
                {
                        length = bb.getInt();
                        if(bb.getInt() != typeId || length > 4096)
                        {
                                traversable = false;
                                break;
                        }
//            System.out.printf("traverse %d of type %d%n", length, typeId);
                        int advance = bb.position() + (length - 8);
                        if(advance < bb.remaining())
                        {
                                bb.position(advance);
                        }
                        else
                        {
                                break;
                        }
                }


                if(traversable && b == 0)
                {
                        return RecordType.PDAQ;
                }
                else if (traversable)
                {
                        return RecordType.SECOND_BUILD;
                }
                else
                {
                        return RecordType.UNKNOWN;
                }


        }
}
