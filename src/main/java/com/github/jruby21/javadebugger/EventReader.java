package com.github.jruby21.javadebugger;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;

import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import java.io.PrintStream;

class EventReader extends Thread
{
    private VirtualMachine vm = null;
    private PrintStream     out = null;
    
    public EventReader(VirtualMachine v, PrintStream o)
    {
        vm = v;
        out = o;
    }

    public void run()
    {
        boolean connected = true;

        while (connected)

            {
                try {
                    EventIterator it = vm.eventQueue().remove().eventIterator();

                    while (it.hasNext()) {

                        Event event = (Event) it.next();

                        if (event instanceof BreakpointEvent) {
                            BreakpointEvent bp = (BreakpointEvent) event;
                            out.println("breakpoint,"
                                               + ((Integer) bp.request().getProperty(JavaDebuggerProxy.NumberProperty)).intValue()
                                               + "," + (new DebuggerThread(bp.thread())).toString()
                                               + "," + (new DebuggerLocation(bp.location())).toString());
                        } else if (event instanceof StepEvent) {
                            StepEvent se = (StepEvent) event;
                            out.println("step," + (new DebuggerThread(se.thread())).toString() + "," + (new DebuggerLocation(se.location())).toString());
                            se.request().disable();
                        } else if (event instanceof MethodEntryEvent) {
                            ;
                        } else if (event instanceof MethodExitEvent) {
                            ;
                        } else if (event instanceof ClassPrepareEvent) {
                            out.println("classloaded," + ((ClassPrepareEvent) event).referenceType().name());
                        } else if (event instanceof ClassUnloadEvent) {
                            ;
                        } else if (event instanceof VMDeathEvent)   {
                            out.println("VMDeath");
                        } else if (event instanceof VMDisconnectEvent)   {
                            out.println("VMDisconnectEvent");
                        } else if (event instanceof ExceptionEvent)   {
                            ExceptionEvent  e        = (ExceptionEvent) event;
                            ObjectReference re       = e.exception();
                            Field           msgField = re.referenceType().fieldByName("detailMessage"); 
                            StringReference msgVal   = (StringReference) re.getValue(msgField); 
                            
                            out.println("exception,"
                                               + e.exception().type().name()
                                               + ","
                                               + (new DebuggerLocation(e.catchLocation())).toString()
                                               + ","
                                               + (msgVal == null ? "" : msgVal.value()));
                        } else if (event instanceof ThreadStartEvent) {
                            out.println("thread,start," 
                                               + (new DebuggerThread(((ThreadStartEvent) event).thread())).toString());
                        } else if (event instanceof ThreadDeathEvent) {
                            out.println("thread,death,"
                                               + (new DebuggerThread(((ThreadStartEvent) event).thread())).toString());
                        } else if (event instanceof VMStartEvent) {
                            out.println("vm,started");
                        } else {
                            ;
                        }
                    }
                } catch (InterruptedException exc) {
                    // Do nothing. Any changes will be seen at top of loop.
                } catch (VMDisconnectedException discExc) {
                    out.println("exception," + discExc);
                    connected = false;
                }
            }
    }
}
