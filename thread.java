import com.sun.jdi.*;
import java.util.*;

class thread
{
    private ThreadReference tr = null;

    public thread(ThreadReference t)
    {
        tr = t;
    }

    public String frame(int frameNumber)
    {
        StackFrame    frame = null;
        StringBuilder sb    = new StringBuilder("(frame "
                                                + frameNumber);

        try {
            sb.append("/" + tr.frameCount() + " ");
            
            if (!tr.isSuspended() || frameNumber < 0 || frameNumber >= tr.frameCount())

                return(sb.toString() + ")");

            frame = tr.frame(frameNumber);

            if (frame == null)
        
                return(sb.toString() + ")");
      
            Location loc = frame.location();

            sb.append(" (location "
                      + ((loc != null) ? loc.toString() : "")
                      + ")");
            
            List<LocalVariable> vars = frame.visibleVariables();

            if (vars.size() == 0)

                {
                    sb.append("(arguments ) (locals ))");
                    return(sb.toString());
                }

            Map<LocalVariable, Value> values = frame.getValues(vars);

            // arguments

            StringBuilder ab = new StringBuilder("(arguments ");
            StringBuilder lb = new StringBuilder("(locals ");
            
            for (LocalVariable var : vars)

                {
                    Value  val = values.get(var);
                    String s   = "("
                        + var.name()
                        + " "
                        + ((val == null) ? "null" : val.toString())
                        + ") ";

                    if (var.isArgument())

                        ab.append(s);

                    else

                        lb.append(s);
                }

            sb.append(ab.toString() + ")" + " " + lb.toString() + ")");
        } catch (AbsentInformationException | IncompatibleThreadStateException | IndexOutOfBoundsException e) {}

        return(sb.toString() + ")");
    }
        
    public String toString()
    {
        StringBuilder b = new StringBuilder("(thread "
                                            + tr.uniqueID()
                                            + " \'"
                                            + tr.name()
                                            + "\' ");

        switch(tr.status())

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
            b.append(tr.frameCount());
        } catch (com.sun.jdi.IncompatibleThreadStateException e)
            {
                b.append("noframecount");
            }
        
        b.append(" ");
        b.append(tr.isAtBreakpoint());
        b.append(" ");
        b.append(tr.isSuspended());
        b.append(")");
        
        return b.toString();
    }
}
