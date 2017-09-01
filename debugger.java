import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.IncompatibleThreadStateException;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.Map;
 
public class debugger
{
    private VirtualMachine vm = null;
    private EventReader    er = null;
    
    public static void main(String args[]) throws Exception

    {
        debugger d = new debugger();
        d.go(System.in);
    }
        
    void go(InputStream input) throws Exception
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));

        for (String s = in.readLine();
             !s.equalsIgnoreCase("exit");
             s = in.readLine())

            {
                String [] tokens = s.split("[ \t]+");

                if (tokens [0].equalsIgnoreCase("attach"))

                    {
                        // find ProcessAttachingConnector
 
                        List<AttachingConnector> l = Bootstrap.virtualMachineManager().attachingConnectors();

                        AttachingConnector ac = null;

                        for (AttachingConnector c: l) {
                    
                            if (c.name().equals("com.sun.jdi.SocketAttach")) {
                                ac = c;
                                break;
                            }
                        }
        
                        if (ac == null)

                            throw new RuntimeException("Unable to locate ProcessAttachingConnector");

                        Map<String,Connector.Argument> env = ac.defaultArguments();

                        env.get("hostname").setValue(tokens [1]);
                        env.get("port").setValue(tokens [2]);
                        System.out.println(env.get("port"));
                        System.out.println(env.get("hostname"));
        
                        vm = ac.attach(env);
                        new EventReader(vm).go();
                    }
            }
    }
}
