package icecube.daq.cli.filter;

import icecube.daq.cli.stream.RecordType;
import icecube.daq.payload.PayloadException;
import icecube.daq.performance.binary.record.pdaq.DomHitRecordReader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 * Implements a hit stream filter that selects hits based on
 * a DOM-specific time window starting at each hit marked with
 * the specific trigger source flag.
 *
 * Implementation motivated by the Mainboard LED trigger use case
 */
public class TriggerSourceFilter implements Filter
{

    private final short targetSourceBit;
    private final long windowLen;

    /**
     * Trigger source bits as defined in DOMAPP_CPU_FPGA_Interface document
     *
     * -------------------------------------------------
     * Table 3: Trigger Source register
     *
     * Bit Function
     * 0 SPE discriminator
     * 1 MPE discriminator
     * 2 CPU forced
     * 3 Frontend pulser
     * 4 On board LED
     * 5 Flasher board
     * 6 Frontend R2R
     * 7 ATWD R2Rmv
     * 8 Local coincidence UP
     * 9 Local coincidence DOWN
     * -------------------------------------------------
     */
    public static short SPE = 0x1;
    public static short MPE = 0x1 << 1;
    public static short CPU = 0x1 << 2;
    public static short PULSER = 0x1 << 3;
    public static short MB_LED = 0x1 << 4;
    public static short FLASHER = 0x1 << 5;
    public static short FRONT_END_R2R = 0x1 << 6;
    public static short ATWD_R2RMV = 0x1 << 7;
    public static short LC_UP = 0x1 << 8;
    public static short LC_DOWN = 0x1 << 9;

    public TriggerSourceFilter(short targetSourceBit, long windowLen)
    {
        this.targetSourceBit = targetSourceBit;
        this.windowLen = windowLen;
    }

    static class Window
    {
        final long start_t;
        long end_t;

        Window(long start_t, long end_t)
        {
            this.start_t = start_t;
            this.end_t = end_t;
        }

        boolean inWindow(long timestamp)
        {
            return timestamp >= start_t && timestamp <= end_t;
        }
    }

    @Override
    public String describe()
    {
        return String.format("selects records from a dom for [%d] 1e-10 seconds after a hit with" +
                "trigger source bit %d set", windowLen, targetSourceBit);
    }

    @Override
    public Predicate<ByteBuffer> asPredicate(RecordType recordType)
    {

        if(windowLen > 0)
        {
            return new Predicate<ByteBuffer>()
            {
                @Override
                public boolean test(ByteBuffer byteBuffer)
                {
                    try {
                        DomHitRecordReader rr = DomHitRecordReader.resolve(byteBuffer);

                        short triggerSource = rr.getTriggerFlag(byteBuffer);

                        return (triggerSource & targetSourceBit) == targetSourceBit;
                    } catch (PayloadException e) {
                        throw new Error(e);
                    }
                }
            };
        }
        else
        {

            return new Predicate<ByteBuffer>()
            {
                HashMap<Long, Window> activeWindows = new HashMap(64);

                @Override
                public boolean test(ByteBuffer byteBuffer)
                {
                    try {
                        DomHitRecordReader rr = DomHitRecordReader.resolve(byteBuffer);

                        long mbid = rr.getDOMID(byteBuffer);
                        short triggerSource = rr.getTriggerFlag(byteBuffer);
                        long hitTime = rr.getUTC(byteBuffer);
                        Window curWindow = activeWindows.get(mbid);

                        boolean isTriggered = triggerSource == targetSourceBit;

                        // create, update or replace current window
                        if(isTriggered)
                        {
                            if(curWindow == null)
                            {
                                activeWindows.put(mbid, new Window(hitTime, hitTime+windowLen));
                                return true;
                            } else if (curWindow.inWindow(hitTime)) {
                                curWindow.end_t = hitTime + windowLen;
                                return true;
                            }
                            else
                            {
                                activeWindows.put(mbid, new Window(hitTime, hitTime+windowLen));
                                return true;
                            }
                        }
                        else
                        {
                            return ( curWindow != null && curWindow.inWindow(hitTime) );
                        }

                    } catch (PayloadException e) {
                        throw new Error(e);
                    }
                }
            };
        }

    }

}
