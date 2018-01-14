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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class DebuggerFrame
{
    int                                  frameNumber = 0;
    Location                          loc        = null;
    List<LocalVariable>           vars       = null;
    Map<LocalVariable, Value> values     = null;
    ObjectReference               thises     = null; 
    String                            error       = null;

    private                           ThreadReference threadReference;

    public DebuggerFrame(ThreadReference tr, int fn)
    {
        threadReference = tr;
        frameNumber     = fn;
            
        if (!tr.isSuspended())

            error = "error,not suspended,thread," + tr.uniqueID();

        else

            {
                try
                    {
                        int              framecount = tr.frameCount();
                        StackFrame  sf             = null;

                        if (fn < 0 || fn >= framecount)

                            error = "error,no frame number, " + fn + ",thread," + tr.uniqueID();

                        else if (null == (sf = tr.frame(fn)))

                            error = "error,no frame," + fn + ",thread," + tr.uniqueID();

                        else

                            {
                                loc = sf.location();
                                vars   = sf.visibleVariables();
                                if (vars != null) values = sf.getValues(vars);
                                thises = sf.thisObject();
                            }
                    } catch (InvalidStackFrameException e)  {
                    error = "error,not suspended,thread," + tr.uniqueID();
                } catch (IncompatibleThreadStateException e) {
                    error = "error,not suspended,thread," + tr.uniqueID();;
                } catch (IndexOutOfBoundsException e) {
                    error = "error,no frame number, " + fn + ",thread," + tr.uniqueID();
                } catch (AbsentInformationException e) {
                    error = "error,absent information, " + fn + ",thread," + tr.uniqueID();;
                }
            }
    }

    public String showLocation()    { return  ((loc == null) ? "no location available" : (new DebuggerLocation(loc)).toString()); }
    public String showThis()          { return (error != null) ? error :  "(" + getValueString(thises) + ")"; }
    public String showLocals()       { return (error != null) ? error : "(" + showVars(false) + ")"; }
    public String showArguments() {return (error != null) ? error : "(" + showVars(true) + ")"; }

    private String showVars(boolean argument)
    {
        StringBuilder b = new StringBuilder();

        if (vars != null)

            {
                for (LocalVariable var : vars)

                    {
                        if ((argument && var.isArgument())
                            || (!argument && !var.isArgument()))

                            b.append(" (\""
                                     + var.name()
                                     + "\" "
                                     + ((values == null) ? "null" : getValueString(values.get(var)))
                                     +")");
                    }
            }

        return b.toString();
    }
    
    private String getValueString(Value v)
    {
        if (v == null)

            return "\"null\"";

        if (v instanceof ClassLoaderReference)  return "error,unsupported value type,ClassLoaderReference";
        if (v instanceof ClassObjectReference)   return "error,unsupported value type,ClassObjectReference";
        if (v instanceof ThreadGroupReference) return "error,unsupported value type,ThreadGroupReference";
        if (v instanceof ThreadReference)         return "error,unsupported value type,ThreadReference";
        if (v instanceof VoidValue)                  return "error,unsupported value type,VoidValue";

        if (v instanceof BooleanValue)      return "\"" + Boolean.toString(((BooleanValue) v).value()) + "\"";
        if (v instanceof ByteValue)           return "\"" + Byte.toString(((ByteValue) v).value()) + "\"";
        if (v instanceof CharValue)          return "\"" + Character.toString(((CharValue) v).value()) + "\"";
        if (v instanceof DoubleValue)       return "\"" + Double.toString(((DoubleValue) v).value()) + "\"";
        if (v instanceof FloatValue)          return "\"" + Float.toString(((FloatValue) v).value()) + "\"";
        if (v instanceof IntegerValue)       return "\"" + Integer.toString(((IntegerValue) v).value()) + "\"";
        if (v instanceof LongValue)         return "\"" + Long.toString(((LongValue) v).value()) + "\"";
        if (v instanceof ShortValue)         return "\"" + Short.toString(((ShortValue) v).value()) + "\"";
        if (v instanceof StringReference)  return "\"" + ((StringReference) v).value() + "\"";

        if (v instanceof ArrayReference)  return arrayToString((ArrayReference) v);

        if ((v.type() instanceof ReferenceType) && (v.type() instanceof ClassType))

            {
                StringBuilder            b  = new StringBuilder();
                List<InterfaceType>  it   = ((ClassType)  v.type()).allInterfaces();

                // look inside lists and maps

                for (InterfaceType i : it)
                    
                    {
                        if (i.name().equals("java.util.List"))     return listToString((ObjectReference) v);
                        if (i.name().equals("java.util.Map"))   return mapToString((ObjectReference) v);
                    }

                // It's just an ordinary object

                List<Field>	fld = ((ClassType) v.type()).allFields();
                b.append("(");
                
                for (Field f : fld)

                    b = b.append("( \""
                                 + f.name()
                                 + "\" "
                                 + getValueString(((ObjectReference) v).getValue(f))
                                 + " )");

                b.append(") ");
                return b.toString();
            }
        
        return "";
    }

    private String  arrayToString(ArrayReference arrayReference)
    {
        StringBuilder     b   = new StringBuilder("( \"type\" \"array\" ) ( \"size\"  \"" + Integer.toString(arrayReference.length()) + "\") ( \"contents\" ");
        int                  len = (arrayReference.length() > 20 ? 20 : arrayReference.length());

        for (int i = 0; i < len; i++)

            b = b.append("( \"" + i + "\" " + getValueString(arrayReference.getValue(i)) + ")");

        b.append(" ) ");
        return b.toString();
    }

    private String listToString(ObjectReference objectReference)
    {
        StringBuilder    b                 = new StringBuilder("( \"type\" \"List\" ) ( \"size\" ");

        // get the list's size

        DebuggerValue size = getValueOfSingleRemoteCall(objectReference, "size");
        if (size.error != null) return returnError(b, size.error);

        int  sz  = ((IntegerValue) size.value).value();

        b.append("\"" + Integer.toString(sz) + "\")");

        if (sz > 20) sz = 20;

        // get the list's contents

        b.append("( \"contents\" ");

        ArrayList<Value> alv  = new ArrayList<Value> ();
        alv.add(threadReference.virtualMachine().mirrorOf(0));

        DebuggerMethod get = remoteMethod(objectReference, "get", alv);
        if (get.error != null) return returnError(b, get.error);

        for (int j = 0; j != sz; j++)

            {
                b = b.append("( \"" + j + "\" ");

                alv.clear();
                alv.add(threadReference.virtualMachine().mirrorOf(j));

                DebuggerValue vv = invokeRemoteMethod(objectReference, get.method, alv);
                if (vv.error != null) return returnError(b, vv.error);

                b.append(getValueString(vv.value));
                b.append(")");
            }

        b.append(" ) ");
        return b.toString();
    }

    private String mapToString(ObjectReference mapReference)
    {
    StringBuilder    b = new StringBuilder("( \"type\" \"Map\" ) ( \"size\" " );

        // get the map's size

        DebuggerValue size = getValueOfSingleRemoteCall(mapReference, "size");
        if (size.error != null) return returnError(b, size.error);

        int  sz  = ((IntegerValue) size.value).value();

        b = b.append("\"" + Integer.toString(sz) + "\")");

        if (sz > 20) sz = 20;

        b.append("( \"contents\" ");

        // set of map's keys
                                        
        DebuggerValue keySet = getValueOfSingleRemoteCall(mapReference, "keySet");
        if (keySet.error != null) return returnError(b, keySet.error);
        
        // keysIterator is an iterator of the set of keys

        DebuggerValue keySetIterator = getValueOfSingleRemoteCall(((ObjectReference) keySet.value), "iterator");
        if (keySetIterator.error != null) return returnError(b, keySetIterator.error);
            
        // map's contents
        ArrayList<Value> emptyList = new ArrayList<Value> ();
        DebuggerMethod next  = remoteMethod (((ObjectReference) keySetIterator.value), "next", emptyList);
        if (next.error != null) return returnError(b, next.error);

        // oddly enough, remoteMethod won't work without an appropriate
        // argument in the keyList

        DebuggerValue key  = invokeRemoteMethod(((ObjectReference) keySetIterator.value), next.method, emptyList);
        if (key.error != null) return returnError(b, key.error);

        ArrayList<Value> keyList    = new ArrayList<Value> ();
        keyList.add(key.value);

        DebuggerMethod get   = remoteMethod (mapReference, "get", keyList);
        if (get.error != null) return returnError(b, get.error);

        for (int j = 0; j != sz; j++)

            {
                b.append("( " + getValueString(key.value) + " ");
                
                DebuggerValue entry = invokeRemoteMethod(mapReference, get.method, keyList);
                if (entry.error != null) return returnError(b, entry.error);
                    
                b.append(getValueString(entry.value) + " )");

                if (j != (sz - 1))

                    {
                        key  = invokeRemoteMethod(((ObjectReference) keySetIterator.value), next.method, emptyList);
                        if (key.error != null) return returnError(b, key.error);
                        keyList.clear();
                        keyList.add(key.value);
                    }
            }
            
        b.append(")");
        return b.toString();
    }

    private String returnError(StringBuilder b, String error)
    {
        b.append(" \"");
        b.append(error);
        b.append("\"))");
        return b.toString();
    }

    private DebuggerValue getValueOfSingleRemoteCall(ObjectReference objectReference, String methodName)
    {
        ArrayList<Value> alv  = new ArrayList<Value> ();            
        DebuggerMethod m    = remoteMethod(objectReference, methodName, alv);
            
        return (m.error == null) ? invokeRemoteMethod(objectReference, m.method, alv)
            : new DebuggerValue(null, m.error);
    }
    
    private DebuggerValue invokeRemoteMethod (ObjectReference o, Method m, List<Value> arguments)
    {
        try {
            return new DebuggerValue(o.invokeMethod(threadReference, m,  arguments, 0), null);
        } catch (InvalidTypeException ex) {
            return new DebuggerValue(null, "error,InvalidTypeException in getValueString " + ex.toString());
        } catch (ClassNotLoadedException ce) {
            return new DebuggerValue(null, "error,ClassNotLoaded in getValueString " + ce.toString());
        } catch (IncompatibleThreadStateException ie) {
            return new DebuggerValue(null, "error,IncompatibleThreadStateException in getValueString " + ie.toString());
        } catch (InvocationException te) {
            return new DebuggerValue(null, "error,InvocationException in getValueString " + te.toString());
        }
    }

    private DebuggerMethod remoteMethod (ObjectReference o, String name, List<Value> arguments)
    {
        Method  m = null;

        try {
            List<Method> methods = ((ReferenceType) o.type()).methodsByName(name);
        
            for (Method mm : methods)

                { 
                    List<Type>                   argumentTypes     = mm.argumentTypes(); 
                    ARGUMENT_MATCHING argumentMatching = argumentsMatching(argumentTypes, arguments); 
                    
                    if (argumentMatching == ARGUMENT_MATCHING.MATCH)
                        
                        return new DebuggerMethod(mm, null);
                    
                    if (argumentMatching == ARGUMENT_MATCHING.ASSIGNABLE)
                        
                        { 
                            if (m != null)
                                
                                return new DebuggerMethod(null, "error,Multiple methods with name " + mm.name() + " matched to specified arguments. ");
                            
                            m = mm;
                        } 
            }
        } catch (ClassNotLoadedException ce) {
            return new DebuggerMethod(null, "error,ClassNotLoaded in getValueString " + ce.toString());
        }

        return new DebuggerMethod(m, (m != null) ? null : "error,method " + name + "not found");
    }
    
    private enum ARGUMENT_MATCHING { 
        MATCH, ASSIGNABLE, NOT_MATCH 
    } 

    private ARGUMENT_MATCHING argumentsMatching(List<Type> argumentTypes, List<Value> arguments) throws ClassNotLoadedException
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
 
    private boolean isAssignable(Type from, Type to) throws ClassNotLoadedException
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

    class DebuggerValue
    {
        public Value value;
        public String error;

        public DebuggerValue(Value v, String e)
        {
            value = v;
            error  = e;
        }
    }

    class DebuggerMethod
    {
        public Method method;
        public String   error;

        public DebuggerMethod(Method m, String e)
        {
            method = m;
            error    = e;
        }
    }
}
