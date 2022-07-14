package icecube.daq.domapp;

/**
 * Enumerates the DAQ modes
 *
 * Mirrors DSCmessageSPIstatus.h
 *
 * #define DAQ_MODE_ATWD_FADC 0
 * #define DAQ_MODE_FADC      1
 * #define DAQ_MODE_TS        2
 */
public enum DAQMode
{

    ATWD_FADC((byte)0, "atwd_fadc"),
    FADC((byte)1, "fadc"),
    TS((byte)2, "timestamp");

    final byte val;
    final String xmlval;

    DAQMode(byte val, String xmlval)
    {
        this.val = val;
        this.xmlval = xmlval;
    }


    public byte asByte() { return val; }

    public String asXMLValue() { return xmlval; }

    public static DAQMode resolve(byte val)
    {
        switch (val)
        {
            case 0: return ATWD_FADC;
            case 1: return FADC;
            case 2: return TS;

            default: return null;
        }
    }

    public static DAQMode resolve(String xmlval)
    {
        switch (xmlval)
        {
            case "fadc_atwd": return ATWD_FADC;
            case "fadc": return FADC;
            case "timestamp": return TS;

            default: return null;
        }
    }


}
