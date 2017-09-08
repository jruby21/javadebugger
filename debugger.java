// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger
// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" -agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y foo
// /home/jruby/tools/jdk1.8.0_131/bin/javac -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger.java

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.IncompatibleThreadStateException;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

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
                System.out.println("b1 ");
        debugger d = new debugger();
                System.out.println("b2 ");
        d.go(System.in);
                System.out.println("b3 ");
    }
        
    void go(InputStream input) throws Exception
    {
                System.out.println("b4 ");
        BufferedReader in = new BufferedReader(new InputStreamReader(input));

        for (String s = in.readLine();
             !s.equalsIgnoreCase("exit");
             s = in.readLine())

            {
                System.out.println("a " + s);
            expr(new parser(s));
            }
    }

    private String hostname = null;
    private String port            = null;
    private String threadid    = null;
    private String frameid      = null;
    private thread tr                 = null;
    
    void expr(parser parse)
    {
        parser.TOKEN tok0;
        parser.TOKEN tok1;
        System.out.println("b ");
        while (parse.hasNext())
            
            {
                        System.out.println("c ");
                switch (parse.next())
                    
                    {
                    case DONE:
                        return;

                    case ATTACH:

                        tok0           = parse.next();
                        hostname = parse.getString();
                        tok1           = parse.next();
                        port            = parse.getString();

                        if (tok0 == parser.TOKEN.STRING
                            && tok1 == parser.TOKEN.STRING)

                            attach(hostname, port);

                        else

                            {
                                System.out.println("error - attach hostname port");
                                parse.clear();
                            }
                        
                        break;

                    case CONTINUE:
                        vm.resume();
                        break;

                    case NEXT:

                        if (parse.next() == parser.TOKEN.STRING)

                            step(parse.getString(),
                                 StepRequest.STEP_LINE,
                                 StepRequest.STEP_OVER);
                                    
                        else

                            {
                                System.out.println("error - step thread-id");
                                parse.clear();
                            }

                        break;
                        
                    case QUIT:
                        System.exit(0);
                        
                    case RUN:

                        if (parse.next() == parser.TOKEN.STRING)

                            {
                                ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
                                r.addClassFilter(parse.getString());
                                r.enable();
                                vm.resume();
                            }

                        else

                            {
                                System.out.println("error - run main-class");
                                parse.clear();
                            }

                        break;

                    case THREAD:

                        tok0 = parse.next();
                        
                        if (tok0 == parser.TOKEN.ALL)

                            {
                                System.out.print("threads ");
        
                                for (ThreadReference thr: vm.allThreads())

                                    System.out.print((new thread(thr)).toString() + " ");
                            }

                        else if (tok0 == parser.TOKEN.STRING)

                            {
                                tr = getThread(parse.getString());

                                if (tr != null)

                                    System.out.println(tr.toString());
                            }

                        else

                            {
                                System.out.println("error - thread (all | thread-id");
                                parse.clear();
                            }

                        break;


                    case FRAME:

                        tok0           = parse.next();
                        tr                = getThread(parse.getString());
                        tok1           = parse.next();
                        frameid     = parse.getString();
                        
                        if (tok0 != parser.TOKEN.STRING
                            ||  tok1 != parser.TOKEN.STRING)
                            
                            {
                                System.out.println("error - frame thread-id frame-id");
                                parse.clear();
                            }

                        else if (tr == null)
                            
                            {
                                System.out.println("error - no such thread");
                                parse.clear();
                            }

                        else

                            {
                                try {
                                    System.out.println(tr.frame(Integer.parseInt(frameid)));
                                } catch (NumberFormatException e) {
                                    System.out.println("error - frame id must be an integer");
                                    parse.clear();
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

    private void step(String threadID, int size, int depth)
        {
            thread tr = getThread(threadID);

            if (tr != null)

                {
                    List<StepRequest> srl = vm.eventRequestManager().stepRequests();
                    StepRequest sr = null;
                                        
                    for (StepRequest s : srl)

                        {
                            if (s.thread()   == tr.getThread()
                                && s.size()  == size
                                && s.depth() == depth)
                                
                                sr = s;
                        }

                    if (sr == null)

                        {
                            sr = vm.eventRequestManager().createStepRequest(tr.getThread(),
                                                                            size,
                                                                            depth);

                            sr.addClassExclusionFilter("java.*");
                            sr.addClassExclusionFilter("sun.*");
                            sr.addClassExclusionFilter("com.sun.*");
                        }

                    if (sr != null)

                        {
                            sr.addCountFilter(1);
                            sr.enable();
                            vm.resume();
                        }
                }
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

