package icecube.daq.cli.filter;

import icecube.daq.cli.options.RecordTypeOption;
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

    private final short targetSource;
    private final long windowLen;

    public TriggerSourceFilter(short targetSource, long windowLen)
    {
        this.targetSource = targetSource;
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
                "trigger source %d", windowLen, targetSource);
    }

    @Override
    public Predicate<ByteBuffer> asPredicate(RecordTypeOption.RecordType recordType)
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

                        return triggerSource == targetSource;
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

                        boolean isTriggered = triggerSource == targetSource;

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
