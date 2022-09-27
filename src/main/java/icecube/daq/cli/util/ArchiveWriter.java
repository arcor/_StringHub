package icecube.daq.cli.util;

import icecube.daq.bindery.BufferConsumer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.zip.GZIPOutputStream;

/**
 * Supports streaming byte buffers to a file with
 * on-the-fly compression inferred from the filename.
 */
public class ArchiveWriter implements BufferConsumer
{
    final WritableByteChannel channel;

    public ArchiveWriter(File file) throws IOException
    {
        this(file, 8096);
    }

    public ArchiveWriter(File file, int bufSize) throws IOException
    {
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file), bufSize);

        if(file.getName().endsWith(".gz"))
        {
            channel = Channels.newChannel(new GZIPOutputStream(fos));
        }
        else if(file.getName().endsWith(".bz") || file.getName().endsWith(".bz2"))
        {
            channel = Channels.newChannel(new BZip2CompressorOutputStream(fos));
        }
        else
        {
            channel = Channels.newChannel(fos);
        }
    }


    @Override
    public void consume(ByteBuffer buf) throws IOException
    {
        try
        {
            channel.write(buf);
        }
        catch (IOException e)
        {
            throw new Error(e);
        }
    }

    @Override
    public void endOfStream(long token) throws IOException
    {
        channel.close();
    }

}
