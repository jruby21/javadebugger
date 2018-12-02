package com.github.jruby21.javadebugger;

import java.util.concurrent.ArrayBlockingQueue;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;

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
                    EventIterator it = vm.eventQueue().remove().eventIterator();

                    while (it.hasNext()) {

                        queue.add(new EventObject((Event) it.next()));
                    }
                } catch (InterruptedException exc) {
                    // Do nothing. Any changes will be seen at top of loop.
                } catch (VMDisconnectedException d) {
                    connected = false;
                }
            }
    }
}
