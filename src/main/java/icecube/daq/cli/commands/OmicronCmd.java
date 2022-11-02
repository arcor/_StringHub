package icecube.daq.cli.commands;

import icecube.daq.bindery.*;
import icecube.daq.cli.filter.Filter;
import icecube.daq.cli.options.*;
import icecube.daq.cli.stream.RecordType;
import icecube.daq.cli.util.DomResolver;
import icecube.daq.configuration.XMLConfig;
import icecube.daq.domapp.*;
import icecube.daq.dor.DOMChannelInfo;
import icecube.daq.dor.Driver;
import icecube.daq.performance.binary.record.pdaq.DaqBufferRecordReader;
import icecube.daq.performance.common.PowersOfTwo;
import icecube.daq.performance.diagnostic.DataCollectorAggregateContent;
import icecube.daq.performance.diagnostic.DiagnosticTrace;
import icecube.daq.performance.diagnostic.MeterContent;
import icecube.daq.performance.diagnostic.Metered;
import icecube.daq.performance.diagnostic.cpu.CPUUtilizationContent;
import icecube.daq.time.gps.GPSService;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.FlasherboardConfiguration;
import org.apache.log4j.Logger;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@CommandLine.Command(name = "omicron", description = "Stand-alone StringHub data acquisition utility",
        subcommands = {AutoComplete.GenerateCompletion.class,
                       CommandLine.HelpCommand.class},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        abbreviateSynopsis = true, usageHelpWidth = 120,
        footer = "%n  Examples:%n%n" +
                "    Take a 15 minute run into files:%n" +
                "%n" +
                "      omicron --dom-config spts-ichub29.xml --run-length 15m --output data%n" +
                "%n" +
                "      omicron -c spts-ichub29.xml -l 15m -o data.bz2%n" +
                "%n" +
                "    Utilize a log file:%n" +
                "%n" +
                "      omicron -c spts-ichub29.xml -l 15m -o data --log run.log --log-level DEBUG%n" +
                "%n" +
                "    Spool data into a rotating spool of 8 hours:%n" +
                "%n" +
                "      omicron --dom-config spts-ichub29.xml --run-length 24h --output spool:spooldir:8h%n" +
                "%n" +
                "    Pluck specific DOMs out of a config file:%n" +
                "%n" +
                "      omicron -c spts-ichub29.xml --doms Larry_Talbot,Egg_Nebula -l 15m -o data%n" +
                "%n" +
                "    Abort a run after configuring DOMs:%n" +
                "%n" +
                "    omicron --dom-config spts-ichub29.xml --stage configured%n" +
                "%n" +
                "    Enable flashers on two doms in the config:%n" +
                "%n" +
                "    omicron --dom-config spts-ichub29.xml --extended-mode=true \\%n" +
                "            --flasher dom:64a8ac5299b1,brightness:1,width:2,delay:3,mask:4,rate:5 \\%n" +
                "            --flasher dom:Egg_Nebula,brightness:1,width:2,delay:3,mask:0x3,rate:5%n" +
                "%n" +
                "    Capture MainboardLED trigger source events into an event file:%n" +
                "%n" +
                "    omicron --dom-config spts-ichub29.xml -l 15m -o data --extended-mode=true \\%n" +
                "            --event-filter triggersource:0x10:1000000 --event-output LEDEvents \\%n"
)
public class OmicronCmd implements Callable<Integer>
{

    private static final Logger logger = Logger.getLogger(OmicronCmd.class);


    static class OmicronOptions
    {
        @CommandLine.Option(names={"--dom-config", "-c"}, required = true,
                paramLabel = "FILE", description = "DOM config file")
        String domconfig;

        @CommandLine.Option(names={"--run-length", "-l"}, required = false,
                description = "Run length%n" +
                        " example:%n" +
                        "   \"60s\"%n" +
                        "   \"1m\"%n" +
                        "   \"1h30m\"%n" +
                        "   (default: ${DEFAULT-VALUE})",
                defaultValue = "30s",
                paramLabel = "DURATION",
                converter = TimeOption.DurationParameterConverter.class)
        TimeOption.TimeDuration runLength;

