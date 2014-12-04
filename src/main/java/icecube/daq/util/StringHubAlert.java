package icecube.daq.util;

import icecube.daq.juggler.alert.AlertException;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.payload.impl.UTCTime;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringHubAlert
{
    /** Logging object */
    private static final Log LOG = LogFactory.getLog(StringHubAlert.class);

    /** Default alert priority */
    public static final Alerter.Priority DEFAULT_PRIORITY =
        Alerter.Priority.SCP;

    /** Placeholder for alerts without a card number */
    public static final int NO_CARD = Integer.MIN_VALUE;

    /** Placeholder for alerts without a pair number */
    public static final int NO_PAIR = Integer.MIN_VALUE;

    /** Placeholder for alerts without an A/B DOM specifier */
    public static final char NO_SPECIFIER = (char) 0;

    /** Placeholder for alerts without a run number */
    public static final int NO_RUNNUMBER = Integer.MIN_VALUE;

    /** Placeholder for alerts without a DAQ time */
    public static final long NO_UTCTIME = Long.MIN_VALUE;

    /**
     * Send a DOM alert.
     */
    public static final void sendDOMAlert(Alerter alerter,
                                          Alerter.Priority priority,
                                          String condition, int card, int pair,
                                          char dom, String mbid, String name,
                                          int string, int position,
                                          int runNumber, long utcTime)
    {
        if (alerter == null || !alerter.isActive()) {
            return;
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        if (dom != NO_SPECIFIER) {
            vars.put("card", card);
            vars.put("pair", pair);
            vars.put("dom", dom);
        }
        if (mbid != null) {
            vars.put("mbid", mbid);
        }
        if (name != null) {
            vars.put("name", name);
        }
        vars.put("string", string);
        vars.put("position", position);
        if (runNumber != NO_RUNNUMBER) {
            vars.put("runNumber", runNumber);
        }
        if (utcTime != NO_UTCTIME) {
            vars.put("exact-time", UTCTime.toDateString(utcTime));
        }

        try {
            alerter.sendAlert(priority, condition, vars);
        } catch (AlertException ae) {
            LOG.error("Cannot send " + condition + " alert", ae);
        }
    }
}
