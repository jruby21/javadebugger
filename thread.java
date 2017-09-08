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
        StackFrame                             frame             = null;
        Location                                   loc                  = null;
        List<LocalVariable>               vars               = null;
        Map<LocalVariable, Value> values           = null;
        int                                              framecount  = 0;
        StringBuilder                           sb                   = new StringBuilder("frame "
                                                                                         + frameNumber);

        if (!tr.isSuspended())

                return(sb.toString() + " error  \"thread not suspended\"");            

        try {
            framecount = tr.frameCount();
        } catch (IncompatibleThreadStateException e)  {
            return(sb.toString() + " error  \"thread not suspended\"");
        }
      
        sb.append("/" + framecount +  " ");

        if (frameNumber < 0 || frameNumber >= framecount)

                return(sb.toString() + " error  \"no such frame\"");            

        try {
            frame = tr.frame(frameNumber);
            loc      = frame.location();
        } catch (InvalidStackFrameException e)  {
            return(sb.toString() + " error  \"invalid frame state\"");
        } catch (IncompatibleThreadStateException e) {
            return(sb.toString() + " error  \"invalid thread state\"");
        } catch (IndexOutOfBoundsException e) {
            return(sb.toString() + " error  \"no such frame\"");
        }

        if (frame == null)
        
            return(sb.toString() + " error  \"no such frame\"");
      
        sb.append((loc != null) ? (new location(loc)).toString() : " location ");

        try {
            vars     = frame.visibleVariables();
            values = frame.getValues(vars);
        } catch (InvalidStackFrameException e)  {
            return(sb.toString() + " error  \"invalid frame state\"");
        } catch (AbsentInformationException e) {
            return(sb.toString() + " error  \"values missing\"");
        } catch (NativeMethodException e) {
            return(sb.toString() + " error  \"native method\"");
        }
        
        // arguments

        StringBuilder ab = new StringBuilder(" arguments ");
        StringBuilder lb  = new StringBuilder(" locals ");

        if (vars != null)

            {
                for (LocalVariable var : vars)

                    {
                        String  s = var.name() + " ";

                        if  (values != null)

                            {
                                Value  val = values.get(var);
                                s = s + " " 
                                    + ((val == null) ? " null" : val.toString())
                                    + " ";
                            }

                        else

                            s = s + " null";

                        if (var.isArgument())

                            ab.append(s);

                        else

                            lb.append(s);
                    }
            }
        
        return(sb.toString()
               + " " + ab.toString()
               + " " + lb.toString());
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder("thread "
                                            + tr.uniqueID()
                                            + " \'"
                                            + tr.name());

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
        
        return b.toString();
    }
}
