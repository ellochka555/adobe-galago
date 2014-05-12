package edu.umass.cs.ciir.galagoadobe.galago;


import org.lemurproject.galago.core.tools.*;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

import java.net.InetAddress;
import java.io.IOException;
import java.io.PrintStream;


public class AdobeStatServer extends AppFunction
{
    @Override
    public String getName() {
        return "adobe-stat-server";
    }

    @Override
    public String getHelpString() {
        return "galago adobe-stat-server --index=/path/to/index/ --port=8080[optional]\n";
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception{

        if(!p.containsKey("index")){
            output.println(getHelpString());
            return;
        }

        int port = (int) p.get("port", 0);
        if(port == 0){
            port = Utility.getFreePort();
        }else{
            if(!Utility.isFreePort(port)){
                throw new IOException("Tried to bind port "+port+" which is in use.");
            }
        }

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });

        resource_handler.setResourceBase("./src/main/webapp/");
        Search search = new Search(p);

        Server server = new Server(port);
        URLMappingHandler mh = new URLMappingHandler();

        mh.setHandler("/stream", new StreamContextHandler(search));
        mh.setHandler("/xml", new XMLContextHandler(search));
        mh.setHandler("/json", new JSONContextHandler(search));
        mh.setDefault(new SearchWebHandler(search));

        ContextHandler staticctx = new ContextHandler("/adobe");
        staticctx.setHandler(resource_handler);

        ContextHandler statsctx = new ContextHandler("/adobe/statserver");
        statsctx.setHandler(new StatsHandler(p));

        ContextHandler namesctx = new ContextHandler("/adobe/names");
        namesctx.setHandler(new NamesHandler(p));

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { staticctx, statsctx, namesctx, mh });
        server.addHandler(handlers);
        server.start();
        output.println("Server: http://localhost:"+port+"/adobe");

        InetAddress address = InetAddress.getLocalHost();
        String masterURL = String.format("http://%s:%d%s", address.getHostAddress(), port, "/adobe");
        output.println("ServerIP: " + masterURL);

    }

}
