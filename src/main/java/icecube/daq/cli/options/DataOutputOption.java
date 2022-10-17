package icecube.daq.cli.options;

import icecube.daq.bindery.BufferConsumer;
import icecube.daq.cli.util.ArchiveWriter;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.spool.RecordSpool;
import org.apache.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Encapsulates a syntax to specify data output on the command line.
 *
 */
public class DataOutputOption
{

    private static final Logger logger = Logger.getLogger(DataOutputOption.class);


    public static class FileOutputOption
    {
        public final String filename;

        public FileOutputOption(String filename)
        {
            this.filename = filename;
        }

        public BufferConsumer plumbOutput() throws IOException
        {
            logger.info(String.format("Opening output file [%s]", filename));
            return new ArchiveWriter(new File(filename));
        }

        public BufferConsumer plumbOutput(String streamId) throws IOException
        {
            // convert  foo.gz to foo.basename.gz
            //          foo.bar.gz to foo.bar.basename.gz
            //          .gz to basename.gz
            //
            //          foo to foo.basename
            //          foo.bar to foo.bar.basename
            String streamFileName;
            if(filename.endsWith(".gz") || filename.endsWith(".bz2"))
            {
                String tmpName = "";
                String[] tokens = filename.split("\\.");
                for (int i = 0; i < tokens.length; i++) {

                    String dot = (i>0)?".":"";
                    if(i == (tokens.length - 1))
                    {
                        tmpName +=  dot + streamId + "." + tokens[i];
                    }
                    else
                    {
                        tmpName += dot + tokens[i];
                    }

                }
                streamFileName = tmpName;
            }
            else
            {
                streamFileName = filename + "." + streamId;
            }
            logger.info(String.format("Opening output file [%s]", streamFileName));
            return new ArchiveWriter(new File(streamFileName));

        }


        public static class Converter implements CommandLine.ITypeConverter<FileOutputOption>
        {

            @Override
            public FileOutputOption convert(String s) throws Exception
            {
                if (s == null || s.equals("/dev/null")) {
                    throw new CommandLine.TypeConversionException("File output path[ " + s + "] is not permitted");
                } else {
                    return new FileOutputOption(s);
                }
            }
        }
    }


    public static class SpoolOutputOption
    {
        public final SpoolOption spool;

        public SpoolOutputOption(SpoolOption spool)
        {
            this.spool = spool;
        }

        public BufferConsumer plumbOutput(RecordReader rr) throws IOException
        {
            RecordSpool recordSpool = spool.plumbSpool(rr);
            return new BufferConsumer()
            {
                @Override
                public void consume(ByteBuffer buf) throws IOException
                {
                    recordSpool.store(buf);
                }

                @Override
                public void endOfStream(long token) throws IOException
                {
                    recordSpool.closeWrite();
                }
            };
        }

        public BufferConsumer plumbOutput(RecordReader rr, String StreamId) throws IOException
        {
            String streamSpoolName = spool.spoolName + "-" + StreamId;
            SpoolOption streamSpoolOption = new SpoolOption(spool.spoolDir, streamSpoolName,
                    spool.numFiles, spool.fileInterval);
            RecordSpool streamSpool = streamSpoolOption.plumbSpool(rr);
            return new BufferConsumer()
            {
                @Override
                public void consume(ByteBuffer buf) throws IOException
                {
                    streamSpool.store(buf);
                }

                @Override
                public void endOfStream(long token) throws IOException
                {
                    streamSpool.closeWrite();
                }
            };
        }

        static class Converter implements CommandLine.ITypeConverter<SpoolOutputOption>
        {
            @Override
            public SpoolOutputOption convert(String s) throws Exception
            {
                SpoolOption.Converter converter = new SpoolOption.Converter();
                SpoolOption spoolOption = converter.convert(s);

                return new SpoolOutputOption(spoolOption);
            }
        }

    }


    // compose file and spool under a single option
    public static interface ComplexOutputOption
    {

        public BufferConsumer plumbOutput(RecordReader rr) throws IOException;
        public BufferConsumer plumbOutput(RecordReader rr, String streamID) throws IOException;

        static class Converter implements CommandLine.ITypeConverter<ComplexOutputOption>
        {
            @Override
            public ComplexOutputOption convert(String s) throws Exception
            {
                if (s==null || s.equals("/dev/null"))
                {
                    return new ComplexOutputOption()
                    {
                        BufferConsumer nullConsumer = new BufferConsumer()
                        {
                            @Override
                            public void consume(ByteBuffer buf) throws IOException
                            {

                            }

                            @Override
                            public void endOfStream(long token) throws IOException
                            {

                            }
                        };

                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr) throws IOException
                        {
                            return nullConsumer;
                        }

                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr, String streamID) throws IOException
                        {
                            return nullConsumer;
                        }
                    };
                }
                else if(s.startsWith("spool:"))
                {
                    SpoolOutputOption.Converter converter = new SpoolOutputOption.Converter();
                    SpoolOutputOption spoolOutput = converter.convert(s.substring(6));
                    return new ComplexOutputOption()
                    {
                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr) throws IOException
                        {
                            return spoolOutput.plumbOutput(rr);
                        }

                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr, String streamID) throws IOException
                        {
                            return spoolOutput.plumbOutput(rr, streamID);
                        }
                    };
                }
                else {
                    return new ComplexOutputOption()
                    {
                        FileOutputOption fileOutputOption = new FileOutputOption(s);

                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr) throws IOException
                        {
                            return fileOutputOption.plumbOutput();

                        }

                        @Override
                        public BufferConsumer plumbOutput(RecordReader rr, String streamID) throws IOException
                        {
                            return fileOutputOption.plumbOutput(streamID);
                        }
                    };
                }
            }
        }

    }


}