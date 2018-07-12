// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger
// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" -agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y test.foo 3 4
// /home/jruby/tools/jdk1.8.0_131/bin/javac -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger.java

package com.github.jruby21.javadebugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
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

public class JavaDebuggerProxy
{
    public  static final String NumberProperty = "breakpointNumber";
    private static       int    bpcount        = 0;

    private VirtualMachine vm             = null;
    private int            pcount         = 0;
    private DebuggerOutput debuggerOutput = new DebuggerOutput(System.out);

    private enum TOKEN { ACCESS, ARGUMENTS, ATTACH, BACK, BREAK, BREAKS, CATCH, CLASSES, CLEAR, DONE, FIELDS, INTO, LOCALS, MODIFY, NEXT, NUMBER, PREPARE, QUIT, RUN, STACK, STRING, THREAD, THIS};

    public static void main(String args[]) throws Exception    {

        JavaDebuggerProxy d = new JavaDebuggerProxy();
        d.go(System.in, System.out);
    }

    void go(InputStream input, PrintStream out) throws Exception {

        HashMap<String, CommandDescription> keywords = new HashMap<String, CommandDescription>();

        keywords.put("access",    new CommandDescription(TOKEN.ACCESS,    3, "access  classname fieldname"));
        keywords.put("arguments", new CommandDescription(TOKEN.ARGUMENTS, 4, "arguments thread-id frame-id variables"));
        keywords.put("attach",    new CommandDescription(TOKEN.ATTACH,    3, "attach hostname port"));
        keywords.put("back",      new CommandDescription(TOKEN.BACK,      2, "back thread-id"));
        keywords.put("break",     new CommandDescription(TOKEN.BREAK,     3, "break class-name <line-number|method name>"));
        keywords.put("breaks",    new CommandDescription(TOKEN.BREAKS,    1, "breaks"));
        keywords.put("classes",   new CommandDescription(TOKEN.CLASSES,   1, "classes"));
        keywords.put("clear",     new CommandDescription(TOKEN.CLEAR,     2, "clear breakpoint-number"));
        keywords.put("catch",     new CommandDescription(TOKEN.CATCH,     2, "catch on|off"));
        keywords.put("fields",    new CommandDescription(TOKEN.FIELDS,    2, "fields class-name"));
        keywords.put("into",      new CommandDescription(TOKEN.INTO,      2, "into thread-id"));
        keywords.put("locals",    new CommandDescription(TOKEN.LOCALS,    4, "locals thread-id frame-id variables"));
        keywords.put("modify",    new CommandDescription(TOKEN.MODIFY,    3, "modify  classname fieldname"));
        keywords.put("next",      new CommandDescription(TOKEN.NEXT,      2, "next thread-id"));
        keywords.put("prepare",   new CommandDescription(TOKEN.PREPARE,   2, "prepare main-class"));
        keywords.put("quit",      new CommandDescription(TOKEN.QUIT,      1, "quit"));
        keywords.put("run",       new CommandDescription(TOKEN.RUN,       1, "run"));
        keywords.put("stack",     new CommandDescription(TOKEN.STACK,     2, "stack thread-id"));
        keywords.put("this",      new CommandDescription(TOKEN.THIS,      3, "this thread-id frame-id"));
        keywords.put("threads",   new CommandDescription(TOKEN.THREAD,    1, "threads"));

        debuggerOutput.output_proxyStarted( );

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));

            for (String s = in.readLine().trim();
                 !s.equalsIgnoreCase("exit");
                 s = in.readLine().trim())   {
                if (!s.isEmpty()) {
                    String [] tokens = s.split(",");
                    expr(tokens,
                         keywords.get(tokens [0].toLowerCase().trim()),
                         out);
                }
            }
        } catch (Throwable t) {
            debuggerOutput.output_internalException(t);
        } finally {
            debuggerOutput.output_proxyExited( );
            debuggerOutput.close();
            if (vm != null) {
                vm.exit(0);
            }
            System.exit(0);
        }
    }

    void expr(String [] tokens, CommandDescription command, PrintStream out)   {

        ThreadReference trr  = null;
        StackFrame            sf    = null;
        String []                    refs = null;

        if (command == null)  {

            debuggerOutput.output_error("unknown command :" + tokens [0]);
            return;
        }

        if (tokens.length != command.length)  {

            debuggerOutput.output_error(command.format);
            return;
        }

        for (int i = 0;
             i != tokens.length;
             i++)   {

            tokens [i] = tokens [i].trim();

            if (tokens [i].isEmpty())   {

                debuggerOutput.output_error("field " + i + " in command is empty.");
                return;
            }
        }

        if (vm == null
            && command.token != TOKEN.ATTACH
            && command.token != TOKEN.QUIT) {

            debuggerOutput.output_error("no virtual machine");
            return;
        }

        try {

            switch (command.token)   {

            case ACCESS:

                try {
                    Field                   f = getField(tokens [1], tokens [2]);
                    AccessWatchpointRequest w = null;

                    for (AccessWatchpointRequest a : vm.eventRequestManager().accessWatchpointRequests()) {

                        if (a.field().equals(f)) {

                                w = a;
                            }
                    }

                    if (w == null) {

                        w = vm.eventRequestManager().createAccessWatchpointRequest(f);
                    }

                    w.setEnabled(true);
                    debuggerOutput.output_accessWatchpointSet();

                } catch (ClassNotPreparedException c) {
                    debuggerOutput.output_error("Class " + tokens [1] + " not prepared.");
                } catch (UnsupportedOperationException e) {
                    debuggerOutput.output_error("Virtual machine does not support access watchpoints.");
                }

                break;


            case ARGUMENTS:

                localVariables(tokens [1], tokens [2], tokens [3], true);
                break;


            case ATTACH:

                attach(tokens [1], tokens [2]);
                break;

            case BACK:

                step(tokens [1],
                     StepRequest.STEP_LINE,
                     StepRequest.STEP_OUT);

                break;


            case BREAK:

                List<ReferenceType> classes = vm.classesByName(tokens [1]);

                if (classes == null || classes.isEmpty())  {

                    debuggerOutput.output_error("no class named " + tokens [1]);
                    break;
                }

                // line number or method name

                List<Location> locs = null;

                if (tokens [2].matches("^\\d+$")) {

                    locs = classes.get(0).locationsOfLine(Integer.parseInt(tokens [2]));

                } else {

                    List<Method> m = classes.get(0).methodsByName(tokens [2]);

                    if (m == null || m.isEmpty()) {
                        debuggerOutput.output_error("no method named " + tokens [2]);
                        break;
                    }

                    locs = m.get(0).allLineLocations();
                }

                if (locs == null || locs.isEmpty())    {
                    debuggerOutput.output_error("no line/method named " + tokens [2]);
                    break;
                }

                Location          bl = locs.get(0);
                BreakpointRequest br = null;

                for (BreakpointRequest b : vm.eventRequestManager().breakpointRequests())  {

                    if (bl.equals(b.location())) {
                        br = b;
                    }
                }

                if (br == null)   {
                    br = vm.eventRequestManager().createBreakpointRequest(bl);
                    br.putProperty(NumberProperty, new Integer(bpcount++));
                }

                br.enable();
                debuggerOutput.output_breakpointCreated (((Integer) br.getProperty(NumberProperty)).intValue(), bl);

                break;


            case BREAKS:

                debuggerOutput.output_breakpointList(vm.eventRequestManager().breakpointRequests());
                break;


            case CATCH:
                boolean          enable = tokens [1].equalsIgnoreCase("on");
                ExceptionRequest er     = null;

                for (ExceptionRequest e : vm.eventRequestManager().exceptionRequests())

                    {
                        if (e.exception() == null)

                            er = e;
                    }

                if (er == null)

                    {
                        er = vm.eventRequestManager().createExceptionRequest(null, true, true);
                    }

                if (enable)

                    {
                        er.addClassFilter("test.*");
                        er.enable();
                        debuggerOutput.output_catchEnabled(true);
                    }

                else

                    {
                        er.disable();
                        debuggerOutput.output_catchEnabled(false);
                    }

                break;


            case CLASSES:

                debuggerOutput.output_classes(vm.allClasses());
                break;



            case CLEAR:

                int               breakpointId = -1;

                if (!tokens [1].equalsIgnoreCase("all")) {

                    breakpointId = Integer.parseInt(tokens [1]);
                }

                for (BreakpointRequest b : vm.eventRequestManager().breakpointRequests()) {

                    if (breakpointId == -1
                        || ((Integer) b.getProperty(NumberProperty)).intValue() == breakpointId)  {
                        b.disable();
                    }
                }

                debuggerOutput.output_breakpointList(vm.eventRequestManager().breakpointRequests());

                break;


            case FIELDS:

                for (ReferenceType r : vm.classesByName(tokens [1])) {
                    debuggerOutput.output_fields(r.name(), r.allFields());
                }

                break;

            case INTO:

                step(tokens [1],
                     StepRequest.STEP_LINE,
                     StepRequest.STEP_INTO);

                break;

            case LOCALS:

                localVariables(tokens [1], tokens [2], tokens [3], false);
                break;


            case MODIFY:

                try {
                    Field                          f = getField(tokens [1], tokens [2]);
                    ModificationWatchpointRequest  w = null;

                    for (ModificationWatchpointRequest m :  vm.eventRequestManager().modificationWatchpointRequests()) {

                        if (f.equals(m.field())) {

                         w = m;
                        }
                    }

                    if (w == null) {

                        w = vm.eventRequestManager().createModificationWatchpointRequest(f);
                    }

                    w.setEnabled(true);
                    debuggerOutput.output_modificationWatchpointSet();

                } catch (ClassNotPreparedException c) {
                    debuggerOutput.output_error("Class " + tokens [1] + " not prepared.");
                } catch (UnsupportedOperationException e) {
                    debuggerOutput.output_error("Virtual machine does not support modification watchpoints.");
                }

                break;

            case NEXT:

                step(tokens [1],
                     StepRequest.STEP_LINE,
                     StepRequest.STEP_OVER);

                break;

            case PREPARE:

                ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
                r.disable();
                r.addClassFilter(tokens [1]);
                r.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                r.enable();
                debuggerOutput.output_preparingClass (tokens [1]);
                break;

            case QUIT:

                debuggerOutput.output_proxyExited ( );
                debuggerOutput.close();
                System.exit(0);

            case RUN:

                debuggerOutput.output_vmResumed ( );
                vm.resume();
                break;


            case STACK:

                debuggerOutput.output_stack(tokens [1]);

                for (StackFrame sfp : getThreadReference(tokens [1]).frames()) {

                    debuggerOutput.outputLocation(sfp.location());
                }

                debuggerOutput.outputEnd();

                break;


            case THIS:

                trr = getThreadReference(tokens[1]);
                debuggerOutput.output_this(tokens [1],
                                           tokens [2],
                                           trr.frame(Integer.parseInt(tokens [2])).thisObject(),
                                           trr,
                                           tokens [2].split("[.]"));

                break;


            case THREAD:

                debuggerOutput.output_threadList( );

                for (ThreadReference ttr: vm.allThreads()) {

                    debuggerOutput.outputThreadReference(ttr);
                }

                debuggerOutput.outputEnd();

                break;


            default:
                debuggerOutput.output_error("unknown command :" + tokens [0]);
                break;
            }
        } catch (NumberFormatException e) {
            debuggerOutput.output_error(e.getMessage());
        } catch (AbsentInformationException e) {
            debuggerOutput.output_error(e.getMessage());
        } catch (IncompatibleThreadStateException e) {
            debuggerOutput.output_error(e.getMessage());
        } catch (InvalidStackFrameException e) {
            debuggerOutput.output_error(e.getMessage());
        } catch (IllegalArgumentException e) {
            debuggerOutput.output_error(e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            debuggerOutput.output_error(e.getMessage());
        }
    }


    private void localVariables(String thread, String frame, String tok3, boolean isArgument)
    {
        if (isArgument) {
            debuggerOutput.output_arguments(thread, frame);
        } else {
            debuggerOutput.output_local(thread, frame);
        }

        try {
            ThreadReference   tr   = getThreadReference(thread);
            StackFrame        sf   = tr.frame(Integer.parseInt(frame));
            ArrayList<String> name = new ArrayList<String>();
            ArrayList<Value>  val  = new ArrayList<Value>();
            String []         refs = tok3.split("[.]");

            // Why do we do this nonsense with the lists?  Because accessing the
            // virtual machine to display the values can cause sf.getValue() to
            // fail with a 'Thread has been resumed' error. So we do all the
            // sf.getValue() calls before any display calls.

            for (LocalVariable lv : sf.visibleVariables()) {

                if (isArgument == lv.isArgument()
                    && (refs[0].equals("*") || refs[0].equals(lv.name()))) {

                    name.add(lv.name());
                    val.add(sf.getValue(lv));
                }
            }

            Iterator<String> iname = name.iterator();
            Iterator<Value>  vname = val.iterator();

            while (iname.hasNext()) {
                debuggerOutput.output_variable(iname.next(), vname.next(), tr, refs);
            }
        } catch (IncompatibleThreadStateException e) {
        } catch (AbsentInformationException e) {
        }

        debuggerOutput.output_endargument();
    }

    private void attach(String host, String port)  {
        try {
            AttachingConnector ac = null;

            for (AttachingConnector c : Bootstrap.virtualMachineManager().attachingConnectors()) {

                if (c.name().equals("com.sun.jdi.SocketAttach")) {
                    ac = c;
                    break;
                }
            }

            if (ac == null)  {

                debuggerOutput.output_error("unable to locate ProcessAttachingConnector");
                return;
            }

            Map<String,Connector.Argument> env = ac.defaultArguments();

            env.get("hostname").setValue(host);
            env.get("port").setValue(port);

            vm = ac.attach(env);

            if (vm == null) {

                debuggerOutput.output_error("failed to create virtual machine");

            } else {
                new EventReader(vm, debuggerOutput).start();
                debuggerOutput.output_vmCreated( );
            }
        } catch (IOException | IllegalConnectorArgumentsException e) {
            debuggerOutput.output_internalException(e);
        }
    }

    private void step(String threadId, int size, int depth)  {

        ThreadReference tr = getThreadReference(threadId);

        if (tr != null)  {

            List<StepRequest> srl = vm.eventRequestManager().stepRequests();
            StepRequest       sr  = null;

            for (StepRequest s : srl)  {

                if (s.thread()   == tr
                    && s.size()  == size
                    && s.depth() == depth)

                    sr = s;
            }

            if (sr == null)  {

                sr = vm.eventRequestManager().createStepRequest(tr, size, depth);

                sr.addClassExclusionFilter("java.*");
                sr.addClassExclusionFilter("sun.*");
                sr.addClassExclusionFilter("com.sun.*");
            }

            if (sr != null)  {
                debuggerOutput.output_stepCreated ( );
                sr.addCountFilter(1);
                sr.enable();
                vm.resume();
            } else {
                debuggerOutput.output_error("step creation failed");
            }
        }
    }

    private ThreadReference getThreadReference(String id)
        throws NumberFormatException, IllegalArgumentException
    {
        long tid = Long.parseLong(id);

        for (ThreadReference tr: vm.allThreads()) {
            if (tr.uniqueID() == tid) {
                return tr;
            }
        }

        throw new IllegalArgumentException("no thread with id " + id);
    }

    private Field getField(String className, String fieldName)
        throws ClassNotPreparedException
    {
        for (ReferenceType r : vm.classesByName(className)) {

            Field f = r.fieldByName(fieldName);

            if (f != null) {

                return f;
            }
        }

        return null;
    }


    class CommandDescription    {
        public TOKEN  token;
        public int          length;
        public String   format;

        CommandDescription(TOKEN t, int len, String s)
        {
            token  = t;
            length = len;
            format = s;
        }
    }
}
