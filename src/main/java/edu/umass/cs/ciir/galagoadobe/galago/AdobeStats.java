package edu.umass.cs.ciir.galagoadobe.galago;

import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Document.DocumentComponents;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.iterator.DataIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FakeParameters;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.core.index.IndexPartReader;
import org.lemurproject.galago.core.index.KeyIterator;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.retrieval.iterator.CountIterator;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.parse.TagTokenizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.PrintStream;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

public class AdobeStats extends AppFunction{

    @Override
    public String getName() {
        return "adobe-stats";
    }

    @Override
    public String getHelpString() {
        return "galago adobe-stats --index=/path/to/index/\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception{

        if(!p.containsKey("index")) {
            output.print(getHelpString());
            return;
        }

        List<String> names = StatsGenerator.getNames(p);


        Map<String, StatTracker> threadToStats = StatsGenerator.getStatsPerThread(p, null, true);

/*
        Map<String, StatTracker> forumToStats = new HashMap<String, StatTracker>();

        for(String thrd : threadToStats.keySet()){
            docarr = thrd.split("/");
            docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0];
            thread = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");
            stats = forumToStats.get(thread);
            if (stats == null){
                forumToStats.put(thread, threadToStats.get(thrd));
            }else{
                forumToStats.put(thread, stats.combine(threadToStats.get(thrd)));
            }

        }

        for (String thrd : threadToStats.keySet()){
            output.print(thrd+":\n");
            output.print(threadToStats.get(thrd).toString());
        }


        for (String thrd : forumToStats.keySet()){
            output.print(thrd+":\n");
            output.print(forumToStats.get(thrd).toString());
        }
*/
        StatTracker stats = new StatTracker(new NGrams(), 0, 0, 0, 0, 0, 0);
        for(String thrd : threadToStats.keySet()){
            stats = stats.combine(threadToStats.get(thrd));
        }
        output.print("\nTOTAL:\n"+stats.toString()+"\n");

    }
}
