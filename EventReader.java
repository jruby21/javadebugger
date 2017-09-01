import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.IncompatibleThreadStateException;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.Map;

class EventReader
{
    private VirtualMachine vm = null;
    
    public EventReader(VirtualMachine v)
    {
        vm = v;
    }

    public void go()
    {
        EventQueue     queue     = vm.eventQueue();
        boolean        connected = true;
 
        System.out.println("Attached! Now listing threads ...");
 
        // list all threads

        System.out.print("(threads ");
        
        for (ThreadReference thr: vm.allThreads())

            System.out.print(threadString(thr) + " ");

        System.out.println(")");
 
        System.out.println("Debugger done.");
        
        while (connected)

            {
            try {
                EventSet      eventSet         = queue.remove();
                boolean       resumeStoppedApp = false;
                EventIterator it               = eventSet.eventIterator();

                while (it.hasNext()) {

                    Event event = (Event) it.next();
                    System.out.println(event);

                    if (event instanceof ExceptionEvent) {
                        ;
                    } else if (event instanceof BreakpointEvent) {
                        ;
                    } else if (event instanceof WatchpointEvent) {
                        ;
                    } else if (event instanceof StepEvent) {
                        ;
                    } else if (event instanceof MethodEntryEvent) {
                        ;
                    } else if (event instanceof MethodExitEvent) {
                        ;
                    } else if (event instanceof ClassPrepareEvent) {
                        ;
                    } else if (event instanceof ClassUnloadEvent) {
                        ;
                    } else if (event instanceof ThreadStartEvent) {
                        System.out.println("(threadstart " 
                                           + threadString(((ThreadStartEvent) event).thread())
                                           + ")");
                    } else if (event instanceof ThreadDeathEvent) {
                        System.out.println("(threaddeath " 
                                           + threadString(((ThreadDeathEvent) event).thread())
                                           + ")");
                    } else if (event instanceof VMStartEvent) {
                        System.out.println("(vmstart " 
                                           + threadString(((VMStartEvent) event).thread())
                                           + ")");
                    } else {
                        ;
                    }
                }
            } catch (InterruptedException exc) {
                // Do nothing. Any changes will be seen at top of loop.
            } catch (VMDisconnectedException discExc) {
                //                handleDisconnectedException();
                break;
            }
        }
    }

    private String threadString(ThreadReference t)
    {
        StringBuilder b = new StringBuilder("(thread "
                                            + t.uniqueID()
                                            + " \'"
                                            + t.name()
                                            + "\' ");

        switch(t.status())

            {
            case ThreadReference.THREAD_STATUS_MONITOR:
                b.append("monitor");
                break;
                
            case ThreadReference.THREAD_STATUS_NOT_STARTED:
                b.append("notStarted");
                break;
                
            case ThreadReference.THREAD_STATUS_RUNNING:
                b.append("running");
                break;
                
            case ThreadReference.THREAD_STATUS_SLEEPING:
                b.append("sleeping");
                break;
                
            case ThreadReference.THREAD_STATUS_UNKNOWN:
            default:
                b.append("unknown");
                break;
                
            case ThreadReference.THREAD_STATUS_WAIT:
                b.append("waiting");
                break;
                
            case ThreadReference.THREAD_STATUS_ZOMBIE:
                b.append("zombie");
                break;
            }

        b.append(" ");
        
        try {
            b.append(t.frameCount());
        } catch (com.sun.jdi.IncompatibleThreadStateException e)
            {
                b.append("noframecount");
            }
        
        b.append(" ");
        b.append(t.isAtBreakpoint());
        b.append(" ");
        b.append(t.isSuspended());
        b.append(")");
        
        return b.toString();
    }
}
    
