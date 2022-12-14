package icecube.daq.sender;

import icecube.daq.bindery.BufferConsumer;
import icecube.daq.io.DAQOutputChannelManager;
import icecube.daq.monitoring.IRunMonitor;
import icecube.daq.monitoring.MonitoringData;
import icecube.daq.monitoring.SenderMXBean;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.performance.binary.buffer.IndexFactory;
import icecube.daq.performance.binary.record.pdaq.DaqBufferRecordReader;
import icecube.daq.performance.binary.store.RecordStore;
import icecube.daq.performance.common.PowersOfTwo;
import icecube.daq.spool.FilesHitSpool;
import icecube.daq.util.IDOMRegistry;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static icecube.daq.performance.binary.store.RecordStoreFactory.*;

/**
 * A facade into the Sender subsystem.
 *
 * The Sender subsystem connects the acquisition hit stream and readout
 * request stream to the hit spool, hit output channel and data readout
 * channel so that hit data may be dispensed to these consumers cohesively.
 * It also exports a monitor interface.
 *
 *
 *                             Monitor
 *                                +
 *                                |
 *                                |
 *                                |
 *                         +------+------+
 *                         |             |
 *  Hits In    +---------> |             +--------> Hit Channel
 *                         |   Sender    |
 *  ReadoutReq +---------> |             +--------> Readout Channel
 *                         |             |
 *                         +------+------+
 *                                |
 *                                |
 *                                |
 *                                v
 *                              Spool
 *
 *
 * Originally this functionality resided in icecube.daq.sender.Sender but
 * has been broken out into smaller pieces.
 *
 * Managing memory and cpu performance is critical for this subsystem so
 * configuration choices are defined here as well.
 *
 * todo: remove legacy sender after the rhinelander release
 * todo: allow for dynamic consumer add/remove.
 * todo: move setters to create() arguments if possible.
 * todo: document lifecycle (shutdown mechanism)
 *
 */
public interface SenderSubsystem
{

    /** Logger. */
    public static Logger logger = Logger.getLogger(SenderSubsystem.class);

    /**
     * starts the subsystem.
     */
    public void startup();

    /**
     * Sets the run monitor.
     * @param runMonitor The run monitor instance.
     */
    public void setRunMonitor(IRunMonitor runMonitor);

    /**
     * Request to send SLC hits to trigger.
     */
    public void forwardIsolatedHitsToTrigger();


    /**
     * Sets the hit output channel.
     * @param hitOut The hit output channel.
     */
    public void setHitOutput(DAQOutputChannelManager hitOut);

    /**
     * Sets the hit readout output channel.
     * @param dataOut The readout output channel.
     */
    public void setDataOutput(DAQOutputChannelManager dataOut);

    /**
     * Access the hit consumer interface. This will be connected
     * to the sorter output.
     * @return The hit input interface.
     */
    public BufferConsumer getHitInput();

    /**
     * Access the handler for hit readout requests. This will be connected
     * to the request reader.
     * @return The readout request handler.
     */
    public RequestHandler getReadoutRequestHandler();

    /**
     * Access the monitoring interface. This will be registered under
     * as the "sender" MBean.
     * @return The monitoring MBean.
     */
    public SenderMXBean getMonitor();


    /**
     * Factory for constructing use-case specific implementations.
     */
    public static enum Factory
    {

