package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;

import java.nio.ByteBuffer;


/**
 * Record readers for various record types containing SNDAQ records
 */
public class SNDAQRecordReader
{


    /**
     * In pdaq format
     */
    public static class PDAQ_SNDAQRecordReader extends DaqBufferRecordReader
    {
        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      | type (uint32)=302    |        mbid (uint64)                       |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|               padding (uint64)              |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| #scalars |   type2   |                   scalars (byte[length - 36]) ...                 |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "#scalars: uint16\n" +
                "type2: uint16\n";

        public static final PDAQ_SNDAQRecordReader instance = new PDAQ_SNDAQRecordReader();

        private static final SNPayloadReader payloadReader = new SNPayloadReader(32);

        public short getScalarArrayLen(final ByteBuffer buffer)
        {
            return getScalarArrayLen(buffer, 0);
        }
        public short getScalarArrayLen(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarArrayLen(buffer, offset);
        }
        public short getScalarArrayLen(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getScalarArrayLen(buffer, offset);
        }

        public short getScalarType(final ByteBuffer buffer)
        {
            return getScalarType(buffer, 0);
        }
        public short getScalarType(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarType(buffer, offset);
        }
        public short getScalarType(final RecordBuffer buffer, final int offset)
        {

            return payloadReader.getScalarType(buffer, offset);
        }

        public byte[] getScalarData(final ByteBuffer buffer)
        {
            return getScalarData(buffer, 0);
        }
        public byte[] getScalarData(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarData(buffer, offset);

        }
        public byte[] getScalarData(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getScalarData(buffer, offset);
        }

        @Override
        public String describe()
        {
            return DOC;
        }
    }

    /**
     * In second build format
     */
    public static class SECONDBUILD_SNDAQRecordReader extends SecondBuildRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      | type (uint32)=16     |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                mbid (uint64)                | #scalars |   type2   |  scalars ...        | ...                 |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "#scalars: uint16\n" +
                "type2: uint16\n" +
                "scalars: byte[length - 28]";

        public static SECONDBUILD_SNDAQRecordReader instance = new SECONDBUILD_SNDAQRecordReader();

        private static final SNPayloadReader payloadReader = new SNPayloadReader(24);

        public short getScalarArrayLen(final ByteBuffer buffer)
        {
            return getScalarArrayLen(buffer, 0);
        }
        public short getScalarArrayLen(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarArrayLen(buffer, offset);
        }
        public short getScalarArrayLen(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getScalarArrayLen(buffer, offset);
        }

        public short getScalarType(final ByteBuffer buffer)
        {
            return getScalarType(buffer, 0);
        }
        public short getScalarType(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarType(buffer, offset);
        }
        public short getScalarType(final RecordBuffer buffer, final int offset)
        {

            return payloadReader.getScalarType(buffer, offset);
        }

        public byte[] getScalarData(final ByteBuffer buffer)
        {
            return getScalarData(buffer, 0);
        }
        public byte[] getScalarData(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getScalarData(buffer, offset);

        }
        public byte[] getScalarData(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getScalarData(buffer, offset);
        }
        @Override
        public String describe()
        {
            return DOC;
        }
    }


    private static class SNPayloadReader
    {
        private final int payloadOffset;

        private SNPayloadReader(int payloadOffset)
        {
            this.payloadOffset = payloadOffset;
        }

        public short getScalarArrayLen(final ByteBuffer buffer, final int offset)
        {
            return buffer.getShort(payloadOffset + offset);
        }
        public short getScalarArrayLen(final RecordBuffer buffer, final int offset)
        {
            return buffer.getShort(payloadOffset + offset);
        }


        public short getScalarType(final ByteBuffer buffer, final int offset)
        {
            return buffer.getShort(payloadOffset + offset + 2);
        }
        public short getScalarType(final RecordBuffer buffer, final int offset)
        {
            return buffer.getShort(payloadOffset + offset + 2);
        }

        public byte[] getScalarData(final ByteBuffer buffer, final int offset)
        {
            short len = getScalarArrayLen(buffer, offset);
            byte[] data = new byte[len];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.get(payloadOffset + offset + 4 + i);
            }
            return data;

        }
        public byte[] getScalarData(final RecordBuffer buffer, final int offset)
        {
            short len = getScalarArrayLen(buffer, offset);
            byte[] data = new byte[len];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.getByte(payloadOffset + offset + 4 + i);
            }
            return data;
        }
    }


}
