package edu.umass.cs.ciir.galagoadobe.heritrix;


import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.modules.ProcessResult;
import org.archive.uid.RecordIDGenerator;
import org.archive.uid.UUIDGenerator;
import org.archive.util.ArchiveUtils;
import org.archive.io.RecordingInputStream;
import org.archive.io.ReplayInputStream;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import ca.uottawa.balie.Balie;
import ca.uottawa.balie.Token;
import ca.uottawa.balie.TokenList;
import ca.uottawa.balie.Tokenizer;
import ca.uottawa.balie.LanguageIdentification;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.StringBuilder;
import org.archive.net.UURI;
import org.archive.spring.ConfigPath;
import org.archive.util.FileUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;



public class trecWriterProcessor extends Processor{


    protected RecordIDGenerator generator = new UUIDGenerator();
    public RecordIDGenerator getRecordIDGenerator() {
        return generator;
    }
    public void setRecordIDGenerator(RecordIDGenerator generator) {
        this.generator = generator;
    }
    private static final Logger logger =
            Logger.getLogger(trecWriterProcessor.class.getName());
    final public static String TREC_FILE = "adobe-trec-file.trectext";

    private static Map<String, Boolean> map = new HashMap<String, Boolean>();


    public trecWriterProcessor() {

    }

    protected ConfigPath path = new ConfigPath("trec writer top level directory", "${launchId}/trec");
    public ConfigPath getPath() {
        return this.path;
    }
    public void setPath(ConfigPath s) {
        this.path = s;
    }


