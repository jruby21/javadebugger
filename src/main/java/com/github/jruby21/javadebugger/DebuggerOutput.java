package com.github.jruby21.javadebugger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VoidValue;

import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.StepRequest;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ArrayBlockingQueue;

class DebuggerOutput extends Thread {

    private final static String ACCESSWATCHPOINT_RESPONSE          = "accesswatchpoint";
    private final static String ACCESSWATCHPOINTSET_RESPONSE   = "accesswatchpointset";
    private final static String ARGUMENTS_RESPONSE                            = "arguments";
    private final static String BREAKPOINTCLEARED_RESPONSE         = "breakpointcleared";
    private final static String BREAKPOINTCREATED_RESPONSE         = "breakpointcreated";
    private final static String BREAKPOINTENTERED_RESPONSE         = "breakpointentered";
    private final static String BREAKPOINTLIST_RESPONSE                   = "breakpointlist";
    private final static String CATCHENABLED_RESPONSE                     = "catchenabled";
    private final static String CLASSES_RESPONSE                                    = "classes";
    private final static String CLASSPREPARED_RESPONSE                    = "classprepared";
    private final static String CLASSUNLOADED_RESPONSE                   = "classunloaded";
    private final static String COMMAND_READY_RESPONSE                 = "commandready";
    private final static String ERROR_RESPONSE                     = "error";
    private final static String EXCEPTION_RESPONSE                 = "exception";
    private final static String FIELDS_RESPONSE                    = "fields";
    private final static String INTERNALEXCEPTION_RESPONSE         = "internalexception";
    private final static String LOCALS_RESPONSE                    = "locals";
    private final static String MODIFICATIONWATCHPOINT_RESPONSE    = "modificationwatchpoint";
    private final static String MODIFICATIONWATCHPOINTSET_RESPONSE = "modificationwatchpointset";
    private final static String PREPARINGCLASS_RESPONSE            = "preparingclass";
    private final static String PROXYEXITED_RESPONSE               = "proxyexited";
    private final static String PROXYSTARTED_RESPONSE              = "proxystarted";
    private final static String STACK_RESPONSE                     = "stack";
    private final static String STEPCREATED_RESPONSE               = "stepcreated";
    private final static String STEP_RESPONSE                      = "step";
    private final static String THIS_RESPONSE                      = "this";
    private final static String THREADDIED_RESPONSE                = "threaddied";
    private final static String THREADLIST_RESPONSE                = "threadlist";
    private final static String THREADSTARTED_RESPONSE             = "threadstarted";
    private final static String VMCREATED_RESPONSE                 = "vmcreated";
    private final static String VMDIED_RESPONSE                    = "vmdied";
    private final static String VMDISCONNECTED_RESPONSE            = "vmdisconnected";
    private final static String VMRESUMED_RESPONSE                 = "vmresumed";
    private final static String VMSTARTED_RESPONSE                 = "vmstarted";

    private ArrayBlockingQueue<EventOrCommandObject> queue    = null;
    private HashMap<String, CommandDescription>      keywords = null;
    private PrintStream                              out      = null;

    public  static final String NumberProperty = "breakpointNumber";
    private static          int      bpcount           = 0;

    private VirtualMachine    vm                 = null;
    private int                          pcount             = 0;
    private EventSet               lastEventSet      = null;

    private enum TOKEN { ACCESS, ARGUMENTS, ATTACH, BACK, BREAK, BREAKS, CATCH, CLASSES, CLEAR, DONE, FIELDS, INTO, LOCALS, MODIFY, NEXT, NUMBER, PREPARE, QUIT, RUN, STACK, STRING, THREAD, THIS};

    public DebuggerOutput(ArrayBlockingQueue<EventOrCommandObject> q, PrintStream o) {
        out   = o;
        queue = q;

        keywords = new HashMap<String, CommandDescription>();

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
        keywords.put("this",      new CommandDescription(TOKEN.THIS,      4, "this thread-id frame-id"));
        keywords.put("threads",   new CommandDescription(TOKEN.THREAD,    1, "threads"));
    }

    public void run() {

        out.println(PROXYSTARTED_RESPONSE);
        out.println(COMMAND_READY_RESPONSE);
        output_log("Entering DebuggerOutput: " + identify.timestamp + " " + identify.directory);

        while (true) {
            try {
                queue.take().evaluate(this);
            } catch (InterruptedException exc) {
                    // Do nothing. Any changes will be seen at top of loop.
        }
    }
    }

    public void command(String s) {

        if (!s.isEmpty()) {
            executeCommand(s);
            out.println(COMMAND_READY_RESPONSE);
        }
    }

