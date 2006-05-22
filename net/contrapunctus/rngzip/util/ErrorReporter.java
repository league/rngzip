package net.contrapunctus.rngzip.util;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import java.io.PrintStream;

public class ErrorReporter implements ErrorHandler
{
   protected PrintStream out;
   protected final int max;
   protected int count;

   public ErrorReporter(PrintStream p, int n) { out=p; max=n; }
   public ErrorReporter(PrintStream p) { out=p; max=50; }
   public ErrorReporter(int n) { out=System.err; max=n; }
   public ErrorReporter() { out=System.err; max=50; }

   public void fatalError(SAXParseException exn)
      throws SAXParseException
   {
      count++;
      report("fatal", exn);
      throw exn;
   }

   public void error(SAXParseException exn)
   {
      count++;
      report("error", exn);
   }

   public void warning(SAXParseException exn)
   {
      report("warning", exn);
   }

   protected void report(String kind, SAXParseException exn)
   {
      out.printf("%s:%d.%d: %s: %s%n",
                 exn.getSystemId(), exn.getLineNumber(),
                 exn.getColumnNumber(), kind, exn.getMessage());
   }
}
