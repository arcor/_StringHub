package icecube.daq.cli.util;

import icecube.daq.payload.impl.UTCTime;

public interface UTCResolver
{
    String resolve(long utc);
    final UTCResolver NIL_RESOLVER = new UTCResolver()
    {
        @Override
        public String resolve(long utc)
        {
            return "";
        }
    };

    final UTCResolver DEFAULT = new UTCResolver()
    {
        @Override
        public String resolve(long utc)
        {
            return UTCTime.toDateString(utc);
        }
    };

    static UTCResolver create(final int dataYear)
    {
        return new UTCResolver()
        {
            @Override
            public String resolve(long utc)
            {
                return UTCTime.toDateString(utc, dataYear);
            }
        };
    }

}