    private void executeCommand(String s) {

        String []          tokens  = s.split(",");
        CommandDescription command = keywords.get(tokens [0].toLowerCase().trim());

        if (command == null)  {

            output_error("unknown command :" + tokens [0]);
            return;
        }

        if (tokens.length != command.length)  {

            output_error(command.format);
            return;
        }

        for (int i = 0;
             i != tokens.length;
             i++)   {

            tokens [i] = tokens [i].trim();

            if (tokens [i].isEmpty())   {

                output_error("field " + i + " in command is empty.");
                return;
            }
        }

        if (vm == null
            && command.token != TOKEN.ATTACH
            && command.token != TOKEN.QUIT) {

            output_error("no virtual machine");
            return;
        }

        try {
            ThreadReference trr  = null;
            StackFrame      sf   = null;
            String []       refs = null;

            switch (command.token)   {

            case ACCESS:

                Field f = getField(tokens [1], tokens [2]);

                if (f == null) {

                    output_error("No field " + tokens [2] + " in class " + tokens [1] + ".");

                } else {

                    AccessWatchpointRequest w = null;

                    for (AccessWatchpointRequest a : vm.eventRequestManager().accessWatchpointRequests()) {

                        if (a.field().equals(f)) {

                            w = a;
                        }
                    }

                    if (w == null) {

                        w = vm.eventRequestManager().createAccessWatchpointRequest(f);
                    }

                    out.println(ACCESSWATCHPOINTSET_RESPONSE + "," + tokens [1] + "," + tokens [2]);
                    w.setEnabled(true);
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

                    output_error("no class named " + tokens [1]);
                    break;
                }

                // line number or method name

                List<Location> locs = null;

                if (tokens [2].matches("^\\d+$")) {

                    locs = classes.get(0).locationsOfLine(Integer.parseInt(tokens [2]));

                } else {

                    List<Method> m = classes.get(0).methodsByName(tokens [2]);

                    if (m == null || m.isEmpty()) {
                        output_error("no method named " + tokens [2]);
                        break;
                    }

                    locs = m.get(0).allLineLocations();
                }

                if (locs == null || locs.isEmpty())    {
                    output_error("no line/method named " + tokens [2]);
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

                out.print(BREAKPOINTCREATED_RESPONSE + "," + Integer.toString(((Integer) br.getProperty(NumberProperty)).intValue()));
                outputLocation(bl);
                out.print("\n");

                break;


            case BREAKS:

                output_breakpointList(vm.eventRequestManager().breakpointRequests());
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
                        out.println(CATCHENABLED_RESPONSE + ",true");
                    }

                else

                    {
                        er.disable();
                        out.println(CATCHENABLED_RESPONSE + ",false");
                    }

                break;


            case CLASSES:

                out.print(CLASSES_RESPONSE);

                for (ReferenceType r : vm.allClasses()) {
                    out.print("," + r.name());
                }

                out.print("\n");

                break;



            case CLEAR:

                int  breakpointId = -1;

                if (!tokens [1].equalsIgnoreCase("all")) {

                    breakpointId = Integer.parseInt(tokens [1]);
                }

                for (BreakpointRequest b : vm.eventRequestManager().breakpointRequests()) {

                    if (breakpointId == -1
                        || ((Integer) b.getProperty(NumberProperty)).intValue() == breakpointId)  {
                        b.disable();
                    }
                }

                out.println(BREAKPOINTCLEARED_RESPONSE + "," + Integer.toString(breakpointId));
                output_breakpointList(vm.eventRequestManager().breakpointRequests());

                break;


            case FIELDS:

                for (ReferenceType r : vm.classesByName(tokens [1])) {
                    output_fields(r.name(), r.allFields());
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
                    Field                          ff = getField(tokens [1], tokens [2]);
                    ModificationWatchpointRequest  w  = null;

                    if (ff == null) {

                        output_error("No field " + tokens [2] + " in class " + tokens [1] + ".");

                    } else {

                        for (ModificationWatchpointRequest m :  vm.eventRequestManager().modificationWatchpointRequests()) {

                            if (ff.equals(m.field())) {

                                w = m;
                            }
                        }

                        if (w == null) {

                            w = vm.eventRequestManager().createModificationWatchpointRequest(ff);
                        }

                        w.setEnabled(true);
                        out.println(MODIFICATIONWATCHPOINTSET_RESPONSE);
                    }
                } catch (ClassNotPreparedException c) {
                    output_error("Class " + tokens [1] + " not prepared.");
                } catch (UnsupportedOperationException e) {
                    output_error("Virtual machine does not support modification watchpoints.");
                }

                break;

            case NEXT:

                step(tokens [1],
                     StepRequest.STEP_LINE,
                     StepRequest.STEP_OVER);

                break;

            case PREPARE:

                // Maybe the class is already loaded

                out.println(PREPARINGCLASS_RESPONSE + "," + tokens [1]);

                List<ReferenceType> loaded = vm.classesByName(tokens [1]);

                if (!loaded.isEmpty()) {
                    out.println(CLASSPREPARED_RESPONSE + "," + loaded.get(0).name());
                } else {
                    ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
                    r.disable();
                    r.addClassFilter(tokens [1]);
                    r.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                    r.enable();
                    vm.resume();
                }

                break;

            case QUIT:

                out.println(PROXYEXITED_RESPONSE);
                out.flush();
                out.close();
                vm.dispose();
                vm.exit(0);
                System.exit(0);
                break;

            case RUN:

                out.println(VMRESUMED_RESPONSE);
                vm.resume();
                break;


            case STACK:

                out.print(STACK_RESPONSE + "," + tokens [1]);

                for (StackFrame sfp : getThreadReference(tokens [1]).frames()) {

                    outputLocation(sfp.location());
                }

                out.print("\n");
                break;


            case THIS:

                trr = getThreadReference(tokens[1]);
                disableAllRequests();
                out.println(THIS_RESPONSE + ","
                            + tokens [1] + ","
                            + tokens [2] +",("
                            + outputValue(trr.frame(Integer.parseInt(tokens [2])).thisObject(),
                                          trr,
                                          tokens [3].split("[.]"), 0)
                            +")");
                renableAllRequests();
                break;


            case THREAD:

                out.print(THREADLIST_RESPONSE);

                for (ThreadReference tr : vm.allThreads()) {
                    outputThreadReference(tr);
                }

                out.print("\n");

                break;


            default:
                output_error("unknown command :" + tokens [0]);
                break;
            }
        } catch (AbsentInformationException e) {
            output_error(e.toString());
        } catch (ClassNotPreparedException e) {
            output_error(e.toString());
        } catch (IncompatibleThreadStateException e) {
            output_error(e.toString());
        } catch (InvalidStackFrameException e) {
            output_error(e.toString());
        } catch (IllegalArgumentException e) {
            output_error(e.toString());
        } catch (IndexOutOfBoundsException e) {
            output_error(e.toString());
        } catch (UnsupportedOperationException e) {
            output_error(e.toString());
        } catch (Throwable t) {
            output_internalException(t);
        }
    }

