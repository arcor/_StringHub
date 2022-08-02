package icecube.daq.cli.util;

import icecube.daq.cli.data.CLIData;
import icecube.daq.trigger.config.DomSet;
import icecube.daq.trigger.config.DomSetFactory;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.IDOMRegistry;
import icecube.daq.util.LocatePDAQ;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class DomSetResolver
{
    static Logger logger = Logger.getLogger(DomSetResolver.class);


    private final List<DomSet> domsets;
    private final IDOMRegistry registry;

    private final Map<String, DomSet> nameLookup;

    public static String sourceLocation;

    private static DomSetResolver instance;

    public static DomSetResolver instance()
    {
        synchronized (DomSetResolver.class)
        {
            if(instance == null)
            {
                instance = load();
            }
        }

        return instance;

    }

    private DomSetResolver(List<DomSet> domsets , IDOMRegistry registry)
    {
        this.domsets = domsets;
        this.registry = registry;

        nameLookup = domsets.stream().collect(Collectors.toMap(ds -> ds.getName(), ds -> ds));
    }

    private static DomSetResolver load()
    {
        IDOMRegistry registry = DomResolver.instance();
        DomSetFactory.setDomRegistry(registry);

        try {

            File configDirectory = LocatePDAQ.findConfigDirectory();
            sourceLocation = configDirectory.getAbsolutePath().concat("/trigger/domset-definitions.xml");
            List<DomSet> domsets = DomSetFactory.parseDomSet(new FileInputStream(sourceLocation));

            return  new DomSetResolver(domsets, registry);
        } catch (Throwable th) {
            logger.warn("could not load domset-definitions.xml from environment: " + th.getMessage());
        }

        // load from resources
        logger.warn("Loading internal fallback-domset-definitions.xml ...could be stale");
        try {
            sourceLocation = CLIData.FALLBACK_DOM_SETS.getLocation();
            logger.warn("loading dom sets from "  + sourceLocation);
            List<DomSet> domsets = DomSetFactory.parseDomSet(CLIData.FALLBACK_DOM_SETS.getStream());
            return  new DomSetResolver(domsets, registry);
        } catch (Throwable th) {
            logger.warn("could not load fallback dom set", th);
        }

        return new DomSetResolver(new ArrayList<DomSet>(0), registry);

    }

    public List<String> list()
    {
        return domsets.stream().map(ds -> ds.getName()).collect(Collectors.toList());
    }


    public List<DOMInfo> resolve(String name)
    {

        DomSet domSet = nameLookup.get(name);

        if(domSet == null)
        {
            logger.warn(String.format("No Domset named [%s]", name));
            return new ArrayList<>();
        }

        List<Long> domIDs = domSet.getDomIDs();
        List<DOMInfo> acc = new ArrayList<>(domIDs.size());


        for (Long mbid : domIDs)
        {
            acc.add(registry.getDom(mbid));
        }

        return acc;
    }

}
