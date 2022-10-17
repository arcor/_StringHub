package icecube.daq.time.gps;

import org.apache.log4j.Logger;

/**
 * Factory providing system-wide access to the GPS service.
 * <p>
 * The GPS service builds on the DOR GPS snapshot buffer providing:
 * <ul>
 *     <li>One-to-many fan out of GPS snapshots from card to channel level</li>
 *     <li>Enforces stability invariant </li>
 *     <li>Monitoring, error handling and alerting</li>
 * </ul>
 * <p>
 * In normal deployments, the service will be a backed by a
 * DSB GPS card. Certain test deployments configured without
 * GPS hardware may configure this factory to provide a fallback
 * service. The fallback service does not provide meaningful UTC
 * time reconstructions.
 * <p>
 * Configuration
 * <pre>
 *
 *    icecube.daq.time.gps.gps-mode = [dsb]
 *
 *           dsb:      The master clock will be used via DSB card.
 *           no-dsb:   No GPS hardware, time reconstruction assumes zero DOR
 *                     clock offset.
 *           discover: Will use GPS hardware if available, falling back to
 *                     the no-dsb mode.
 *
 *</pre>
 *
 */

public class GPSService
{

    private static final Logger logger = Logger.getLogger(GPSService.class);

    public static final String GPS_MODE_PROPERTY = "icecube.daq.time.gps.gps-mode";

    /**
     * Configures the GPS mode, one of dsb, no-dsb, discover.
     */
    public static final String GPS_MODE_SETTING =
            System.getProperty(GPS_MODE_PROPERTY, "dsb");

    /** The singleton, system wide service instance. */
    private static final IGPSService service;

    // supported modes
    public static enum GPSMode
    {
        DSB("dsb")
                {
                    @Override
                    protected IGPSService initService()
                    {
                        return new DSBGPSService();
                    }
                },
        NO_DSB("no-dsb")
                {
                    @Override
                    protected IGPSService initService()
                    {
                        // This is not an appropriate production mode so log
                        // a warning
                        logger.warn("Running without GPS hardware," +
                                " UTC reconstruction will be impacted.");
                        return new NullGPSService();
                    }
                },
        DISCOVER("discover"){
            @Override
            protected IGPSService initService()
            {
                // This is not an appropriate production mode so log
                // a warning
                logger.warn("Running in relaxed GPS hardware mode," +
                        " UTC reconstruction may be impacted.");
                return new FailsafeGPSService( new DSBGPSService(),
                        new NullGPSService());
            }
        };

        public final String key;

        GPSMode(String key)
        {
            this.key = key;
        }

        protected abstract IGPSService initService();

        static GPSMode resolve(String key)
        {
            switch (key)
            {
                case "dsb": return DSB;
                case "no-dsb": return NO_DSB;
                case "discover": return DISCOVER;
                default: throw new Error("Unknown GPS mode: [" + key + "]");
            }
        }
    }


    // initialize the service
    static
    {
        service = GPSMode.resolve(GPS_MODE_SETTING).initService();
    }


    /**
     * Provides access the configured GPS service.
     *
     * @return The GPS service.
     */
    public static IGPSService getInstance() { return service; }


}
