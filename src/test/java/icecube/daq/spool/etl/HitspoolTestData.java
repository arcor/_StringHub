package icecube.daq.spool.etl;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Encapsulates hitspool instances for testing
 */
public class HitspoolTestData
{

    // available spools
    enum TestSpool
    {
        /** a full hitspool directory captured from SPS */
        SPS_15E10_36000_FULL_SPOOL("sps-ichub01-15E10-36000-spool.tar.gz", "hitspool"),
        /** Just the hitspool.db database */
        SPS_15E10_36000_SPOOL_DB_ONLY("sps-ichub01-db-only-15E10-36000-spool.tar.gz", "hitspool"),
        /** a hitspool db generated with interval=15E10, numFiles=72000*/
        _15E10_72000_SPOOL_DB_ONLY("15E10_72000_SPOOL_DB_ONLY.tar.gz", "hitspool"),

        EMPTY_SPOOL("empty.tar.gz", "hitspool");

        final String srcFile;
        final String dstDir;

        TestSpool(String srcFile, String dstDir)
        {
            this.srcFile = srcFile;
            this.dstDir = dstDir;
        }

        void extractTo(Path dir) throws IOException
        {
            extractTarB(dir);
        }

        // tar extract implemented by calling "tar" in subprocess
        void extractTarA(Path dir) throws IOException
        {
            // src
            InputStream is = getClass().getResourceAsStream(srcFile);

            // sink
            ProcessBuilder builder = new ProcessBuilder("tar", "-C", dir.toString(), "-zxf", "-");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedOutputStream os = new BufferedOutputStream(process.getOutputStream());

            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer, 0,1024)) != -1)
            {
                os.write(buffer, 0, read);
            }

            os.flush();
            os.close();

            try {
                int status = process.waitFor();
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        // tar extract implemented in-process using commons-compress
        private void extractTarB(Path dir) throws IOException
        {

            BufferedInputStream fileIn = new BufferedInputStream(getClass().getResourceAsStream(srcFile));
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


    }

    /** Test cases extend this to inherit setup/cleanup functions**/
    public static class Client
    {

        private final String directoryPrefix;
        private final TestSpool seedData;

        private Path tempDir;

        // NOTE: in spool contexts <parent>/hitspool/... is implied, in others
        //       like Metadata.java <parent>/hitspool/ is passed specifically
        //
        protected Path parentDir;
        protected Path databaseDir;

        public Client(HitspoolTestData.TestSpool testData)
        {
            this("pdaqTemporaryTestData", testData);
        }

        public Client(String directoryPrefix, TestSpool seedData)
        {
            this.directoryPrefix = directoryPrefix;
            this.seedData = seedData;
        }

        @Before
        public void setup() throws IOException
        {
            tempDir = Files.createTempDirectory(directoryPrefix);
            System.out.printf("Extracting test hitspool [%s] to temp dir [%s] ...%n",
                    seedData.srcFile, tempDir.toAbsolutePath().toString());


            seedData.extractTo(tempDir);

            parentDir = tempDir;
            databaseDir = tempDir.resolve(seedData.dstDir);
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