        /**
         * StringHub is configured to use a file-based hitspool for storage,
         * fronted with a fixed-size in-memory ring-buffer to increase the
         * performance of readout requests.
         *
         * When hit spooling is disabled, the fallback configuration uses an
         * unbounded memory store that is pruned in reaction to readout
         * requests.
         */
        STRING_HUB_COMPONENT
                {
                    /** The size of the in-memory data store. */
                    public final PowersOfTwo RING_SIZE = PowersOfTwo._536870912;
                    /** The min interval of data that will be held in memory.*/
                    public final long MIN_MEMORY_SPAN = 600000000000L;
                    /** The max interval of data that will be held in memory.*/
                    public final long MAX_MEMORY_SPAN = 900000000000L;
                    /** The increment for allocating the in-memory store. */
                    public final int MEMORY_SEGMENT_SIZE = (20 * 1024 * 1024);
                    /** The indexing strategy.*/
                    public final IndexFactory INDEX_MODE =
                       IndexFactory.UTCIndexMode.SPARSE_ONE_THOUSANDTHS_SECOND;
                    /** The data type. */
                    public final DaqBufferRecordReader DATA_TYPE =
                            DaqBufferRecordReader.instance;

                    @Override
                    public SenderSubsystem create(final int hubId,
                                           final IByteBufferCache hitCache,
                                           final IByteBufferCache readoutCache,
                                           final IDOMRegistry domRegistry)
                            throws IOException
                    {

                        final RecordStore.OrderedWritable storage;
                        final HitSpoolConfig hitSpoolConfig =
                                HitSpoolConfig.loadHitSpoolConfig();
                        if(hitSpoolConfig != null)
                        {
                            final long fileIntervalUTC =
                                    (long) (hitSpoolConfig.fileInterval * 1e10);

                            // file spool, un-syncronized
                            final RecordStore.OrderedWritable fileSpool =
                                    FILE.createFileSpool(
                                            DATA_TYPE,
                                            INDEX_MODE,
                                            hitSpoolConfig.directory,
                                            fileIntervalUTC,
                                            hitSpoolConfig.numFiles,
                                            false);
                            // ring memory, un-synchronized
                            final RecordStore.Prunable memorySpool =
                                    MEMORY.createRing(
                                            DATA_TYPE,
                                            INDEX_MODE,
                                            RING_SIZE,
                                            false);
                            //  read-through, synchronized
                            storage =
                                    READ_THROUGH.createReadThrough(
                                            DATA_TYPE,
                                            memorySpool,
                                            fileSpool,
                                            MIN_MEMORY_SPAN,
                                            MAX_MEMORY_SPAN,
                                            true);
                        }
                        else
                        {
                            // expanding memory, un-sychronized
                            final RecordStore.Prunable expandingArray =
                                    MEMORY.createExpandingArray(
                                            DATA_TYPE,
                                            INDEX_MODE,
                                            MEMORY_SEGMENT_SIZE,
                                            false);
                            // auto-pruned by query, synchronized
                            storage = QUERY_PRUNED.wrap(expandingArray, true);
                        }

                        return new NewSender(hubId, hitCache,
                                readoutCache, domRegistry, storage);
                    }

                    @Override
                    public SenderSubsystem createLegacy(final int hubId,
                                           final IByteBufferCache hitCache,
                                           final IByteBufferCache readoutCache,
                                           final IDOMRegistry domRegistry)
                            throws IOException
                    {

                        final HitSpoolConfig hitSpoolConfig =
                                HitSpoolConfig.loadHitSpoolConfig();

                        return new LegacySender(hubId, hitCache, readoutCache,
                                domRegistry, hitSpoolConfig);
                    }
                },
        /**
         * The replay component stores hits in unbounded memory that is
         * pruned in reaction to readout requests.
         */
        REPLAY_COMPONENT
                {
                    /** The increment for allocating the in-memory store. */
                    public final int MEMORY_SEGMENT_SIZE = (20 * 1024 * 1024);
                    /** The indexing strategy.*/
                    public final IndexFactory INDEX_MODE =
                       IndexFactory.UTCIndexMode.SPARSE_ONE_THOUSANDTHS_SECOND;
                    /** The data type. */
                    public final DaqBufferRecordReader DATA_TYPE =
                            DaqBufferRecordReader.instance;

                    @Override
                    public SenderSubsystem create(final int hubId,
                                           final IByteBufferCache hitCache,
                                           final IByteBufferCache readoutCache,
                                           final IDOMRegistry domRegistry)
                            throws IOException
                    {
                        // expanding memory, un-synchronized
                        final RecordStore.Prunable expandingArray =
                                MEMORY.createExpandingArray(DATA_TYPE,
                                        INDEX_MODE,
                                        MEMORY_SEGMENT_SIZE,
                                        false);
                        // auto-pruned by query, synchronized
                        final RecordStore.OrderedWritable storage =
                                QUERY_PRUNED.wrap(expandingArray, true);

                        return new NewSender(hubId, hitCache, readoutCache,
                                domRegistry, storage);
                    }

                    @Override
                    public SenderSubsystem createLegacy(final int hubId,
                                          final IByteBufferCache hitCache,
                                          final IByteBufferCache readoutCache,
                                          final IDOMRegistry domRegistry)
                            throws IOException
                    {
                        return new LegacySender(hubId, hitCache, readoutCache,
                                domRegistry, null);
                    }
                };

        /**
         * Constructs the SenderSubsystem.
         *
         * @param hubId The hub id;
         * @param hitCache Manages buffer accounting related to the hit stream.
         * @param readoutCache Manages buffer accounting related to the data
         *                     readout stream.
         * @param domRegistry Provides details of the DOM deployment.
         * @return The SenderSubsystem.
         * @throws IOException An error creating the subsystem.
         */
        public abstract SenderSubsystem create(final int hubId,
                                        final IByteBufferCache hitCache,
                                        final IByteBufferCache readoutCache,
                                        final IDOMRegistry domRegistry)
                throws IOException;

        /**
         * Constructs the SenderSubsystem utilizing legacy components.
         *
         * Provided to mitigate escaping defects with the newly developed
         * binary stores.
         *
         * @param hubId The hub id;
         * @param hitCache Manages buffer accounting related to the hit stream.
         * @param readoutCache Manages buffer accounting related to the data
         *                     readout stream.
         * @param domRegistry Provides details of the DOM deployment.
         * @return The SenderSubsystem.
         * @throws IOException An error creating the subsystem.
         */
        public abstract SenderSubsystem createLegacy(final int hubId,
                                           final IByteBufferCache hitCache,
                                           final IByteBufferCache readoutCache,
                                           final IDOMRegistry domRegistry)
                throws IOException;

    }


