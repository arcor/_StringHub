package icecube.daq.cli.commands;

import icecube.daq.bindery.BufferConsumer;
import icecube.daq.cli.options.*;
import icecube.daq.cli.stream.DataSource;
import icecube.daq.cli.stream.RecordType;
import icecube.daq.cli.util.ArchiveWriter;
import icecube.daq.cli.util.UTCResolver;
import icecube.daq.performance.binary.record.RecordReader;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Command to set up a processing pipeline for pdaq data
 */
@CommandLine.Command(name = "process", description = "Utility for processing pdaq records",
        mixinStandardHelpOptions = true,
        subcommands = {
                ProcessCmd.Extract.class,
                ProcessCmd.Info.class,
                ProcessCmd.Shred.class})
public class ProcessCmd
{


    static class OutputOptions implements CommandLine.ITypeConverter<BufferConsumer>
    {
        @CommandLine.Option(names = {"--output"}, required = false,
                description = "The output target%n" +
                        "   examples:%n" +
                        "    \"/x/y/out.dat\"%n" +
                        "    \"/x/y/out.dat.gz\"%n" +
                        "    \"/x/y/out.dat.bz2\"%n",
                converter = OutputOptions.class)
        BufferConsumer output = new BufferConsumer()
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
        public BufferConsumer convert(String s) throws Exception
        {
            return new ArchiveWriter(new File(s));
        }
    }

    static class Meter implements Consumer<ByteBuffer>
    {
        long byteCount;
        long recordCount;
        long start_ns, stop_ns;
        @Override
        public void accept(ByteBuffer byteBuffer)
        {
            recordCount++;
            byteCount += byteBuffer.remaining();
        }

        void recordStartTime()
        {
            start_ns = System.nanoTime();
        }
        void recordStopTime()
        {
            stop_ns = System.nanoTime();
        }

        double elapsedSeconds()
        {
            return (stop_ns - start_ns) / 1e9;
        }

        double throughput()
        {
            final double elapseSec = elapsedSeconds();
            if(elapseSec > 0)
            {
                return byteCount / elapseSec / 1024 / 1024;
            }
            else
            {
                return 0.0;
            }

        }
    }

    static class Summary implements Consumer<ByteBuffer>
    {

        final RecordType recordType;
        RecordReader.LongField mbidField;
        RecordReader.LongField orderingField;

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long lastTime = minTime;

        boolean monotonic = true;

        // use indirection to save processing time for
        // non-mbid, non-utc records
//        Consumer<ByteBuffer> mbidProcessor;
//        Consumer<ByteBuffer> utcProcessor;

        Summary(RecordType recordType)
        {
            this.recordType = recordType;

            this.mbidField = recordType.rr.getMbidField();
            this.orderingField = recordType.rr.getOrderingField();
        }

        @Override
        public void accept(ByteBuffer byteBuffer)
        {
            try {
                long mbid = mbidField.value(byteBuffer, 0);
            } catch (Error e) {
                //some records don't have mbids
            }


            try {
                long utc = orderingField.value(byteBuffer, 0);
                if(lastTime != Long.MAX_VALUE && utc < lastTime){
                    monotonic = false;
                }
                minTime = Math.min(utc, minTime);
                maxTime = Math.max(utc, maxTime);
                lastTime = utc;

            } catch (Error e) {
                //some records don't have utc
            }
        }

    }

    @CommandLine.Command(name = "extract", description = "Extract records from a source", mixinStandardHelpOptions = true)
    public static class Extract implements Callable<Integer>
    {

        @CommandLine.Mixin
        PipelineOption pipelineOption = new PipelineOption();

        @CommandLine.Mixin
        DataSourceOption sourceOptions = new DataSourceOption();

        @CommandLine.Mixin
        OutputOptions outputOptions = new OutputOptions();


        @Override
        public Integer call() throws Exception
        {

            DataSource dataSource = sourceOptions.getDataSource();
            RecordType recordType = dataSource.getRecordType();
            PipelineOption.StreamTail tail = new PipelineOption.StreamTail(dataSource.stream(), recordType);

            // install meter
            Meter meter = new Meter();

            // install pipeline
            tail = new PipelineOption.StreamTail(dataSource.stream().peek(meter), recordType);
            tail = pipelineOption.plumb(tail);


            // process stream
            meter.recordStartTime();
            tail.stream.forEach(new Consumer<ByteBuffer>()
            {
                @Override
                public void accept(ByteBuffer byteBuffer)
                {
                    try {
                        outputOptions.output.consume(byteBuffer);
                    } catch (IOException e) {
                        throw new Error(e);
                    }
                }
            });


            outputOptions.output.endOfStream(-1);

                meter.recordStopTime();

                System.out.printf("Processed %d bytes (%d %s records) in %.02f seconds (%.02f mb/s)%n",
                        meter.byteCount, meter.recordCount, recordType.keyword,
                        meter.elapsedSeconds(), meter.throughput());



            return 0;
        }
    }