    private void localVariables(String thread, String frame, String tok3, boolean isArgument) throws IncompatibleThreadStateException, AbsentInformationException
    {
        ThreadReference  tr        = getThreadReference(thread);
        StackFrame            sf        = tr.frame(Integer.parseInt(frame));
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<Value> val      = new ArrayList<Value>();
        String []                   refs    = tok3.split("[.]");

        // Why do we do this nonsense with the lists?  Because accessing the
        // virtual machine (in debuggerOutput.output_variable) to display
        // the values can cause sf.getValue() to fail with a 'Thread has
        // been resumed' error. So we do all the sf.getValue() calls before
        // any display calls.

        for (LocalVariable lv : sf.visibleVariables()) {

            if (isArgument == lv.isArgument()
                && (refs[0].equals("*") || refs[0].equals(lv.name()))) {

                name.add(lv.name());
                val.add(sf.getValue(lv));
            }
        }

        out.print(((isArgument) ? ARGUMENTS_RESPONSE : LOCALS_RESPONSE) + ","
                  + thread + ","
                  + frame + ",(");

        Iterator<String> itn = name.iterator();
        Iterator<Value>  itv = val.iterator();

        disableAllRequests();
        while (itn.hasNext()) {
            out.print("(\"" + itn.next() + "\" " + outputValue(itv.next(), tr, refs, 1) + " ) ");
        }

        renableAllRequests();
        out.println(")");
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

                output_error("unable to locate ProcessAttachingConnector");
                return;
            }

            Map<String,Connector.Argument> env = ac.defaultArguments();

            env.get("hostname").setValue(host);
            env.get("port").setValue(port);

            vm = ac.attach(env);

