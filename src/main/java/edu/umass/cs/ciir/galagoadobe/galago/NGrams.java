package edu.umass.cs.ciir.galagoadobe.galago;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


public class NGrams {

    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortByValue( Map<K, V> map, int n )
    {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (-1)*(o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<K, V>();
        int i = 1;
        for (Map.Entry<K, V> entry : list)
        {
            if(n != -1 && i++ > n) break;
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public Map<String, Integer> uni;
    public Map<String, Integer> bi;
    public Map<String, Integer> tri;
    public Map<String, Boolean> sent;
    public boolean all;

    public NGrams(String sen, Map<String, Boolean> sentiWords, boolean allowAll) {
        uni = new HashMap<String, Integer>();
        bi = new HashMap<String, Integer>();
        tri = new HashMap<String, Integer>();
        this.sent = sentiWords;
        this.all = allowAll;
        this.addSentence(sen);
    }
    public NGrams(Map<String, Boolean> sentiWords, boolean allowAll){
        this(null, sentiWords, allowAll);
    }
    public NGrams(Map<String,Integer> uni, Map<String,Integer> bi, Map<String,Integer> tri
            , Map<String, Boolean> sentiWords, boolean allowAll){
        this.uni = uni;
        this.bi = bi;
        this.tri = tri;
        this.all = allowAll;
        this.sent = sentiWords;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uni", NGrams.sortByValue(this.uni, 1000));
        json.put("bi", NGrams.sortByValue(this.bi, 1000));
        json.put("tri", NGrams.sortByValue(this.tri, 1000));
        return json;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\t\tUNI\n");
        int i = 0;
        for(String key : sortByValue(this.uni, 5).keySet()){
            sb.append("\t\t");
            sb.append(i++);
            sb.append('\t');
            sb.append(key);
            sb.append('\t');
            sb.append(this.uni.get(key));
            sb.append('\n');
        }
        sb.append("\t\tBI\n");
        i = 0;
        for(String key : sortByValue(this.bi, 5).keySet()){
            sb.append("\t\t");
            sb.append(i++);
            sb.append('\t');
            sb.append(key);
            sb.append('\t');
            sb.append(this.bi.get(key));
            sb.append('\n');
        }
        sb.append("\t\tTRI\n");
        i = 0;
        for(String key : sortByValue(this.tri, 5).keySet()){
            sb.append("\t\t");
            sb.append(i++);
            sb.append('\t');
            sb.append(key);
            sb.append('\t');
            sb.append(this.tri.get(key));
            sb.append('\n');
        }
        return sb.toString();
    }

    public void addSentence(String sen){
        if(sen == null) return;
        String[] arr = sen.split(" ");
        String[] cur = new String[3];
        Integer number = null;
        boolean skip = false;

        for(int i = 0; i < arr.length; i++){
            cur[2] = cur[1];
            cur[1] = cur[0];
            cur[0] = arr[i];
            skip = false;
            for(String s : cur){
                if(s != null && (s.equals("www") || s.equals("com") || s.startsWith("http")) ){
                    skip = true;
                }
            }
            if(skip) continue;
            if(cur[2] != null && cur[1] != null && cur[0] != null
                    && (this.all || sent.get(cur[2]) != null || sent.get(cur[1]) != null || sent.get(cur[0]) != null) ){
                number = tri.get(cur[2]+" "+cur[1]+" "+cur[0]);
                if(number == null){
                    tri.put(cur[2]+" "+cur[1]+" "+cur[0], 1);
                }else{
                    tri.put(cur[2]+" "+cur[1]+" "+cur[0], number+1);
                }
            }
            if(cur[1] != null && cur[0] != null
                    && (this.all || sent.get(cur[1]) != null || sent.get(cur[0]) != null)){
                number = bi.get(cur[1]+" "+cur[0]);
                if(number == null){
                    bi.put(cur[1]+" "+cur[0], 1);
                }else{
                    bi.put(cur[1]+" "+cur[0], number+1);
                }
            }
            number = uni.get(cur[0]);
            if(number == null && (this.all || sent.get(cur[0]) != null)){
                uni.put(cur[0], 1);
            }else if(this.all || sent.get(cur[0]) != null){
                uni.put(cur[0], number+1);
            }
        }
    }

    public NGrams combine(NGrams other){
        Integer number = null;
        for(String key : other.uni.keySet()){
            number = this.uni.get(key);
            if(number != null){
                this.uni.put(key, number+other.uni.get(key));
            }else{
                this.uni.put(key, other.uni.get(key));
            }
        }
        for(String key : other.bi.keySet()){
            number = this.bi.get(key);
            if(number != null){
                this.bi.put(key, number+other.bi.get(key));
            }else{
                this.bi.put(key, other.bi.get(key));
            }
        }
        for(String key : other.tri.keySet()){
            number = this.tri.get(key);
            if(number != null){
                this.tri.put(key, number+other.tri.get(key));
            }else{
                this.tri.put(key, other.tri.get(key));
            }
        }
        return this;
    }
}
