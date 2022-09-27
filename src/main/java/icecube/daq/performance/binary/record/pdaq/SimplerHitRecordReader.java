package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.UTCRecordReader;

import java.nio.ByteBuffer;

/**
 * A simpler hit record
 * -----------------------------------------------------------------------------
 * | length [uint4]    |  type [uint4]=1   |           utc [uint8]             |
 * -----------------------------------------------------------------------------
 * | channelID [uint2] | trig-mode [uint2] |
 * -----------------------------------------
 */
public class SimplerHitRecordReader extends TypeCodeRecordReader
        implements UTCRecordReader
{

    private static final String DOC =
            "--------------------------------------------------------------------------------------------\n" +
            "| length (uint32)      | type (uint32)=1      |        utc (uint64)                        |\n" +
            "--------------------------------------------------------------------------------------------\n" +
            "|channel_id | trig_mode|\n" +
            "------------------------\n" +
            "channel_id: (uint16)\n" +
            "trig_mode: (uint16)\n";

    public static final SimplerHitRecordReader instance =
            new SimplerHitRecordReader();

    protected SimplerHitRecordReader(){}

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

    public short getChannelId(final ByteBuffer buffer)
    {
        return buffer.getShort(16);
    }
    public short getChannelId(final ByteBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 16);
    }
    public short getChannelId(final RecordBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 16);
    }

    public short getTriggerMode(final ByteBuffer buffer)
    {
        return buffer.getShort(18);
    }
    public short getTriggerMode(final ByteBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 18);
    }
    public short getTriggerMode(final RecordBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + 18);
    }

    /**
     * encapsulate fields for generic use
     */
    public static final LongField UTC_FIELD = new LongField(){
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
        return RecordReader.NO_MBID_FIELD;
    }


    @Override
    public String describe()
    {
        return DOC;
    }
}
