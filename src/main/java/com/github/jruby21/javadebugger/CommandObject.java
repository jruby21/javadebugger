package com.github.jruby21.javadebugger;

class CommandObject extends EventOrCommandObject {

    private String command = null;

    public CommandObject(String s) {command = s;}
    public void evaluate(DebuggerOutput d) {d.command(command);}
}
