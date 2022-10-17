package icecube.daq.cli.options;

import icecube.daq.cli.stream.FileScheme;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

/**
 * Encapsulates a syntax to specify record types on the command line.
 */
public interface FileSchemeOption
{
    // keyword for heuristic discovery of file scheme
    public static final String DISCOVER = "discover";

    // teaches picocli the completion values
    public static class CompletionValues extends FileScheme.CompletionValues
    {
        CompletionValues(){

        }
    }

    FileScheme getScheme(File path) throws IOException;


    class Known implements FileSchemeOption
    {
        private final FileScheme scheme;

        public Known(FileScheme scheme)
        {
            this.scheme = scheme;
        }

        @Override
        public FileScheme getScheme(File path) throws IOException
        {
            return scheme;
        }
    }

    class Discover implements FileSchemeOption
    {

        @Override
        public FileScheme getScheme(File path) throws IOException
        {
            return FileScheme.discover(path, true);
        }
    }

    public static class Converter implements CommandLine.ITypeConverter<FileSchemeOption>
    {

        @Override
        public FileSchemeOption convert(String keyword) throws Exception
        {
            if(DISCOVER.equals(keyword))
            {
                return new FileSchemeOption.Discover();
            }
            else
            {
                FileScheme scheme = FileScheme.lookup(keyword);
                if(scheme == null)
                {
                    throw new CommandLine.TypeConversionException("Unrecognized file scheme name: " + keyword);

                }
                return new FileSchemeOption.Known(scheme);
            }

        }
    }

}
