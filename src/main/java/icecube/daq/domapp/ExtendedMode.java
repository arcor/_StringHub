package icecube.daq.domapp;

import org.apache.log4j.Logger;

/**
 * Encapsulates the ExtendedMode control flag.
 *
 * The ExtendedMode flag acts as an interlock for protected
 * DomApp configurations and functions. The flag is set by the
 * operator at runtime.
 */
public final class ExtendedMode
{
    private static boolean extendedModeEnable = false;

    private static Logger logger = Logger.getLogger(ExtendedMode.class);

    /**
     * EnhancedMode protections should be enforced proactively in the acquisition application,
     * but this may be disabled in order to test DOMApp's internal extended mode restrictions
     */
    public static String ENFORCE_OVERRIDE_KEY = "icecube.daq.domapp.enforceExtendedModeSettings";
    private static boolean enforce = Boolean.parseBoolean(System.getProperty(ENFORCE_OVERRIDE_KEY, "true"));

    public static void enableExtendedMode()
    {
        logger.warn("Enabling Extended Mode");
        extendedModeEnable = true;
    }

    public static boolean isExtendedModeEnable()
    {
        return extendedModeEnable;
    }

    public static boolean enforce()
    {
        return enforce;
    }
}
