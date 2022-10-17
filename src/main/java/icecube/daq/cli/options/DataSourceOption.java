package icecube.daq.cli.options;

import icecube.daq.cli.stream.*;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Encapsulates a syntax to specify data sources on the command line.
 */
public class DataSourceOption
{

    @CommandLine.Option(names = {"--source"}, required = true,
            paramLabel = "PATH",
            description = "The path to the data. [FILE | DIR | stdin] %n%n" +
                    "if path is a directory the --file-scheme argument controls selection%n" +
                    "and ordering of file from that path")
    private String source;

    @CommandLine.Option(names = {"--record-type"}, required = false,
            paramLabel = "RECORD_SPEC",
            description = "The record type of the data source%n%n" +
                    "RECORD_SPEC: [${COMPLETION-CANDIDATES}]%n",
            completionCandidates = RecordTypeOption.CompletionValues.class,
            converter = RecordTypeOption.Converter.class)
    private RecordTypeOption recordTypeOption = new RecordTypeOption.Discover();


    @CommandLine.Option(names = {"--file-scheme"}, required = false,
            paramLabel = "FILE_SCHEME",
            description = "The file scheme of the data source, only applicable when specifying a directory source" +
                    "The file scheme of the data source%n%n" +
                    "FILE_SCHEME: [${COMPLETION-CANDIDATES}]%n",
            completionCandidates = FileSchemeOption.CompletionValues.class,
            converter = FileSchemeOption.Converter.class)
    private FileSchemeOption fileSchemeOption = new FileSchemeOption.Discover();


    public RecordType resolvedRecordType;
    public FileScheme resolvedFileScheme;

    public DataSource getDataSource() throws IOException
    {

        if("stdin".equals(source))
        {
            // peek
            BufferedInputStream is = new BufferedInputStream(System.in);
            
            int peekLength = 256;
            ByteBuffer bb = ByteBuffer.allocate(peekLength);

            is.mark(peekLength);

            Channels.newChannel(is).read(bb);

            is.reset();

            bb.flip();

            resolvedRecordType = recordTypeOption.getRecordType(bb);
            resolvedFileScheme = FileScheme.NONE;
            return new DataSource.InputStreamSource(is, resolvedRecordType, "stdin");
        }
        else if(Files.exists(Paths.get(source)) && Files.isRegularFile(Paths.get(source)))
        {
            // peek
            BufferedInputStream is = new BufferedInputStream(DataInput.loadFiles(source), 1024);
            int peekLength = 256;
            ByteBuffer bb = ByteBuffer.allocate(peekLength);

            is.mark(peekLength);

            Channels.newChannel(is).read(bb);

            is.reset();

            bb.flip();

            resolvedRecordType = recordTypeOption.getRecordType(bb);
            resolvedFileScheme = FileScheme.NONE;

            return new  DataSource.FileSource(source, resolvedRecordType, is);
        }
        else if(Files.exists(Paths.get(source)) && Files.isDirectory(Paths.get(source)))
        {
            resolvedFileScheme = fileSchemeOption.getScheme(new File(source));

            if(resolvedFileScheme == null)
            {
                throw new IOException("Could not resolve the data file scheme.");
            }

            File[] files = resolvedFileScheme.listOrdered(source, true);
            
            // peek
            BufferedInputStream is = new BufferedInputStream(DataInput.loadFiles(files), 1024);
            int peekLength = 256;
            ByteBuffer bb = ByteBuffer.allocate(peekLength);

            is.mark(peekLength);

            Channels.newChannel(is).read(bb);

            is.reset();

            bb.flip();

            resolvedRecordType = recordTypeOption.getRecordType(bb);
            
            
            return new  DataSource.DirSource(source, resolvedFileScheme, resolvedRecordType, is);
        }
        else {
            throw new CommandLine.TypeConversionException(
                    String.format("bad data source argument [%s]", source));
        }
    }
    
}