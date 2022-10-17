package icecube.daq.cli.stream;

import icecube.daq.performance.binary.record.RecordReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Generalizes the process of adapting an input source of records into a Stream<ByteBuffer>.
 */
public class RecordStream
{


    /**
     * Adapt an input stream to a stream of records
     * @param src the source stream.
     * @param rr The record reader with knowledge of the record format.
     * @return A stream of records.
     * */
    public static Stream<ByteBuffer> parseRecordsToStream(InputStream src, RecordReader rr)
    {
        RecordIterator recordIterator = new RecordIterator(new RecordReaderDeserializer(src, rr));
        return StreamSupport.stream(recordIterator.asIterable().spliterator(), false);
    }


    /**
     * Assumes records are prefixed by a 32-bit BE length field.
     * @param src the source stream.
     * @return A stream of records.
     */
    public static Stream<ByteBuffer> parseLengthPrependedRecordsToStream(InputStream src)
    {
//        RecordIterator recordIterator = new RecordIterator(new LengthPrependedDeserializer.FromInputStream(src));
        RecordIterator recordIterator = new RecordIterator(new LengthPrependedDeserializer.FromChannel(Channels.newChannel(src)));

        return StreamSupport.stream(recordIterator.asIterable().spliterator(), false);
    }


    /**
     *  Iterates records through a deserializer implementation
     */
    private static class RecordIterator implements Iterator<ByteBuffer>
    {

        final Deserializer deserializer;
        ByteBuffer next;



        public RecordIterator(Deserializer deserializer)
        {
            this.deserializer = deserializer;
        }

        @Override
        public boolean hasNext()
        {
            // support gratuitous calls to hasNext();
            if(next == null) {
                loadNext();
            }

            return next != null;
        }

        @Override
        public ByteBuffer next()
        {
            // normally next will be populated do to a call to hasNext(),
            // but a badly coded iteration is supported here.
            if(next == null) {
                loadNext();
            }

            ByteBuffer tmp = next;
            next = null;
            return tmp;
        }

        private void loadNext()
        {
            try {
                next = deserializer.deserialize();
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        public Iterable<ByteBuffer> asIterable()
        {

            return new Iterable<ByteBuffer>()
            {
                @Override
                public Iterator<ByteBuffer> iterator()
                {
                    return RecordIterator.this;
                }
            };
        }
    }

    // deserialize a record from an Input stream
    interface Deserializer
    {
        /**
         * Extract a single record from the input stream
         *
         * @return The record or null at end of stream.
         * @throws IOException Error reading stream or invalid record.
         */
        ByteBuffer deserialize() throws IOException;
    }

    public static class RecordReaderDeserializer implements Deserializer
    {

        private final ReadableByteChannel channel;
        private final RecordReader rr;

        public RecordReaderDeserializer(InputStream src, RecordReader rr)
        {
           this(Channels.newChannel(src), rr);
        }

        public RecordReaderDeserializer(ReadableByteChannel channel, RecordReader rr)
        {
            this.channel = channel;
            this.rr = rr;
        }

        @Override
        public ByteBuffer deserialize() throws IOException
        {
            return rr.deserialize(channel);
        }
    }

    /**
     * Generic deserializers that assumes a BE 32 bit length-prepended record format
     */
    public static interface LengthPrependedDeserializer
    {
        /**
         * Utilize an InputStream for input
         */
        public static class FromInputStream implements Deserializer
        {

            final InputStream is;

            final ByteBuffer header = ByteBuffer.allocate(4);
            long count;

            public FromInputStream(InputStream is)
            {
                this.is = is;
            }

            @Override
            public ByteBuffer deserialize() throws IOException
            {
                byte[] hdr = new byte[4];

                for (int i = 0; i < 4; i++)
                {
                    int read = is.read(hdr, i, 1);
                    if(read<1)
                    {
                        if(i == 0) { return null; }
                        else
                        {
                            throw new RecordReader.IncompleteRecordException("Reading byte " + i + "of length header at idx " + count);
                        }
                    }
                    count++;
                }

                int length =  hdr[0] << 24 | (hdr[1] & 0xFF) << 16 | (hdr[2] & 0xFF) << 8 | (hdr[3] & 0xFF);

                if(length < 4)
                {

                    throw new IOException(String.format("Invalid record length: %d at idx %d", length, (count-4) ));
                }

                byte[] record = new byte[length];
                System.arraycopy(hdr, 0, record, 0, 4);

                int idx = 4;
                while(idx < length)
                {
                    int read = is.read(record, idx, (length-idx));
                    if(read < 0)
                    {
                        throw new RecordReader.IncompleteRecordException("Reading byte " + idx + " of record at " + count);
                    }
                    idx += read;
                    count += read;
                }


                return ByteBuffer.wrap(record);
            }

        }


        /**
         * Utilize a channel for input
         */
        public static class FromChannel implements Deserializer
        {

            final ReadableByteChannel channel;

            final ByteBuffer header = ByteBuffer.allocate(4);
            long count;

            public FromChannel(ReadableByteChannel channel)
            {
                this.channel = channel;
            }


            @Override
            public ByteBuffer deserialize() throws IOException
            {

                header.clear();
                int read=0;
                try {
                    while (read < 4) {
                        int read1 = channel.read(header);
                        if(read1<0)
                        {
                            if(read == 0) { return null; }
                            else
                            {
                                throw new RecordReader.IncompleteRecordException("Reading byte " + read + "of length header at idx " + count);
                            }
                        }
                        read += read1;
                    }
                } catch (ClosedChannelException e) {
                    if(read == 0) { return null; }
                    else
                    {
                        throw new RecordReader.IncompleteRecordException("Reading byte " + read + "of length header at idx " + count);
                    }
                }


                header.flip();
                int length =  header.getInt();

                if(length < 4)
                {

                    throw new IOException(String.format("Invalid record length: %d at idx %d", length, (count-4) ));
                }

                ByteBuffer record = ByteBuffer.allocate(length);
                record.put((ByteBuffer) header.flip());

                read=0;
                try {
                    while (read < length - 4) {
                        read += channel.read(record);
                    }
                } catch (ClosedChannelException e) {
                    throw new RecordReader.IncompleteRecordException("Reading byte " + read + " of record at " + count);

                }

                record.flip();
                return record;
            }
        }


    }


}