    /**
     * Encapsulates the hitspool configuration parameters.
     */
    public static class HitSpoolConfig
    {
        /** Default interval for each hitspool file (in seconds) */
        public static final double DEFAULT_HITSPOOL_INTERVAL = 15.0;
        /** Maximum number of hitspool files */
        public static final int DEFAULT_HITSPOOL_MAXFILES = 72000;

        public final File directory;
        public final double fileInterval;
        public final int numFiles;

        public HitSpoolConfig(final File directory, final double fileInterval,
                              final int numFiles)
        {
            this.directory = directory;
            this.fileInterval = fileInterval;
            this.numFiles = numFiles;
        }

        /**
         * Load config from environment.
         *
         * This logic was ported from StringHubComponent.
         *
         * @return The configured hitspool parameters, null if the top
         * level parameter is not defined.
         */
        private static HitSpoolConfig loadHitSpoolConfig()
        {
            final String directory = System.getProperty("hitspool.directory");
            if (directory == null) {
                return null;
            }

            double interval = DEFAULT_HITSPOOL_INTERVAL;

            final String ivalStr = System.getProperty("hitspool.interval");
            if (ivalStr != null) {
                try {
                    double tmpIval = Double.parseDouble(ivalStr);
                    interval = tmpIval;
                } catch (NumberFormatException nfe) {
                    logger.error("Bad hitspool interval \"" + ivalStr +
                            "\"; falling back to default " + interval);
                }
            }

            int numFiles = DEFAULT_HITSPOOL_MAXFILES;

            final String numStr = System.getProperty("hitspool.maxfiles");
            if (numStr != null) {
                try {
                    int tmpVal = Integer.parseInt(numStr);
                    numFiles = tmpVal;
                } catch (NumberFormatException nfe) {
                    logger.error("Bad number of hitspool files \"" + numStr +
                            "\"; falling back to default " + numFiles);
                }
            }

            return new HitSpoolConfig(new File(directory), interval, numFiles);
        }

    }


    /**
     * Encapsulates legacy objects to realize the SenderSubsystem
     * interface.
     *
     * The initialization code was ported from StringHubComponent.
     */
    public static class LegacySender implements SenderSubsystem
    {

        /** Legacy Sender implementation */
        private final Sender sender;

        // A trivial adapter for request input.
        private final RequestHandler requestHandler = new RequestHandler()
        {
            @Override
            public void addRequest(final IReadoutRequest request)
                    throws IOException
            {
                sender.addRequest((IPayload) request);
            }

            @Override
            public void addRequestStop() throws IOException
            {
                sender.addRequestStop();
            }
        };

        /**
         * This may be a reference to Hitspool or Sender depending on
         * the success of creating hitspool.
         */
        private BufferConsumer hitInput;

        public LegacySender(final int hubId,
                            final IByteBufferCache hitCache,
                             final IByteBufferCache readoutCache,
                             final IDOMRegistry domRegistry,
                             final HitSpoolConfig hitspool)
        {
            sender = new Sender(hubId, readoutCache);
            sender.setHitCache(hitCache);
            sender.setDOMRegistry(domRegistry);

            if(hitspool != null)
            {
                try
                {
                    hitInput = createHitspooler(hitspool, sender);
                }
                catch (IOException ioe)
                {
                    logger.error("Cannot create hitspooler", ioe);
                    hitInput = sender;
                }
            }
            else
            {
                hitInput = sender;
            }
        }

        @Override
        public void startup()
        {
            sender.reset();
        }

        @Override
        public void setRunMonitor(final IRunMonitor runMonitor)
        {
            sender.setRunMonitor(runMonitor);
        }

        @Override
        public void forwardIsolatedHitsToTrigger()
        {
            sender.forwardIsolatedHitsToTrigger();
        }

        @Override
        public void setHitOutput(final DAQOutputChannelManager hitOut)
        {
            sender.setHitOutput(hitOut);
        }

        @Override
        public void setDataOutput(final DAQOutputChannelManager dataOut)
        {
            sender.setDataOutput(dataOut);
        }

        @Override
        public BufferConsumer getHitInput()
        {
            return hitInput;
        }

        @Override
        public RequestHandler getReadoutRequestHandler()
        {
            return requestHandler;
        }

        @Override
        public SenderMXBean getMonitor()
        {
            MonitoringData monData = new MonitoringData();
            monData.setSenderMonitor(sender);
            return monData;
        }

        /**
         * hitspool creation taken from StringHub Component
         */
        private static BufferConsumer createHitspooler(HitSpoolConfig config,
                                                       BufferConsumer consumer)
                throws IOException
        {

            // send hits to hit spooler which forwards them to the sorter
            BufferConsumer hitSpooler =
                    new FilesHitSpool(consumer, config.directory,
                                      (long) (config.fileInterval * 1E10),
                            config.numFiles);

            return hitSpooler;
        }

    }


}
