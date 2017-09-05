import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

class parser implements Iterator
{
    public enum TOKEN {ALL, ATTACH, DONE, FRAME, NUMBER, STRING, THREAD, START, STOP};

    static HashMap<String, TOKEN> keywords = new HashMap<String, TOKEN>();

    static {
        keywords.put("(",        TOKEN.START);
        keywords.put(")",        TOKEN.STOP);
        keywords.put("all",      TOKEN.ALL);
        keywords.put("attach",   TOKEN.ATTACH);
        keywords.put("frame",    TOKEN.FRAME);
        keywords.put("thread",   TOKEN.THREAD);
    }

    private String [] tokens = null;
    private int       index  = 0;
    private int       number = -1;
    private String    string = "";

    public int    getNumber() { return number; }
    public String getString() { return string; }
    
    public parser(String in)
    {
        tokens = in.split("[ \t]+");
        index  = 0;
    }

    public boolean	hasNext() {return index < tokens.length;}

    public TOKEN	next()
    {
        if (index >= tokens.length)

            return TOKEN.DONE;

        String t = tokens [index++];
        
        if (t.startsWith("("))

            {
                tokens [--index] = t.substring(1);
                return TOKEN.START;
            }

        if (t.endsWith(")"))

            {
                t = t.substring(0, t.length() - 1);
                tokens [--index] = ")";
            }

        TOKEN tok = keywords.get(t);

        if (tok == null)

            {
                string = t;
                return TOKEN.STRING;
            }

        return tok;
    }
}

