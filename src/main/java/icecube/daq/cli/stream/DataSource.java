package icecube.daq.cli.stream;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

/**
 * Encapsulates a pdaq record data source.
 */
public interface DataSource
{

    RecordType getRecordType();

    Stream<ByteBuffer> stream() throws IOException;

    String describe();


    public static class FileSource implements DataSource
    {
        final String filepath;
        final RecordType type;
        final InputStream is;

        public FileSource(String filepath, RecordType type) throws IOException
        {
           this(filepath, type, new BufferedInputStream(DataInput.loadFile(filepath), 32768));
        }

        public FileSource(String filepath, RecordType type, InputStream is)
        {
            this.filepath = filepath;
            this.type = type;
            this.is = is;
        }

        @Override
        public RecordType getRecordType()
        {
            return type;
        }

        @Override
        public Stream<ByteBuffer> stream() throws IOException
        {
            return RecordStream.parseRecordsToStream(is, type.rr);
        }


        @Override
        public String describe()
        {
            return String.format("%s:%s", type.keyword, filepath);
        }

    }

    public static class DirSource implements DataSource
    {
        final String dir;
        final FileScheme scheme;
        final RecordType type;
        final InputStream is;

        public DirSource(String dir, FileScheme scheme, RecordType type) throws IOException
        {
            this.dir = dir;
            this.scheme = scheme;
            this.type = type;

            File[] files = scheme.listOrdered(dir, true);
            this.is = new BufferedInputStream(DataInput.loadFiles(files), 32768);
        }

        public DirSource(String dir, FileScheme scheme, RecordType type, InputStream is)
        {
            this.dir = dir;
            this.scheme = scheme;
            this.type = type;
            this.is = is;
        }

        @Override
        public RecordType getRecordType()
        {
            return type;
        }

        @Override
        public Stream<ByteBuffer> stream() throws IOException
        {
            return RecordStream.parseRecordsToStream(is, type.rr);
        }


        @Override
        public String describe()
        {
            return String.format("%s:%s:%s", scheme.keyword, type.keyword, dir);
        }

    }

    public static class InputStreamSource implements DataSource
    {
        final InputStream is;
        final RecordType type;
        final String description;

        public InputStreamSource(InputStream is, RecordType type, String description)
        {

            this.is = is;
            this.type = type;
            this.description = description;
        }

        @Override
        public RecordType getRecordType()
        {
            return type;
        }

        @Override
        public Stream<ByteBuffer> stream() throws IOException
        {
            return RecordStream.parseRecordsToStream(is, type.rr);
        }



        @Override
        public String describe()
        {
            return String.format("%s:%s", type.keyword, description);
        }

    }

}
