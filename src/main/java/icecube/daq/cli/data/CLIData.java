package icecube.daq.cli.data;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Packages data into the jar file
 */
public enum CLIData
{

    /**
     * a packaged copy of default dom geometry for cases where
     * ~/config is not available. Non-authoritative and may go
     * out-of-date, so prefer looking to ~/config first.
     */
    FALLBACK_DOM_GEOMETRY("fallback-default-dom-geometry.xml.bz2"),

    /**
     * a packaged copy of the NIST leap seconds file for cases where
     * ~/config is not available. Non-authoritative and may go
     * out-of-date, so prefer looking to ~/config first.
     */
    FALLBACK_LEAPSECOND_FILE("fallback-leap-seconds.txt");


    /** The resource name.*/
    private final String fileName;


    CLIData(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * Load the data set into a stream.
     */
    public InputStream getStream() throws IOException
    {
        InputStream stream = this.getClass().getResourceAsStream(fileName);
        if(fileName.endsWith(".gz"))
        {
            return new GZIPInputStream(stream);
        }
        else if(fileName.endsWith("bz2"))
        {
            return new BZip2CompressorInputStream(stream);
        }
        else
        {
            return stream;
        }
    }
}