    @Override
    protected void innerProcess(CrawlURI uri) throws InterruptedException {
        CrawlURI curi = (CrawlURI) uri;
        String scheme = curi.getUURI().getScheme();

        UURI uuri = curi.getUURI();

        if( !"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)){
            return;
        }
        RecordingInputStream recis = curi.getRecorder().getRecordedInput();
        if (0L == recis.getResponseContentLength()) {
            return;
        }



        File destFile = null;

        try{
            InputStream input = (InputStream) recis.getMessageBodyReplayInputStream();
            String baseDir = getPath().getFile().getAbsolutePath();

            List<String> dirpath = getFilePath(input, curi.toString());

            if (dirpath == null) {
                return;
            }
            StringBuilder pathBuilder = new StringBuilder();
            for ( String part : dirpath ){
                pathBuilder.append(part);
                pathBuilder.append(File.separator);
            }




            if(shouldWrite(curi)){
                List<String> prettyXML = htmlToXml((InputStream) recis.getMessageBodyReplayInputStream()
                        , curi.toString());
                if( prettyXML == null ){
                    return;
                }

                destFile = new File(baseDir + File.separator + pathBuilder.toString());
                destFile.mkdirs();
                destFile = new File(baseDir + File.separator + pathBuilder.toString()+ TREC_FILE);
                destFile.createNewFile();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destFile, true)));
                for (String s : prettyXML){
                    out.println(s);
                }
                out.close();
            }else{

                //DUP LOGIC HERE PROBABLY
                return;
            }
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            logger.log(Level.SEVERE, "Failed write of Records: "
                    + curi.toString(), e);
        }
    }

    protected boolean shouldWrite(CrawlURI curi){
        return true;
    }

    private List<String> getFilePath(InputStream input, String uri) throws IOException {
        Document doc = Jsoup.parse(input, null, uri);
        Elements directories = doc.select("#jive-breadcrumb span a");
        if (directories == null)
            return null;
        List<String> fileloc = new ArrayList<String>();
        for ( Element e : directories ){
            fileloc.add(e.text().replace(" ", "-"));
        }

        return fileloc;
    }

    private List<String> htmlToXml(InputStream input, String uri) throws IOException {

        Document doc = Jsoup.parse(input, null, uri);

        Element thread = doc.select("#jive-thread-messages-container").first();
        if (thread == null){
            return null;
        }
        List<String> trecdocs = new ArrayList<String>();

        Elements directories = doc.select("#jive-breadcrumb span a");
        StringBuilder sb = new StringBuilder();
        String delim = File.separator;
        for ( Element e : directories ){
            sb.append(e.text().replace(" ", "-"));
            sb.append(delim);
        }
        sb.deleteCharAt(sb.length()-1);
        String headerBegin = sb.toString();

        Element OP = thread.select(".jive-thread-post").first();
        if( OP == null ){
            return null;
        }
        String subject = OP.select(".jive-thread-post-body"
                +" .jive-thread-post-body-container"
                +" .jive-thread-post-subject"
                +" .jive-thread-post-subject-content"
                +" h2"
                +" a").first().text();

        String OPtimestamp = OP.select(".jive-thread-post-body"
                +" .jive-thread-post-body-container"
                +" .jive-thread-post-subject"
                +" .jive-thread-post-subject-content"
                +" h3").first().text();

        Elements ids = OP.select("a");
        List<Element> idli = ids.subList(0, 2);
        String OPmsgID = idli.get(0).attr("name");
        String threadID = idli.get(1).attr("name");

        String OPauthor = OP.select(".jive-thread-post-body"
                +" .jive-author-cc"
                +" .jive-author-avatar-container-cc"
                +" .jive-username-link-wrapper"
                +" a").first().text();

        Element miniParas = OP.select(".jive-thread-post-body"
                + " .jive-thread-post-body-container"
                + " .jive-thread-post-message"
                + " .jive-rendered-content").first();

        String OPcontent = paragraphsToString(miniParas);

        if(map.get(headerBegin+"-"+threadID+"-"+OPmsgID) == null){
            map.put(headerBegin+"-"+threadID+"-"+OPmsgID, true);
            trecdocs.add(makeTrecFormattedString(subject
                    , OPtimestamp
                    , OPmsgID
                    , threadID
                    , OPauthor
                    , OPcontent
                    , headerBegin));
        }

        Element correct = thread.select("#jive-inline-answer-outer-div").first();
        if( correct != null){
            String cauthor = correct.select("#jive-inline-answer-inner-avatar"
                +" .jive-author-avatar-container"
                +" .jive-username-link-wrapper"
                +" a.jiveTT-hover-user").first().text();
            String ctimestamp = correct.select("#jive-inline-answer-inner-text"
                +" #jive-inline-correct-answer-header"
                +" div.font-color-meta").first().text();
            Element cMiniParas = correct.select("#jive-inline-answer-inner-text"
                    + " #jive-inline-correct-answer-body"
                    + " .jive-rendered-content").first();
            String cmsgID = correct.select("#jive-inline-answer-inner-text"
                    +" #jive-inline-correct-answer-body"
                    +" div"
                    +" strong"
                    +" a").first().attr("href").replace("#", "");

            String ccontent = paragraphsToString(cMiniParas);

            if(map.get(headerBegin+"-"+threadID+"-"+cmsgID) == null){
                map.put(headerBegin+"-"+threadID+"-"+cmsgID, true);
                trecdocs.add(makeTrecFormattedString(subject
                        , ctimestamp
                        , cmsgID
                        , threadID
                        , cauthor
                        , ccontent
                        , headerBegin));
            }
        }

        String oauthor = null;
        String otimestamp = null;
        Element oMiniParas = null;
        String omsgID = null;
        String ocontent = null;
        String olang = null;
        Tokenizer otokenizer = null;
        TokenList otokenList;
        StringBuilder oTaggedContent = null;

        Elements otherComments = thread.select("ul.jive-discussion-replies"
                +" li.reply"
                +" div.jive-thread-message"
                +" div"
                +" div.jive-thread-reply");
        if( !otherComments.isEmpty() ){
            for ( Element comment : otherComments){
                omsgID = comment.select("a").first().attr("name");
                otimestamp = comment.select(".jive-thread-reply-body-container"
                    +" .jive-thread-reply-subject"
                    +" .jive-thread-reply-date"
                    +" span").text();
                oauthor = comment.select(".jive-thread-reply-body-container"
                        +" .jive-thread-reply-subject"
                        +" .jive-reply-thread-username"
                        +" .jive-username-link").first().attr("data-username");
                oMiniParas = comment.select(".jive-thread-reply-body-container"
                        +" .jive-thread-reply-message"
                        +" .jive-rendered-content").first();
                ocontent = paragraphsToString(oMiniParas);

                if(map.get(headerBegin+"-"+threadID+"-"+omsgID) == null){
                    map.put(headerBegin+"-"+threadID+"-"+omsgID, true);
                    trecdocs.add(makeTrecFormattedString(subject
                            , otimestamp
                            , omsgID
                            , threadID
                            , oauthor
                            , ocontent
                            , headerBegin));
                }
            }
        }

        if( trecdocs.isEmpty() ){
            return null;
        }
        return trecdocs;
    }

    private String makeTrecFormattedString(final String subject, final String timestamp
            , final String messageID, final String threadID, final String author
            , final String content, final String headerBegin)
    {
        assert subject != null;
        assert timestamp != null;
        assert messageID != null;
        assert threadID != null;
        assert author != null;
        assert content != null;
        assert headerBegin != null;
        StringBuilder sb = new StringBuilder();
        sb.append("<DOC>\n<DOCNO>");
            sb.append(headerBegin);
            sb.append("-");
            sb.append(threadID);
            sb.append("-");
            sb.append(messageID);
        sb.append("</DOCNO>\n<TEXT>\n<subject>");
            sb.append(subject);
        sb.append("</subject>\n");
            sb.append("<post author=\"");
                sb.append(author);
            sb.append("\" timestamp=\"");
                sb.append(timestamp);
            sb.append("\">\n");

            sb.append(content);
        sb.append("</post>\n</TEXT>\n</DOC>\n\n");

        return sb.toString();
    }
      
    private String paragraphsToString(Element miniParas){
        LanguageIdentification languageIdentifier = new LanguageIdentification();
        StringBuilder content = new StringBuilder();
        String holder = null;


        if (miniParas.select("br").size() == 0){
            for( Element e : miniParas.children() ){
                if( e.hasClass("jive-quote") ){
                    content.append("<quote>\n");
                    content.append(paragraphsToString(e));
                    content.append("</quote>");
                }else if ( !e.hasText() || (e.outerHtml().contains("&nbsp;") && e.text().length() == 1) ){
                    content.append('\n');
                }else{
                    if(e.outerHtml().contains("&nbsp;")){
                        holder = Jsoup.parse(e.outerHtml().replaceAll("&nbsp;", " ")).text();
                    } else {
                        holder = e.text();
                    }
                    content.append(holder);
                    content.append(" ");
                }
            }
        }else{
            content.append(miniParas.text());
        }

        holder = content.append('\n').toString();
        String lang = languageIdentifier.DetectLanguage(holder);
        Tokenizer tokenizer = new Tokenizer(lang, true);
        tokenizer.Tokenize(holder);
        TokenList tokenList = tokenizer.GetTokenList();
        StringBuilder TaggedContent = new StringBuilder();
        for( int i = 0; i < tokenList.getSentenceCount(); i++ ){
            TaggedContent.append("<s>");
            TaggedContent.append(tokenList.SentenceText(i,true,true));
            TaggedContent.append("</s>");
        }

        return TaggedContent.toString();
    }

    protected boolean shouldProcess(CrawlURI uri){
        if ( uri.getFetchStatus() <= 0 ) {
            return false;
        }

        long recordLength = uri.getContentSize();
        if ( recordLength <= 0 ){
            return false;
        }
        return true;
    }



}