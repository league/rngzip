package net.contrapunctus.rngzip.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import java.io.PrintStream;

/**
 * This class produces nice-looking error messages on a designated
 * <code>PrintStream</code> in response to
 * <code>SAXParseException</code>s.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class ErrorReporter implements ErrorHandler
{
   /**
    * The stream to which error messages are printed.  The default
    * constructor sets it to <code>System.err</code>.
    */
   protected final PrintStream out;

   /**
    * A count of the number of fatal and non-fatal errors (but not
    * warnings) received so far.
    * @see #count()
    */
   protected int count;

   /** 
    * Construct a reporter that outputs to the designated stream.
    */
   public ErrorReporter(PrintStream p) { out=p; }

   /**
    * Construct a reporter that outputs to <code>System.err</code>.
    */
   public ErrorReporter() { out=System.err; }

   /**
    * Return a count of the number of fatal and non-fatal errors (but
    * not warnings) received so far.
    */
   public int count()
   {
      return count;
   }

   /**
    * Print a message for a fatal error.  This re-throws the provided
    * exception after printing a message.  The idea is that parsing
    * should not continue at all after a fatal error.
    */
   public void fatalError(SAXParseException exn)
      throws SAXParseException
   {
      count++;
      report("fatal", exn);
      throw exn;
   }

   /**
    * Print a message for a non-fatal error.  This does not re-throw
    * the provided exception, so that parsing can continue.
    */
   public void error(SAXParseException exn)
   {
      count++;
      report("error", exn);
   }

   /**
    * Print a warning message.  This does not re-throw the exception,
    * and does not increment the error count.
    */
   public void warning(SAXParseException exn)
   {
      report("warning", exn);
   }

   /**
    * All of the error reporting methods ultimately call this method
    * to print the message.  It uses the format
    * <code>file:line.column: kind: message</code>.
    */
   protected void report(String kind, SAXParseException exn)
   {
      out.printf("%s:%d.%d: %s: %s%n",
                 exn.getSystemId(), exn.getLineNumber(),
                 exn.getColumnNumber(), kind, exn.getMessage());
   }
}
