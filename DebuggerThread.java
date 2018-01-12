import com.sun.jdi.*;
import java.util.*;

class DebuggerThread
{
    private ThreadReference tr = null;

    public DebuggerThread(ThreadReference t)
    {
        tr = t;
    }

    public int   frameCount() { try {return tr.frameCount(); } catch (IncompatibleThreadStateException e) { return 0; } }
    public long threadID()     { return tr.uniqueID(); }

    public ThreadReference  getThreadReference() { return tr; }
    
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

    public DebuggerFrame getFrame(int fn)
    {
        return new DebuggerFrame(tr, fn);
    }
}
