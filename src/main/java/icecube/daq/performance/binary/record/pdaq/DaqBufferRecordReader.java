package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.UTCRecordReader;

import java.nio.ByteBuffer;

/**
 * A base definition of DAQ Buffer records.
 *
 * --------------------------------------------------------------------------------------------
 * | length (uint32)      | type (uint32)        |        mbid (uint64)                       |
 * --------------------------------------------------------------------------------------------
 * |               padding (uint64)              |        utc (uint64)                        |
 * --------------------------------------------------------------------------------------------
 * |                                payload (byte[length-32]) ...                             |
 * --------------------------------------------------------------------------------------------
 *
 *
 */
public class DaqBufferRecordReader extends TypeCodeRecordReader
        implements UTCRecordReader
{

    private static final String DOC =
        "--------------------------------------------------------------------------------------------\n" +
        "| length (uint32)      | type (uint32)        |        mbid (uint64)                       |\n" +
        "--------------------------------------------------------------------------------------------\n" +
        "|               padding (uint64)              |        utc (uint64)                        |\n" +
        "--------------------------------------------------------------------------------------------\n" +
        "|                                payload (byte[length-32]) ...                             |\n" +
        "--------------------------------------------------------------------------------------------\n" +
        "type 2   : Engineering Hit\n" +
        "type 3   : Delta Compressed Hit\n" +
        "type 102 : Monitoring Record\n" +
        "type 202 : Time Calibration Record\n" +
        "type 302 : Supernova Record\n";


    public static final DaqBufferRecordReader instance =
            new DaqBufferRecordReader();

    public static final int DOM_ID_OFFSET = 8;
    public static final int PADDING_OFFSET = 16;
    public static final int UTC_OFFSET = 24;

    protected DaqBufferRecordReader(){}

    public long getDOMID(final ByteBuffer buffer)
    {
        return buffer.getLong(DOM_ID_OFFSET);
    }
    public long getDOMID(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + DOM_ID_OFFSET);
    }
    public long getDOMID(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + DOM_ID_OFFSET);
    }

    public long getPadding(final ByteBuffer buffer)
    {
        return buffer.getLong(PADDING_OFFSET);
    }
    public long getPadding(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + PADDING_OFFSET);
    }
    public long getPadding(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + PADDING_OFFSET);
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

    public boolean isEOS(ByteBuffer buffer)
    {
        return (getLength(buffer) == 32 && getUTC(buffer) == Long.MAX_VALUE);
    }


    /**
     * encapsulate fields for generic use
     */
    public static final RecordReader.LongField MBID_FIELD = new RecordReader.LongField(){
        @Override
        public long value(ByteBuffer buffer, int offset)
        {
            return instance.getDOMID(buffer, offset);
        }

        @Override
        public long value(RecordBuffer buffer, int offset)
        {
            return instance.getDOMID(buffer, offset);
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