            if (vm == null) {

                output_error("failed to create virtual machine");

            } else {
                new EventReader(queue, vm).start();
                out.println(VMCREATED_RESPONSE);
            }
        } catch (IOException | IllegalConnectorArgumentsException e) {
            output_internalException(e);
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
                sr.addCountFilter(1);
                sr.enable();
                vm.resume();
            } else {
                output_error("step creation failed");
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

    public void event(EventSet event) {

        boolean       resume = true;
        EventIterator it     = event.eventIterator();

        lastEventSet = event;

        while (it.hasNext()) {
            Event e = it.next();
            resume = resume && eventHandler(e);
        }

        if (resume) {
            event.resume();
        }
    }

    private boolean eventHandler(Event event) {
        try {
            if (event instanceof AccessWatchpointEvent) {
                AccessWatchpointEvent ae = (AccessWatchpointEvent) event;

                out.print(ACCESSWATCHPOINT_RESPONSE + ",7");
                outputThreadReference(ae.thread());
                outputLocation(ae.location());
                out.println("," + ae.object().referenceType().name() + ","
                            + ae.field().name()
                            + ",("
                            + outputSingleLevelValue(ae.valueCurrent(), ae.thread())
                            + ")");

                // if you want a step to work and not invoke this breakpoint
                // again.
                event.request().setEnabled(false);

                return false;
            }

            if (event instanceof BreakpointEvent) {
                BreakpointEvent bp = (BreakpointEvent) event;

                out.print(BREAKPOINTENTERED_RESPONSE + ","
                          + Integer.toString(((Integer) bp.request().getProperty(NumberProperty)).intValue()));
                outputThreadReference(bp.thread());
                outputLocation(bp.location());
                out.print("\n");
                return false;
            }

            if (event instanceof ClassPrepareEvent) {

                // In rare cases, this event may occur in a debugger
                // system thread within the target VM. Debugger
                // threads take precautions to prevent these events,
                // but they cannot be avoided under some conditions,
                // especially for some subclasses of Error. If the
                // event was generated by a debugger system thread,
                // the value returned by this method is null, and if
                // the requested suspend policy for the event was
                // EventRequest.SUSPEND_EVENT_THREAD, all threads
                // will be suspended instead, and the
                // EventSet.suspendPolicy() will reflect this
                // change.

                // Note that the discussion above does not apply to
                // system threads created by the target VM during its
                // normal (non-debug) operation.

                if (((ClassPrepareEvent) event).thread() != null) {
                    out.println(CLASSPREPARED_RESPONSE + "," + ((ClassPrepareEvent) event).referenceType().name());
                } else {
                    ((ClassPrepareRequest) event.request()).enable();
                }

                return false;
            }

            if (event instanceof ClassUnloadEvent) {
                out.println(CLASSUNLOADED_RESPONSE + ","
                            + ((ClassUnloadEvent) event).className());

                return true;
            }

            if (event instanceof ExceptionEvent)   {
                output_exception((ExceptionEvent) event);
                return true;
            }

            if (event instanceof MethodEntryEvent) {
                return true;
            }

            if (event instanceof MethodExitEvent) {
                return true;
            }

            if (event instanceof ModificationWatchpointEvent) {
                ModificationWatchpointEvent me = (ModificationWatchpointEvent) event;

                out.print(MODIFICATIONWATCHPOINT_RESPONSE);
                outputLocation(me.location());
                out.println("," + me.object().referenceType().name() + "," + me.field().name() + ",("
                            + outputSingleLevelValue(me.valueCurrent(), me.thread())
                            + "),("
                            + outputSingleLevelValue(me.valueToBe(), me.thread())
                            + ")");

                // if you want a step to work and not invoke this breakpoint
                // again.
                event.request().setEnabled(false);

                return false;
            }

            if (event instanceof StepEvent) {
                StepEvent se = (StepEvent) event;

                out.print(STEP_RESPONSE);
                outputThreadReference(se.thread());
                outputLocation(se.location());
                out.print("\n");
                se.request().disable();

                return false;
            }

            if (event instanceof ThreadStartEvent) {
                out.print(THREADSTARTED_RESPONSE);
                outputThreadReference(((ThreadStartEvent) event).thread());
                out.print("\n");
                return true;
            }

            if (event instanceof ThreadDeathEvent) {
                out.print(THREADDIED_RESPONSE);
                outputThreadReference(((ThreadDeathEvent) event).thread());
                out.print("\n");
                return true;
            }

            if (event instanceof VMDeathEvent)   {
                out.println(VMDIED_RESPONSE);
                return true;
            }

            if (event instanceof VMDisconnectEvent)   {
                out.println(VMDISCONNECTED_RESPONSE);
                return true;
            }

            if (event instanceof VMStartEvent) {
                out.println(VMSTARTED_RESPONSE);
                return false;
            }
        } catch (VMDisconnectedException d) {
            out.println(VMDISCONNECTED_RESPONSE);
        }

        return true;
    }

    public final void output_log(String msg) {
        out.println("log,\"" + msg + "\""); }

    public final void output_fields (String cl, List<Field> fields) {
        out.print(FIELDS_RESPONSE + "," + cl);

        for (Field f : fields) {

            if (f != null) {

                out.print("," + f.name()
                          + "," + f.typeName()
                          + "," + f.declaringType().name()
                          + "," + f.isEnumConstant()
                          + "," + f.isTransient()
                          + "," + f.isVolatile()
                          + "," + f.isFinal()
                          + "," + f.isStatic());
            }
        }

        out.print("\n");
     }

    public final void output_exception(ExceptionEvent e) {
        ObjectReference re     = e.exception();
        StringReference msgVal = (StringReference) re.getValue(re.referenceType().fieldByName("detailMessage"));
        String          s      = "";

        try {
            s = outputSingleLevelValue(getValueOfSingleRemoteCall(re, "getStackTrace", e.thread()),
                                       e.thread());
        } catch (InvalidTypeException ie) {
        } catch (ClassNotLoadedException ie) {
        } catch (IncompatibleThreadStateException ie) {
        } catch (InvocationException ie) {
        } catch (IllegalArgumentException ie) {
        }

        synchronized(this) {
            out.print(EXCEPTION_RESPONSE + "," + re.type().name() + ",");
            outputLocation(e.catchLocation());
            out.println(",(" + s + ")");
        }}

    public final void output_error (String error) {
        out.println("\n" + ERROR_RESPONSE + "," + error);}

    public final void output_breakpointList (List<BreakpointRequest> bp) {

        BreakpointRequest [] bps = new BreakpointRequest [1000];

        for (BreakpointRequest b : bp)  {
            bps [((Integer) b.getProperty(NumberProperty)).intValue()] = b;
        }

        out.print(BREAKPOINTLIST_RESPONSE);

        for (int i = 0; i != bps.length; i++)   {

            if (bps [i] != null && bps [i].isEnabled()) {

                out.print("," + i);
                outputLocation(bps [i].location());
            }
        }

        out.print("\n");
    }

    public void output_internalException(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter   pw = new PrintWriter(sw);

        t.printStackTrace(pw);

        out.println("\n" + INTERNALEXCEPTION_RESPONSE
                    + "," + t.toString() + "," + sw.toString().replace('\n', '\t'));
    }

    private void outputLocation(Location loc) {

        if (loc == null) {

            out.print(",,,");
            return;
        }

        String refName = loc.declaringType().name();
        int    iDot    = refName.lastIndexOf('.');
        String pkgName = (iDot >= 0) ? refName.substring(0, iDot+1) : "";

        out.print(", " + pkgName.replace('.', File.separatorChar));

        try {
            out.print(loc.sourceName() + "," + loc.lineNumber() + "," + loc.method().name());
        } catch (AbsentInformationException e) {
            out.print(",,");
        }
    }

    private void outputThreadReference(ThreadReference tr) {

        if (tr == null) {

            out.print(",,,,,,");
            return;
        }

        out.print("," + tr.uniqueID()
                  + ","
                  + tr.name()
                  + ",");

        switch(tr.status()) {

        case ThreadReference.THREAD_STATUS_MONITOR:
            out.print("monitor,");
            break;

        case ThreadReference.THREAD_STATUS_NOT_STARTED:
            out.print("notStarted,");
            break;

        case ThreadReference.THREAD_STATUS_RUNNING:
            out.print("running,");
            break;

        case ThreadReference.THREAD_STATUS_SLEEPING:
            out.print("sleeping,");
            break;

        case ThreadReference.THREAD_STATUS_UNKNOWN:
        default:
            out.print("unknown,");
            break;

        case ThreadReference.THREAD_STATUS_WAIT:
            out.print("waiting,");
            break;

        case ThreadReference.THREAD_STATUS_ZOMBIE:
            out.print("zombie,");
            break;
        }

        try {
            out.print(tr.frameCount());
        } catch (com.sun.jdi.IncompatibleThreadStateException e)  {}

        out.print(","
                  + tr.isAtBreakpoint()
                  + ","
                  + tr.isSuspended());
    }

    private String outputSingleLevelValue(Value v, ThreadReference tr) {
        String [] refs = new String [] {"*"};
        return outputValue(v, tr, refs, 0);
    }

    private String outputValue(Value v, ThreadReference tr, String [] refs, int depth) {

        StringBuilder sb = new StringBuilder();

        if (depth >= refs.length) {
            return "";
        }

        if (v == null) {
            sb.append("\"null\"");
        }

        // unhandled value types

        else if (v instanceof ClassLoaderReference) {
            sb.append("unsupported value type,ClassLoaderReference");
        }
        else if (v instanceof ClassObjectReference) {
            sb.append("unsupported value type,ClassObjectReference");
        }
        else if (v instanceof ThreadGroupReference) {
            sb.append("unsupported value type,ThreadGroupReference");
        }

        // primitive value types

        else if (v instanceof ThreadReference) {
            sb.append("\"" + Long.toString(((ThreadReference) v).uniqueID()) + "\"");
        }

        else if (v instanceof VoidValue) {
            sb.append("VoidValue");
        }

        else if (v instanceof BooleanValue) {
            sb.append("\"" + Boolean.toString(((BooleanValue) v).value()) + "\"");
        }
        else if (v instanceof ByteValue) {
            sb.append("\"" + Byte.toString(((ByteValue) v).value()) + "\"");
        }
        else if (v instanceof CharValue) {
            sb.append("\"" + Character.toString(((CharValue) v).value()) + "\"");
        }
        else if (v instanceof DoubleValue) {
            sb.append("\"" + Double.toString(((DoubleValue) v).value()) + "\"");
        }
        else if (v instanceof FloatValue) {
            sb.append("\"" + Float.toString(((FloatValue) v).value()) + "\"");
        }
        else if (v instanceof IntegerValue) {
            sb.append("\"" + Integer.toString(((IntegerValue) v).value()) + "\"");
        }
        else if (v instanceof LongValue) {
            sb.append("\"" + Long.toString(((LongValue) v).value()) + "\"");
        }
        else if (v instanceof ShortValue) {
            sb.append("\"" + Short.toString(((ShortValue) v).value()) + "\"");
        }
        else if (v instanceof StringReference) {
            sb.append("\"" + ((StringReference) v).value() + "\"");
        }

        // compound value types

        else if (v instanceof ArrayReference)  {

            printArray(sb, (ArrayReference) v, v.type().name(), tr, refs, depth);
        }

        else if ((v.type() instanceof ReferenceType) && (v.type() instanceof ClassType))  {

            List<InterfaceType>  it   = ((ClassType)  v.type()).allInterfaces();

            // look inside lists and maps

            for (InterfaceType i : it) {

                if (i.name().equals("java.util.List")) {

                    printList(sb, ((ObjectReference) v), v.type().name(), tr, refs, depth);
                    return sb.toString();
                }

                if (i.name().equals("java.util.Map")) {
                    mapToString(sb, (ObjectReference) v, tr, refs, depth);
                    return sb.toString();
                }
            }

           // It's just an ordinary object

            List<Field>	  fld = ((ClassType) v.type()).allFields();

            sb.append("(\"type\" \"" + v.type().name() + "\" )  ( \"fields\" ");

            for (Field f : fld) {

                if (refs [depth].equals("*") || refs [depth].equals(f.name())) {
                    sb.append("( \""
                              + f.name()
                              + "\" "
                              + outputValue(((ObjectReference) v).getValue(f), tr, refs, depth + 1)
                              + " )");
                }
            }

            sb.append(") ");
        }

        else {
            sb.append("unknown value type");
        }

        return sb.toString();
    }

    private void  printArray(StringBuilder sb, ArrayReference arrayReference, String ty, ThreadReference tr, String [] refs, int depth) {
        sb.append("( \"type\" \"" + ty + "\" ) ");

        int    size   = arrayReference.length();
        int [] bounds = arrayListPreamble(sb, size, refs, depth);

        for (int i = bounds [0]; i < bounds [1]; i++) {
            sb.append("( \""
                      + i
                      + "\" "
                      + outputValue(arrayReference.getValue(i), tr, refs, depth+1)
                      + ")");
        }

        sb.append(" ) ");
    }

    private void  printList(StringBuilder sb, ObjectReference listReference, String ty, ThreadReference tr, String [] refs, int depth) {

        sb.append("( \"type\" \"" + ty + "\" ) ");

        StringBuilder nb = new StringBuilder();

        try {
            int size = ((IntegerValue) getValueOfSingleRemoteCall(listReference, "size", tr)).value();

            int [] bounds = arrayListPreamble(nb,
                                              size,
                                              refs,
                                              depth);

            ArrayList<Value> emptyList = new ArrayList<Value> ();
            int              k         = 0;

            emptyList.add(vm.mirrorOf(k));
            Method           get       = remoteMethod(listReference,
                                                      "get",
                                                      emptyList);

            for (int i = bounds [0]; i < bounds [1]; i++) {

                ArrayList<Value> indexList = new ArrayList<Value> ();
                indexList.add(vm.mirrorOf(i));

                nb.append("( \""
                          + i
                          + "\" "
                          + outputValue(invokeRemoteMethod(listReference, get, indexList, tr),
                                        tr,
                                        refs,
                                        depth + 1)
                          + ")");
            }
        } catch (InvalidTypeException e) { nb = new StringBuilder();output_log(e.toString());
        } catch (ClassNotLoadedException e) { nb = new StringBuilder();output_log(e.toString());
        } catch (IncompatibleThreadStateException e) { nb = new StringBuilder();output_log(e.toString());
        } catch (InvocationException e) { nb = new StringBuilder();output_log(e.toString());
        } catch (IllegalArgumentException e) { nb = new StringBuilder();output_log(e.toString()); }

        sb.append(nb.toString() + " ) ");
    }

    private int [] arrayListPreamble(StringBuilder sb, int size, String [] refs, int depth) {

        int bottom = 0;
        int top    = (20 > size) ? size : 20;

        sb.append(" ( \"size\"  \"" + size + "\") ( \"contents\" ");

        if (!refs [depth].equals("*")) {

            try {
                String [] s = refs [depth].split("-");

                if (s.length > 0) {
                    bottom = Integer.parseInt(s [0]);
                    if (bottom >= size) {
                        bottom = size - 1;
                    }
                }

                if (s.length > 1) {
                    top = Integer.parseInt(s [1]);
                    if (top > size) {
                        top = size;
                    }
                } else {
                    top = bottom + 1;
                }
            } catch (NumberFormatException e) {
                bottom = 0;
                top    = (20 > size) ? size : 20;
            }
        }

        int [] ret = new int [2];

        ret [0] = bottom;
        ret [1] = top;

        return ret;
    }

    private void mapToString(StringBuilder sb, ObjectReference mapReference, ThreadReference tr, String [] refs, int depth) {

        int rparen = 0;
        sb.append("( \"type\" \"Map\" )");

        try {
            // get the map's size

            int size = ((IntegerValue) getValueOfSingleRemoteCall(mapReference, "size", tr)).value();

            sb.append("(\"size\" \""
                      + Integer.toString(size)
                      + "\") (\"contents\" ");rparen++;

            // keys could come from the local command or from the keyset

            ArrayList<Value> keys = new ArrayList<Value> ();

            if (!refs [depth].equals("*"))

                {
                    for (String s : refs [depth].split(":")) {
                        keys.add(tr.virtualMachine().mirrorOf(s));
                    }
                }

            else

                {
                    // Get iterator of map's keys

                    Value keySetIterator = getValueOfSingleRemoteCall((ObjectReference) getValueOfSingleRemoteCall(mapReference, "keySet", tr),
                                                                      "iterator",
                                                                      tr);

                    // oddly enough, remoteMethod won't work without an appropriate
                    // argument in the keyList

                    ArrayList<Value> emptyList = new ArrayList<Value> ();
                    Method           next      = remoteMethod(((ObjectReference) keySetIterator),
                                                              "next",
                                                              emptyList);

                    for (int i = 0; i != ((size > 20) ? 20 : size); i++) {

                        keys.add(invokeRemoteMethod(((ObjectReference) keySetIterator),
                                                    next,
                                                    emptyList,
                                                    tr));
                    }
                }

            if (keys.size() != 0) {
                ArrayList<Value> kl = new ArrayList<Value> ();
                kl.add(keys.get(0));
                Method get = remoteMethod (mapReference, "get", kl);

                // here come the contents of the map

                for (Value v : keys) {

                    ArrayList<Value> keyList = new ArrayList<Value> ();
                    keyList.add(v);

                    sb.append("( ");rparen++;

                    sb.append(" "
                              + outputSingleLevelValue(v, tr)
                              + " "
                              + outputValue(invokeRemoteMethod(mapReference, get, keyList, tr),
                                            tr,
                                            refs,
                                            depth + 1));

                    sb.append(" )");rparen--;
                }
            }
        } catch (InvalidTypeException e) {
        } catch (ClassNotLoadedException e) {
        } catch (IncompatibleThreadStateException e) {
        } catch (InvocationException e) {
        } catch (IllegalArgumentException e) {
        }

        while (rparen != 0) {sb.append(")");rparen--;}
    }

    private Value getValueOfSingleRemoteCall(ObjectReference objectReference,
                                             String methodName,
                                             ThreadReference tr) throws
                                                 InvalidTypeException,
                                                 ClassNotLoadedException,
                                                 IncompatibleThreadStateException,
                                                 InvocationException,
                                                 IllegalArgumentException
    {
        return(invokeRemoteMethod(objectReference,
                                  remoteMethod(objectReference, methodName, new ArrayList<Value> ()),
                                  new ArrayList<Value> (),
                                  tr));
    }

    private Value invokeRemoteMethod(ObjectReference o,
                                     Method          m,
                                     List<Value>     arguments,
                                     ThreadReference tr) throws
                                         InvalidTypeException,
                                         ClassNotLoadedException,
                                         IncompatibleThreadStateException,
                                         InvocationException
    {
        return o.invokeMethod(tr, m, arguments, 0);
    }

    private Method remoteMethod (ObjectReference o, String name, List<Value> arguments) throws
        ClassNotLoadedException,
        IllegalArgumentException
    {
        Method       m       = null;
        List<Method> methods = ((ReferenceType) o.type()).methodsByName(name);

        for (Method mm : methods)  {

            List<Type>        argumentTypes    = mm.argumentTypes();
            ARGUMENT_MATCHING argumentMatching = argumentsMatching(argumentTypes, arguments);

            if (argumentMatching == ARGUMENT_MATCHING.MATCH) {

                return mm;
            }

            if (argumentMatching == ARGUMENT_MATCHING.ASSIGNABLE) {

                if (m != null) {
                    throw new IllegalArgumentException("Multiple methods with name " + mm.name() + " matched to specified arguments.");
                }

                m = mm;
            }
        }

        if (m !=null)

            return m;

        throw new IllegalArgumentException("method " + name + " not found");
    }

    private enum ARGUMENT_MATCHING {
        MATCH, ASSIGNABLE, NOT_MATCH
    }

    private ARGUMENT_MATCHING argumentsMatching(List<Type> argumentTypes, List<Value> arguments) throws ClassNotLoadedException  {

        if (argumentTypes.size() != arguments.size()) {
            return ARGUMENT_MATCHING.NOT_MATCH;
        }

        Iterator<Value> argumentIterator = arguments.iterator();
        Iterator<Type> argumentTypesIterator = argumentTypes.iterator();
        ARGUMENT_MATCHING result = ARGUMENT_MATCHING.MATCH;

        while (argumentIterator.hasNext())  {

            Value argumentValue = argumentIterator.next();
            Type argumentType   = argumentTypesIterator.next();

            if (argumentValue == null && argumentType instanceof PrimitiveValue) {
                return ARGUMENT_MATCHING.NOT_MATCH;
            }

            if (argumentValue != null
                && !(argumentValue.type().equals(argumentType))
                && !isAssignable(argumentValue.type(), argumentType)) {
                return ARGUMENT_MATCHING.NOT_MATCH;
            }

            if (argumentValue != null
                && !(argumentValue.type().equals(argumentType))) {
                result = ARGUMENT_MATCHING.ASSIGNABLE;
            }
        }

        return result;
    }

    private boolean isAssignable(Type from, Type to) throws ClassNotLoadedException {
        if (from.equals(to))  {
            return true;
        }
        if (from instanceof BooleanType)  {
            return to instanceof BooleanType;
        }
        if (to instanceof BooleanType) {
            return false;
        }
        if (from instanceof PrimitiveType) {
            return to instanceof PrimitiveType;
        }
        if (to instanceof PrimitiveType)   {
            return false;
        }

        if (from instanceof ArrayType && !(to instanceof ArrayType)) {
            return to.name().equals("java.lang.Object");
        }

        if (from instanceof ArrayType)  {
            Type fromArrayComponent = ((ArrayType)from).componentType();
            Type toArrayComponent    = ((ArrayType)to).componentType();

            if (fromArrayComponent instanceof PrimitiveType) {
                return fromArrayComponent.equals(toArrayComponent);
            }

            return !(toArrayComponent instanceof PrimitiveType) && isAssignable(fromArrayComponent, toArrayComponent);
        }

        if (from instanceof ClassType)  {
            ClassType superClass = ((ClassType)from).superclass();

            if (superClass != null && isAssignable(superClass, to))  {
                return true;
            }

            for (InterfaceType interfaceType : ((ClassType)from).interfaces()) {
                if (isAssignable(interfaceType, to)) {
                    return true;
                }
            }
        }

        if (from instanceof InterfaceType)   {
            for (InterfaceType interfaceType : ((InterfaceType)from).subinterfaces()) {

                if (isAssignable(interfaceType, to)) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
      When we invoke a method on the target virtual machine, we unsuspend all
      the threads on that machine for an instant. If one of those threads hits a
      breakpoint, a breakpoint event will be posted to the event queue and
      received by the EventReader thread at the same time we are waiting for the
      result of the method on the target machine, causing a deadlock. So, we
      disable all event requests before invoking a method. This will cause us to
      miss the breakpoint event but it's unlikely that we will get into this
      situation (although we did in testing) and there is, in any case, nothing
      we can do (we could try interrupting the EventReader thread or polling the
      event queue but decided not to on the grounds that it is too complex and
      artificial).
    */

    private List<EventRequest> toEnable = null;

    private void disableAllRequests() {

        EventRequestManager erm       = vm.eventRequestManager();
        List<EventRequest>  toDisable = new ArrayList<EventRequest>();

        toEnable = new ArrayList<EventRequest>();

        toDisable.addAll(erm.accessWatchpointRequests());
        toDisable.addAll(erm.breakpointRequests());
        toDisable.addAll(erm.classPrepareRequests());
        toDisable.addAll(erm.classUnloadRequests());
        toDisable.addAll(erm.exceptionRequests());
        toDisable.addAll(erm.methodEntryRequests());
        toDisable.addAll(erm.methodExitRequests());
        toDisable.addAll(erm.modificationWatchpointRequests());
        toDisable.addAll(erm.monitorContendedEnteredRequests());
        toDisable.addAll(erm.monitorContendedEnterRequests());
        toDisable.addAll(erm.monitorWaitedRequests());
        toDisable.addAll(erm.monitorWaitRequests());
        toDisable.addAll(erm.stepRequests());
        toDisable.addAll(erm.threadDeathRequests());
        toDisable.addAll(erm.threadStartRequests());
        toDisable.addAll(erm.vmDeathRequests());

        for (Iterator<EventRequest> it = toDisable.iterator();
             it.hasNext();) {

            EventRequest e = it.next();

            if (e.isEnabled()) {
                e.setEnabled(false);
                toEnable.add(e);
            }
        }
    }

    private void renableAllRequests() {

        if (toEnable != null) {

            Iterator<EventRequest>  it = toEnable.iterator();

            while (it.hasNext()) {
                it.next().setEnabled(true);
            }

            toEnable = null;
        }
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
