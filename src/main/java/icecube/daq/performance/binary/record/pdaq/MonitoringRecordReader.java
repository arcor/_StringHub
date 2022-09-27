package icecube.daq.performance.binary.record.pdaq;

import icecube.daq.domapp.DOMAppUtil;
import icecube.daq.performance.binary.buffer.RecordBuffer;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.RecordUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MonitoringRecordReader
{

    public static class RAW_MonitoringRecordReader implements RecordReader
    {
        protected final int OFFSET;

        public static final int HARDWARE_TYPE = 0xc8;
        public static final int CONFIG_TYPE = 0xc9;
        public static final int CONFIG_CHANGE_TYPE = 0xca;
        public static final int ASCII_TYPE = 0xcB;
        public static final int GENERIC_TYPE = 0xcc;


        RAW_MonitoringRecordReader(int offset)
        {
            OFFSET = offset;
        }

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "|   length  |   type    |   domclk (uint48)               |     Monitoring Record ...      |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "length:  uint16\n" +
                "type:    uint16\n" +
                "type 0xc8   : Hardware Monitoring Record\n" +
                "type 0xcb   : ASCII Monitoring Record\n";


        @Override
        public int getLength(final ByteBuffer buffer)
        {
            return getLength(buffer, 0);
        }

        @Override
        public int getLength(final ByteBuffer buffer, final int offset)
        {
            return buffer.getShort(OFFSET + offset);
        }

        @Override
        public int getLength(final RecordBuffer buffer, final int offset)
        {
            return buffer.getShort(OFFSET + offset);
        }

        public short getType(final ByteBuffer buffer)
        {
            return getType(buffer, 0);
        }

        public short getType(final ByteBuffer buffer, final int offset)
        {
            return buffer.getShort(OFFSET + offset + 2);
        }

        public short getType(final RecordBuffer buffer, final int offset)
        {
            return buffer.getShort(OFFSET + offset + 2);
        }

        public long getDOMClock(final ByteBuffer buffer)
        {
            return getDOMClock(buffer, 0);
        }

        public long getDOMClock(final ByteBuffer buffer, final int offset)
        {
            return DOMAppUtil.decodeClock6B(buffer, OFFSET + offset + 4);
        }

        public long getDOMClock(final RecordBuffer buffer, final int offset)
        {
            long domclk = 0L;
            for (int itb = 0; itb < 6; itb++)
            {
                int x = ((int) buffer.getByte(OFFSET + offset + 4 + itb)) & 0xff;
                domclk = (domclk << 8) | x;
            }
            return domclk;
        }


        @Override
        public String describe()
        {
            return DOC;
        }

    }


    public static class RAW_HardwareMonitoringRecordReader extends RAW_MonitoringRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length=64| type=0xc8 |   domclk (uint48)               | ver | pad |      ...            |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                ...  data (uint16[27])                                    |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| speScalar (uint32)   |  mpeScalar (uint32   |" +
                "-----------------------------------------------\n" +
                "length:  uint16 = 64\n" +
                "type:    uint16 = 0xc8 = Hardware Monitoring Record\n" +
                "ver:     uint8 = Event Version\n" +
                "pad:     uint8 = alignment padding\n" +
                "data:    Array of 27 uint16 Monitoring Values:\n" +
                "         0: ADC_VOLTAGE_SUM              14: DAC_ATWD1_RAMP_RATE\n" +
                "         1: ADC_5V_POWER_SUPPLY          15: DAC_PMT_FE_PEDESTAL\n" +
                "         2: ADC_PRESSURE                 16: DAC_MULTIPLE_SPE_THRESH\n" +
                "         3: ADC_5V_CURRENT               17: DAC_SINGLE_SPE_THRESH\n" +
                "         4: ADC_3_3V_CURRENT             18: DAC_LED_BRIGHTNESS\n" +
                "         5: ADC_2_5V_CURRENT             19: DAC_FAST_ADC_REF\n" +
                "         6: ADC_1_8V_CURRENT             20: DAC_INTERNAL_PULSER\n" +
                "         7: ADC_MINUS_5V_CURRENT         21: DAC_FE_AMP_LOWER_CLAMP\n" +
                "         8: DAC_ATWD0_TRIGGER_BIAS       22: DAC_FL_REF\n" +
                "         9: DAC_ATWD0_RAMP_TOP           23: DAC_MUX_BIAS\n" +
                "         10: DAC_ATWD0_RAMP_RATE         24: PMT_BASE_HV_SET_VALUE\n" +
                "         11: DAC_ATWD_ANALOG_REF         25: PMT_BASE_HV_MONITOR_VALUE\n" +
                "         12: DAC_ATWD1_TRIGGER_BIAS      26: DOM_MB_TEMPERATURE\n" +
                "         13: DAC_ATWD1_RAMP_TOP\n";



        RAW_HardwareMonitoringRecordReader(int offset)
        {
            super(offset);
        }

        public short getEventVersion(ByteBuffer buffer)
        {
            return getEventVersion(buffer, 0);
        }

        public short getEventVersion(ByteBuffer buffer, int offset)
        {
            return buffer.get(super.OFFSET + offset + 10);
        }

        public short getEventVersion(RecordBuffer buffer, int offset)
        {
            return buffer.getByte(super.OFFSET + offset + 10);
        }

        public int[] getMonitoringValues(ByteBuffer buffer)
        {
            return getMonitoringValues(buffer, 0);
        }

        public int[] getMonitoringValues(ByteBuffer buffer, int offset)
        {
            int[] data = new int[27];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.getShort(super.OFFSET + offset + 12 + i*2) & 0xFFFF;
            }
            return data;
        }

        public int[] getMonitoringValues(RecordBuffer buffer, int offset)
        {
            int[] data = new int[27];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.getShort(super.OFFSET + offset + 12 + i*2) & 0xFFFF;
            }
            return data;
        }

        public int getSPEScalar(ByteBuffer buffer)
        {
            return getSPEScalar(buffer, 0);
        }

        public int getSPEScalar(ByteBuffer buffer, int offset)
        {
            return buffer.getInt(super.OFFSET + offset + 66);
        }

        public int getSPEScalar(RecordBuffer buffer, int offset)
        {
            return buffer.getInt(super.OFFSET + offset + 66);
        }


        public int getMPEScalar(ByteBuffer buffer)
        {
            return getMPEScalar(buffer, 0);
        }

        public int getMPEScalar(ByteBuffer buffer, int offset)
        {
            return buffer.getInt(super.OFFSET + offset + 70);
        }

        public int getMPEScalar(RecordBuffer buffer, int offset)
        {
            return buffer.getInt(super.OFFSET + offset + 70);
        }
    }


    public static class RAW_ConfigMonitoringRecordReader extends RAW_MonitoringRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length=54 | type=0xc9 |   domclk (uint48)               | ver | pad1| hw_cfg_len|  ...   |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| ... mbid (uint48)     |  pad2   |            hw_base_id (uint8)            |  fpga_bld   |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "| sfw_cfg_l | mb_sfw_bld| a  | b  | c  | d  | e  | f  | g  | h  | data_cfg_l |trig_info ...|  " +
                "--------------------------------------------------------------------------------------------\n" +
                "|    ...    | atwd_info (uint4)   |\n" +
                "-----------------------------------\n" +
                "length:      uint16 = 54\n" +
                "type:        uint16 = 0xc9 = Config Monitoring Record\n" +
                "ver:         uint8 = Event Version\n" +
                "pad1:        uint8 = alignment padding\n" +
                "hw_cfg_len:  uint16 = 18\n" +
                "pad2:        uint16 = alignment padding\n" +
                "fpga_bld:    uint16\n" +
                "sfw_cfg_l:   uint16 = 10\n" +
                "mb_sfw_bld:  uint16\n" +
                "a:           uint8 =  msg_hdlr_major\n" +
                "b:           uint8 =  msg_hdlr_minor\n" +
                "c:           uint8 =  exp_ctrl_major\n" +
                "d:           uint8 =  exp_ctrl_minor\n" +
                "e:           uint8 =  slo_ctrl_major\n" +
                "f:           uint8 =  slo_ctrl_minor\n" +
                "g:           uint8 =  data_acc_major\n" +
                "h:           uint8 =  data_acc_minor\n" +
                "data_cfg_l:  uint16 = 8 \n" +
                "trig_info:   uint32\n";



        RAW_ConfigMonitoringRecordReader(int offset)
        {
            super(offset);
        }

        public short getEventVersion(ByteBuffer buffer)
        {
            return getEventVersion(buffer, 0);
        }

        public short getEventVersion(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 10);
        }

        public short getEventVersion(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 10);
        }


        public int getHWCfgLen(ByteBuffer buffer)
        {
            return getHWCfgLen(buffer, 0);
        }

        public int getHWCfgLen(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 12);
        }

        public int getHWCfgLen(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 12);
        }


        public long getMbid(ByteBuffer buffer)
        {
            return getMbid(buffer, 0);
        }

        public long getMbid(ByteBuffer buffer, int offset)
        {
            int pos = super.OFFSET + offset + 14;
            long mbid = 0L;
            for (int itb = 0; itb < 6; itb++)
            {
                int x = ((int) buffer.get(pos + itb)) & 0xff;
                mbid = (mbid << 8) | x;
            }
            return mbid;
        }

        public long getMbid(RecordBuffer buffer, int offset)
        {
            int pos = super.OFFSET + offset + 14;
            long mbid = 0L;
            for (int itb = 0; itb < 6; itb++)
            {
                int x = ((int) buffer.getByte(pos + itb)) & 0xff;
                mbid = (mbid << 8) | x;
            }
            return mbid;
        }


        public long getHWBaseId(ByteBuffer buffer)
        {
            return getHWBaseId(buffer, 0);
        }

        public long getHWBaseId(ByteBuffer buffer, int offset)
        {
            return buffer.getLong(super.OFFSET + offset + 22);
        }

        public long getHWBaseId(RecordBuffer buffer, int offset)
        {
            return buffer.getLong(super.OFFSET + offset + 22);
        }


        public int getFPGABuildNum(ByteBuffer buffer)
        {
            return getFPGABuildNum(buffer, 0);
        }

        public int getFPGABuildNum(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 30);
        }

        public int getFPGABuildNum(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 30);
        }


        public int getSfwCfgLen(ByteBuffer buffer)
        {
            return getSfwCfgLen(buffer, 0);
        }

        public int getSfwCfgLen(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 32);
        }

        public int getSfwCfgLen(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 32);
        }


        public int getMBSfwBuildNum(ByteBuffer buffer)
        {
            return getMBSfwBuildNum(buffer, 0);
        }

        public int getMBSfwBuildNum(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 34);
        }

        public int getMBSfwBuildNum(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 34);
        }


        public short getMsgHandlerMajor(ByteBuffer buffer)
        {
            return getMsgHandlerMajor(buffer, 0);
        }

        public short getMsgHandlerMajor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 36);
        }

        public short getMsgHandlerMajor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 36);
        }


        public short getMsgHandlerMinor(ByteBuffer buffer)
        {
            return getMsgHandlerMinor(buffer, 0);
        }

        public short getMsgHandlerMinor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 37);
        }

        public short getMsgHandlerMinor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 37);
        }


        public short getExpCtrlMajor(ByteBuffer buffer)
        {
            return getExpCtrlMajor(buffer, 0);
        }

        public short getExpCtrlMajor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 38);
        }

        public short getExpCtrlMajor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 38);
        }


        public short getExpCtrlMinor(ByteBuffer buffer)
        {
            return getExpCtrlMinor(buffer, 0);
        }

        public short getExpCtrlMinor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 39);
        }

        public short getExpCtrlMinor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 39);
        }


        public short getSlowCtrlMajor(ByteBuffer buffer)
        {
            return getSlowCtrlMajor(buffer, 0);
        }

        public short getSlowCtrlMajor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 40);
        }

        public short getSlowCtrlMajor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 40);
        }


        public short getSlowCtrlMinor(ByteBuffer buffer)
        {
            return getSlowCtrlMinor(buffer, 0);
        }

        public short getSlowCtrlMinor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 41);
        }

        public short getSlowCtrlMinor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 41);
        }


        public short getDataAccessMajor(ByteBuffer buffer)
        {
            return getDataAccessMajor(buffer, 0);
        }

        public short getDataAccessMajor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 42);
        }

        public short getDataAccessMajor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 42);
        }


        public short getDataAccessMinor(ByteBuffer buffer)
        {
            return getDataAccessMinor(buffer, 0);
        }

        public short getDataAccessMinor(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 43);
        }

        public short getDataAccessMinor(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 43);
        }



        public int getDataCfgLen(ByteBuffer buffer)
        {
            return getDataCfgLen(buffer, 0);
        }

        public int getDataCfgLen(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 44);
        }

        public int getDataCfgLen(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint16(buffer, super.OFFSET + offset + 44);
        }



        public long getTrigConfigInfo(ByteBuffer buffer)
        {
            return getTrigConfigInfo(buffer, 0);
        }

        public long getTrigConfigInfo(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint32(buffer, super.OFFSET + offset + 46);

        }

        public long getTrigConfigInfo(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint32(buffer, super.OFFSET + offset + 46);
        }

        public long getATWDReadoutInfo(ByteBuffer buffer)
        {
            return getATWDReadoutInfo(buffer, 0);
        }

        public long getATWDReadoutInfo(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint32(buffer, super.OFFSET + offset + 50);
        }

        public long getATWDReadoutInfo(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint32(buffer, super.OFFSET + offset + 50);
        }
    }


    public static class RAW_ConfigChangeMonitoringRecordReader extends RAW_MonitoringRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length    | type=0xca |   domclk (uint48)               | mt | mst |      ...args...     |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "length:      uint16 = 54\n" +
                "type:        uint16 = 0xca = Config Change Monitoring Record\n" +
                "mt:          uint8 = Message Type\n" +
                "                     1: MESSAGE_HANDLER\n" +
                "                     2: DOM_SLOW_CONTROL\n" +
                "                     3: DATA_ACCESS\n" +
                "                     4: EXPERIMENT_CONTROL\n" +
                "                     5: TEST_MANAGER\n" +
                "mst:         uint8 = function code\n" +
                "hw_cfg_len:  uint16 = 18\n" +
                "args:        variable = arguments passed to service function\n";


        RAW_ConfigChangeMonitoringRecordReader(int offset)
        {
            super(offset);
        }

        public short getMT(ByteBuffer buffer)
        {
            return getMT(buffer, 0);
        }

        public short getMT(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 10);
        }

        public short getMT(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 10);
        }

        public short getMST(ByteBuffer buffer)
        {
            return getMST(buffer, 0);
        }

        public short getMST(ByteBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 11);
        }

        public short getMST(RecordBuffer buffer, int offset)
        {
            return RecordUtil.extractUint8(buffer, super.OFFSET + offset + 11);
        }

        public byte[] getParameters(ByteBuffer buffer)
        {
            return getParameters(buffer, 0);
        }

        public byte[] getParameters(ByteBuffer buffer, int offset)
        {
            int length = getLength(buffer, offset) - 12;
            byte[] data = new byte[length];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.get(super.OFFSET + offset + 12 + i);
            }
            return data;
        }

        public byte[] getParameters(RecordBuffer buffer, int offset)
        {
            int length = getLength(buffer, offset) - 12;
            byte[] data = new byte[length];
            for (int i = 0; i < data.length; i++)
            {
                data[i] = buffer.getByte(super.OFFSET + offset + 12 + i);
            }
            return data;
        }
    }


        public static class PDAQ_MonitoringRecordReader extends DaqBufferRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      | type (uint32)        |        mbid (uint64)                       |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|               padding (uint64)              |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|   length  |   type    |   domclk (uint48)               |     Monitoring Record ...      |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "length:  uint16\n" +
                "type:    uint16\n" +
                "type 0xc8   : Hardware Monitoring Record\n" +
                "type 0xcb   : ASCII Monitoring Record\n";

        public static final PDAQ_MonitoringRecordReader instance = new PDAQ_MonitoringRecordReader();
        public static final RAW_MonitoringRecordReader monitoringRecordReader = new RAW_MonitoringRecordReader(32);
        public static final RAW_HardwareMonitoringRecordReader hardwareMonitoringRecordReader  = new RAW_HardwareMonitoringRecordReader(32);
        public static final RAW_ConfigMonitoringRecordReader configMonitoringRecordReader  = new RAW_ConfigMonitoringRecordReader(32);
        public static final RAW_ConfigChangeMonitoringRecordReader configChangeMonitoringRecordReader  = new RAW_ConfigChangeMonitoringRecordReader(32);


        public String describe()
        {
            return DOC;
        }

    }

    public static class SECONDBUILD_MonitoringRecordReader extends SecondBuildRecordReader
    {

        private static final String DOC =
                "--------------------------------------------------------------------------------------------\n" +
                "| length (uint32)      |    type (uint32)     |        utc (uint64)                        |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                mbid (uint64)                |   length  |   type    | domclk (uint48) ...|\n" +
                "|-------------------------------------------------------------------------------------------\n" +
                "|    ...    |     Monitoring Record ...                                                    |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "|                                            ...                                           |\n" +
                "--------------------------------------------------------------------------------------------\n" +
                "length:  uint16\n" +
                "type:    uint16\n" +
                "type 0xc8   : Hardware Monitoring Record\n" +
                "type 0xcb   : ASCII Monitoring Record\n";


        public static final SECONDBUILD_MonitoringRecordReader instance = new SECONDBUILD_MonitoringRecordReader();
        public static final RAW_MonitoringRecordReader monitoringRecordReader = new RAW_MonitoringRecordReader(24);
        public static final RAW_HardwareMonitoringRecordReader hardwareMonitoringRecordReader  = new RAW_HardwareMonitoringRecordReader(24);

        public static final RAW_ConfigMonitoringRecordReader configMonitoringRecordReader  = new RAW_ConfigMonitoringRecordReader(24);
        public static final RAW_ConfigChangeMonitoringRecordReader configChangeMonitoringRecordReader  = new RAW_ConfigChangeMonitoringRecordReader(24);

        public String describe()
        {
            return DOC;
        }

    }
}
