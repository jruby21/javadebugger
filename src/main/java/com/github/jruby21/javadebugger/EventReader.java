package com.github.jruby21.javadebugger;

import java.util.concurrent.ArrayBlockingQueue;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;

import com.sun.jdi.event.EventSet;

class EventReader extends Thread
{
    private ArrayBlockingQueue<EventOrCommandObject> queue    = null;
    private VirtualMachine                        vm       = null;

    public EventReader(ArrayBlockingQueue<EventOrCommandObject> q, VirtualMachine v)
    {
        vm    = v;
        queue = q;
    }

    public void run()
    {
        boolean connected = true;

        while (connected)

            {
                try {
                    EventSet es = vm.eventQueue().remove();
                    queue.add(new EventObject(es));
                } catch (InterruptedException exc) {
                    // Do nothing. Any changes will be seen at top of loop.
                } catch (VMDisconnectedException d) {
                    connected = false;
                }
            }
    }
}
