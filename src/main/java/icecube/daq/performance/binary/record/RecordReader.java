package icecube.daq.performance.binary.record;

import icecube.daq.performance.binary.buffer.RecordBuffer;

import java.nio.ByteBuffer;

/**
 * Base interface for reading binary records from a buffer.
 *
 * A record reader is a stateless object that provides member
 * accessor functions that act against a binary store of records.
 *
 * The intention is to allow for the management of a large number
 * of records without a allocating a large nuber of containment
 * objects.
 *
 * Record readers can be thought of as a static class hierarchy. Java
 * does not support static inheritance. Implementing classes should be
 * modeled as a singleton to reinforce this design concept.
 */
public interface RecordReader
{
    // utilizing ByteBuffer
    public int getLength(ByteBuffer buffer);
    public int getLength(ByteBuffer buffer, int offset);

    // utilizing RecordBuffer
    public int getLength(RecordBuffer buffer, int offset);

    /**
     * provide access to common field types for generic use.
     * May return fail-fast implemetations if record type
     * does not provide the field.
     */
    public default LongField getOrderingField()
    {
        return NO_ORDER_FIELD;
    }
    public default LongField getMbidField()
    {
        return NO_MBID_FIELD;
    }

    /**
     * Optional method to provide a human-readable description of the
     * record format
     */
    public default String describe()
    {
        return "N/A";
    }


    /**
     * Generic access to a long field of a record in a buffer.
     */
    interface LongField
    {
        long value(ByteBuffer buffer, int offset);

        long value(RecordBuffer buffer, int offset);
    }

    /**
     * Fail fast reader for records without ordering field
     */
    static RecordReader.LongField NO_ORDER_FIELD = new RecordReader.LongField()
    {
        static final String MSG = "No ordering field in record";

        @Override
        public long value(ByteBuffer buffer, int offset)
        {
            throw new Error(MSG);
        }

        @Override
        public long value(RecordBuffer buffer, int offset)
        {
            throw new Error(MSG);
        }
    };

    /**
     * Fail fast reader for records without mbid field
     */
    static RecordReader.LongField NO_MBID_FIELD = new RecordReader.LongField()
    {
        static final String MSG = "No mbid field in record";

        @Override
        public long value(ByteBuffer buffer, int offset)
        {
            throw new Error(MSG);
        }

        @Override
        public long value(RecordBuffer buffer, int offset)
        {
            throw new Error(MSG);
        }
    };
}
