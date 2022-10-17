package icecube.daq.cli.stream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DataInput
{

    static Logger logger = Logger.getLogger(DataInput.class);

    public static InputStream loadFiles(List<String> files)
            throws IOException
    {
        return loadFiles(files.toArray(new String[files.size()]));
    }

    public static InputStream loadFiles(String... filenames)
            throws IOException
    {

        File[] orderedFiles = new File[filenames.length];
        for (int i = 0; i < orderedFiles.length; i++) {
            orderedFiles[i] = new File(filenames[i]);
        }

        return loadFiles(orderedFiles);
    }

    public static InputStream loadFile(String filename)
            throws IOException
    {
        return loadFile(new File(filename));
    }

    public static InputStream loadFile(File file)
            throws IOException
    {
        return new BufferedInputStream(openFile(file));
    }

    private static InputStream openFile(File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) {
            return new GZIPInputStream(fis);
        }
        if (file.getName().endsWith(".bz") || file.getName().endsWith(".bz2")) {
            return new BZip2CompressorInputStream(fis);
        } else {
            return fis;
        }
    }


    public static InputStream loadFiles(File[] orderedFiles)
            throws IOException
    {

        Enumeration<InputStream> streams = new Enumeration<InputStream>()
        {
            int idx = 0;

            @Override
            public boolean hasMoreElements()
            {
                return idx < orderedFiles.length;
            }

            @Override
            public InputStream nextElement()
            {
                try {
                    if (hasMoreElements()) {
                        File file = orderedFiles[idx++]; //post increments
                        logger.info(String.format("Reading source file [%d of %d]  [%s]", idx, orderedFiles.length, file.getName()));
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis, 32 * 1024);
                        if (file.getName().endsWith(".gz")) {
                            return new GZIPInputStream(bis);
                        }
                        if (file.getName().endsWith(".bz2") || file.getName().endsWith(".bz")) {
                            return new BZip2CompressorInputStream(bis);
                        } else {
                            return fis;
                        }
                    } else {
                        throw new Error("misuse");
                    }
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        };


        return new SequenceInputStream(streams);
    }

}