    @CommandLine.Command(name = "info", description = "Summarize the content of the data source", mixinStandardHelpOptions = true)
    public static class Info implements Callable<Integer>
    {

        @CommandLine.Mixin
        DataSourceOption sourceOption = new DataSourceOption();

        @CommandLine.Mixin
        UTCOption utcOption = new UTCOption();

        @Override
        public Integer call() throws Exception
        {
            // force utc time reconstruction, we just
            // want to provide the user a data year option
            utcOption.resolveUtcTime = true;

            DataSource dataSource = sourceOption.getDataSource();
            RecordType recordType = dataSource.getRecordType();

            Summary summary = new Summary(recordType);
            Meter meter = new Meter();
            meter.recordStartTime();
            dataSource.stream().peek(meter).forEach(summary);
            meter.recordStopTime();


            System.out.printf("Data Source:     %s%n", dataSource.describe());
            System.out.printf("Record Type:     %s%n", recordType.keyword);
            System.out.printf("Time Range:      [%d - %d]%n", summary.minTime, summary.maxTime);

            UTCResolver utcResolver = utcOption.getResolver();
            if(utcOption.resolveUtcTime)
            {
                System.out.printf("          :      [%s - %s]%n", utcResolver.resolve(summary.minTime), utcResolver.resolve(summary.maxTime));
            }

            TimeOption.TimeDuration timeDuration = new TimeOption.TimeDuration(summary.maxTime - summary.minTime);
            System.out.printf("Time Duration:   %s%n", timeDuration.toString());
            System.out.printf("Well Ordered:    %s%n", summary.monotonic ? "Yes" : "No");
            System.out.printf("Num Bytes:       %d%n", meter.byteCount);
            System.out.printf("Num Records:     %d%n", meter.recordCount);
            System.out.printf("Processing Time: %02f seconds%n", meter.elapsedSeconds());
            System.out.printf("Processing Rate: %02f mb/s%n", meter.throughput());


            return 0;
        }
    }

    @CommandLine.Command(name = "shred", description = "split a data source into multiple files based on demux criteria", mixinStandardHelpOptions = true)
    public static class Shred implements Callable<Integer>
    {
        @CommandLine.Mixin
        DataSourceOption  sourceOption;


        @Override
        public Integer call() throws Exception
        {
            DataSource dataSource = sourceOption.getDataSource();

            //dataSource.stream()

            throw new Error("Unimplimented");

        }
    }


    public static void main(String[] args)
    {

        CommandLine cmd = new CommandLine(new ProcessCmd());
        cmd.execute(args);

    }


    static class Test
    {
        public static void main(String[] args)
        {

//            String input = "/Users/tbendfelt/proj/data/reference/pdaq-sn.dat";
//            String input = "/Users/tbendfelt/proj/data/reference/2ndbuild-sets/tcal";
//            String input = "/Users/tbendfelt/proj/data/reference/2ndbuild-sets/moni";
//            String input = "/Users/tbendfelt/proj/data/reference/2ndbuild-sets/sn";

//            String output = "/Users/tbendfelt/proj/data/reference/out.dat.bz2";
//            String output = "/tmp/out.dat";

            ProcessCmd.main(new String[]{"--help"});
//            ProcessCmd.main(new String[]{"extract", "--help"});
//            ProcessCmd.main(new String[]{"extract", "--source", input});
//            ProcessCmd.main(new String[]{"extract", "--source", input, "--output", output});
//            ProcessCmd.main(new String[]{"extract", "--source", input, "--output", output,
//            "--filter", "a"});
//            ProcessCmd.main(new String[]{"extract", "--filter", "a",
//                    "--filter", "a",
//                    "--filter", "b",
//                    "--filter", "c",
//                    "--filter", "d",
//                    "--filter", "e"});

//            ProcessCmd.main(new String[]{"info", "--help"});
//            ProcessCmd.main(new String[]{"info", "--source", input, "--resolve-times"});




        }
    }

}
