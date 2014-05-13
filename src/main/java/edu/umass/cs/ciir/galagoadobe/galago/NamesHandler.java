package edu.umass.cs.ciir.galagoadobe.galago;

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
import java.util.Arrays;
import java.util.ArrayList;


public class NamesHandler extends ContextHandler {

    private Parameters params;
    public NamesHandler(Parameters p){
        this.params = p;
    }

    @Override
    public void handle(String target,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       int dispatch) throws IOException, ServletException {

        response.setHeader("Content-Type", "application/json; charset=utf-8");
        ServletOutputStream outputStream = response.getOutputStream();
        IOUtils.write(getJSON(), outputStream);
        outputStream.close();
        response.flushBuffer();

        return;
    }


    public static List<String> getList(Parameters params){
        List<String> msgs = null;
        try{
            msgs = StatsGenerator.getNames(params);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        if(msgs == null) return null;

        List<String> names = new ArrayList<String>();
        String forum = null;
        String[] docarr = null;
        for( String s : msgs ){
            docarr = s.split("/");
            docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0];
            forum = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");
            if(!names.contains(forum))
                names.add(forum);
        }
        return names;
    }

    public String getJSON() throws IOException {
        List<String> msgs = null;
        try{
            msgs = StatsGenerator.getNames(params);
        }catch(Exception e){
            throw new IOException(e);
        }
        if(msgs == null) return "{\"names\":[\"error\"]}";

        List<String> names = new ArrayList<String>();
        String forum = null;
        String[] docarr = null;
        for( String s : msgs ){
            docarr = s.split("/");
            docarr[docarr.length-1] = docarr[docarr.length-1].split("-")[0];
            forum = Arrays.toString(docarr).replace(", ", "/").replaceAll("[\\[\\]]", "");
            if(!names.contains(forum))
                names.add(forum);
        }

        try{
            JSONObject json = new JSONObject();
            json.put("names", names);
            return json.toString();
        }catch(Exception e){
            throw new IOException();
        }
    }

}
