package icecube.daq.cli.transform;

import icecube.daq.cli.stream.RecordType;
import icecube.daq.performance.binary.record.pdaq.SecondBuildRecordReader;

import java.nio.ByteBuffer;
import java.util.function.Function;

public interface Transform
{
    String describe();

    Function<ByteBuffer, ByteBuffer> asMapper(RecordType inputType);

     RecordType outputType(RecordType inputType);


     static Transform secondBuildToPdaq()
     {
         return new Transform()
         {
             @Override
             public String describe()
             {
                 return "Transforms a second build record into its corresponding pdaq internal format";
             }

             @Override
             public Function<ByteBuffer, ByteBuffer> asMapper(RecordType inputType)
             {
                 return new Function<ByteBuffer, ByteBuffer>()
                 {
                     @Override
                     public ByteBuffer apply(ByteBuffer bb)
                     {
                         int len = SecondBuildRecordReader.instance.getLength(bb);
                         int type = SecondBuildRecordReader.instance.getTypeId(bb);
                         long utc = SecondBuildRecordReader.instance.getUTC(bb);
                         long mbid = SecondBuildRecordReader.instance.getDOMId(bb);

                         final int pdaqType;
                         switch (type)
                         {
                             case 4:
                                 pdaqType = 202;
                                 break;
                             case 5:
                                 pdaqType = 102;
                                 break;
                             case 16:
                                 pdaqType = 302;
                                 break;
                             default:
                                 throw new Error(String.format("Unsupported type [%s]", type));
                         }

                         ByteBuffer pdaq = ByteBuffer.allocate(len + 8);
                         pdaq.putInt(len + 8);
                         pdaq.putInt(pdaqType);
                         pdaq.putLong(mbid);
                         pdaq.putLong(0);
                         pdaq.putLong(utc);
                         pdaq.put((ByteBuffer) bb.slice().position(bb.position() + 24));

                         return (ByteBuffer) pdaq.flip();
                     }
                 };
             }

             @Override
             public RecordType outputType(RecordType inputType)
             {
                 switch (inputType)
                 {
                     case SECONDBUILD:
                         return RecordType.PDAQ;
                     case SECONDBUILD_MONI:
                         return RecordType.PDAQ_MONI;
                     case SECONDBUILD_SN:
                         return RecordType.PDAQ_SN;
                     case SECONDBUILD_TCAL:
                         return RecordType.PDAQ_TCAL;
                     default:
                         throw new Error(String.format("Unsupported input type [%s]", inputType.keyword));
                 }
             }
         };
     }

}
