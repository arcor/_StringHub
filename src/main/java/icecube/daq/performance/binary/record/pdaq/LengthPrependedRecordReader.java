package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * Base implementations of length-prepended record readers.
 *
 * _16Bit:
 * ----------------------------------------------------------------------
 * | length [uint16] |                     ...                           |
 * ----------------------------------------------------------------------
 *
 * _32Bit
 * ----------------------------------------------------------------------
 * | length [uint32]                |                     ...           |
 * ----------------------------------------------------------------------
 *
 */
public interface LengthPrependedRecordReader
{

    public static class _16Bit implements RecordReader
    {
        public static final _16Bit instance = new _16Bit();

        @Override
        public ByteBuffer deserialize(ReadableByteChannel src) throws IOException
        {
            ByteBuffer header = ByteBuffer.allocate(2);
            int read=0;
            try {
                while (read < 2) {
                    int read1 = src.read(header);
                    if(read1<0)
                    {
                        if(read == 0) { return null; }
                        else
                        {
                            throw new IncompleteRecordException("Reading byte " + read + "of header");
                        }
                    }
                    read += read1;
                }
            } catch (ClosedChannelException e) {
                if(read == 0) { return null; }
                else
                {
                    throw new IncompleteRecordException("Reading byte " + read + "of length header");
                }
            }


            header.flip();
            int length =  header.getShort();

            if(length < 2)
            {

                throw new IOException(String.format("Invalid record length: %d", length));
            }

            ByteBuffer record = ByteBuffer.allocate(length);
            record.put((ByteBuffer) header.flip());

            read=0;
            try {
                while (read < length - 2) {
                    read += src.read(record);
                }
            } catch (ClosedChannelException e) {
                throw new IncompleteRecordException(String.format("Reading byte %d", read));

            }


            record.flip();
            return record;
        }

        @Override
        public int getLength(final ByteBuffer buffer)
        {
            return buffer.getShort(0);
        }

        @Override
        public int getLength(final ByteBuffer buffer, final int offset)
        {
            return buffer.getShort(offset);
        }

        @Override
        public int getLength(final RecordBuffer buffer, final int offset)
        {
            return buffer.getShort(offset);
        }
    }

    public static class _32Bit implements RecordReader
    {
        public static final _32Bit instance = new _32Bit();

        @Override
        public ByteBuffer deserialize(ReadableByteChannel src) throws IOException
        {
            ByteBuffer header = ByteBuffer.allocate(4);
            int read=0;
            try {
                while (read < 4) {
                    int read1 = src.read(header);
                    if(read1<0)
                    {
                        if(read == 0) { return null; }
                        else
                        {
                            throw new IncompleteRecordException("Reading byte " + read + "of header");
                        }
                    }
                    read += read1;
                }
            } catch (ClosedChannelException e) {
                if(read == 0) { return null; }
                else
                {
                    throw new IncompleteRecordException("Reading byte " + read + "of length header");
                }
            }


            header.flip();
            int length =  header.getInt();

            if(length < 4)
            {

                throw new IOException(String.format("Invalid record length: %d", length));
            }

            ByteBuffer record = ByteBuffer.allocate(length);
            record.put((ByteBuffer) header.flip());

            read=0;
            try {
                while (read < length - 4) {
                    read += src.read(record);
                }
            } catch (ClosedChannelException e) {
                throw new IncompleteRecordException(String.format("Reading byte %d", read));

            }


            record.flip();
            return record;
        }

        @Override
        public int getLength(final ByteBuffer buffer)
        {
            return buffer.getInt(0);
        }

        @Override
        public int getLength(final ByteBuffer buffer, final int offset)
        {
            return buffer.getInt(offset);
        }

        @Override
        public int getLength(final RecordBuffer buffer, final int offset)
        {
            return buffer.getInt(offset);
        }
    }
}
