package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.buffer.RecordBuffers;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.common.BufferContent;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests LengthPrependedRecordReader.java
 */
public class LengthPrependedRecordReaderTest
{

    static class Case
    {
        final RecordReader subject;
        final ByteBuffer dummy;
        final int expectedLength;

        Case(RecordReader subject, ByteBuffer dummy, int expectedLength)
        {
            this.subject = subject;
            this.dummy = dummy;
            this.expectedLength = expectedLength;
        }
    }


    Case[] cases;

    @Before
    public void setUp() throws Exception
    {
        short DUMMY_LENGTH = 2345;
        ByteBuffer dummy16 = ByteBuffer.allocate(DUMMY_LENGTH);
        dummy16.putShort(DUMMY_LENGTH);
        while(dummy16.remaining() > 0)
        {
            dummy16.put((byte) (Math.random() * 256));
        }
        dummy16.flip();

        ByteBuffer dummy32 = ByteBuffer.allocate(DUMMY_LENGTH);
        dummy32.putInt(DUMMY_LENGTH);
        while(dummy32.remaining() > 0)
        {
            dummy32.put((byte) (Math.random() * 256));
        }
        dummy32.flip();

        cases = new Case[]
                {
                        new Case(LengthPrependedRecordReader._16Bit.instance, dummy16, DUMMY_LENGTH),
                        new Case(LengthPrependedRecordReader._32Bit.instance, dummy32, DUMMY_LENGTH)
                };
    }


    @Test
    public void testDeserialize() throws IOException
    {
        for(Case c: cases)
        {
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(c.dummy.array()));
            ByteBuffer result = c.subject.deserialize(channel);
            assertTrue(c.dummy.equals(result));

            assertEquals(c.expectedLength, c.subject.getLength(result));
        }
    }


    @Test
    public void testByteBufferNoOffset()
    {
        for(Case c: cases)
        {
            assertEquals(c.expectedLength, c.subject.getLength(c.dummy));

        }
    }

    @Test
    public void testByteBufferWithOffset()
    {
        for(Case c: cases)
        {
            int OFFSET = 23423;
            ByteBuffer buf = ByteBuffer.allocate(c.expectedLength + OFFSET);
            buf.position(OFFSET);
            buf.put(c.dummy);
            buf.flip();

            assertEquals(c.expectedLength, c.subject.getLength(buf, OFFSET));
        }
    }

    @Test
    public void testRecordBufferNoOffset()
    {
        for(Case c: cases)
        {
            RecordBuffer rb =
                    RecordBuffers.wrap(c.dummy, BufferContent.ZERO_TO_CAPACITY);
            assertEquals(c.expectedLength, c.subject.getLength(rb, 0));
        }
    }

    @Test
    public void testRecordBufferWithOffset()
    {
        for(Case c: cases)
        {
            int OFFSET = 23423;
            ByteBuffer buf = ByteBuffer.allocate(c.expectedLength + OFFSET);
            buf.position(OFFSET);
            buf.put(c.dummy);
            buf.flip();
            RecordBuffer rb =
                    RecordBuffers.wrap(buf, BufferContent.ZERO_TO_CAPACITY);

            assertEquals(c.expectedLength, c.subject.getLength(rb, OFFSET));
        }
    }

}
