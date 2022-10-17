package icecube.daq.cli.stream;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Models various file naming schemes that convey data type and ordering
 * details. E.G.
 *
 * HitSpool-300.dat
 * HitSpool-301.dat
 * HitSpool-302.dat
 *
 * 2ndbuild
 *   tcal_133661_000001_13017753_13609468.dat.bz2
 *   sn_133661_000001_13017753_13609468.dat.bz2
 *   moni_133661_000001_13017753_13609468.dat.bz2
 *   moni_133661_000002_13017753_13609468.dat.bz2
 *   moni_133661_000003_13017753_13609468.dat.bz2
 *
 * evbuilder
 *
 *         physics_136706_000000_0_2561.dat
 *         physics_136706_000001_2562_5031.dat
 *         physics_136706_000002_5032_7640.dat
 *         physics_136706_000003_7641_10090.dat
 *
 */
public enum FileScheme
{
    SECONDBUILD_TCAL("2ndbuild-tcal", new PdaqDataFileFilter("tcal"), new PdaqDataFileComparator(),
            "SecondBuild tcal archive")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return isPdaqDataComplete(files);
                }
            },
    SECONDBUILD_MONI("2ndbuild-moni", new PdaqDataFileFilter("moni"), new PdaqDataFileComparator(),
            "SecondBuild moni archive")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return isPdaqDataComplete(files);
                }
            },
    SECONDBUILD_SN("2ndbuild-sn", new PdaqDataFileFilter("sn"), new PdaqDataFileComparator(),
            "SecondBuild sn archive")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return isPdaqDataComplete(files);
                }
            },
    EVBUILD_PHYSICS("physics-events", new PdaqDataFileFilter("physics"), new PdaqDataFileComparator(),
            "Physics event files")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return isPdaqDataComplete(files);
                }
            },
    HITSPOOL("hitspool", new HitspoolFilter("HitSpool"), new HitspoolComparator(),
            "Hitspool archive")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return isHitspoolComplete(files);
                }
            },
    USER_DEFINED("user_defined", f -> true, (a,b)-> 0,"user supplied files passed through verbatim")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return true;
                }
            },
    NONE("none", f -> false, (a,b)-> 0,"no file scheme")
            {
                @Override
                boolean isComplete(File[] files)
                {
                    return false;
                }
            };

    // teaches picocli the completion values
    public static class CompletionValues extends ArrayList<String>
    {
        public CompletionValues(){
            super(Arrays.stream(FileScheme.values()).map(r -> r.keyword).collect(Collectors.toList()));

        }

    }

    public final String keyword;
    final FileFilter filter;
    final Comparator<String> comparator;
    final String description;

    static Map<String, FileScheme> LOOKUP_MAP;

    static Logger logger = Logger.getLogger(FileScheme.class);

    static {
        Map<String, FileScheme> map = new ConcurrentHashMap<>();
        for (FileScheme t : FileScheme.values()) {
            map.put(t.keyword, t);
        }
        LOOKUP_MAP = Collections.unmodifiableMap(map);
    }

    public static FileScheme lookup(String keyword)
    {
        return LOOKUP_MAP.get(keyword);
    }


    /**
     * Detects gaps in the file set
     */
    abstract boolean isComplete(File[] files);

    FileScheme(String keyword, FileFilter filter, Comparator<String> comparator, String description)
    {
        this.keyword = keyword;
        this.filter = filter;
        this.comparator = comparator;
        this.description = description;
    }

    public File[] order(File[] unordered)
    {
        File[] ordered = new File[unordered.length];
        System.arraycopy(unordered, 0, ordered, 0, ordered.length);


        Arrays.sort(ordered, new Comparator<File>()
        {
            @Override
            public int compare(File o1, File o2)
            {
                return FileScheme.this.comparator.compare(o1.getName(), o2.getName()) ;
            }
        });

        return ordered;
    }

    public File[] listOrdered(String dir, boolean requireComplete)
    {
        File d = new File(dir);
        File[] unordered = d.listFiles(this.filter);

        File[] order = order(unordered);

        if(requireComplete)
        {
            if (!isComplete(order))
            {
                throw new Error(String.format("Incomplete set of [%s] data files", name()));
            }
        }
        return order;
    }

    /**
     * Orders pdaq data files
     *
     *   filename format:
     *      <type>_<run>_seq_<first record>_<last_record>.dat[.gz | .bz | .bz2]
     *
     *      EXAMPLE:
     *
     *         tcal_133661_000022_13017753_13609468.dat.bz2
     *         tcal_133661_000023_13609469_14201184.dat.bz2
     *         tcal_133661_000024_14201185_14503834.dat.bz2
     *         tcal_133662_000000_0_591716.dat.bz2
     *         tcal_133662_000001_591717_1183432.dat.bz2
     *         tcal_133662_000002_1183433_1775148.dat.bz2
     *
     *         physics_136706_000000_0_2561.dat
     *         physics_136706_000001_2562_5031.dat
     *         physics_136706_000002_5032_7640.dat
     *         physics_136706_000003_7641_10090.dat
     */
    static class PdaqDataFileComparator implements Comparator<String>
    {

        @Override
        public int compare(final String f1, final String f2)
        {
            String[] fields1 = f1.split("_");
            int run1 = Integer.parseInt(fields1[1]);
            int seq1 = Integer.parseInt(fields1[2]);

            String[] fields2 = f2.split("_");
            int run2 = Integer.parseInt(fields2[1]);
            int seq2 = Integer.parseInt(fields2[2]);

            if (run1 == run2){
                return Integer.compare(seq1, seq2);
            }
            else
            {
                return Integer.compare(run1, run2);
            }
        }

    }


    /**
     * Selects pdaq output files such as 2ndbuild and evbuilder outputs.
     *
     *   filename format:
     *      <type>_<run>_seq_<first record>_<last_record>.dat[.gz | .bz | .bz2]
     *
     *      EXAMPLE:
     *
     *         tcal_133661_000022_13017753_13609468.dat.bz2
     *         tcal_133661_000023_13609469_14201184.dat.bz2
     *         tcal_133661_000024_14201185_14503834.dat.bz2
     *         tcal_133662_000000_0_591716.dat.bz2
     *         tcal_133662_000001_591717_1183432.dat.bz2
     *         tcal_133662_000002_1183433_1775148.dat.bz2
     *
     *         physics_136706_000000_0_2561.dat
     *         physics_136706_000001_2562_5031.dat
     *         physics_136706_000002_5032_7640.dat
     *         physics_136706_000003_7641_10090.dat
     */
    static class PdaqDataFileFilter implements FileFilter
    {
        final String prefix;
        final String regex;

        PdaqDataFileFilter(String prefix)
        {
            this.prefix = prefix;
            this.regex = String.format("%s_\\d+_\\d+_\\d+_\\d+.dat(?:\\.gz|\\.bz|\\.bz2)?", Pattern.quote(prefix));
        }


        @Override
        public boolean accept(final File pathname)
        {
            String fname = pathname.getName();

//            System.out.printf( "[%s] ~= [%s]%n", fname, regex);
            return fname.matches(regex);
        }

        public static void main(String[] args)
        {
            PdaqDataFileFilter filter = new PdaqDataFileFilter("xyzzy");

            boolean accept = filter.accept(new File("xyzzy_133662_000002_1183433_1775148.dat.bz2"));
            System.out.println("accept = " + accept);
        }

    }


    /**
     * Orders hitspool files
     *
     *   filename format:
     *      HitSpool-<seq>.dat
     *
     *      HitSpool-300.dat
     */
    static class HitspoolComparator implements Comparator<String>
    {
        @Override
        public int compare(final String f1, final String f2)
        {
            String num1 = f1.split("\\.")[0].split("-")[1];
            String num2 = f2.split("\\.")[0].split("-")[1];
            return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
        }

    }


    /**
     * Selects hitspool files
     *
     *   filename format:
     *      HitSpool-<seq>.dat
     *
     *      HitSpool-300.dat
     */
    static class HitspoolFilter implements FileFilter
    {
        final String prefix;
        final String regex;

        HitspoolFilter(String prefix)
        {
            this.prefix = prefix;
            this.regex = String.format("%s-\\d+.dat(?:\\.gz|\\.bz|\\.bz2)?", Pattern.quote(prefix));
        }


        @Override
        public boolean accept(final File pathname)
        {
            String fname = pathname.getName();

//            System.out.printf( "[%s] ~= [%s]%n", fname, regex);
            return fname.matches(regex);
        }

        public static void main(String[] args)
        {
            HitspoolFilter filter = new HitspoolFilter("xyzzy");

            boolean accept = filter.accept(new File("xyzzy-1234.dat.bz2"));
            System.out.println("accept = " + accept);
        }

    }


    /**
     * Answers if there are gaps in the file set
     */
    static boolean isPdaqDataComplete(File[] files)
    {
        int curRun = Integer.MIN_VALUE;
        int lastSeq  = Integer.MIN_VALUE;

        for (int i = 0; i < files.length; i++)
        {
//            System.out.println("examine " + files[i].getName());
            File f = files[i];
            String[] fields = f.getName().split("_");
            int run = Integer.parseInt(fields[1]);
            int seq = Integer.parseInt(fields[2]);

            if(run != curRun)
            {
                if(curRun == Integer.MIN_VALUE)
                {
                    curRun = run;
                    lastSeq = seq;
                }
                else
                {
                    if(seq != 0)
                    {
                        logger.warn("Run " + run + " starts at seq " + seq);
                        return false;
                    }
                    curRun = run;
                    lastSeq = seq;
                }
            }

            else
            {
                if(seq != (lastSeq + 1)) {
                    logger.warn("Run " + run + " gap between seq " + lastSeq + " and " + seq );

                    return false;
                }

                lastSeq = seq;
            }
        }

        return true;
    }

    /**
     * Answers if there are gaps in the file set
     */
    static boolean isHitspoolComplete(File[] files)
    {
        int lastSeq  = Integer.MIN_VALUE;

        for (int i = 0; i < files.length; i++)
        {
//            System.out.println("examine " + files[i].getName());
            File f = files[i];
            String num1 = f.getName().split("\\.")[0].split("-")[1];
            int seq = Integer.parseInt(num1);

            if(lastSeq == Integer.MIN_VALUE)
            {
                //first file

            }
            else if(seq != (lastSeq + 1)) {
                logger.warn("Gap between seq " + lastSeq + " and " + seq );
                return false;
            }

            lastSeq = seq;
        }

        return true;
    }


    /**
     * Heuristically discover the file scheme of files under the directory
     * @param dir The directory to examine.
     * @param strict If true, all files under the directory must match the scheme
     * @return The FileScheme matching the structure of the directory
     */
    public static FileScheme discover(File dir, boolean strict) throws IOException
    {
        List<FileScheme> candidates = new ArrayList<>(1);
        for(FileScheme scheme : FileScheme.values()) {
            File[] files = scheme.listOrdered(dir.getPath(), false);
            if (files.length > 0 && scheme != USER_DEFINED) {
                candidates.add(scheme);
            }
        }
        if(candidates.size() != 1)
        {
            return null;
        }
        else
        {
            FileScheme result = candidates.get(0);
            if(strict)
            {

                long numFiles = Files.list(dir.toPath()).filter(p -> Files.isRegularFile(p)).count();
                if(result.listOrdered(dir.getPath(), false).length != numFiles)
                {
                    return null;
                }
            }

            return result;
        }

    }
}
