import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

class parser implements Iterator
{
    public enum TOKEN {ALL, ATTACH, BACK, BREAK, CONTINUE, DONE, FRAME, INTO, NEXT, NUMBER, PREPARE, QUIT, RUN, STACK, STRING, THREAD, THIS};

    static HashMap<String, TOKEN> keywords = new HashMap<String, TOKEN>();

    static {
        keywords.put("all",         TOKEN.ALL);
        keywords.put("attach",    TOKEN.ATTACH);
        keywords.put("back",      TOKEN.BACK);
        keywords.put("break",     TOKEN.BREAK);
        keywords.put("continue", TOKEN.CONTINUE);
        keywords.put("frame",     TOKEN.FRAME);
        keywords.put("into",        TOKEN.INTO);
        keywords.put("next",       TOKEN.NEXT);
        keywords.put("prepare",  TOKEN.PREPARE);
        keywords.put("quit",        TOKEN.QUIT);
        keywords.put("run",        TOKEN.RUN);
        keywords.put("stack",     TOKEN.STACK);
        keywords.put("thread",    TOKEN.THREAD);
        keywords.put("this",        TOKEN.THIS);
    }

    private String [] tokens            = null;
    private int       index               = 0;
    private int       number            = -1;
    private String   string              = "";
        private String   commandString = "";
    
    public int     getNumber()              { return number; }
    public String getString()                 { return string; }
    public String getCommandString()  { return commandString; }
    
    public parser(String in)
    {
        tokens = in.split("[ \t]+");
        index  = 0;
        commandString = in;
    }

    public void      clear()                      { index = tokens.length;          }
    public boolean	hasNext()                 { return index < tokens.length;}
    
    public TOKEN	next()
    {
        if (index >= tokens.length)

            return TOKEN.DONE;

        string = tokens [index++];

        TOKEN tok = keywords.get(string.toLowerCase());

        return (tok == null) ? TOKEN.STRING : tok;
    }
}
