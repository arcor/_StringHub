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


    public static String SUPPRESS_EXTENDED_MODE_KEY = "icecube.daq.domapp.suppressExtendedModeSettings";
    private static boolean suppress = Boolean.parseBoolean(System.getProperty(SUPPRESS_EXTENDED_MODE_KEY, "false"));

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


    /**
     * A transitional mechanism to enable the Basilisk release to operate with
     * either DOM_MB_450 or a prior version if a flag is set.
     *
     * If the legacy mode flag is set, the additional "extended mode" configuration
     * options introduced in DOM_MB_450 will be ignored.
     *
     * This should be removed after some period of time as backward compatible
     * configuration management is not easy and presents pitfalls.
     *
     * TODO: Remove
     */
    public static boolean suppressExtendedModeFeatures()
    {
        return suppress;
    }
}
