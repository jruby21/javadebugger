// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger
// /home/jruby/tools/jdk1.8.0_131/bin/java -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" -agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y test.foo 3 4
// /home/jruby/tools/jdk1.8.0_131/bin/javac -cp ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" debugger.java

package com.github.jruby21.javadebugger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.concurrent.ArrayBlockingQueue;

public class JavaDebuggerProxy
{
    public static void main(String args[]) throws Exception    {

        ArrayBlockingQueue<EventOrCommandObject> queue = new ArrayBlockingQueue<EventOrCommandObject>(128);
        new DebuggerOutput(queue, System.out).start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String         s  = null;

        while ((s = in.readLine()) != null) {

            s = s.trim();

            if (!s.isEmpty()) {

                queue.add(new CommandObject(s));
            }
        }
    }
}
