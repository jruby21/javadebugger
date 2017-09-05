
import java.io.File;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;

class location
{
    Location loc = null;

    public location(Location l) { loc = l; }

    public String toString()
    {
        try {
            String filename = loc.sourceName();
            String refName  = loc.declaringType().name();
            int    iDot     = refName.lastIndexOf('.');
            String pkgName  = (iDot >= 0)? refName.substring(0, iDot+1) : "";

            return "(location "
                + pkgName.replace('.', File.separatorChar) + filename
                + " "
                + loc.lineNumber()
                + " "
                + loc.method().name()
                + " )";
        } catch (AbsentInformationException e) {
            return "(location)";
        }
    }
}
