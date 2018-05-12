package com.github.jruby21.javadebugger;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;

import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
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

import com.sun.jdi.request.ClassPrepareRequest;

import java.io.PrintStream;

class EventReader extends Thread
{
    private VirtualMachine    vm                            = null;
    private DebuggerOutput debuggerOutput = null;

    public EventReader(VirtualMachine v, DebuggerOutput o)
    {
        vm                         = v;
        debuggerOutput = o;
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

                        if (event instanceof AccessWatchpointEvent) {
                            AccessWatchpointEvent ae = (AccessWatchpointEvent) event;
                            debuggerOutput.output_accessWatchpoint(ae.object(), ae.field(), ae.valueCurrent(), ae.thread());
                        } else if (event instanceof BreakpointEvent) {
                            BreakpointEvent bp = (BreakpointEvent) event;
                            debuggerOutput.output_breakpointEntered(((Integer) bp.request().getProperty(JavaDebuggerProxy.NumberProperty)).intValue(),
                                                                 bp.thread(),
                                                                 bp.location());
                        } else if (event instanceof ClassPrepareEvent) {
                                debuggerOutput.output_classPrepared(((ClassPrepareEvent) event).referenceType().name());
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

                                debuggerOutput.output_classPrepared(((ClassPrepareEvent) event).referenceType().name());
                            } else {
                                ((ClassPrepareRequest) event.request()).enable();
                                vm.resume();
                            }
                        } else if (event instanceof ClassUnloadEvent) {
                            debuggerOutput.output_classUnloaded(((ClassUnloadEvent) event).className());
                        } else if (event instanceof ExceptionEvent)   {
                            debuggerOutput.output_exception((ExceptionEvent) event);
                        } else if (event instanceof MethodEntryEvent) {
                            ;
                        } else if (event instanceof MethodExitEvent) {
                            ;
                        } else if (event instanceof ModificationWatchpointEvent) {
                            ModificationWatchpointEvent me = (ModificationWatchpointEvent) event;
                            debuggerOutput.output_modificationWatchpoint(me.object(), me.field(), me.valueCurrent(), me.valueToBe(), me.thread());
                        } else if (event instanceof StepEvent) {
                            StepEvent se = (StepEvent) event;
                            debuggerOutput.output_step(se.thread(), se.location());
                            se.request().disable();
                        } else if (event instanceof ThreadStartEvent) {
                            debuggerOutput.output_threadStarted(((ThreadStartEvent) event).thread());
                        } else if (event instanceof ThreadDeathEvent) {
                            debuggerOutput.output_threadDied(((ThreadDeathEvent) event).thread());
                        } else if (event instanceof VMDeathEvent)   {
                            debuggerOutput.output_vmDied( );
                        } else if (event instanceof VMDisconnectEvent)   {
                            debuggerOutput.output_vmDisconnected( );
                        } else if (event instanceof VMStartEvent) {
                            debuggerOutput.output_vmStarted( );
                        } else {
                            ;
                        }
                    }
                } catch (InterruptedException exc) {
                    // Do nothing. Any changes will be seen at top of loop.
                } catch (VMDisconnectedException d) {
                    debuggerOutput.output_vmDisconnected();
                    connected = false;
                }
            }
    }
}
