import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.IncompatibleThreadStateException;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.Map;
 
public class debugger
{
    private VirtualMachine vm = null;
    private EventReader    er = null;
    
    public static void main(String args[]) throws Exception

    {
        debugger d = new debugger();
        d.go(System.in);
    }
        
    void go(InputStream input) throws Exception
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));

        for (String s = in.readLine();
             !s.equalsIgnoreCase("exit");
             s = in.readLine())

            expr(new parser(s));
    }

    private String hostname = null;
    private String port     = null;
    private thread tr       = null;
    
    void expr(parser parse)
    {
        parser.TOKEN tok;
        
        while (parse.hasNext())

            {
                switch (parse.next())

                    {
                    case START:
                        expr(parse);
                        break;

                    case STOP:
                    case DONE:
                        return;

                    case ATTACH:

                        if (parse.next() == parser.TOKEN.STRING)

                            {
                                hostname = parse.getString();

                                if (parse.next() == parser.TOKEN.STRING)

                                    {
                                        port = parse.getString();
                                        attach(hostname, port);
                                    }
                            }
                        
                        break;

                    case THREAD:

                        tok = parse.next();
                        
                        if (tok == parser.TOKEN.ALL)

                            {
                                System.out.print("(threads ");
        
                                for (ThreadReference thr: vm.allThreads())

                                    System.out.print((new thread(thr)).toString() + " ");

                                System.out.println(")");
                            }

                        else if (tok == parser.TOKEN.STRING)

                            {
                                tr = getThread(parse.getString());

                                if (tr != null)

                                    System.out.println(tr.toString());
                            }

                        break;


                    case FRAME:

                        if (parse.next() == parser.TOKEN.STRING)

                            {
                                tr = getThread(parse.getString());

                                if (tr != null && parse.next() == parser.TOKEN.STRING)

                                    {
                                        try {
                                            System.out.println(tr.frame(Integer.parseInt(parse.getString())));
                                        } catch (NumberFormatException e) {}
                                    }
                            }

                        break;                        
                    }
            }
    }

    private void attach(String host, String port)
    {
        try
            {
                List<AttachingConnector> l = Bootstrap.virtualMachineManager().attachingConnectors();

                AttachingConnector ac = null;
                        
                for (AttachingConnector c: l) {
                    
                    if (c.name().equals("com.sun.jdi.SocketAttach")) {
                        ac = c;
                        break;
                    }
                }
        
                if (ac == null)
                            
                    throw new RuntimeException("Unable to locate ProcessAttachingConnector");

                Map<String,Connector.Argument> env = ac.defaultArguments();
                
                env.get("hostname").setValue(host);
                env.get("port").setValue(port);
                
                vm = ac.attach(env);
                new EventReader(vm).start();
            } catch (IOException | IllegalConnectorArgumentsException e) {
            System.out.println(e.toString()); }
    }

    private thread getThread(String id)
    {
        try

            {
                long threadId = Long.parseLong(id);
                
                for (ThreadReference thr: vm.allThreads())

                    {
                        if (thr.uniqueID() == threadId)

                            return new thread(thr);
                    }
            } catch (NumberFormatException e) {}

        return null;
    }
}

