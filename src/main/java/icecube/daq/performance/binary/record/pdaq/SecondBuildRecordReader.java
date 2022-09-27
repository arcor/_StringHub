package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.UTCRecordReader;

import java.nio.ByteBuffer;

/**
 * A base definition of SecondBuild records.
 *
 * ----------------------------------------------------------------------
 * | length [uint4] |  type (uint4)  |         utc[uint8]               |
 * ----------------------------------------------------------------------
 * |          mbid [unit8]          |           payload...              |
 * ----------------------------------------------------------------------
 * |              ...                                                   |
 * ----------------------------------------------------------------------
 *
 *
 *
 */
public class SecondBuildRecordReader extends TypeCodeRecordReader
        implements UTCRecordReader
{

    private static final String DOC =
            "--------------------------------------------------------------------------------------------\n" +
            "| length (uint32)      |    type (uint32)     |        utc (uint64)                        |\n" +
            "--------------------------------------------------------------------------------------------\n" +
            "|                mbid (uint64)                |      payload (byte[length-24]) ...         |\n" +
            "--------------------------------------------------------------------------------------------\n" +
            "|                                            ...                                           |\n" +
            "--------------------------------------------------------------------------------------------\n" +
            "type 4   : Time Calibration Record\n" +
            "type 5   : Monitoring Record\n" +
            "type 16  : Supernova Record\n";

    public static final SecondBuildRecordReader instance =
            new SecondBuildRecordReader();

    public static final int UTC_OFFSET = 8;
    public static final int DOM_ID_OFFSET = 16;

    protected SecondBuildRecordReader(){}

    public long getDOMId(final ByteBuffer buffer)
    {
        return buffer.getLong(DOM_ID_OFFSET);
    }
    public long getDOMId(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + DOM_ID_OFFSET);
    }
    public long getDOMId(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + DOM_ID_OFFSET);
    }


    @Override
    public long getUTC(final ByteBuffer buffer)
    {
        return buffer.getLong(UTC_OFFSET);
    }
    @Override
    public long getUTC(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + UTC_OFFSET);
    }
    @Override
    public long getUTC(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + UTC_OFFSET);
    }


    public byte[] getPayloadRecord(final ByteBuffer buffer)
    {
        // Note: there may be no data
        byte[] data = new byte[getLength(buffer)-24];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = buffer.get(24 + i);
        }
        return data;
    }
    public byte[] getPayloadRecord(final ByteBuffer buffer, final int offset)
    {
        // Note: there may be no data
        byte[] data = new byte[getLength(buffer, offset)- 24];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = buffer.get(offset + 24 + i);
        }
        return data;
    }
    public byte[] getPayloadRecord(final RecordBuffer buffer, final int offset)
    {
        // Note: there may be no data
        byte[] data = new byte[getLength(buffer, offset)- 24];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = buffer.getByte(offset + 24 + i);
        }
        return data;
    }


    /**
     * encapsulate fields for generic use
     */
    public static final RecordReader.LongField MBID_FIELD = new RecordReader.LongField(){
        @Override
        public long value(ByteBuffer buffer, int offset)
        {
            return instance.getDOMId(buffer, offset);
        }

        @Override
        public long value(RecordBuffer buffer, int offset)
        {
            return instance.getDOMId(buffer, offset);
        }
    };

    public static final RecordReader.LongField UTC_FIELD = new RecordReader.LongField(){
        @Override
        public long value(ByteBuffer buffer, int offset)
        {
            return instance.getUTC(buffer, offset);
        }

        @Override
        public long value(RecordBuffer buffer, int offset)
        {
            return instance.getUTC(buffer, offset);
        }
    };


    @Override
    public LongField getOrderingField()
    {
        return UTC_FIELD;
    }

    @Override
    public LongField getMbidField()
    {
        return MBID_FIELD;
    }

    @Override
    public String describe()
    {
        return DOC;
    }
}
