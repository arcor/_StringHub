package icecube.daq.domapp;


/**
 * Models Self Local Coincidence settings
 *
 * Mirrors DSCmessageAPIstatus.h
 *
 * typedef enum {
 *   SELF_LC_MODE_NONE=0,
 *   SELF_LC_MODE_SPE=1,
 *   SELF_LC_MODE_MPE=2
 * } SELF_LC_MODE_T;
 * #define MAX_SELF_LC_MODE SELF_LC_MODE_MPE
 */
public class SelfLCConfiguration
{

    /**
     * Self LC modes
     */
    public enum SelfLCMode
    {

        /**
         * No LC required to send WF
         */
        SELF_LC_MODE_NONE((byte)0, "none"),
        /**
         * Either UP or DOWN LC signal will cause WF x-mit
         */
        SELF_LC_MODE_SPE((byte)1, "spe"),
        /**
         * Only state of UP LC signal matters
         */
        SELF_LC_MODE_MPE((byte)2, "mpe");



        // domapp interface value
        final byte val;

        // xml config value
        final String xmlval;

        SelfLCMode(byte val, String xmlval)
        {
            this.val = val;
            this.xmlval = xmlval;
        }

        public byte asByte() { return val; }

        public String asXML() { return xmlval; }

        public static SelfLCMode resolve(byte val)
        {
            switch (val)
            {
                case 0: return SELF_LC_MODE_NONE;
                case 1: return SELF_LC_MODE_SPE;
                case 2: return SELF_LC_MODE_MPE;

                default: return null;
            }
        }

        public static SelfLCMode resolve(String xmlval)
        {
            switch (xmlval)
            {
                case "none": return SELF_LC_MODE_NONE;
                case "spe": return SELF_LC_MODE_SPE;
                case "mpe": return SELF_LC_MODE_MPE;

                default: return null;
            }
        }

    }

    SelfLCMode mode;
    int window;

    public SelfLCConfiguration()
    {
        mode = SelfLCMode.SELF_LC_MODE_NONE;
        window = 200;
    }

    public void setMode(SelfLCMode mode)
    {
        this.mode = mode;
    }

    public SelfLCMode getMode()
    {
        return mode;
    }

    public void setWindow(int window)
    {
        this.window = window;
    }

    public int getWindow()
    {
        return window;
    }

    public String prettyPrint(String ident)
    {
        return  "SelfLCConfiguration{" + "\n" +
                ident + "  mode     =   " + mode + "\n" +
                ident + "  window   =   " + window + "\n" +
                ident + '}';
    }
}
