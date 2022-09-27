package icecube.daq.performance.binary.record;

import icecube.daq.performance.binary.buffer.RecordBuffer;

import java.nio.ByteBuffer;

public class RecordUtil
{

    public static short extractUint8(ByteBuffer bb, int pos)
    {
        return (short) (bb.get(pos) & 0xff);
    }

    public static short extractUint8(RecordBuffer bb, int pos)
    {
        return (short) (bb.getByte(pos) & 0xff);
    }


    public static int extractUint16(ByteBuffer bb, int pos)
    {
        return (short) (bb.getShort(pos) & 0xffff);
    }

    public static int extractUint16(RecordBuffer bb, int pos)
    {
        return (short) (bb.getShort(pos) & 0xffff);
    }

    public static long extractUint32(ByteBuffer bb, int pos)
    {
        return ((long) bb.getInt(pos)) & 0xffffffffL;

    }

    public static long extractUint32(RecordBuffer bb, int pos)
    {
        return ((long) bb.getInt(pos) & 0xffffffffL);


    }


}
