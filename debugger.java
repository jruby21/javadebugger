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
        debugger d = new debugger();
        d.go(System.in);
    }
        
    void go(InputStream input) throws Exception
    {
        System.out.println("Debugger started");
        BufferedReader in = new BufferedReader(new InputStreamReader(input));

        for (String s = in.readLine();
             !s.equalsIgnoreCase("exit");
             s = in.readLine())

            expr(new parser(s));
    }

    private String              hostname   = null;
    private String              port       = null;
    private String              threadid   = null;
    private String              frameid    = null;
    private thread              tr         = null;

    void expr(parser parse)
    {
        parser.TOKEN tok0;
        parser.TOKEN tok1;

        while (parse.hasNext())
            
            {
                switch (parse.next())
                    
                    {
                    case DONE:
                        return;

                    case ATTACH:

                        tok0     = parse.next();
                        hostname = parse.getString();
                        tok1     = parse.next();
                        port     = parse.getString();

                        if (tok0 != parser.TOKEN.STRING
                            || hostname == null || hostname.length() == 0
                            || tok1 != parser.TOKEN.STRING
                            || port == null || port.length() == 0)

                            System.out.println("error,attach hostname port");
                            
                        else

                            attach(hostname, port);
                        
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

                            System.out.println("error,missing thread-id");

                        break;

                    case INTO:

                        if (parse.next() == parser.TOKEN.STRING)

                            step(parse.getString(),
                                 StepRequest.STEP_LINE,
                                 StepRequest.STEP_INTO);
                                    
                        else

                            System.out.println("error,missing thread-id");

                        break;
                        
                    case BACK:

                        if (parse.next() == parser.TOKEN.STRING)

                            step(parse.getString(),
                                 StepRequest.STEP_LINE,
                                 StepRequest.STEP_OUT);
                                    
                        else

                            System.out.println("error,missing thread-id");

                        break;


                    case BREAK:

                        try
                            
                            {
                                parser.TOKEN tk0 = parse.next();
                                String       cl  = parse.getString();
                                parser.TOKEN tk1 = parse.next();
                                String       ln  = parse.getString();
                        
                                if (tk0 != parser.TOKEN.STRING
                                    || tk1 != parser.TOKEN.STRING
                                    || cl == null || cl.length() == 0
                                    || ln == null || ln.length() == 0)

                                    {
                                        System.out.println("error,break class-name <line-number|method name>");
                                        break;
                                    }
                        
                                List<ReferenceType> classes = vm.classesByName(cl);

                                if (classes == null || classes.isEmpty())

                                    {
                                        System.out.println("error,no such class");
                                        break;
                                    }

                                // line number or method name

                                List<Location> locs = null;
                                
                                if (ln.matches("^\\d+$"))

                                    locs = classes.get(0).locationsOfLine(Integer.parseInt(ln));

                                else

                                    {
                                        List<Method> m = classes.get(0).methodsByName(ln);

                                        if (m == null || m.isEmpty())

                                            {
                                                System.out.println("error,no such method");
                                                break;
                                            }

                                        locs = m.get(0).allLineLocations();
                                    }

                                if (locs == null || locs.isEmpty())

                                    {
                                        System.out.println("error,no such line");
                                        break;
                                    }

                                Location                       bl  = locs.get(0);
                                BreakpointRequest         br  = null;
                                List<BreakpointRequest> brs = vm.eventRequestManager().breakpointRequests();

                                for (BreakpointRequest b : brs)

                                    {
                                        if (bl.equals(b.location()))

                                            br = b;
                                    }

                                if (br == null)
                                    
                                    br = vm.eventRequestManager().createBreakpointRequest(bl);

                                br.enable();

                            } catch (NumberFormatException e) {
                            System.out.println("error,line nunmber must be an integer.");
                        } catch (AbsentInformationException a) {
                            System.out.println("error,no such line number.");
                        }

                        break;
                        
                    case QUIT:
                        System.exit(0);


                    case PREPARE:
                        
                        if (vm == null)

                            System.out.println("error,no virtual machine");

                        else if (parse.next() != parser.TOKEN.STRING)

                            System.out.println("error,prepare main-class");

                        else

                            {
                                ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
                                r.addClassFilter(parse.getString());
                                r.enable();
                            }

                        break;
                        

                    case RUN:

                        if (vm == null)

                            System.out.println("error,no virtual machine");

                        else

                            vm.resume();

                        break;

                        
                    case THREAD:

                        tok0 = parse.next();
                        
                        if (tok0 == parser.TOKEN.ALL)

                            {
                                for (ThreadReference thr: vm.allThreads())

                                    System.out.print((new thread(thr)).toString());

                                System.out.print("endthread\n");
                            }

                        else if (tok0 == parser.TOKEN.STRING)

                            {
                                tr = getThread(parse.getString());

                                if (tr != null)

                                    System.out.println(tr.toString() + "endthread\n");
                            }

                        else

                            System.out.println("error - thread (all | thread-id");

                        break;


                    case FRAME:

                        tok0           = parse.next();
                        tr                = getThread(parse.getString());
                        tok1           = parse.next();
                        frameid     = parse.getString();
                        
                        if (tok0 != parser.TOKEN.STRING
                            ||  tok1 != parser.TOKEN.STRING)
                            
                            System.out.println("error,frame thread-id frame-id");

                        else if (tr == null)
                            
                            System.out.println("error,no such thread");

                        else

                            {
                                try {
                                    System.out.println(tr.frame(Integer.parseInt(frameid)));
                                } catch (NumberFormatException e) {
                                    System.out.println("error,frame id must be an integer");
                                }
                            }

                        break;

                    default:
                        System.out.println("error,unknown command");
                        break;
                    }
            }
    }

    private void attach(String host, String port)
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
                    
            System.out.println("attach error - unable to locate ProcessAttachingConnector");

        else

            {
                try {
                    Map<String,Connector.Argument> env = ac.defaultArguments();
                
                    env.get("hostname").setValue(host);
                    env.get("port").setValue(port);
                
                    vm = ac.attach(env);
                    new EventReader(vm).start();
                } catch (IOException | IllegalConnectorArgumentsException e) {
                    System.out.println("attach error - " + e.toString());
                    return;
                }

                if (vm == null)

                    System.out.println("attach error - failed to create virtual machine");

                else

                    System.out.println("attach created virtual machine");
            }
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

