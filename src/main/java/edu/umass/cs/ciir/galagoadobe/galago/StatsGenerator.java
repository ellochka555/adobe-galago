package edu.umass.cs.ciir.galagoadobe.galago;


import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.parse.TagTokenizer;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.iterator.DataIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;
import org.lemurproject.galago.tupleflow.FakeParameters;
import org.lemurproject.galago.tupleflow.Parameters;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

public class StatsGenerator {

    public static Map<String, Boolean> stopWords;
    public static Map<String, Boolean> sentiWords;
    public static Document.DocumentComponents docCompCnfg;

    static {
        try{
            docCompCnfg = new Document.DocumentComponents(true,true,false);
            stopWords = new HashMap<String, Boolean>();
            sentiWords = new HashMap<String, Boolean>();

            BufferedReader reader = new BufferedReader(new InputStreamReader(docCompCnfg.getClass().getResourceAsStream("/sentiwordlist.txt")));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                sentiWords.put(line, true);
            }
            reader = new BufferedReader(new InputStreamReader(docCompCnfg.getClass().getResourceAsStream("/inquery")));
            line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                stopWords.put(line, true);
            }
        }catch(Exception e){
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public static List<String> getNames(Parameters p) throws Exception {
        Retrieval r = RetrievalFactory.instance(p);

        Map<String, Boolean> names = new HashMap<String,Boolean>();
        ScoringContext sc = new ScoringContext();

        String docname = null;
        DiskIndex index = new DiskIndex(p.getString("index"));
        DataIterator<String> namesItr = index.getNamesIterator();

        while (!namesItr.isDone()){
            long docId = namesItr.currentCandidate();
            sc.document = docId;
            docname = namesItr.data(sc);
            if(names.get(docname) == null){
                names.put(docname, true);
            }
            namesItr.movePast(docId);
        }

        return new ArrayList(names.keySet());
    }

    public static Map<String, StatTracker> getStatsPerThread(Parameters p, List<String> names, boolean allowAll) throws Exception{
        Retrieval r = RetrievalFactory.instance(p);

        if(names == null){
            names = getNames(p);
        }

        // get ready to get tags
        Parameters tokenParams = new Parameters();
        List<String> fieldItems = new ArrayList<String>();
        fieldItems.add("s");
        fieldItems.add("subject");
        fieldItems.add("post");
        fieldItems.add("quote");
        tokenParams.set("fields", fieldItems);
        FakeParameters tokenFakeParams = new FakeParameters(tokenParams);
        TagTokenizer tagTokenizer = new TagTokenizer(tokenFakeParams);

        Document curd = null; //the current document
        int wordlen = 0; //keeps track of # of characters in a sentence
        int wordcount = 0; //keeps track of # of words (defined by a space)
        int sencount = 0; //keeps track of # of sentence tags
        int senlen = 0; //keeps track of # of words by the begin and end index (for sanity, assert mini-test)
        int zeros = 0; //keeps track of # of sentences with 0 length (by senlen)
        int sentNumWords = 0;
        String[] docarr = null; // holder for splits to find subforums/threads
        String thread = null; // holder for thread name (vs message name)
        StatTracker stats = null; // holder for stats
        Map<String, StatTracker> threadToStats = new HashMap<String, StatTracker>(); // Thread String to stats Map
        StringBuilder sb = new StringBuilder(); // Builds a sentence
        NGrams ngram = null; // NGram-ifys sentences.


        for(String post : names){
            curd = r.getDocument(post, docCompCnfg);
            tagTokenizer.process(curd);

            ngram = new NGrams(sentiWords, allowAll);
            for(Tag t : curd.tags){
                if(t.name.equals("s")){

                    for(int i = t.begin; i < t.end; i++){
                        wordlen = wordlen+curd.terms.get(i).length();
                        wordcount++;
                        if(stopWords.get(curd.terms.get(i)) != null){
                            continue;
                        }
                        if(sentiWords.get(curd.terms.get(i)) != null){

                            sentNumWords++;

                            //System.out.print("TERM: "+curd.terms.get(i)+"\t");
                        }
                        sb.append( curd.terms.get(i) );
                        sb.append(" ");
                    }
                    if(sb.length()-1 <= 0){
                        ngram.addSentence(null);
                    }else {
                        sb.setLength(sb.length() - 1);
                        ngram.addSentence(sb.toString());
                    }
                    sb = new StringBuilder();
                    senlen = senlen+(t.end-t.begin);
                    if (t.end-t.begin == 0){
                        zeros++;
                    }
                    sencount++;
                }
            }

            //sanity check
            assert(wordcount == senlen);

            //get thread ID
            docarr = post.split("/");
            docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0]
                    +"-"+docarr[docarr.length-1].split("-")[1];
            thread = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");

            //get previous stats
            stats = threadToStats.get(thread);
            if (stats == null){
                threadToStats.put(thread, new StatTracker(ngram, 1, 1, sencount, senlen, wordlen, zeros, sentNumWords));
            } else {
                threadToStats.put(thread, stats.combine(new StatTracker(ngram, 0, 1, sencount, senlen, wordlen, zeros, sentNumWords)));
            }
            wordlen = 0;
            wordcount = 0;
            sencount = 0;
            senlen = 0;
            zeros = 0;
            sentNumWords = 0;
        }
        return threadToStats;
    }

}
