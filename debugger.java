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
        } finally {
            System.out.println("proxy,exit");
            if (vm != null) vm.exit(0);
            System.exit(0);
        }
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
                                    System.out.println(tr.frame(Integer.parseInt(frameid)));
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

                            System.out.println(tr.stack());

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
                                            System.out.println(tr.thises(Integer.parseInt(frameid)));
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

    public static String getValueString (ThreadReference tr, Value v)
    {
        if (v == null)

            return "\"null\"";

        if (v instanceof BooleanValue)  return "\"" + Boolean.toString(((BooleanValue) v).value()) + "\"";
        if (v instanceof ByteValue)        return "\"" + Byte.toString(((ByteValue) v).value()) + "\"";
        if (v instanceof CharValue)       return "\"" + Character.toString(((CharValue) v).value()) + "\"";
        if (v instanceof DoubleValue)    return "\"" + Double.toString(((DoubleValue) v).value()) + "\"";
        if (v instanceof FloatValue)       return "\"" + Float.toString(((FloatValue) v).value()) + "\"";
        if (v instanceof IntegerValue)    return "\"" + Integer.toString(((IntegerValue) v).value()) + "\"";
        if (v instanceof LongValue)      return "\"" + Long.toString(((LongValue) v).value()) + "\"";
        if (v instanceof ShortValue)      return "\"" + Short.toString(((ShortValue) v).value()) + "\"";
        if (v instanceof StringReference)  return "\"" + ((StringReference) v).value() + "\"";

        if (v instanceof ArrayReference)

            {
                StringBuilder     b   = new StringBuilder();
                ArrayReference av  = (ArrayReference) v;
                int                  len = (av.length() > 20 ? 20 : av.length());

                b.append("( \"type\" \"array\" )");
                b.append("( \"size\" \"" + Integer.toString(av.length()) + "\") ");
                
                if (len > 20) len = 20;

                b.append("( \"contents\" ");

                for (int i = 0; i < len; i++)
                    
                    b = b.append("( \"" + i + "\" " + getValueString(tr, av.getValue(i)) + ")");

                b.append(" ) ");
                return b.toString();
            }

        if ((v.type() instanceof ReferenceType) && (v.type() instanceof ClassType))

            {
                StringBuilder            b  = new StringBuilder();
                List<InterfaceType>  it   = ((ClassType)  v.type()).allInterfaces();

                for (InterfaceType i : it)

                    {
                        if (i.name().equals("java.util.List"))

                            {
                                b = b.append("( \"type\" \"List\" )");

                                ArrayList<Value> alv = new ArrayList<Value> ();
                                Value                vv  = invokeRemoteMethod(tr,
                                                                                             ((ObjectReference) v),
                                                                                             remoteMethod (((ReferenceType) v.type()), "size", alv),
                                                                                             alv);
                                int                    sz  = ((IntegerValue) vv).value();

                                b = b.append("( \"size\" \"" + Integer.toString(sz) + "\")");
                                if (sz > 20) sz = 20;

                                alv.add(tr.virtualMachine().mirrorOf(0));

                                Method m = remoteMethod(((ReferenceType) v.type()), "get", alv);

                                if (m != null)

                                    {
                                        b.append("( \"contents\" ");
                                        
                                        for (int j = 0; j != sz; j++)

                                            {
                                                alv.clear();
                                                alv.add(tr.virtualMachine().mirrorOf(j));
                                                b = b.append("( \""
                                                             + j
                                                             + "\" "
                                                             + getValueString(tr,
                                                                              invokeRemoteMethod (tr, ((ObjectReference) v), m, alv))
                                                             + ")");
                                            }

                                        b.append(" ) ");
                                    }
                                
                                return b.toString();
                            }

                        if (i.name().equals("java.util.Map"))

                            {
                                b = b.append("( \"type\" \"Map\" )");

                                ArrayList<Value> emptyList = new ArrayList<Value> ();
                                Value                vv  = invokeRemoteMethod(tr,
                                                                                             ((ObjectReference) v),
                                                                                             remoteMethod (((ReferenceType) v.type()), "size", emptyList),
                                                                                             emptyList);

                                if (vv != null)

                                    {
                                        int                    sz  = ((IntegerValue) vv).value();

                                        b = b.append("( \"size\" \"" + Integer.toString(sz) + "\")");

                                        if (sz != 0)

                                            {
                                                if (sz > 20) sz = 20;

                                                // set of map's keys
                                        
                                                vv  = invokeRemoteMethod(tr,
                                                                         ((ObjectReference) v),
                                                                         remoteMethod (((ReferenceType) v.type()), "keySet", emptyList),
                                                                         emptyList);

                                                if (vv != null)

                                                    {
                                                        // keysIterator is an iterator of the set of keys
                                                
                                                        Value keysIterator  = invokeRemoteMethod(tr,
                                                                                                 ((ObjectReference) vv),
                                                                                                 remoteMethod (((ReferenceType) vv.type()), "iterator", emptyList),
                                                                                                 emptyList);

                                                        if (keysIterator != null)

                                                            {
                                                                Method             next  = remoteMethod (((ReferenceType) keysIterator.type()), "next", emptyList);
                                                                Value                key  = invokeRemoteMethod(tr, ((ObjectReference) keysIterator), next, emptyList);
                                                                ArrayList<Value> keyList    = new ArrayList<Value> ();

                                                                keyList.add(key);

                                                                Method              get   = (key == null) ? null : remoteMethod (((ReferenceType) v.type()), "get", keyList);
                                                                Value                entry = invokeRemoteMethod(tr, ((ObjectReference) v), get, keyList);

                                                                b.append("( \"contents\" ");

                                                                for (int j = 0;
                                                                     j != sz && entry != null;
                                                                     j++)

                                                                    {
                                                                        b.append("( "
                                                                                 + getValueString(tr, key)
                                                                                 + " "
                                                                                 + getValueString(tr, entry)
                                                                                 + " )");

                                                                        if (j != (sz - 1))

                                                                            {
                                                                                key   = invokeRemoteMethod(tr, ((ObjectReference) keysIterator), next, emptyList);
                                                                                keyList.clear();
                                                                                keyList.add(key);                                                                                
                                                                                entry = (key == null) ? null : invokeRemoteMethod(tr, ((ObjectReference) v), get, keyList);
                                                                            }
                                                                    }

                                                                b.append(")");
                                                            }
                                                    }
                                            }
                                    }

                                return b.toString();
                            }
                    }

                List<Field>	fld = ((ClassType) v.type()).allFields();

                for (Field f : fld)

                    b = b.append("( \""
                                 + f.name()
                                 + "\" "
                                 + getValueString(tr, ((ObjectReference) v).getValue(f))
                                 + " )");
                
                b.append(") ");
                return b.toString();
            }

        else if (v instanceof ClassLoaderReference) ;
        else if (v instanceof ClassObjectReference) ;
        else if (v instanceof ThreadGroupReference) ;
        else if (v instanceof ThreadReference) ;
        else if (v instanceof VoidValue) ;

        return "";
    }

    private static Method remoteMethod (ReferenceType r, String name, List<Value> arguments)
    {
        try {
            return findMethod(r.methodsByName(name), arguments);
        } catch (ClassNotLoadedException ce) {
            System.out.println("error,ClassNotLoaded in getValueString " + ce.toString());
        }

        return null;
    }
    
    private static Value invokeRemoteMethod (ThreadReference tr, ObjectReference o, Method m, List<Value> arguments)
    {
        try {
            return (m == null) ? null : o.invokeMethod(tr, m,  arguments, 0);
        } catch (InvalidTypeException ex) {
            System.out.println("error,InvalidTypeException in getValueString " + ex.toString());
        } catch (ClassNotLoadedException ce) {
            System.out.println("error,ClassNotLoaded in getValueString " + ce.toString());
        } catch (IncompatibleThreadStateException ie) {
            System.out.println("error,IncompatibleThreadStateException in getValueString " + ie.toString());
        } catch (InvocationException te) {
            System.out.println("error,InvocationException in getValueString " + te.toString());
        }
    
        return null;
    }
    
     private static Method findMethod(List<Method> methods, List<Value> arguments) throws ClassNotLoadedException
     { 
         Method m = null;
        
         for (Method mm : methods)

             { 
                 List<Type>                   argumentTypes     = mm.argumentTypes(); 
                 ARGUMENT_MATCHING argumentMatching = argumentsMatching(argumentTypes, arguments); 

                 if (argumentMatching == ARGUMENT_MATCHING.MATCH)

                     return mm;

                 if (argumentMatching == ARGUMENT_MATCHING.ASSIGNABLE)

                     { 
                         if (m != null)

                             { 
                                 System.out.println("error,Multiple methods with name " + mm.name() + " matched to specified arguments. ");
                                 return null;
                             }

                         m = mm;
                     } 
             }
        
         return m; 
     }
    
    private enum ARGUMENT_MATCHING { 
        MATCH, ASSIGNABLE, NOT_MATCH 
    } 

     private static ARGUMENT_MATCHING argumentsMatching(List<Type> argumentTypes, List<Value> arguments) throws ClassNotLoadedException
    { 
         if (argumentTypes.size() != arguments.size())

             return ARGUMENT_MATCHING.NOT_MATCH;
        
         Iterator<Value> argumentIterator = arguments.iterator(); 
         Iterator<Type> argumentTypesIterator = argumentTypes.iterator(); 
         ARGUMENT_MATCHING result = ARGUMENT_MATCHING.MATCH; 

         while (argumentIterator.hasNext())

             { 
                 Value argumentValue = argumentIterator.next(); 
                 Type argumentType   = argumentTypesIterator.next(); 

                 if (argumentValue == null && argumentType instanceof PrimitiveValue)

                     return ARGUMENT_MATCHING.NOT_MATCH; 

                 if (argumentValue != null
                     && !(argumentValue.type().equals(argumentType))
                     && !isAssignable(argumentValue.type(), argumentType))

                     return ARGUMENT_MATCHING.NOT_MATCH;

                 if (argumentValue != null
                     && !(argumentValue.type().equals(argumentType)))

                     result = ARGUMENT_MATCHING.ASSIGNABLE;
             } 

         return result; 
     } 
 
    private static boolean isAssignable(Type from, Type to) throws ClassNotLoadedException
    {
        if (from.equals(to))                     return true; 
        if (from instanceof BooleanType)  return to instanceof BooleanType; 
        if (to instanceof BooleanType)      return false; 
        if (from instanceof PrimitiveType)  return to instanceof PrimitiveType; 
        if (to instanceof PrimitiveType)     return false; 
        
        if (from instanceof ArrayType && !(to instanceof ArrayType))
            
            return to.name().equals("java.lang.Object");
        
        if (from instanceof ArrayType)

            { 
                Type fromArrayComponent = ((ArrayType)from).componentType(); 
                Type toArrayComponent    = ((ArrayType)to).componentType(); 
                
                if (fromArrayComponent instanceof PrimitiveType)  return fromArrayComponent.equals(toArrayComponent); 

                return !(toArrayComponent instanceof PrimitiveType) && isAssignable(fromArrayComponent, toArrayComponent); 
            } 

        if (from instanceof ClassType)

            {
                ClassType superClass = ((ClassType)from).superclass(); 
            
                if (superClass != null && isAssignable(superClass, to))  return true; 

                for (InterfaceType interfaceType : ((ClassType)from).interfaces()) { 
                    if (isAssignable(interfaceType, to)) return true;
                } 
            }

        if (from instanceof InterfaceType)

            {
                for (InterfaceType interfaceType : ((InterfaceType)from).subinterfaces()) { 
                
                    if (isAssignable(interfaceType, to)) return true;
                } 
            } 
 
        return false; 
    }
 }
