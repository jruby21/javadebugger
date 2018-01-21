// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger
// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" -agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y foo
// /home/jruby/tools/jdk1.8.0_131/bin/javac -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger.java

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.StepRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class debugger
{
    public static final String NumberProperty = "breakpointNumber";

    private VirtualMachine vm       = null;
    private EventReader    er        = null;
    private int                 bpcount = 0;

    private enum TOKEN {ARGUMENTS, ATTACH, BACK, BREAK, BREAKS, CLEAR, CONTINUE, DONE, FRAME, INTO, LOCALS, NEXT, NUMBER, PREPARE, QUIT, RUN, STACK, STRING, THREAD, THIS};

    public static void main(String args[]) throws Exception    {

        debugger d = new debugger();
        d.go(System.in, System.out);
    }

    void go(InputStream input, PrintStream out) throws Exception {

        HashMap<String, CommandDescription> keywords = new HashMap<String, CommandDescription>();

        keywords.put("attach",    new CommandDescription(TOKEN.ATTACH, 3, "attach hostname port"));
        keywords.put("back",      new CommandDescription(TOKEN.BACK, 2, "back thread-id"));
        keywords.put("break",     new CommandDescription(TOKEN.BREAK, 3, "break class-name <line-number|method name>"));
        keywords.put("breaks",    new CommandDescription(TOKEN.BREAKS, 1, "breaks"));
        keywords.put("clear",      new CommandDescription(TOKEN.CLEAR, 2, "clear breakpoint-number"));
        keywords.put("continue", new CommandDescription(TOKEN.CONTINUE, 1, "continue"));
        keywords.put("frame",     new CommandDescription(TOKEN.FRAME, 3, "frame thread-id frame-id"));
        keywords.put("into",        new CommandDescription(TOKEN.INTO, 2, "back thread-id"));
        keywords.put("next",       new CommandDescription(TOKEN.NEXT, 2, "back thread-id"));
        keywords.put("prepare",  new CommandDescription(TOKEN.PREPARE, 2, "prepare main-class"));
        keywords.put("quit",        new CommandDescription(TOKEN.QUIT, 1, "quit"));
        keywords.put("run",        new CommandDescription(TOKEN.RUN, 1, "run"));
        keywords.put("stack",     new CommandDescription(TOKEN.STACK, 2, "stack thread-id"));
        keywords.put("this",       new CommandDescription(TOKEN.THIS, 3, "this thread-id frame-id"));
        keywords.put("threads",  new CommandDescription(TOKEN.THREAD, 1, "threads"));

        out.println("proxy,started");

        try {
                    out.println("go");
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
out.println("gone");
            for (String s = in.readLine().trim();
                 !s.equalsIgnoreCase("exit");
                 s = in.readLine().trim())   {
                System.out.println("received :" + s);
                if (!s.isEmpty()) {

                    String [] tokens = s.split(",");
                    expr(tokens,
                         keywords.get(tokens [0].toLowerCase().trim()),
                         out);
                }
            }
        } catch (IllegalArgumentException e) {
            out.println("error,IllegalArgumentException," + e.toString());
            e.printStackTrace(out);
        } catch (IOException e) {
            out.println("error,IOException," + e.toString());
            e.printStackTrace(out);
        } catch (Throwable t) {
            out.println("error,thowable," + t.toString());
            t.printStackTrace(out);
        } finally {
            out.println("proxy,exit");
            out.flush();
            if (vm != null) {
                vm.exit(0);
            }
            System.exit(0);
        }
    }

    void expr(String [] tokens, CommandDescription command, PrintStream out)   {

        DebuggerThread        tr            = null;

        if (command == null)  {

            out.println("error,unknown command :" + tokens [0]);
            return;
        }

        if (tokens.length != command.length)  {

            out.println("error," + command.format);
            return;
        }

        for (int i = 0;
             i != tokens.length;
             i++)   {

            tokens [i] = tokens [i].trim();

            if (tokens [i].isEmpty())   {

                out.println("error,field " + i + " in command is empty.");
                return;
            }
        }

        switch (command.token)   {

        case ATTACH:

            attach(tokens [1], tokens [2], out);
            break;

        case BACK:

            step(tokens [1],
                 StepRequest.STEP_LINE,
                 StepRequest.STEP_OUT);

            break;


        case BREAK:

            try  {

                List<ReferenceType> classes = vm.classesByName(tokens [1]);

                if (classes == null || classes.isEmpty())  {

                    out.println("error,no class named " + tokens [1]);
                    break;
                }

                // line number or method name

                List<Location> locs = null;

                if (tokens [2].matches("^\\d+$")) {

                    locs = classes.get(0).locationsOfLine(Integer.parseInt(tokens [2]));

                } else {

                    List<Method> m = classes.get(0).methodsByName(tokens [2]);

                    if (m == null || m.isEmpty()) {
                        out.println("error,no method named " + tokens [2]);
                        break;
                    }

                    locs = m.get(0).allLineLocations();
                }

                if (locs == null || locs.isEmpty())    {
                    out.println("error,no line/method named " + tokens [2]);
                    break;
                }

                Location                       bl  = locs.get(0);
                BreakpointRequest         br  = null;
                List<BreakpointRequest> brs = vm.eventRequestManager().breakpointRequests();

                for (BreakpointRequest b : brs)  {

                    if (bl.equals(b.location())) {
                        br = b;
                    }
                }

                if (br == null)   {
                    br = vm.eventRequestManager().createBreakpointRequest(bl);
                    br.putProperty(NumberProperty, new Integer(bpcount++));
                }

                br.enable();
                out.println("break," + br.getProperty(NumberProperty).toString() + ",created," + bl.toString());
            } catch (NumberFormatException e) {
                out.println("error,line nunmber must be an integer.");
            } catch (AbsentInformationException a) {
                out.println("error,no such line number.");
            }

            break;


        case BREAKS:

            List<BreakpointRequest> brs = vm.eventRequestManager().breakpointRequests();
            BreakpointRequest []      bps = new BreakpointRequest [500];

            out.print("breakpoints");

            for (BreakpointRequest b : brs)  {
                bps [((Integer) b.getProperty(NumberProperty)).intValue()] = b;
            }

            for (int i = 0; i != bps.length; i++)   {

                if (bps [i] != null && bps [i].isEnabled()) {

                    out.print(",breakpoint," + i + "," + (new DebuggerLocation(bps [i].location())).toString());
                }
            }

            out.print("\n");
            break;


        case CLEAR:

            try {
                long                            breakpointId = Long.parseLong(tokens [1]);
                List<BreakpointRequest> bres           = vm.eventRequestManager().breakpointRequests();

                for (BreakpointRequest b : bres)  {

                    if (((Integer) b.getProperty(NumberProperty)).intValue() == breakpointId)  {
                        b.disable();
                        out.println("cleared," + breakpointId);
                    }
                }

                out.println("error,no breakpoint number " + tokens [1]);
            } catch (NumberFormatException e) {
                out.println("error,breakpointId should be numeric not " + tokens [1]);
            }

            break;


        case FRAME:

            tr = getThread(tokens [1]);

            if (tr == null) {

                out.println("error,no such thread");

            } else {

                try {
                    DebuggerFrame fr = tr.getFrame(Integer.parseInt(tokens [2]));
                    out.println("locals,"
                                + fr.showLocals()
                                + "\narguments,"
                                + fr.showArguments());
                } catch (NumberFormatException e) {
                    out.println("error,frame id must be an integer");
                }
            }

            break;

        case INTO:

            step(tokens [1],
                 StepRequest.STEP_LINE,
                 StepRequest.STEP_INTO);

            break;

        case NEXT:

            step(tokens [1],
                 StepRequest.STEP_LINE,
                 StepRequest.STEP_OVER);

            break;

        case PREPARE:

            if (vm == null) {

                out.println("error,no virtual machine");

            } else {

                ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
                r.addClassFilter(tokens [1]);
                r.enable();
                out.println("prepared," + tokens [1]);
            }

            break;

        case QUIT:
            if (vm != null) {
                vm.exit(0);
            }
            out.println("proxy,exit");
            out.flush();
            System.exit(0);

        case CONTINUE:
        case RUN:

            if (vm == null) {

                out.println("error,no virtual machine");

            } else {

                vm.resume();
                out.println("resuming");
            }

            break;

        case STACK:

            if (null == (tr = getThread(tokens [1]))) {

                out.println("error,no such thread," + tokens [1]);

            } else {

                out.println("stack," + tr.threadID());

                for (int i = 0; i != tr.frameCount(); i++) {

                    out.println("," + tr.getFrame(i).showLocation());
                }
            }

            break;


        case THIS:

            tr = getThread(tokens [1]);

            if (tr == null) {

                out.println("error,no such thread," );

            } else {

                try  {
                    out.println("this," + tr.getFrame(Integer.parseInt(tokens [2])).showThis());
                } catch (NumberFormatException e) {out.println("error,frame id must be an integer," + tokens [2]);}
            }

            break;


        case THREAD:

            out.print("threads");

            for (ThreadReference thr: vm.allThreads()) {

                out.print("," + (new DebuggerThread(thr)).toString());
            }

            out.print("\n");

            break;


        default:
            out.println("error,unknown command :" + tokens [0] + ":");
            break;
        }
    }

    private void attach(String host, String port, PrintStream out)  {
        try {
            List<AttachingConnector> l = Bootstrap.virtualMachineManager().attachingConnectors();

            AttachingConnector ac = null;

            for (AttachingConnector c: l) {

                if (c.name().equals("com.sun.jdi.SocketAttach")) {
                    ac = c;
                    break;
                }
            }

            if (ac == null)  {

                out.println("error,unable to locate ProcessAttachingConnector");
                return;
            }

            Map<String,Connector.Argument> env = ac.defaultArguments();

            env.get("hostname").setValue(host);
            env.get("port").setValue(port);

            vm = ac.attach(env);

            if (vm == null) {

                out.println("error,failed to create virtual machine");

            } else {

                new EventReader(vm, out).start();
                out.println("vm,created");
            }
        } catch (IOException | IllegalConnectorArgumentsException e) {
            out.println("exception, " + e.toString());
        }
    }

    private void step(String threadId, int size, int depth)  {

        DebuggerThread tr = getThread(threadId);

        if (tr != null)  {

            List<StepRequest> srl = vm.eventRequestManager().stepRequests();
            StepRequest sr = null;

            for (StepRequest s : srl)  {

                if (s.thread()   == tr.getThreadReference()
                    && s.size()  == size
                    && s.depth() == depth)

                    sr = s;
            }

            if (sr == null)  {

                sr = vm.eventRequestManager().createStepRequest(tr.getThreadReference(),
                                                                size,
                                                                depth);

                sr.addClassExclusionFilter("java.*");
                sr.addClassExclusionFilter("sun.*");
                sr.addClassExclusionFilter("com.sun.*");
            }

            if (sr != null)  {
                sr.addCountFilter(1);
                sr.enable();
                vm.resume();
            }
        }
    }

    private DebuggerThread getThread(String id) {
        try {
            long threadId = Long.parseLong(id);

            for (ThreadReference thr: vm.allThreads()) {
                if (thr.uniqueID() == threadId) {
                    return new DebuggerThread(thr);
                }
            }
        } catch (NumberFormatException e) {}

        return null;
    }

    class CommandDescription    {
        public debugger.TOKEN token;
        public int                     length;
        public String                format;

        CommandDescription(debugger.TOKEN t, int len, String s)
        {
            token  = t;
            length = len;
            format = s;
        }
    }
}
