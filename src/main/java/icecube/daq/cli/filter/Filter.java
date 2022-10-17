package icecube.daq.cli.filter;

import icecube.daq.cli.options.TimeOption;
import icecube.daq.cli.stream.RecordType;
import icecube.daq.payload.PayloadException;
import icecube.daq.performance.binary.record.RecordReader;
import icecube.daq.performance.binary.record.pdaq.DomHitRecordReader;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Collection of record filters
 */
public interface Filter
{

    String describe();

    Predicate<ByteBuffer> asPredicate(RecordType recordType);

    static Filter negateFilter(Filter toNegate)
    {
        return new Filter()
        {
            @Override
            public String describe()
            {
                return String.format("negates (%s)", toNegate.describe());
            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                Predicate<ByteBuffer> delegate = toNegate.asPredicate(recordType);

                return new Predicate<ByteBuffer>()
                {
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        return !delegate.test(byteBuffer);
                    }
                };
            }
        };
    }

    static Filter timeRangeFilter(final TimeOption.TimeInterval interval)
    {
        return new Filter()
        {
            @Override
            public String describe()
            {
                return "Selects records in time interval " + interval;
            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                final RecordReader.LongField utcField = recordType.rr.getOrderingField();
                return new Predicate<ByteBuffer>()
                {
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        return interval.inRange(utcField.value(byteBuffer, 0));
                    }
                };
            }
        };
    }

    static Filter limitCountFilter(long count)
    {
        return new Filter()
        {
            @Override
            public String describe()
            {
                return String.format("Restricts the stream to the first %d records", count);
            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                return new Predicate<ByteBuffer>()
                {
                    long currentCount = 0;
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        return currentCount++ < count;
                    }
                };
            }
        };
    }

    static Filter limitSizeFilter(long maxBytes)
    {
        return new Filter()
        {
            @Override
            public String describe()
            {
                return String.format("Restricts the stream to at most the first %d bytes", maxBytes);
            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                return new Predicate<ByteBuffer>()
                {
                    long currentSize = 0;
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        long tmp = currentSize + byteBuffer.remaining();
                        if(tmp <= maxBytes)
                        {
                            currentSize = tmp;
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }
                };
            }
        };
    }

    static Filter mbidFilter(long... mbid)
    {
        return new Filter()
        {

            @Override
            public String describe()
            {
                return "Selects records from modules in [" + Arrays.toString(mbid) + "]";

            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                final RecordReader.LongField mbidField = recordType.rr.getMbidField();

                // number of mbids:
                // 0  : use short circuit
                // 1  : use == comparison
                // 1+ : use hash lookup

                if(mbid.length == 0)
                {
                    return new Predicate<ByteBuffer>()
                    {
                        @Override
                        public boolean test(ByteBuffer bb)
                        {
                            return false;
                        }
                    };
                }
                else if(mbid.length == 1)
                {
                    final long wanted = mbid[0];
                    return new Predicate<ByteBuffer>()
                    {
                        @Override
                        public boolean test(ByteBuffer bb)
                        {
                            return mbidField.value(bb, 0) == wanted;
                        }
                    };
                }
                else
                {
                    final Set<Long> wanted = new HashSet<>(mbid.length);
                    for (int i = 0; i < mbid.length; i++) {
                        wanted.add(mbid[i]);
                    }

                    return new Predicate<ByteBuffer>()
                    {
                        @Override
                        public boolean test(ByteBuffer bb)
                        {
                            return wanted.contains(mbidField.value(bb, 0));
                        }
                    };
                }
            }
        };
    }

    static Filter triggerModeFilter(short mode)
    {
        return new Filter()
        {

            @Override
            public String describe()
            {
                return "Selects hit record with a specific (pdaq) trigger mode value";
            }

            @Override
            public Predicate<ByteBuffer> asPredicate(RecordType recordType)
            {
                return new Predicate<ByteBuffer>()
                {
                    @Override
                    public boolean test(ByteBuffer byteBuffer)
                    {
                        try {
                            DomHitRecordReader rr = DomHitRecordReader.resolve(byteBuffer);

                            short triggerMode = rr.getTriggerMode(byteBuffer);

                            return triggerMode == triggerMode;

                        } catch (PayloadException e) {
                            throw new Error(e);
                        }
                    }
                };
            }
        };
    }
}
