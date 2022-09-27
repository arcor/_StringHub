package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.UTCRecordReader;

import java.nio.ByteBuffer;

/**
 * A simple hit record
 * -----------------------------------------------------------------------------
 * | length [uint4]    |  type [uint4]=1   |           utc [uint8]             |
 * -----------------------------------------------------------------------------
 * | trig-type [uint4] | config-id [uint4] |  srcid [uint4] |  mbid [uint8]... |
 * -----------------------------------------------------------------------------
 * | ...               | trig-mode [uint2] |
 * -----------------------------------------
 */
public class SimpleHitRecordReader extends TypeCodeRecordReader implements UTCRecordReader
{

    public static final SimpleHitRecordReader instance =
            new SimpleHitRecordReader();

    protected SimpleHitRecordReader(){}

    @Override
    public long getUTC(final ByteBuffer buffer)
    {
        return buffer.getLong(8);
    }
    @Override
    public long getUTC(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + 8);
    }
    @Override
    public long getUTC(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + 8);
    }

    public int getTriggerType(final ByteBuffer buffer)
    {
        return buffer.getInt(16);
    }
    public int getTriggerType(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 16);
    }
    public int getTriggerType(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 16);
    }

    public int getConfigId(final ByteBuffer buffer)
    {
        return buffer.getInt(20);
    }
    public int getConfigId(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 20);
    }
    public int getConfigId(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 20);
    }

    public int getSourceId(final ByteBuffer buffer)
    {
        return buffer.getInt(24);
    }
    public int getSourceId(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 24);
    }
    public int getSourceId(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + 24);
    }

    public long getDOMID(final ByteBuffer buffer)
    {
        return buffer.getLong(28);
    }
    public long getDOMID(final ByteBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + 28);
    }
    public long getDOMID(final RecordBuffer buffer, final int offset)
    {
        return buffer.getLong(offset + 28);
    }

    public short getTriggerMode(final ByteBuffer buffer)
    {
        return buffer.getShort(36);
    }
    public short getTriggerMode(final ByteBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 36);
    }
    public short getTriggerMode(final RecordBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 36);
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
        return MBID_FIELD;
    }

    @Override
    public LongField getMbidField()
    {
        return UTC_FIELD;
    }
}
