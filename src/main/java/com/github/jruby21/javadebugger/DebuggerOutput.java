package com.github.jruby21.javadebugger;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
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
import com.sun.jdi.Location;
import com.sun.jdi.LocalVariable;
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
import com.sun.jdi.VoidValue;

import com.sun.jdi.event.ExceptionEvent;

import com.sun.jdi.request.BreakpointRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class DebuggerOutput {

    private final static String ACCESSWATCHPOINT_RESPONSE          = "accesswatchpoint";
    private final static String ACCESSWATCHPOINTSET_RESPONSE       = "accesswatchpointset";
    private final static String ARGUMENTS_RESPONSE                 = "arguments";
    private final static String BREAKPOINTCLEARED_RESPONSE         = "breakpointcleared";
    private final static String BREAKPOINTCREATED_RESPONSE         = "breakpointcreated";
    private final static String BREAKPOINTENTERED_RESPONSE         = "breakpointentered";
    private final static String BREAKPOINTLIST_RESPONSE            = "breakpointlist";
    private final static String CATCHENABLED_RESPONSE              = "catchenabled";
    private final static String CLASSES_RESPONSE                   = "classes";
    private final static String CLASSPREPARED_RESPONSE             = "classprepared";
    private final static String CLASSUNLOADED_RESPONSE             = "classunloaded";
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

    PrintStream out = null;

    public DebuggerOutput(PrintStream o) {
        out = o;
    }

    public final void output_vmStarted ( ) {
        out.println(VMSTARTED_RESPONSE);}

    public final void output_vmResumed ( ) {
        out.println(VMRESUMED_RESPONSE);}

    public final void output_vmDisconnected ( ) {
        out.println(VMDISCONNECTED_RESPONSE);}

    public final void output_vmDied () {
        out.println(VMDIED_RESPONSE);}

    public final void output_vmCreated ( ) {
        out.println(VMCREATED_RESPONSE);}

    public final void output_variable(String name, Value value, ThreadReference tr, String [] refs) {
        out.print("(\"" + name + "\" ");
        outputValue(value, tr, refs, 1);
        out.print(" ) ");
    }

    public final void output_threadStarted (ThreadReference tr) {
        out.print(THREADSTARTED_RESPONSE);
        outputThreadReference(tr);
        outputEnd();}

    public final void output_threadList ( ) {
        out.print(THREADLIST_RESPONSE);}

    public final void output_threadDied (ThreadReference tr) {
        out.print(THREADDIED_RESPONSE);
        outputThreadReference(tr);
        outputEnd();}

    public final void output_this (String thread, String frame, ObjectReference th, ThreadReference tr, String [] refs) {
        out.print(THIS_RESPONSE + "," + thread + "," + frame +",(");
        outputValue(th, tr, refs, 1);
        out.println(")");}

    public final void output_stepCreated ( ) {
        out.println(STEPCREATED_RESPONSE);}

    public final void output_step ( ThreadReference tr, Location loc) {
        out.print(STEP_RESPONSE);
        outputThreadReference(tr);
        outputLocation(loc);
        outputEnd();}
    public final void output_stack (String threadId ) {
        out.print(STACK_RESPONSE + "," + threadId);}
    public final void output_proxyStarted ( ) {
        out.println(PROXYSTARTED_RESPONSE);}
    public final void output_proxyExited ( ) {
        out.println(PROXYEXITED_RESPONSE);}
    public final void output_preparingClass (String className) {
        out.println(PREPARINGCLASS_RESPONSE + "," + className);}
    public final void output_modificationWatchpoint( ObjectReference object, Field field, Value past, Value future, ThreadReference tr) {
        out.print(MODIFICATIONWATCHPOINT_RESPONSE + "," + object.referenceType().name() + "," + field.name() + ",(");
        outputValue(past, tr, new String [0], 0);
        out.print("),(");
        outputValue(future, tr, new String [0], 0);
        out.println(")");
    }
    public final void output_modificationWatchpointSet ( ) {
        out.println(MODIFICATIONWATCHPOINTSET_RESPONSE);}
    public final void output_log(String msg) {
        out.println("log," + msg); }
    public final void output_local(String thread, String frame) {
        out.print(LOCALS_RESPONSE +"," + thread + "," + frame +",("); }
    public final void output_fields (String cl, List<Field> fields) {
        out.print(FIELDS_RESPONSE + "," + cl);
         for (Field f : fields) {
             outputField(f);
         }
         out.print("\n");
     }

    public final void output_exception(ExceptionEvent e) {
        ObjectReference re     = e.exception();
        StringReference msgVal = (StringReference) re.getValue(re.referenceType().fieldByName("detailMessage"));

        out.print(EXCEPTION_RESPONSE + "," + re.type().name());
        outputLocation(e.catchLocation());
        out.print("," + (msgVal == null ? "" : msgVal.value()) + ",(");

        try {
            outputValue(getValueOfSingleRemoteCall(re, "getStackTrace", e.thread()),
                        e.thread(),
                        new String [0],
                        0);
        } catch (InvalidTypeException ie) {
        } catch (ClassNotLoadedException ie) {
        } catch (IncompatibleThreadStateException ie) {
        } catch (InvocationException ie) {
        } catch (IllegalArgumentException ie) {
        }

        out.println(")");}

    public final void output_error (String error) {
        out.println("\n" + ERROR_RESPONSE + "," + error);}

    public final void output_classUnloaded ( String className) {
        out.println(CLASSUNLOADED_RESPONSE + "," + className);}

    public final void output_classPrepared ( String className) {
        out.println(CLASSPREPARED_RESPONSE + "," + className);}

    public final void output_classes (List<ReferenceType> refs) {
        out.print(CLASSES_RESPONSE);
        for (ReferenceType r : refs) {
            out.print("," + r.name());
        }
        out.print("\n");
    }

    public final void output_catchEnabled(boolean enable) {
        out.println(CATCHENABLED_RESPONSE + "," + enable);}

    public final void output_breakpointList (List<BreakpointRequest> bp) {

        BreakpointRequest [] bps = new BreakpointRequest [1000];

        for (BreakpointRequest b : bp)  {
            bps [((Integer) b.getProperty(JavaDebuggerProxy.NumberProperty)).intValue()] = b;
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

    public final void output_breakpointEntered ( int breakId, ThreadReference tr, Location loc) {
        out.print(BREAKPOINTENTERED_RESPONSE + "," + Integer.toString(breakId));
        outputThreadReference(tr);
        outputLocation(loc);
        outputEnd();}

    public final void output_breakpointCreated ( int breakId, Location loc) {
        out.print(BREAKPOINTCREATED_RESPONSE + "," + Integer.toString(breakId));
        outputLocation(loc);
        outputEnd();}

    public final void output_breakpointCleared ( int breakId) {
        out.println(BREAKPOINTCLEARED_RESPONSE + "," + Integer.toString(breakId));}

    public final void output_arguments(String thread, String frame) {
        out.print(ARGUMENTS_RESPONSE +"," + thread + "," + frame +",("); }

    public final void output_endargument() {
        out.println(")"); }

    public final void output_accessWatchpointSet ( ) {
        out.println(ACCESSWATCHPOINTSET_RESPONSE);}

    public final void output_accessWatchpoint ( ObjectReference object, Field field, Value value, ThreadReference tr) {
        out.print(ACCESSWATCHPOINT_RESPONSE + "," + object.referenceType().name() + "," + field.name() + ",(");
        outputValue(value, tr, new String [0], 0);
        out.println(")");
    }

    public final void output_integer(int i) {
        out.print("," + i);}

    public final void outputEnd() {
        out.print("\n");}

    void close() {
        out.flush();
        out.close(); }

    public void output_internalException(Throwable t)
    {
        StringWriter   sw = new StringWriter();
        PrintWriter     pw = new PrintWriter(sw);

        t.printStackTrace(pw);

        out.println("\n" + INTERNALEXCEPTION_RESPONSE
                    + "," + t.getMessage() + "," + sw.toString().replace('\n', '\t'));
    }

    public void outputLocation(Location loc) {

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


    public void outputField(Field f) {

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

    public void outputThreadReference(ThreadReference tr) {

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

    private void outputValue(Value v, ThreadReference tr, String [] refs, int depth) {

        if (depth > 20) {
            return;
        }

        if (v == null) {
            out.print("\"null\"");
        }

        // unhandled value types

        else if (v instanceof ClassLoaderReference) {
            out.print("unsupported value type,ClassLoaderReference");
        }
        else if (v instanceof ClassObjectReference) {
            out.print("unsupported value type,ClassObjectReference");
        }
        else if (v instanceof ThreadGroupReference) {
            out.print("unsupported value type,ThreadGroupReference");
        }
        else if (v instanceof ThreadReference) {
            out.print("unsupported value type,ThreadReference");
        }
        else if (v instanceof VoidValue) {
            out.print("unsupported value type,VoidValue");
        }

        // primitive value types

        else if (v instanceof BooleanValue) {
            out.print("\"" + Boolean.toString(((BooleanValue) v).value()) + "\"");
        }
        else if (v instanceof ByteValue) {
            out.print("\"" + Byte.toString(((ByteValue) v).value()) + "\"");
        }
        else if (v instanceof CharValue) {
            out.print("\"" + Character.toString(((CharValue) v).value()) + "\"");
        }
        else if (v instanceof DoubleValue) {
            out.print("\"" + Double.toString(((DoubleValue) v).value()) + "\"");
        }
        else if (v instanceof FloatValue) {
            out.print("\"" + Float.toString(((FloatValue) v).value()) + "\"");
        }
        else if (v instanceof IntegerValue) {
            out.print("\"" + Integer.toString(((IntegerValue) v).value()) + "\"");
        }
        else if (v instanceof LongValue) {
            out.print("\"" + Long.toString(((LongValue) v).value()) + "\"");
        }
        else if (v instanceof ShortValue) {
            out.print("\"" + Short.toString(((ShortValue) v).value()) + "\"");
        }
        else if (v instanceof StringReference) {
            out.print("\"" + ((StringReference) v).value() + "\"");
        }

        // compound value types

        else if (v instanceof ArrayReference)  {
            printArray((ArrayReference) v, "array", tr, refs, depth++);
        }

        else if ((v.type() instanceof ReferenceType) && (v.type() instanceof ClassType))  {

            List<InterfaceType>  it   = ((ClassType)  v.type()).allInterfaces();

            // look inside lists and maps

            for (InterfaceType i : it) {

                if (i.name().equals("java.util.List")) {

                    try {
                        printArray((ArrayReference) getValueOfSingleRemoteCall(((ObjectReference) v), "toArray", tr),
                                   "list",
                                   tr,
                                   refs,
                                   depth++);
                    } catch (InvalidTypeException e) {
                    } catch (ClassNotLoadedException e) {
                    } catch (IncompatibleThreadStateException e) {
                    } catch (InvocationException e) {
                    } catch (IllegalArgumentException e) {
                    }

                    return;
                }

                if (i.name().equals("java.util.Map")) {
                    mapToString((ObjectReference) v, tr, refs, depth++);
                    return;
                }
            }

            // It's just an ordinary object

            List<Field>	  fld = ((ClassType) v.type()).allFields();

            out.print("(\"type\" \"" + v.type().name() + "\" )  ( \"fields\" ");
            depth++;

            for (Field f : fld) {

                if (depth >= refs.length
                    && (refs [depth].equals("*") || refs [depth].equals(f.name()))) {
                    out.print("( \"" + f.name() + "\" ");
                    outputValue(((ObjectReference) v).getValue(f), tr, refs, depth + 1);
                    out.print(" )");
                }
            }

            out.print(") ");
        }

        else {
            out.print("unknown value type");
        }
    }

    private void  printArray(ArrayReference arrayReference, String ty, ThreadReference tr, String [] refs, int depth) {

        out.print("( \"type\" \"" + ty + "\" ) ( \"size\"  \"" + Integer.toString(arrayReference.length()) + "\") ( \"contents\" ");

        int [] bounds = arrayListBounds(arrayReference.length(), refs, depth);

        for (int i = bounds [0]; i < bounds [1]; i++) {
            out.print("( \"" + i + "\" ");
            outputValue(arrayReference.getValue(i), tr, refs, depth+1);
            out.print(")");
        }

        out.print(" ) ");
    }

    private int [] arrayListBounds(int length, String refs [], int depth)
    {
        int [] bounds = new int [2];

        bounds [0] = 0;
        bounds [1]  = (length  > 20 ? 20 : length);

        try {
            if (depth < refs.length) {
                String [] s = refs [depth].split("-");
                if (s.length > 0) {
                    bounds [0] = Integer.parseInt(s [0]);
                    bounds [1] = (s.length > 1) ?  Integer.parseInt(s [1]) : bounds [0] + 1;
                }
            }
        } catch (NumberFormatException e) {
        }

        return bounds;
    }

    private void mapToString(ObjectReference mapReference, ThreadReference tr, String [] refs, int depth) {

        int rparen = 0;
        out.print("( \"type\" \"Map\" )");

        try {
            // get the map's size

            int size = ((IntegerValue) getValueOfSingleRemoteCall(mapReference, "size", tr)).value();

            out.print("(\"size\" \""
                      + Integer.toString(size)
                      + "\") (\"contents\" ");rparen++;

            // keys could come from the local command or from the keyset

            ArrayList<Value> keys = new ArrayList<Value> ();

            if (depth < refs.length)

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

                    out.print("( ");rparen++;
                    outputValue(v, tr, new String [0], 0);
                    out.print(" ");

                    ArrayList<Value> keyList = new ArrayList<Value> ();
                    keyList.add(v);

                    outputValue(invokeRemoteMethod(mapReference, get, keyList, tr),
                                tr,
                                refs,
                                depth++);

                    out.print(" )");rparen--;
                }
            }
        } catch (InvalidTypeException e) {
        } catch (ClassNotLoadedException e) {
        } catch (IncompatibleThreadStateException e) {
        } catch (InvocationException e) {
        } catch (IllegalArgumentException e) {
        }

        while (rparen != 0) {out.print(")");rparen--;}
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

    private Value invokeRemoteMethod (ObjectReference o,
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

            List<Type>                       argumentTypes     = mm.argumentTypes();
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
}
