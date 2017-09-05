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

class EventReader extends Thread
{
    private VirtualMachine vm = null;
    
    public EventReader(VirtualMachine v)
    {
        vm = v;
    }

    public void run()
    {
        EventQueue     queue     = vm.eventQueue();
        boolean        connected = true;
 
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
                    } else if (event instanceof VMDeathEvent)   {
                        System.out.println("(VMDeath)");
                    } else if (event instanceof VMDisconnectEvent)   {
                        System.out.println("(VMDisconnectEvent)");
                    } else if (event instanceof ExceptionEvent)   {
                        ExceptionEvent  e        = (ExceptionEvent) event;
                        ObjectReference re       = e.exception();
                        Field           msgField = re.referenceType().fieldByName("detailMessage"); 
                        StringReference msgVal   = (StringReference) re.getValue(msgField); 

                        System.out.println("(exception "
                                           + e.exception().type().name()
                                           + " "
                                           + (msgVal == null ? "null" : msgVal.value())
                                           + ")");
                    } else if (event instanceof ThreadStartEvent) {
                        System.out.println("(threadstart " 
                                           + (new thread(((ThreadStartEvent) event).thread())).toString()
                                           + ")");
                    } else if (event instanceof ThreadDeathEvent) {
                        System.out.println("(threaddeath " 
                                           + (new thread(((ThreadStartEvent) event).thread())).toString()
                                           + ")");
                    } else if (event instanceof VMStartEvent) {
                        System.out.println("(vmstart " 
                                           + (new thread(((VMStartEvent) event).thread())).toString()
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
}
    
