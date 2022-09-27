package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.UTCRecordReader;

import java.nio.ByteBuffer;

/**
 * Event version 5 record reader.
 */
public class EventV5RecordReader extends TypeCodeRecordReader
        implements UTCRecordReader
{

    public static final EventV5RecordReader instance =
            new EventV5RecordReader();

    public static final int UTC_OFFSET = 8;

    public static final int UTC_INTERVAL_OFFSET = 16;
    public static final int YEAR_OFFSET = 20;
    public static final int UID_OFFSET = 22; // "event number"
    public static final int RUN_NUM_OFFSET = 26;
    public static final int SUBRUN_OFFSET = 30;
    public static final int NUM_HITS_OFFSET = 34;
    public static final int HIT_RECORDS_OFFSET = 38;

    protected EventV5RecordReader(){}

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

    public int getTimeInteval(final ByteBuffer buffer)
    {
        return buffer.getInt(UTC_INTERVAL_OFFSET);
    }
    public int getTimeInteval(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + UTC_INTERVAL_OFFSET);
    }
    public int getTimeInteval(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + UTC_INTERVAL_OFFSET);
    }


    public short getYear(final ByteBuffer buffer)
    {
        return buffer.getShort(YEAR_OFFSET);
    }
    public short getYear(final ByteBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + YEAR_OFFSET);
    }
    public short getYear(final RecordBuffer buffer, final int offset)
    {
        return buffer.getShort(offset + YEAR_OFFSET);
    }

    public int getUID(final ByteBuffer buffer)
    {
        return buffer.getInt(UID_OFFSET);
    }
    public int getUID(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + UID_OFFSET);
    }
    public int getUID(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + UID_OFFSET);
    }

    public int getRunNumber(final ByteBuffer buffer)
    {
        return buffer.getInt(RUN_NUM_OFFSET);
    }
    public int getRunNumber(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + RUN_NUM_OFFSET);
    }
    public int getRunNumber(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + RUN_NUM_OFFSET);
    }

    public int getSubrun(final ByteBuffer buffer)
    {
        return buffer.getInt(SUBRUN_OFFSET);
    }
    public int getSubrun(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + SUBRUN_OFFSET);
    }
    public int getSubrun(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + SUBRUN_OFFSET);
    }

    public int getNumHits(final ByteBuffer buffer)
    {
        return buffer.getInt(NUM_HITS_OFFSET);
    }
    public int getNumHits(final ByteBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + NUM_HITS_OFFSET);
    }
    public int getNumHits(final RecordBuffer buffer, final int offset)
    {
        return buffer.getInt(offset + NUM_HITS_OFFSET);
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
        return RecordReader.NO_MBID_FIELD;
    }

    @Override
    public LongField getMbidField()
    {
        return UTC_FIELD;
    }

}