        @CommandLine.Option(names={"--output", "-o"}, required = false,
                description = "Write data to specified output files%n" +
                " example:%n" +
                "   run1 yields [run1.hits, run1.moni, ...]%n" +
                "   run1.gz yields [run1.hits.gz, ...]%n" +
                "   run1.bz2 yields [run1.hits.bz2, ...]%n" +
                "   spool:dir:name%n" +
                "      yields [./dir/name-hits/hits.db, ./dir/name-moni/moni.db, ... ]%n" +
                "   spool:dir:name:4h30m%n" +
                "      spool configured for 4h30 minutes in 15 second increments%n" +
                "   spool:dir:name:1024:15s%n" +
                "      spool configured for 1024, 15 second wide files%n" +
                "(default:/dev/null)",
                paramLabel = "OUTPUT_SPEC",
                converter = DataOutputOption.ComplexOutputOption.Converter.class,
                defaultValue = "/dev/null"

        )
        DataOutputOption.ComplexOutputOption outputProvider;

        @CommandLine.Option(names={"--extended-mode"}, arity = "0",
                description = "Enable extended mode DOM features (default: ${DEFAULT-VALUE})")
        boolean extendedMode = false;

        @CommandLine.Option(names={"--softboot"}, required = false,
                description = "Softboot DOMs before run (default: ${DEFAULT-VALUE}")
        boolean softboot = false;

        @CommandLine.Option(names={"--flasher"}, required = false,
                converter = FlasherConfigOption.FlasherConfigCLIConverter.class,
                description = "Enable flashers for doms(s), requires extended mode%n" +
                        " example:%n" +
                        "   --flasher dom:64a8ac5299b1,brightness:1,width:2,delay:3,mask:4,rate:5%n" +
                        "   --flasher dom:Egg_Nebula,brightness:1,width:2,delay:3,mask:0x2,rate:5",
        paramLabel = "<flasher cfg>")
        List<FlasherboardConfiguration> flasherConfigs = new ArrayList<>(0);

        @CommandLine.Option(names={"--stage"}, required = false,
                description = "Truncate the run sequence at a specific stage: " +
                "[${COMPLETION-CANDIDATES}]")
        Stage stage = Stage.STOPPED;

        @CommandLine.Option(names={"--disable-interval-mode"}, required = false,
                description = "Disables interval mode DOM readout ")
        boolean disableInterval = false;

        @CommandLine.Option(names = {"--doms"}, required = false,
                description = "Select only specific DOMs from the config%n" +
                "   examples:%n" +
                        "    \"64a8ac5299b1,e5c34e1fca8f\"%n" +
                        "    \"Larry_Talbot,Egg_Nebula\"%n" +
                        "    \"hub:2029\"",
                paramLabel = "DOM_LIST",
                converter = DomOption.MBIDListConverter.class)
        List<Long> mbids;

        @CommandLine.Option(names = {"--event-filter"}, required = false,
                description = "Extracts hits from the record stream that match a filter criteria%n" +
                "the extracted stream will be written to a new output file named by --event-output%n%n" +
                "HIT_FILTER: [${COMPLETION-CANDIDATES}]%n",
                paramLabel = "HIT_FILTER",
                converter = FilterOption.class,
        completionCandidates = FilterOption.FilterCompletions.class)
        Optional<Filter> extractFilter;

        @CommandLine.Option(names={"--event-output"}, required = false,
                description = "Write extracted event data to specified output%n" +
                        " example:%n" +
                        "   ledTrigger yields [ledTrigger.events]%n" +
                        "   ledTrigger.gz yields [ledTrigger.events.gz]%n" +
                        "   ledTrigger.bz2 yields [ledTrigger.events.bz2, ...]%n" +
                        "(default:/dev/null)",
                converter = DataOutputOption.ComplexOutputOption.Converter.class,
                paramLabel = "OUTPUT_SPEC"

        )
        DataOutputOption.ComplexOutputOption eventOutputProvider;

    }



    static class GPSOptions
    {

        static class CompletionValues extends ArrayList<String>
        {
            CompletionValues(){
                super(Arrays.stream(GPSService.GPSMode.values()).map(r -> r.key).collect(Collectors.toList()));

            }
        }
        @CommandLine.Option(names = {"--gps-mode"}, required = false,
                description = "An option to control the source of DOR/DOM/UTC reconstruction%n%n" +
                              "GPS_MODE: [${COMPLETION-CANDIDATES}] default: ${DEFAULT-VALUE}%n",
                completionCandidates = GPSOptions.CompletionValues.class,
                paramLabel = "GPS_MODE")
        String gpsMode = GPSService.GPSMode.DSB.key;

