package com.github.jruby21.javadebugger;

import com.sun.jdi.event.EventSet;

class EventObject extends EventOrCommandObject {

    private EventSet  event   = null;

    public EventObject(EventSet e)            {event = e;}
    public void evaluate(DebuggerOutput d) {d.event(event);}
}
