package icecube.daq.cli.filter;

import icecube.daq.cli.options.RecordTypeOption;
import icecube.daq.performance.binary.record.pdaq.MonitoringRecordReader;
import icecube.daq.performance.binary.record.pdaq.SecondBuildRecordReader;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

/**
 * Filters acting on monitoring records
 */
public class MoniFilter
{
    public static Filter HARDWARE_FILTER = new TypeFilter(MonitoringRecordReader.RAW_MonitoringRecordReader.HARDWARE_TYPE);
    public static Filter CONFIG_FILTER = new TypeFilter(MonitoringRecordReader.RAW_MonitoringRecordReader.CONFIG_TYPE);
    public static Filter CONFIG_CHANGE_FILTER = new TypeFilter(MonitoringRecordReader.RAW_MonitoringRecordReader.CONFIG_CHANGE_TYPE);
    public static Filter ASCII_FILTER = new TypeFilter(MonitoringRecordReader.RAW_MonitoringRecordReader.ASCII_TYPE);
    public static Filter GENERIC_FILTER = new TypeFilter(MonitoringRecordReader.RAW_MonitoringRecordReader.GENERIC_TYPE);



    private static class TypeFilter implements  Filter
    {
        final int typeVal;
        final String description;

        private TypeFilter(int typeVal)
        {
            this.typeVal = typeVal;
            this.description =  String.format("Selects moni records of type %x", typeVal);
        }

        @Override
        public String describe()
        {
            return description;
        }

        @Override
        public Predicate<ByteBuffer> asPredicate(RecordTypeOption.RecordType recordType)
        {
            if( recordType.rr instanceof SecondBuildRecordReader ||
                    recordType.rr instanceof  MonitoringRecordReader.SECONDBUILD_MonitoringRecordReader)
            {
                return new Predicate<ByteBuffer>()
                {
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        return MonitoringRecordReader.SECONDBUILD_MonitoringRecordReader.monitoringRecordReader.getType(byteBuffer) ==
                                typeVal;
                    }
                };

            } else if (recordType.rr instanceof SecondBuildRecordReader ||
                    recordType.rr instanceof  MonitoringRecordReader.PDAQ_MonitoringRecordReader) {
                return new Predicate<ByteBuffer>()
                {
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        return MonitoringRecordReader.PDAQ_MonitoringRecordReader.monitoringRecordReader.getType(byteBuffer) ==
                                typeVal;
                    }
                };
            }
            else
            {
                throw new Error("Can't apply moni filter to " + recordType.typeName + " records");
            }


        }
    };
}