        /**
         * Note: This needs to be called before the GPSService class
         *       is instantiated.
         */
        void configure()
        {
            System.setProperty(GPSService.GPS_MODE_PROPERTY, gpsMode);
        }


    }

    @CommandLine.Mixin
    OmicronOptions options = new OmicronOptions();

    @CommandLine.Mixin
    GPSOptions gpsOptions = new GPSOptions();

    @CommandLine.Mixin
    DiagnosticTraceOption diagnosticTraceOption = new DiagnosticTraceOption();

    @CommandLine.Mixin
    LogOptions logOptions = new LogOptions();

    // enumerates stages of the run model
    static enum Stage
    {
        IDLE, CONFIGURED, RUNNING, STOPPED;
    }


    @Override
    public Integer call() throws Exception
    {
        runOmicron();
        return 0;
    }


    static String domName(long mbid)
    {
        DOMInfo dom = DomResolver.instance().getDom(mbid);
        if(dom != null)
        {
            return dom.getName();
        }
        else
        {
            return "";
        }
    }




    public void runOmicron() throws Exception
    {

        logOptions.configure();

        gpsOptions.configure();

        diagnosticTraceOption.configure();


        if(options.extendedMode)
        {
            ExtendedMode.enableExtendedMode();
        }


        final Driver driver = Driver.getInstance();
        final ArrayList<DataCollector> collectors;


        XMLConfig xmlConfig = new XMLConfig();
        xmlConfig.parseXMLConfig(Files.newInputStream(Paths.get(options.domconfig)));

        logger.info("Begin logging at " + new Date());

        collectors = new ArrayList<DataCollector>();

        // used to filter selected doms
        Predicate<Long> wanted = new Predicate<Long>()
        {
            @Override
            public boolean test(Long mbid)
            {
                if(options.mbids == null){return true;}
                else
                {
                    return options.mbids.contains(mbid);
                }
            }
        };

        // Must first count intersection of configured and discovered DOMs
        List<DOMChannelInfo> selected = new ArrayList<>(64);
        List<DOMChannelInfo> activeDOMs = driver.discoverActiveDOMs();

        logger.info(String.format("Discovered %d active DOMS", activeDOMs.size()));
        activeDOMs.forEach((d)->logger.info(String.format("DOM %d%d%c - %s (%s) is active",d.card, d.pair, d.dom,
                d.mbid, domName(d.mbid_numerique))));

        for (DOMChannelInfo chInfo : activeDOMs)
        {

            DOMConfiguration requested = xmlConfig.getDOMConfig(chInfo.mbid);
            if (requested != null && wanted.test(chInfo.mbid_numerique))
            {
                selected.add(chInfo);
            }
        }
        final int nDOM = selected.size();

        // log selected
        selected.forEach((d)->logger.info(String.format("DOM %d%d%c - %s (%s) selected for run",d.card, d.pair, d.dom,
                d.mbid, domName(d.mbid_numerique))));

        // set up trace for the processing stack
        Metered.Buffered sortQueueMeter = diagnosticTraceOption.getSortQueueMeter();
        Metered.UTCBuffered sortMeter = diagnosticTraceOption.getSortMeter();
        Metered.Buffered hitConsumerMeter =  diagnosticTraceOption.getAsyncHitConsumerMeter();


        BufferConsumer hitsChan = options.outputProvider.plumbOutput(DaqBufferRecordReader.instance, "hits");
        BufferConsumer moniChan = options.outputProvider.plumbOutput(DaqBufferRecordReader.instance, "moni");
        BufferConsumer tcalChan = options.outputProvider.plumbOutput(DaqBufferRecordReader.instance, "tcal");
        BufferConsumer scalChan = options.outputProvider.plumbOutput(DaqBufferRecordReader.instance, "scal");


        // install an event filter on to the hit stream if present
        if(options.extractFilter.isPresent())
        {
            Filter filter = options.extractFilter.get();
            Predicate<ByteBuffer> predicate = filter.asPredicate(RecordType.PDAQ_HITS);

            logger.info("Installing event filter: " + filter.describe());


            hitsChan = new BufferConsumerFork(hitsChan, new BufferConsumer()
            {
                BufferConsumer eventChannel = options.eventOutputProvider.plumbOutput(DaqBufferRecordReader.instance,
                        "events");

                @Override
                public void consume(ByteBuffer buf) throws IOException
                {
                    if(predicate.test(buf))
                    {
                        eventChannel.consume(buf);
                    }
                }

                @Override
                public void endOfStream(long token) throws IOException
                {
                    eventChannel.endOfStream(token);
                }
            });
        }

        // consume hits on a dedicated thread to maximize sorter performance
        AsyncSorterOutput asyncHitConsumer = new AsyncSorterOutput(hitsChan, PowersOfTwo._2097152, "hit-consumer", hitConsumerMeter);
        MultiChannelMergeSort hitsSort = new MultiChannelMergeSort(nDOM, asyncHitConsumer, "hits", sortQueueMeter, sortMeter);
        MultiChannelMergeSort moniSort = new MultiChannelMergeSort(nDOM, moniChan, "moni");
        MultiChannelMergeSort tcalSort = new MultiChannelMergeSort(nDOM, tcalChan, "tcal");
        MultiChannelMergeSort scalSort = new MultiChannelMergeSort(nDOM, scalChan, "supernova");

        if(options.softboot)
        {
            List<Thread> softbooters = new ArrayList<>(activeDOMs.size());

            for (DOMChannelInfo chInfo : selected)
            {
                Thread softboot = new Thread("softboot-" + chInfo.mbid)
                {
                    @Override
                    public void run()
                    {
                        logger.warn(String.format("softbooting %s - %s", chInfo.toString(),  chInfo.mbid));
                        try {
                            driver.commReset(chInfo.card, chInfo.pair, chInfo.dom);
                            sleep(20);
                            driver.softboot(chInfo.card, chInfo.pair, chInfo.dom);
                            sleep(20);
                            driver.commReset(chInfo.card, chInfo.pair, chInfo.dom);
                            sleep(20);
                            logger.warn(String.format("Starting DOMApp on %s - %s", chInfo.toString(),  chInfo.mbid));
                            DOMApp domApp = new DOMApp(chInfo.card, chInfo.pair, chInfo.dom);
                            domApp.transitionToDOMApp();
                            sleep(100);
                            domApp.close();
                            sleep(100);
                            logger.warn(String.format("New DOMApp instance running on %s - %s", chInfo.toString(),  chInfo.mbid));


                        } catch (Throwable e) {
                            logger.error(e);
                        }
                    }
                };
                softbooters.add(softboot);

            }

            softbooters.stream().forEach(Thread::start);
            softbooters.stream().forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            });

        }


        for (DOMChannelInfo chInfo : selected)
        {
            DOMConfiguration config = xmlConfig.getDOMConfig(chInfo.mbid);
            hitsSort.register(chInfo.getMainboardIdAsLong());
            moniSort.register(chInfo.getMainboardIdAsLong());
            tcalSort.register(chInfo.getMainboardIdAsLong());
            scalSort.register(chInfo.getMainboardIdAsLong());

            // Associate a GPS service to this card, if not already done
            GPSService.getInstance().startService(chInfo.card);

            DataCollector dc =
                    DataCollectorFactory.buildDataCollector(
                            chInfo.card, chInfo.pair, chInfo.dom, chInfo.mbid, config,
                            hitsSort, moniSort, scalSort, tcalSort,!options.disableInterval);
            collectors.add(dc);
            logger.debug("Starting new DataCollector thread on (" + chInfo.card + "" + chInfo.pair + "" + chInfo.dom + ").");
            logger.debug("DataCollector thread on (" + chInfo.card + "" + chInfo.pair + "" + chInfo.dom + ") started.");
        }


        hitsSort.start();
        moniSort.start();
        scalSort.start();
        tcalSort.start();

        // All collectors are now started at latest by t0
        long t0 = System.currentTimeMillis();

        // List of objects that need removal
        HashSet<DataCollector> reaper = new HashSet<DataCollector>();

        if (logger.isInfoEnabled()) {
            logger.info("Waiting for collectors to initialize");
        }
        for (DataCollector dc : collectors)
        {
            // Note that if you turn SN data off on all doms the extra
            // messaging pushed the us over the timeout here
            // doubling the timeout worked.
            while (dc.isAlive() &&
                    !dc.getRunLevel().equals(RunLevel.IDLE) &&
                    System.currentTimeMillis() - t0 < 30000L)
                Thread.sleep(100);
            if (!dc.isAlive())
            {
                logger.warn("Collector " + dc.getName() + " died in init.");
                reaper.add(dc);
            }
        }

        // ########################
        // # at IDLE
        // ########################
        if(options.stage == Stage.IDLE)
        {
            logger.warn("Exiting omicron at run stage IDLE");
            return;
        }

        diagnosticTraceOption.startTrace(collectors);


        logger.info("Sending CONFIGURE signal to DataCollectors");

        for (DataCollector dc : collectors)
        {
            if (!dc.isAlive())
            {
                logger.warn("Collector " + dc.getName() + " died before config: schedule for removal.");
                reaper.add(dc);
            }
            else
            {
                dc.signalConfigure();
            }
        }

        collectors.removeAll(reaper);
        reaper.clear();

        logger.info("Waiting on DOMs to configure...");

        // Wait until configured
        for (DataCollector dc : collectors)
        {
            if (!dc.isAlive())
            {
                logger.warn("Collector " + dc.getName() + " died during config: schedule for removal.");
                reaper.add(dc);
            }
            else
            {
                while (!dc.getRunLevel().equals(RunLevel.CONFIGURED) && System.currentTimeMillis() - t0 < 30000L)
                {
                    if (logger.isDebugEnabled()) logger.debug("Waiting of DC " + dc.getName() + " to configure.");
                    Thread.sleep(500);
                }
                if (!dc.getRunLevel().equals(RunLevel.CONFIGURED))
                {
                    logger.warn("Collector " + dc.getName() + " stuck configuring: coup de grace.");
                    dc.signalShutdown();
                    reaper.add(dc);
                }
            }
        }

        collectors.removeAll(reaper);
        reaper.clear();

        // ########################
        // # at CONFIGURED
        // ########################
        if(options.stage == Stage.CONFIGURED)
        {
            logger.warn("Exiting omicron at run stage CONFIGURED");
            diagnosticTraceOption.stopTrace();
            return;
        }




        // apply flasher configs if desired (requires EXTENDED MODE to work)
        boolean[] wirePairSemaphore = new boolean[32];
        Map<String, FlasherboardConfiguration> map = options.flasherConfigs.stream().collect(Collectors.toMap(fc -> fc.getMainboardID(), fc -> fc));
        for (DataCollector dc : collectors)
        {
            if(map.containsKey(dc.getMainboardId()))
            {
                int pairIndex = 4 * dc.getCard() + dc.getPair();
                if (wirePairSemaphore[pairIndex])
                    throw new Error("Cannot activate > 1 flasher run per DOR wire pair.");
                wirePairSemaphore[pairIndex] = true;

                FlasherboardConfiguration fbc = map.get(dc.getMainboardId());
                logger.warn( String.format("Applying a flasher configuration to dom:%s (%s) : %s", dc.getMainboardId(),
                        domName(Long.parseLong(dc.getMainboardId(), 16)), fbc.toString()));
                dc.extendedModeFlasherConfig = fbc;
            }
        }

        logger.info(String.format("Starting a %s run...", options.runLength.toString()));



        if(collectors.size() > 0)
        {

            // Quickly fire off a run start now that all are ready
            for (DataCollector dc : collectors)
                if (dc.isAlive()) dc.signalStartRun();

            t0 = (long) (System.currentTimeMillis() + (options.runLength.tenth_nanos/10_000_000));

            while (true)
            {
                long time = System.currentTimeMillis();
                if (time > t0)
                {
                    // ########################
                    // # at RUNNING
                    // ########################
                    if(options.stage == Stage.RUNNING)
                    {
                        logger.warn("Exiting omicron at run stage RUNNING");
                        diagnosticTraceOption.stopTrace();
                        return;
                    }

                    for (DataCollector dc : collectors) if (dc.isAlive()) dc.signalStopRun();
                    break;
                }
                Thread.sleep(1000);
            }


            for (DataCollector dc : collectors) {
                while (dc.isAlive() && !dc.getRunLevel().equals(RunLevel.CONFIGURED)) Thread.sleep(100);
                dc.signalShutdown();
            }

            // ########################
            // # at STOPPED *not really
            // ########################
            logger.warn("Exiting omicron at run stage STOPPED");
        }
        else
        {
            logger.warn("No DataCollectors left to start, aborting run");
        }


        hitsSort.join(Long.MAX_VALUE);
        asyncHitConsumer.join();

        moniSort.join(Long.MAX_VALUE);
        scalSort.join(Long.MAX_VALUE);
        tcalSort.join(Long.MAX_VALUE);

        // kill GPS services
        GPSService.getInstance().shutdownAll();

        // close the outputs
        hitsChan.endOfStream(-1);
        moniChan.endOfStream(-1);
        tcalChan.endOfStream(-1);
        scalChan.endOfStream(-1);

        // kill trace
        diagnosticTraceOption.stopTrace();
    }


    /**
     * Encapsulates the optional injection of a performance trace into the
     * hit processing stack.
     */
    private static class DiagnosticTraceOption
    {
        @CommandLine.Option(names = {"--enable-diagnostic-trace"}, required = false,
                description = "enable diagnostic trace output",
        defaultValue = "false")
        boolean enable;

        @CommandLine.Option(names = {"--diagnostic-trace-period"}, required = false,
                description = "Millisecond period of trace output",
                defaultValue = "60000")
        int period;


        @CommandLine.Option(names = {"--diagnostic-trace-output"}, required = false,
                description = "Where to write trace output",
                defaultValue = "stdout")
        String output;


        private Metered.Buffered sortQueueMeter;
        private Metered.UTCBuffered sortMeter;
        private Metered.Buffered asyncHitConsumerMeter;

        DiagnosticTrace trace;

        void configure()
        {
            if(enable)
            {
                sortQueueMeter = Metered.Factory.bufferMeter(
                        Metered.Factory.ConcurrencyModel.MPMC);
                sortMeter = Metered.Factory.utcBufferMeter();
                asyncHitConsumerMeter = Metered.Factory.bufferMeter();
            }
            else
            {
                sortQueueMeter = new Metered.DisabledMeter();
                sortMeter = new Metered.DisabledMeter();
                asyncHitConsumerMeter = new Metered.DisabledMeter();
            }
        }

        Metered.Buffered getSortQueueMeter()
        {
            return sortQueueMeter;
        }

        Metered.UTCBuffered getSortMeter()
        {
            return sortMeter;
        }

        Metered.Buffered getAsyncHitConsumerMeter()
        {
            return asyncHitConsumerMeter;
        }

        private void startTrace(List<DataCollector> collectors) throws IOException
        {
            if(enable)
            {
                PrintStream dst;
                switch (output.toLowerCase())
                {
                    case "stdout": dst=System.out;
                        break;
                    default:
                        dst = new PrintStream(new BufferedOutputStream(new FileOutputStream(output, true)));
                }
                trace = new DiagnosticTrace(period, 30, dst);
                trace.addTimeContent();
                trace.addAgeContent();
                trace.addHeapContent();
                trace.addGCContent();
                trace.addContent(new DataCollectorAggregateContent(collectors));
                trace.addMeter("sortq", sortQueueMeter, MeterContent.Style.HELD_DATA);
                trace.addMeter("sorter", sortMeter, MeterContent.Style.HELD_DATA,
                        MeterContent.Style.UTC_DELAY,
                        MeterContent.Style.DATA_RATE_OUT);
                trace.addMeter("hitOut", asyncHitConsumerMeter,
                        MeterContent.Style.HELD_DATA,
                        MeterContent.Style.DATA_RATE_OUT);

                CPUUtilizationContent cpu = new CPUUtilizationContent();
                trace.addFlyWeight(cpu);
                trace.addContent(cpu.createSystemUtilizationContent());
                trace.addContent(cpu.createProcessUtilizationContent());

                try
                {
                    //best effort, requires tools.jar on path
                    trace.addContent(cpu.createThreadGroupUtilizationContent(".*", "acc%"));
                    trace.addContent(cpu.createThreadGroupUtilizationContent("[0-7][0-3][AB]", "dcol%"));
                    trace.addContent(cpu.createThreadGroupUtilizationContent("Processor-[0-7][0-3][AB]", "dcproc%"));
                    trace.addContent(cpu.createThreadGroupUtilizationContent("MultiChannelMergeSort-hits", "sort%"));
                    trace.addContent(cpu.createThreadGroupUtilizationContent(".*Compiler.*", "hotsp%"));
                    trace.addContent(cpu.createThreadGroupUtilizationContent(".*GC task.*", "gc%"));
                }
                catch (Exception e)
                {
                    logger.warn("Ignoring Thread CPU tracing:");
                    e.printStackTrace();
                }

                trace.start();
            }
        }

        private void stopTrace()
        {
            if(enable)
            {
                if(trace != null)
                {
                    trace.stop();
                }
            }
        }
    }



    public static void main(String[] args)
    {
        CommandLine cmd = new CommandLine(new OmicronCmd());
        cmd.execute(args);
    }

    // facilitates development
    static class Test
    {
        public static void main(String[] args)
        {
            OmicronCmd.main(new String[]{"--help"});
        }
    }
}
