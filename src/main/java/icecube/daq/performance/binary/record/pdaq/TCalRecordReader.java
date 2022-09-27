package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.performance.binary.buffer.RecordBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Record readers for various record types containing time calibration records
 */
public class TCalRecordReader
{


    /**
     * The dor header originates in firmware and is propagated by the dor driver.
     *
     * This is a little endian word holding two little endian 16-bit fields:
     *
     * E.G. : E0 00 01 00
     *        |  |  |  |
     *        |  |  `--`---> flags (le) = 0x0001
     *        `--`---------> byte count(le) = 0x00e0 = 224
     *
     * byte count is a count of the bytes in tcal record resident in the dor card,
     * the dor card expands the tcal record size, ostensibly to define a fixed-sized
     * record for downstream consumers by fixing the number of waveform samples to 64
     * 16-bit samples. So whereas the dor firmware only contains 48 samples per waveform,
     * the dor driver will zero-fill the tcal record to maintain fixed sized waveforms
     * containing 64 samples. The difference between the tcal record size and the firmware
     * byte count reflects this zero-fill.
     */
    public static class DORHeader
    {
        public static short extractByteCount(int header)
        {

            return (short) (((header >> 24) & 0xFF) | ((header >> 8) & 0xFF00));
        }

        public static short extractFlagBits(int header)
        {

            return (short) (((header >> 8) & 0xFF) | ((header << 8) & 0xFF00));
        }
    }



    public static class PDAQ_TCalRecordReader extends DaqBufferRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      |    type (uint32)     |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                mbid (uint64)                | dor_header (le uint32) |        ...        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| ... dortx (le uint64)|              dorrx (le uint64)                |        ...        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                 ... dorwf (le uint16[64])  ...                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                domrx (le uint64)            |               domtx (le uint64)            |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                     domwf (le uint16[64])  ...                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "dor_header:   LE word originating from firmware, holds two LE 16-bit fields,\n" +
                "                 byte_count: The length of the original record in firmware\n" +
                "                 flag: status flag bits\n" +
                "                 E.G. : E0 00 01 00\n" +
                "                         |  |  |  |\n" +
                "                         |  |  `--`---> flags (le) = 0x0001\n" +
                "                         `--`---------> byte count(le) = 0x00e0 = 224\n";


        public static final PDAQ_TCalRecordReader instance = new PDAQ_TCalRecordReader();

        TCalPayloadReader payloadReader = new TCalPayloadReader(32);

        public int getDorHeader(final ByteBuffer buffer)
        {
            return getDorHeader(buffer, 0);
        }

        public int getDorHeader(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDorHeader(buffer, offset);
        }

        public int getDorHeader(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDorHeader(buffer, offset);
        }

