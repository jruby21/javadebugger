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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
 
public class debugger
{
    public static final String NumberProperty = "breakpointNumber";
    
    private VirtualMachine vm       = null;
    private EventReader    er        = null;
    private int                 bpcount = 0;

    public static void main(String args[]) throws Exception

    {
        debugger d = new debugger();
        d.go(System.in);
    }
        
    void go(InputStream input) throws Exception
    {
        System.out.println("proxy,started");

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));

            for (String s = in.readLine();
                 !s.equalsIgnoreCase("exit");
                 s = in.readLine())
                
                {
                    parser p = new parser(s);
                    expr(p);
                }
        } catch (IllegalArgumentException e) {
            System.out.println("error,IllegalArgumentException," + e.toString());
        } catch (IOException e) {
            System.out.println("error,IOException," + e.toString());
        } catch (Throwable t) {
            System.out.println("error,thowable," + t.toString());
            t.printStackTrace(System.out);
        } finally {
            System.out.println("proxy,exit");
            if (vm != null) vm.exit(0);
            System.exit(0);
        }
    }
    
    private String                  hostname   = null;
    private String                  port       = null;
    private String                  threadid   = null;
    private String                 frameid    = null;
    private DebuggerThread    tr         = null;

    void expr(parser parse)
    {
        parser.TOKEN tok0;
        parser.TOKEN tok1;

        while (parse.hasNext())
            
            {
                switch (parse.next())
                    
                    {
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

                                    {
                                        br = vm.eventRequestManager().createBreakpointRequest(bl);
                                        br.putProperty(NumberProperty, new Integer(bpcount++));
                                    }

                                br.enable();
                                System.out.println("break," + br.getProperty(NumberProperty).toString() + ",created," + bl.toString());
                            } catch (NumberFormatException e) {
                            System.out.println("error,line nunmber must be an integer.");
                        } catch (AbsentInformationException a) {
                            System.out.println("error,no such line number.");
                        }

                        break;


                    case BREAKS:
                        
                        List<BreakpointRequest> brs = vm.eventRequestManager().breakpointRequests();
                        BreakpointRequest []      bps = new BreakpointRequest [500];

                        System.out.print("breakpoints");
                        
                        for (BreakpointRequest b : brs)  bps [((Integer) b.getProperty(NumberProperty)).intValue()] = b;

                        for (int i = 0; i != bps.length; i++)

                            {
                                if (bps [i] != null && bps [i].isEnabled())

                                    System.out.print(",breakpoint," + i + "," + (new location(bps [i].location())).toString());
                            }
                        
                        System.out.print("\n");
                        break;


                    case CLEAR:

                        if (parse.next() != parser.TOKEN.STRING)

                            System.out.println("error,clear breakpoint-number");

                        else

                            {
                             try {
                                 long                            breakpointId = Long.parseLong(parse.getString());
                                 List<BreakpointRequest> bres           = vm.eventRequestManager().breakpointRequests();
                        
                                 for (BreakpointRequest b : bres)

                                     {
                                         if (((Integer) b.getProperty(NumberProperty)).intValue() == breakpointId)

                                             {
                                                 b.disable();
                                                 System.out.println("cleared," + breakpointId);
                                             }
                                     }

                                 System.out.println("error,no such breakpoint");
                             } catch (NumberFormatException e) {
                                 System.out.println("error,breakpointId is numeric");
                             }
                            }
                        
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
                                    DebuggerFrame fr = tr.getFrame(Integer.parseInt(frameid));
                                    System.out.println((fr.error != null) ? fr.error
                                                       : "locals," + fr.showLocals() + "\narguments," + fr.showArguments());
                                } catch (NumberFormatException e) {
                                    System.out.println("error,frame id must be an integer");
                                }
                            }

                        break;

                    case INTO:

                        if (parse.next() == parser.TOKEN.STRING)

                            step(parse.getString(),
                                 StepRequest.STEP_LINE,
                                 StepRequest.STEP_INTO);
                                    
                        else

                            System.out.println("error,missing thread-id");

                        break;

                    case NEXT:

                        if (parse.next() == parser.TOKEN.STRING)

                            step(parse.getString(),
                                 StepRequest.STEP_LINE,
                                 StepRequest.STEP_OVER);
                                    
                        else

                            System.out.println("error,missing thread-id");

                        break;

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
                                System.out.println("prepared," + parse.getString());
                            }

                        break;
                        
                    case QUIT:
                        if (vm != null) vm.exit(0);
                        System.out.println("proxy,exit");
                        System.exit(0);

                    case CONTINUE:
                    case RUN:

                        if (vm == null)

                            System.out.println("error,no virtual machine");

                        else

                            {
                                vm.resume();
                                System.out.println("resuming");
                            }

                        break;

                    case STACK:

                        tok0           = parse.next();
                        
                        if (tok0 != parser.TOKEN.STRING)
                            
                            System.out.println("error,stack thread-id");

                        else if (null == (tr = getThread(parse.getString())))
                            
                            System.out.println("error,no such thread," + parse.getString());

                        else

                            {
                                System.out.println("stack," + tr.threadID());

                                for (int i = 0; i != tr.frameCount(); i++)

                                    System.out.println("," + tr.getFrame(i).showLocation());
                            }

                        break;

                        
                    case THIS:
                        
                        tok0           = parse.next();
                        tr                = getThread(parse.getString());
                        tok1           = parse.next();
                        frameid     = parse.getString();
                        
                        if (tok0 != parser.TOKEN.STRING
                            ||  tok1 != parser.TOKEN.STRING)
                            
                            System.out.println("error,this thread-id frame-id");

                        else if (tr == null)
                            
                            System.out.println("error,no such thread," );

                        else

                            {
                                    try

                                        {
                                            DebuggerFrame fr = tr.getFrame(Integer.parseInt(frameid));
                                            System.out.println("this," + fr.showThis());
                                        } catch (NumberFormatException e) {System.out.println("error,frame id must be an integer," + frameid);}
                            }

                        break;
                        
                        
                    case THREAD:

                        System.out.print("threads");

                        for (ThreadReference thr: vm.allThreads())

                            System.out.print("," + (new thread(thr)).toString());

                        System.out.print("\n");

                        break;


                    default:
                        System.out.println("error,unknown command :" + parse.getCommandString() + ":");
                        break;
                    }
            }
    }

    private void attach(String host, String port)
    {
        try {
            List<AttachingConnector> l = Bootstrap.virtualMachineManager().attachingConnectors();

            AttachingConnector ac = null;
                        
            for (AttachingConnector c: l) {
                    
                if (c.name().equals("com.sun.jdi.SocketAttach")) {
                    ac = c;
                    break;
                }
            }
        
            if (ac == null)

                {
                    System.out.println("error,unable to locate ProcessAttachingConnector");
                    return;
                }

            Map<String,Connector.Argument> env = ac.defaultArguments();
                
            env.get("hostname").setValue(host);
            env.get("port").setValue(port);
                
            vm = ac.attach(env);
            new EventReader(vm).start();

            if (vm == null)

                System.out.println("error,failed to create virtual machine");

            else

                System.out.println("vm,created");
        } catch (IOException | IllegalConnectorArgumentsException e) {
            System.out.println("exception, " + e.toString());
        }
    }

    private void step(String threadID, int size, int depth)
        {
            DebuggerThread tr = getThread(threadID);

            if (tr != null)

                {
                    List<StepRequest> srl = vm.eventRequestManager().stepRequests();
                    StepRequest sr = null;
                                        
                    for (StepRequest s : srl)

                        {
                            if (s.thread()   == tr.getThreadReference()
                                && s.size()  == size
                                && s.depth() == depth)

                                sr = s;
                        }

                    if (sr == null)

                        {
                            sr = vm.eventRequestManager().createStepRequest(tr.getThreadReference(),
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
    
    private DebuggerThread getThread(String id)
    {
        try

            {
                long threadId = Long.parseLong(id);
                
                for (ThreadReference thr: vm.allThreads())

                    {
                        if (thr.uniqueID() == threadId)

                            return new DebuggerThread(thr);
                    }
            } catch (NumberFormatException e) {}

        return null;
    }
}
