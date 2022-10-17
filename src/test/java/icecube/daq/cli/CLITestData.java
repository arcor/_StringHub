package icecube.daq.cli;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collection of pdaq data for testing
 */
public class CLITestData
{

    public interface DataResource
    {
        public void extractTo(Path dir) throws IOException;
        public String sourcePath();
    }

    /** single file resources */
    public enum DataFile implements DataResource
    {

        PDAQ_HITSPOOL_DATA("./data/pdaq/pdaq-hitspool.dat.bz2"),
        PDAQ_MONI_DATA("./data/pdaq/pdaq-moni.dat.bz2"),
        PDAQ_SN_DATA("./data/pdaq/pdaq-sn.dat.bz2"),
        PDAQ_TCAL_DATA("./data/pdaq/pdaq-tcal.dat.bz2");

        final String sourcePath;
        final String filename;

        DataFile(String sourcePath)
        {
            this.sourcePath = sourcePath;
            this.filename = new File(sourcePath).getName();
        }

        public void extractTo(Path dir) throws IOException
        {

            Path target = dir.resolve(filename);

            BufferedInputStream fileIn = new BufferedInputStream(getClass().getResourceAsStream(sourcePath));
            try (OutputStream o = Files.newOutputStream(target)) {
                IOUtils.copy(fileIn, o);
            }
        }

        @Override
        public String sourcePath()
        {
            return sourcePath;
        }

        public String getFilename()
        {
            return filename;
        }

    }


    /** multi file resources  **/
    public enum DataFileSet implements DataResource
    {

        PDAQ_HITSPOOL_DATA("./data/pdaq/hitspool.tgz"),
        SECONDBUILD_MONI_DATA("./data/2ndbuild/moni.tgz"),
        SECONDBUILD_SN_DATA("./data/2ndbuild/sn.tgz"),
        SECONDBUILD_TCAL_DATA("./data/2ndbuild/tcal.tgz"),
        PHYSICS_V5_DATA("./data/physics-eventv5.tgz");

        final String sourcePath;

        DataFileSet(String sourcePath)
        {
            this.sourcePath = sourcePath;
        }

        public void extractTo(Path dir) throws IOException
        {
            BufferedInputStream fileIn = new BufferedInputStream(getClass().getResourceAsStream(sourcePath));
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fileIn);
            TarArchiveInputStream tar = new TarArchiveInputStream(gzipIn);

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {

//                if (!tar.canReadEntryData(entry)) {
//                    continue;
//                }

                File item = dir.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    if (!item.isDirectory() && !item.mkdirs()) {
                        throw new IOException("failed to create directory " + item);
                    }
                } else {
                    File parent = item.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(item.toPath())) {
                        IOUtils.copy(tar, o);
                    }
                }
            }
        }

        @Override
        public String sourcePath()
        {
            return sourcePath;
        }

    }


    /** Test cases extend this to inherit setup/cleanup functions**/
    public static class Client
    {

        private final String directoryPrefix;
        private final DataResource testData;

        private Path tempDir;

        protected Path dataDirectory;

        public Client(DataResource testData)
        {
            this("pdaqTemporaryTestData", testData);
        }

        public Client(String directoryPrefix, DataResource testData)
        {
            this.directoryPrefix = directoryPrefix;
            this.testData = testData;
        }

        @Before
        public void setup() throws IOException
        {
            tempDir = Files.createTempDirectory(directoryPrefix);
//            System.out.printf("Extracting test data [%s] to temp dir [%s] ...%n",
//                    testData.sourcePath(), tempDir.toAbsolutePath().toString());
            testData.extractTo(tempDir);

            dataDirectory = tempDir;
        }


        @After
        public void tearDown() throws IOException
        {
            if(tempDir == null){return;}

            // defense
            if( !tempDir.getFileName().toString().startsWith(directoryPrefix))
            {
                throw new Error("Will not delete strange directory");
            }

            deleteRecursive(tempDir);
        }



        private static void deleteRecursive(Path p) throws IOException
        {
            if(Files.isDirectory(p))
            {
                List<Path> children = Files.list(p).collect(Collectors.toList());
                for(Path child : children)
                {
                    deleteRecursive(child);
                }
            }

            Files.delete(p);
        }
    }


}
