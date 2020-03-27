package com.github.jruby21.javadebugger;

import com.sun.jdi.event.Event;

class EventOrCommandObject {
    private String command = null;
    private Event  event   = null;

    public EventOrCommandObject() {}
    public void evaluate(DebuggerOutput d) {}
}
