package edu.umass.cs.ciir.galagoadobe.galago;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lemurproject.galago.tupleflow.Parameters;
import org.mortbay.jetty.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;


/**
 * Created by nick on 3/22/14.
 */
public class StatsHandler extends ContextHandler {

    private Parameters params;
    public StatsHandler(Parameters p){
        this.params = p;
    }
    private String buildStats(String[] _forums, String[] join, boolean allowAll, boolean buildNGrams){

        Map<String, StatTracker> aggr = aggroStatsBuilder(_forums, join, NamesHandler.getList(params), allowAll);

        try{
            JSONObject json = new JSONObject();
            for(String s : aggr.keySet()){
                if(buildNGrams){
                    json.put(s, aggr.get(s).ngrams.toJSON());
                }else{
                    json.put(s, aggr.get(s).toJSON());
                }
            }
            return json.toString();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, StatTracker> aggroStatsBuilder(String[] _forums, String[] join, List<String> realForums, boolean allowAll){
        List<String> newnames = new ArrayList<String>();
        List<String> names = null;
        try{
            names = StatsGenerator.getNames(params);
        }catch(Exception e){
            return null;
        }
        List<String> forums = new ArrayList<String>();
        List<String> jsonArr = new ArrayList<String>();
        for(String s : _forums){
            forums.add(realForums.get(Integer.parseInt(s)));
        }
        for(String s : join){
            try{
                jsonArr.add(realForums.get(Integer.parseInt(s)));
            }catch(Exception e){
                jsonArr.add(s);
            }
        }
        String[] docarr = null;
        String forum = null;
        for(String s1 : jsonArr){
            for(String s2 : names){
                docarr = s2.split("/");
                docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0];
                forum = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");
                if(s2.startsWith(s1) && !newnames.contains(s2) && forums.contains(forum)){
                    newnames.add(s2);
                }
            }
        }

        Map<String, StatTracker> threadToStats = null;
        try{
            threadToStats = StatsGenerator.getStatsPerThread(params, newnames, allowAll);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        Map<String, StatTracker> aggr = new HashMap<String, StatTracker>();
        for(String s : jsonArr){
            aggr.put(s, new StatTracker(new NGrams(StatsGenerator.sentiWords, allowAll), 0, 0, 0, 0, 0, 0, 0));
        }

        StatTracker stats = null;
        for(String s : threadToStats.keySet()){
            for(String s2 : jsonArr){
                docarr = s.split("/");
                docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0];
                forum = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");
                if(forum.equals(s2)){
                    stats = aggr.get(s2);
                    aggr.put(s2, stats.combine(threadToStats.get(s)));
                    break;
                }else if(s.startsWith(s2) && !jsonArr.contains(forum)){
                    stats = aggr.get(s2);
                    aggr.put(s2, stats.combine(threadToStats.get(s)));
                    break;
                }
            }
        }
        return aggr;
    }

    @Override
    public void handle(String target,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       int dispatch) throws IOException, ServletException {

        JSONObject jsonObj = new JSONObject(request.getParameterMap());
        try{
            String[] join = null;
            String[] _forums = null;
            Boolean filter;
            try{
                filter = new Boolean(((String[])jsonObj.get("b"))[0]);
            }catch(Exception e){
                filter = new Boolean(false);
            }
            try{
                _forums =(String[])jsonObj.get("f[]");
                join = (String[])jsonObj.get("j[]");
            }catch(Exception e){}
            response.setHeader("Content-Type", "application/json; charset=utf-8");
            ServletOutputStream outputStream = response.getOutputStream();
            if(join != null && _forums != null){
                IOUtils.write(buildStats(_forums, join, !filter, false), outputStream);
                outputStream.close();
                response.flushBuffer();
                return;
            }
            join = null;
            try{
                join = (String[])jsonObj.get("ngrams[]");
            }catch(Exception e){}
            if(join != null && _forums != null){
                IOUtils.write(buildStats(_forums, join, !filter, true), outputStream);
                outputStream.close();
                response.flushBuffer();
                return;
            }




        }catch(Exception e){
            e.printStackTrace();
            throw new IOException();
        }

        ServletOutputStream outputStream = response.getOutputStream();
        IOUtils.write("error", outputStream);
        outputStream.close();
        response.flushBuffer();

        return;
    }

}
