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
        Frame                f = getFrame(frameNumber);

        if (f.error != null)

            return f.error;
        
        // arguments

        StringBuilder ab = new StringBuilder();
        StringBuilder lb = new StringBuilder();

        if (f.vars != null)

            {
                for (LocalVariable var : f.vars)

                    {
                        StringBuilder b = (var.isArgument()) ? ab : lb;

                        b.append(" (\""
                            + var.name()
                            + "\" "
                            + ((f.values == null) ? "null" : debugger.getValueString(tr, f.values.get(var)))
                                 +")");
                    }
            }

        return "frame,"
            + frameNumber
            + ",thread,"
            + tr.uniqueID()
            + ","
            + ((f.loc != null) ? (new location(f.loc)).toString() : "location,none")
            + "\nargument,("
            + ab.toString()
            + ")\nlocal,("
            + lb.toString() + ")\n";
    }

    public String stack()
    {
        StringBuilder  b = new StringBuilder("stack," + tr.uniqueID());
        Frame          f  = null;
        int              i = 0;

        for (f = getFrame(i);
             f.error == null;
             f = getFrame(i))

            {
                b.append("," + ((f.loc != null) ? (new location(f.loc)).toString() : "location,none"));
                i++;
            }

        return (i == 0 && f.error != null) ? f.error : b.toString();
    }

    public String thises(int fn)
    {
        Frame          f  = getFrame(fn);

        return (f.error != null) 
            ? f.error
            : "this," + debugger.getValueString(tr, f.thises);
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

    class Frame
    {
        int                                  frameNumber = 0;
        Location                          loc        = null;
        List<LocalVariable>           vars       = null;
        Map<LocalVariable, Value> values     = null;
        ObjectReference               thises     = null; 
        String                            error       = null;
    }

    private Frame getFrame(int fn)
        {
            Frame          fr = new Frame();

            if (!tr.isSuspended())

                fr.error = "error,not suspended,thread," + tr.uniqueID();

            else

                {
                    try
                        {
                            int              framecount = tr.frameCount();
                            StackFrame  sf             = null;

                            if (fn < 0 || fn >= framecount)

                                fr.error = "error,no frame number, " + fn + ",thread," + tr.uniqueID();

                            else if (null == (sf = tr.frame(fn)))

                                fr.error = "error,no frame," + fn + ",thread," + tr.uniqueID();

                            else

                                {
                                    fr.loc = sf.location();
                                    fr.vars   = sf.visibleVariables();
                                    if (fr.vars != null) fr.values = sf.getValues(fr.vars);
                                    fr.thises = sf.thisObject();
                                }
                        } catch (InvalidStackFrameException e)  {
                        fr.error = "error,not suspended,thread," + tr.uniqueID();
                    } catch (IncompatibleThreadStateException e) {
                        fr.error = "error,not suspended,thread," + tr.uniqueID();;
                    } catch (IndexOutOfBoundsException e) {
                        fr.error = "error,no frame number, " + fn + ",thread," + tr.uniqueID();
                    } catch (AbsentInformationException e) {
                        ;
                    }
                }
            
            return fr;
        }
}