        public long getDORTX(final ByteBuffer buffer)
        {
            return getDORTX(buffer, 0);
        }
        public long getDORTX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDORTX(buffer, offset);
        }
        public long getDORTX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDORTX(buffer, offset);
        }

        public long getDORRX(final ByteBuffer buffer)
        {
            return getDORRX(buffer, 0);
        }
        public long getDORRX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDORRX(buffer, offset);
        }
        public long getDORRX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDORRX(buffer, offset);
        }

        public short[] getDORWaveform(final ByteBuffer buffer)
        {
            return getDORWaveform(buffer, 0);
        }
        public short[] getDORWaveform(final ByteBuffer buffer, int offset)
        {
            return payloadReader.getDORWaveform(buffer, offset);
        }

        public short[] getDORWaveform(final RecordBuffer buffer, int offset)
        {
            return payloadReader.getDORWaveform(buffer, offset);
        }

        public long getDOMRX(final ByteBuffer buffer)
        {
            return getDOMRX(buffer, 0);
        }
        public long getDOMRX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDOMRX(buffer, offset);
        }
        public long getDOMRX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDOMRX(buffer, offset);
        }

        public long getDOMTX(final ByteBuffer buffer)
        {
            return getDOMTX(buffer, 0);
        }
        public long getDOMTX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDOMTX(buffer, offset);
        }
        public long getDOMTX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDOMTX(buffer, offset);
        }



        public short[] getDOMWaveform(final ByteBuffer buffer)
        {
            return getDOMWaveform(buffer, 0);
        }
        public short[] getDOMWaveform(final ByteBuffer buffer, int offset)
        {
            return payloadReader.getDOMWaveform(buffer, offset);
        }

        public short[] getDOMWaveform(final RecordBuffer buffer, int offset)
        {
            return payloadReader.getDOMWaveform(buffer, offset);

        }

        @Override
        public String describe()
        {
            return DOC;
        }
    }


    public static class SECONDBUILD_TCalRecordReader extends SecondBuildRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      |    type (uint32)     |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                mbid (uint64)                | dor_header (le uint32) |        ...        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| ... dortx (le uint64)|              dorrx (le uint64)                |        ...        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                 ... dorwf (le uint16[64])  ...                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                domrx (le uint64)            |               domtx (le uint64)            |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                     domwf (le uint16[64])  ...                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|SOH|timestr|qual|                dorclk (uint64)              |\n" +
                "----------------------------------------------------------------\n" +
                "dor_header:   LE word originating from firmware, holds two LE 16-bit fields,\n" +
                "                 byte_count: The length of the original record in firmware\n" +
                "                 flag: status flag bits\n" +
                "                 E.G. : E0 00 01 00\n" +
                "                         |  |  |  |\n" +
                "                         |  |  `--`---> flags (le) = 0x0001\n" +
                "                         `--`---------> byte count(le) = 0x00e0 = 224\n" +
                "SOH (byte):         IRIG Start-of-header character\n" +
                "timestr (byte[12]): IRIG time srting DDD:HH:MM:SS\n" +
                "qual:               Master clock quality marker\n" +
                "                       0x20 : ' ' : VERY GOOD\n" +
                "                       0x2e : '.' : GOOD\n" +
                "                       0x2a : '*' : AVERAGE\n" +
                "                       0x23 : '#' : BAD\n" +
                "                       0x3f : '?' : VERY BAD\n";

        public static final SECONDBUILD_TCalRecordReader instance = new SECONDBUILD_TCalRecordReader();

        TCalPayloadReader payloadReader = new TCalPayloadReader(24);
        GPSRecordReader gpsRecordReader = new GPSRecordReader(316);

        public int getDorHeader(final ByteBuffer buffer)
        {
            return getDorHeader(buffer, 0);
        }

        public int getDorHeader(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDorHeader(buffer, offset);
        }

        public int getDorHeader(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDorHeader(buffer, offset);
        }

        public long getDORTX(final ByteBuffer buffer)
        {
            return getDORTX(buffer, 0);
        }
        public long getDORTX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDORTX(buffer, offset);
        }
        public long getDORTX(final RecordBuffer buffer, final int offset)
        {
         return payloadReader.getDORTX(buffer, offset);
        }

        public long getDORRX(final ByteBuffer buffer)
        {
            return getDORRX(buffer, 0);
        }
        public long getDORRX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDORRX(buffer, offset);
        }
        public long getDORRX(final RecordBuffer buffer, final int offset)
        {
           return payloadReader.getDORRX(buffer, offset);
        }

        public short[] getDORWaveform(final ByteBuffer buffer)
        {
            return getDORWaveform(buffer, 0);
        }
        public short[] getDORWaveform(final ByteBuffer buffer, int offset)
        {
            return payloadReader.getDORWaveform(buffer, offset);
        }

        public short[] getDORWaveform(final RecordBuffer buffer, int offset)
        {
            return payloadReader.getDORWaveform(buffer, offset);
        }

        public long getDOMRX(final ByteBuffer buffer)
        {
            return getDOMRX(buffer, 0);
        }
        public long getDOMRX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDOMRX(buffer, offset);
        }
        public long getDOMRX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDOMRX(buffer, offset);
        }

        public long getDOMTX(final ByteBuffer buffer)
        {
            return getDOMTX(buffer, 0);
        }
        public long getDOMTX(final ByteBuffer buffer, final int offset)
        {
            return payloadReader.getDOMTX(buffer, offset);
        }
        public long getDOMTX(final RecordBuffer buffer, final int offset)
        {
            return payloadReader.getDOMTX(buffer, offset);
        }



        public short[] getDOMWaveform(final ByteBuffer buffer)
        {
            return getDOMWaveform(buffer, 0);
        }
        public short[] getDOMWaveform(final ByteBuffer buffer, int offset)
        {
           return payloadReader.getDOMWaveform(buffer, offset);
        }

        public short[] getDOMWaveform(final RecordBuffer buffer, int offset)
        {
            return payloadReader.getDOMWaveform(buffer, offset);

        }

        public byte getSOH(final ByteBuffer buffer)
        {
            return getSOH(buffer, 0);
        }

        public byte getSOH(final ByteBuffer buffer, final int offset)
        {
            return gpsRecordReader.getSOH(buffer, offset);
        }

        public byte getSOH(final RecordBuffer buffer, final int offset)
        {
            return gpsRecordReader.getSOH(buffer, offset);
        }

        public String getTimestring(final ByteBuffer buffer)
        {
            return getTimestring(buffer, 0);
        }

        public String getTimestring(final ByteBuffer buffer, final int offset)
        {
            return gpsRecordReader.getTimestring(buffer, offset);
        }

        public String getTimestring(final RecordBuffer buffer, final int offset)
        {
            return gpsRecordReader.getTimestring(buffer, offset);
        }

        public byte getQualityMark(final ByteBuffer buffer)
        {
            return getQualityMark(buffer, 0);
        }

        public byte getQualityMark(final ByteBuffer buffer, final int offset)
        {
            return gpsRecordReader.getQualityMark(buffer, offset);
        }

        public byte getQualityMark(final RecordBuffer buffer, final int offset)
        {
            return gpsRecordReader.getQualityMark(buffer, offset);
        }

        public long getDORClock(final ByteBuffer buffer)
        {
            return getDORClock(buffer, 0);
        }

        public long getDORClock(final ByteBuffer buffer, final int offset)
        {
            return  gpsRecordReader.getDORClock(buffer, offset);
        }

        public long getDORClock(final RecordBuffer buffer, final int offset)
        {
            return  gpsRecordReader.getDORClock(buffer, offset);
        }

        @Override
        public String describe()
        {
            return DOC;
        }
    }


    public static class TCalPayloadReader
    {
        final int payloadoffset;


        protected TCalPayloadReader(int payloadoffset)
        {
            this.payloadoffset = payloadoffset;
        }

        public int getDorHeader(final ByteBuffer buffer, final int offset)
        {
            return buffer.getInt(offset + payloadoffset);
        }

        public int getDorHeader(final RecordBuffer buffer, final int offset)
        {
            return buffer.getInt(offset + payloadoffset);
        }

        public long getDORTX(final ByteBuffer buffer, final int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            try {
                return buffer.getLong(offset + payloadoffset + 4);
            } finally {
                buffer.order(orig);
            }
        }
        public long getDORTX(final RecordBuffer buffer, final int offset)
        {
            long val =  ((long)buffer.getByte(offset + payloadoffset + 11) & 0xff) << 56;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 10) & 0xff) << 48;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 9) & 0xff) << 40;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 8) & 0xff) << 32;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 7) & 0xff) << 24;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 6) & 0xff) << 16;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 5) & 0xff) << 8;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 4) & 0xff);

            return val;
        }

        public long getDORRX(final ByteBuffer buffer, final int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            try {
                return buffer.getLong(offset + payloadoffset + 12);
            } finally {
                buffer.order(orig);
            }
        }
        public long getDORRX(final RecordBuffer buffer, final int offset)
        {

            long val =  ((long)buffer.getByte(offset + payloadoffset + 19) & 0xff) << 56;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 18) & 0xff) << 48;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 17) & 0xff) << 40;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 16) & 0xff) << 32;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 15) & 0xff) << 24;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 14) & 0xff) << 16;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 13) & 0xff) << 8;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 12) & 0xff);

            return val;
        }

        public short[] getDORWaveform(final ByteBuffer buffer, int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            short[] ret = new short[64];
            int idx = payloadoffset + offset + 20;

            for(int i=0; i<64; i++)
            {
                ret[i] = buffer.getShort(idx);
                idx+=2;
            }

            buffer.order(orig);
            return ret;
        }

        public short[] getDORWaveform(final RecordBuffer rb, int offset)
        {
            short[] ret = new short[64];
            int idx = payloadoffset + offset + 20;

            for(int i=0; i<64; i++)
            {
                ret[i] = (short) ((rb.getByte(idx++)  & 0xff) | (rb.getByte(idx++)  & 0xff) << 8);
            }

            return ret;
        }

        public long getDOMRX(final ByteBuffer buffer, final int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            try {
                return buffer.getLong(offset + payloadoffset + 148);
            } finally {
                buffer.order(orig);
            }
        }
        public long getDOMRX(final RecordBuffer buffer, final int offset)
        {
            long val =  ((long)buffer.getByte(offset + payloadoffset + 155) & 0xff) << 56;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 154) & 0xff) << 48;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 153) & 0xff) << 40;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 152) & 0xff) << 32;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 151) & 0xff) << 24;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 150) & 0xff) << 16;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 149) & 0xff) << 8;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 148) & 0xff);


            return val;
        }

        public long getDOMTX(final ByteBuffer buffer, final int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            try {
                return buffer.getLong(offset + payloadoffset + 156);
            } finally {
                buffer.order(orig);
            }
        }
        public long getDOMTX(final RecordBuffer buffer, final int offset)
        {
            long val =  ((long)buffer.getByte(offset + payloadoffset + 163) & 0xff) << 56;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 162) & 0xff) << 48;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 161) & 0xff) << 40;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 160) & 0xff) << 32;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 159) & 0xff) << 24;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 158) & 0xff) << 16;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 157) & 0xff) << 8;
                 val |= ((long)buffer.getByte(offset + payloadoffset + 156) & 0xff);



            return val;
        }



        public short[] getDOMWaveform(final ByteBuffer buffer, int offset)
        {
            ByteOrder orig = buffer.order();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            short[] ret = new short[64];
            int idx = payloadoffset + offset + 164;

            for(int i=0; i<64; i++)
            {
                ret[i] = buffer.getShort(idx);
                idx+=2;
            }

            buffer.order(orig);
            return ret;
        }

        public short[] getDOMWaveform(final RecordBuffer rb, int offset)
        {
            short[] ret = new short[64];
            int idx = payloadoffset + 164;

            for(int i=0; i<64; i++)
            {
                ret[i] = (short) ((rb.getByte(idx++)  & 0xff) | (rb.getByte(idx++)  & 0xff) << 8);
            }

            return ret;
        }
    }

    public static class GPSRecordReader
    {
        final int payloadoffset;

        protected GPSRecordReader(int payloadoffset)
        {
            this.payloadoffset = payloadoffset;
        }

        public byte getSOH(final ByteBuffer buffer, final int offset)
        {
            return buffer.get(offset + payloadoffset);
        }

        public byte getSOH(final RecordBuffer buffer, final int offset)
        {
            return buffer.getByte(offset + payloadoffset);
        }

        public String getTimestring(final ByteBuffer buffer, final int offset)
        {
            byte[] timestringbytes = new byte[12];
            for (int i = 0; i < timestringbytes.length; i++) {
              timestringbytes[i] = buffer.get(payloadoffset + offset + 1 + i);
            }

            return new String(timestringbytes);
        }

        public String getTimestring(final RecordBuffer buffer, final int offset)
        {
            return new String(buffer.getBytes(payloadoffset + offset + 1, 12));
        }

        public byte getQualityMark(final ByteBuffer buffer, final int offset)
        {
            return buffer.get(payloadoffset + offset + 13);
        }

        public byte getQualityMark(final RecordBuffer buffer, final int offset)
        {
            return buffer.getByte(payloadoffset + offset + 13);
        }

        public long getDORClock(final ByteBuffer buffer, final int offset)
        {
            return buffer.getLong(payloadoffset + offset + 14);
        }

        public long getDORClock(final RecordBuffer buffer, final int offset)
        {
            return buffer.getLong(payloadoffset + offset + 14);
        }

    }
}
