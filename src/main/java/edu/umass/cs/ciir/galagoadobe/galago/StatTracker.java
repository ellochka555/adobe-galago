package edu.umass.cs.ciir.galagoadobe.galago;


import org.json.JSONException;
import org.json.JSONObject;

public class StatTracker{
    public int wordlen;
    public int words;
    public int sentence;
    public int posts;
    public int threads;
    public int zeros;
    public NGrams ngrams;
    public int sent;

    public StatTracker(NGrams t, int threads, int posts, int sentence, int words, int wordlen, int zeros, int sentiment){
        this.wordlen = wordlen;
        this.words = words;
        this.sentence = sentence;
        this.posts = posts;
        this.threads = threads;
        this.zeros = zeros;
        this.ngrams = t;
        this.sent = sentiment;
    }

    public double getAvgSenPerPost(){
        return ((double)this.sentence)/((double)this.posts);
    }
    public double getAvgWordsPerSen(){
        return ((double)this.words)/((double)this.sentence);
    }
    public double getAvgLenPerWord(){
        return ((double)this.wordlen)/((double)this.words);
    }
    public double getAvgPostsPerThread(){
        return ((double)this.posts)/((double)this.threads);
    }
    public int getNumZeros(){
        return this.zeros;
    }
    public void addSentence(String sen){
        this.ngrams.addSentence(sen);
    }
    public JSONObject toJSON() throws JSONException{
        JSONObject json = new JSONObject();
        json.put("posts", this.posts);
        json.put("threads", this.threads);
        json.put("words", this.words);
        json.put("sen", this.sentence);
        json.put("len", this.wordlen);
        json.put("zeros", this.zeros);
        json.put("sent", this.sent);
        json.put("uni", NGrams.sortByValue(this.ngrams.uni, 10));
        json.put("bi", NGrams.sortByValue(this.ngrams.bi, 10));
        json.put("tri", NGrams.sortByValue(this.ngrams.tri, 10));
        return json;
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\tPost per Thread\t");
        sb.append(this.getAvgPostsPerThread()+"\t"+this.posts+"\t"+this.threads);
        sb.append('\n');
        sb.append("\tSen per Post\t");
        sb.append(this.getAvgSenPerPost()+"\t"+this.sentence+"\t"+this.posts);
        sb.append('\n');
        sb.append("\tWord per Sen\t");
        sb.append(this.getAvgWordsPerSen()+"\t"+this.words+"\t"+this.sentence);
        sb.append('\n');
        sb.append("\tLen per Word\t");
        sb.append(this.getAvgLenPerWord()+"\t"+this.wordlen+"\t"+this.words);
        sb.append('\n');
        sb.append("\t# of 0 len Sen\t");
        sb.append(this.getNumZeros());
        sb.append('\n');
        sb.append("\tNGRAMS\n");
        sb.append(ngrams.toString());
        return sb.toString();
    }
    public StatTracker combine(StatTracker other){
        this.ngrams = this.ngrams.combine(other.ngrams);
        this.threads += other.threads;
        this.posts += other.posts;
        this.sentence += other.sentence;
        this.words += other.words;
        this.wordlen += other.wordlen;
        this.zeros += other.zeros;
        this.sent += other.sent;
        return this;
    }
}
