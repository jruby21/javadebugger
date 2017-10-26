import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.AbsentInformationException;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.Map;

class EventReader extends Thread
{
    private VirtualMachine vm = null;
    
    public EventReader(VirtualMachine v)
    {
        vm = v;
    }

    public void run()
    {
        for (boolean connected = true;
             connected;)

            {
            try {
                EventIterator it = vm.eventQueue().remove().eventIterator();

                while (it.hasNext()) {

                    Event event = (Event) it.next();
                    
                    if (event instanceof BreakpointEvent) {
                        BreakpointEvent bp = (BreakpointEvent) event;
                        System.out.println("breakpoint\n"
                                           + (new thread(bp.thread())).toString()
                                           + (new location(bp.location())).toString()
                                           + "endbreakpoint\n");

                        Location loc = bp.location();
                        ReferenceType or = loc.declaringType();

                        System.out.println("locations object reference: "
                                           + or.toString()
                                           + " " + or.classObject().toString()
                                           + " " + or.sourceName()
                                           + " " + or.name()
                                           + " "  + or.isAbstract()
                                           + " " + or.isFinal()
                                           + " " + or.isInitialized()
                                           + " " + or.isPrepared()
                                           + " " + or.isStatic()
                                           + " " + or.isVerified());

                        for (Method m : or.methods())
                            System.out.println("Method: " + m.name() + " " + m.returnTypeName());

                        for (Field f : or.fields())

                            System.out.println("Field: " + f.name() + " " + f.typeName());
                    } else if (event instanceof StepEvent) {
                        StepEvent se = (StepEvent) event;
                        System.out.println("step\n"
                                           + (new location(se.location())).toString());
                        se.request().disable();
                    } else if (event instanceof MethodEntryEvent) {
                        ;
                    } else if (event instanceof MethodExitEvent) {
                        ;
                    } else if (event instanceof ClassPrepareEvent) {
                        System.out.println("classloaded," + ((ClassPrepareEvent) event).referenceType().name());
                    } else if (event instanceof ClassUnloadEvent) {
                        ;
                    } else if (event instanceof VMDeathEvent)   {
                        System.out.println("VMDeath");
                    } else if (event instanceof VMDisconnectEvent)   {
                        System.out.println("VMDisconnectEvent");
                    } else if (event instanceof ExceptionEvent)   {
                        ExceptionEvent  e        = (ExceptionEvent) event;
                        ObjectReference re       = e.exception();
                        Field           msgField = re.referenceType().fieldByName("detailMessage"); 
                        StringReference msgVal   = (StringReference) re.getValue(msgField); 

                        System.out.println("exception,"
                                           + e.exception().type().name()
                                           + ","
                                           + (new location(e.catchLocation())).toString()
                                           + ","
                                           + (msgVal == null ? "" : msgVal.value()));
                    } else if (event instanceof ThreadStartEvent) {
                        System.out.println("threadstart," 
                                           + (new thread(((ThreadStartEvent) event).thread())).toString());
                    } else if (event instanceof ThreadDeathEvent) {
                        System.out.println("threaddeath."
                                           + (new thread(((ThreadStartEvent) event).thread())).toString());
                    } else if (event instanceof VMStartEvent) {
                        System.out.println("vmstart");
                    } else {
                        ;
                    }
                }
            } catch (InterruptedException exc) {
                // Do nothing. Any changes will be seen at top of loop.
            } catch (VMDisconnectedException discExc) {
                System.out.println("Exception," + discExc);
                connected = false;
                break;
            } catch (AbsentInformationException a)  {
                System.out.println("Exception," + a);
            }
            }
    }
}
