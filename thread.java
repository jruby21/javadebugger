import com.sun.jdi.*;
import java.util.*;

class thread
{
    private ThreadReference tr = null;

    public thread(ThreadReference t)
    {
        tr = t;
    }

    public ThreadReference getThread()
    {
        return tr;
    }
    
    public String frame(int frameNumber)
    {
        StackFrame                frame  = null;
        Location                  loc    = null;
        List<LocalVariable>       vars   = null;
        Map<LocalVariable, Value> values = null;
        StringBuilder             sb     = new StringBuilder("frame,"
                                                             + frameNumber);

        if (!tr.isSuspended())

            return("error,thread " + tr.uniqueID() + " not suspended\n");

        try {
            int framecount = tr.frameCount();

            if (frameNumber < 0 || frameNumber >= framecount)

                return("error,no frame number " + frameNumber + " in thread " + tr.uniqueID() + "\n");

            sb.append("," + framecount +  "\n");

            frame = tr.frame(frameNumber);

            if (frame == null)
        
                return("error,no frame number " + frameNumber + " in thread " + tr.uniqueID() + "\n");

            loc = frame.location();

            if (loc != null)

                sb.append((new location(loc)).toString());

            vars   = frame.visibleVariables();
            values = frame.getValues(vars);
        } catch (InvalidStackFrameException e)  {
            return("error thread " + tr.uniqueID() + " not suspended\n");
        } catch (IncompatibleThreadStateException e) {
            return("error thread " + tr.uniqueID() + " not suspended\n");
        } catch (IndexOutOfBoundsException e) {
            return("error no frame number " + frameNumber + " in thread " + tr.uniqueID() + "\n");
        } catch (AbsentInformationException e) {
            sb.append("error values missing in frame " + frameNumber + " in thread  " + tr.uniqueID() + "\n");
            return(sb.toString());
        } catch (NativeMethodException e) {
            sb.append("error native method in frame " + frameNumber + " in thread  " + tr.uniqueID() + "\n");
            return(sb.toString());
        }
        
        // arguments

        StringBuilder ab = new StringBuilder();
        StringBuilder lb = new StringBuilder();

        if (vars != null)

            {
                for (LocalVariable var : vars)

                    {
                        String  s =  " (\""
                            + var.name()
                            + "\" "
                            + ((values == null) ? "null" : debugger.getValueString(values.get(var)))
                            +")";;
 
                        if (var.isArgument())

                            ab.append(s);

                        else

                            lb.append(s);
                    }
            }
        
        return("argument,(" + ab.toString() + ")\nlocal,(" + lb.toString() + ")\n");
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder("thread,"
                                            + tr.uniqueID()
                                            + ","
                                            + tr.name()
                                            + ",");

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

        b.append(",");
        
        try {
            b.append(tr.frameCount());
        } catch (com.sun.jdi.IncompatibleThreadStateException e)
            {
                b.append("noframecount");
            }
        
        return b.toString()
            + ","
            + tr.isAtBreakpoint()
            + ","
            + tr.isSuspended();
    }
}
