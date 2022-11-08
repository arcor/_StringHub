package icecube.daq.cli.util;

import icecube.daq.cli.data.CLIData;
import icecube.daq.util.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Failsafe DOMRegistry wrapper.
 */
public class DomResolver
{

    static Logger logger = Logger.getLogger(DomResolver.class);

    private static IDOMRegistry instance;

    public static String sourceLocation;

    public static IDOMRegistry instance()
    {
        synchronized (DomResolver.class)
        {
            if(instance == null)
            {
                instance = load();
            }
        }

        return instance;

    }

    private static IDOMRegistry load()
    {

        // defer to the local "config/default-dom-geometry.xml"
        logger.debug("Loading DOM registry from environment");
        try {
            File configDirectory = LocatePDAQ.findConfigDirectory();

            sourceLocation = configDirectory.getAbsolutePath().concat("/default_dom_geometry.xml");
            logger.info("Loading DOM registry from " + sourceLocation);
            return DOMRegistryFactory.load(sourceLocation);
        } catch (Throwable th) {
            logger.warn("could not load default-dom-geometry.xml from environment: " + th.getMessage());
        }
        // load from resources
        logger.warn("Loading internal fallback-default-dom-geometry.xml ...could be stale");
        try {
            sourceLocation = CLIData.FALLBACK_DOM_GEOMETRY.getLocation();
            logger.warn("loading dom geometry from "  + sourceLocation);
            return DOMRegistryFactory.load(CLIData.FALLBACK_DOM_GEOMETRY.getStream());
        } catch (Throwable th) {
            logger.warn("could not load fallback default-dom-geometry.xml: ", th);
        }

        // failsafe
        logger.warn("Using failsafe DOM registry. DOM geometry will not be available.");
        return new IDOMRegistry()
        {
            @Override
            public Iterable<DOMInfo> allDOMs() throws DOMRegistryException
            {
                return new ArrayList<>(0);
            }

            @Override
            public double distanceBetweenDOMs(DOMInfo dom0, DOMInfo dom1)
            {
                return -1;
            }

            @Override
            public double distanceBetweenDOMs(short chan0, short chan1)
            {
                return -1;
            }

            @Override
            public short getChannelId(long mbid)
            {
                return -1;
            }

            @Override
            public DOMInfo getDom(long mbId)
            {
                return null;
            }

            @Override
            public DOMInfo getDom(int major, int minor)
            {
                return null;
            }

            @Override
            public DOMInfo getDom(short channelId)
            {
                return null;
            }

            @Override
            public Set<DOMInfo> getDomsOnHub(int hubId) throws DOMRegistryException
            {
                return new HashSet<>(0);
            }

            @Override
            public Set<DOMInfo> getDomsOnString(int string) throws DOMRegistryException
            {
                return new HashSet<>(0);
            }

            @Override
            public String getName(long mbid)
            {
                return "n/a";
            }

            @Override
            public String getProductionId(long mbid)
            {
                return "000000";
            }

            @Override
            public int getStringMajor(long mbid)
            {
                return -1;
            }

            @Override
            public int getStringMinor(long mbid)
            {
                return -1;
            }

            @Override
            public int size() throws DOMRegistryException
            {
                return 0;
            }
        };
    }


    public static void main(String[] args) throws DOMRegistryException
    {
        IDOMRegistry instance = DomResolver.instance();

        for (DOMInfo dom : instance.allDOMs())
        {
            System.out.printf("-->%s%n", dom.getMainboardId());
        }
    }
}
