package net.contrapunctus.rngzip.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * This is an output interface that prints the output as text to a
 * PrintStream, useful for tracing and debugging.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 */
public class VerboseOutput implements RNGZOutputInterface
{
   private PrintStream out;
   private int choices;
   private int chars, strings;

   /**
    * Create an RNGZOutputInterface that outputs its content to the
    * PrintStream ‘out’.
    */
   public VerboseOutput(PrintStream out)
   {
      this.out = out;
   }

   /**
    * Convenience constructor for printing to ‘System.err’.
    */
   public VerboseOutput()
   {
      this(System.err);
   }

   /**
    * @return a choice encoder that should only be used with the
    * ‘writeChoice’ in this class.
    */
   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      return new VerboseChoiceEncoder(out, limit, id);
   }

   /**
    * @param enc should be a choice encoder returned by the
    * ‘makeChoiceEncoder’ in this class.
    */
   public void writeChoice(ChoiceEncoder enc, int choice) 
      throws IOException
   {
      choices++;
      enc.encode(choice, null);
   }
   
   public void writeContent(List<String> path, String s)
   {
      strings++;
      chars += s.length();
      s = s.replaceAll("[\\n\\t\\r\\f]+", "\\\\s");
      if(s.length() > 64) {
         s = s.substring(0, 64)+"...";
      }
      String p = null;
      if(path != null && path.size() > 0) {
         p = path.get(path.size()-1);
      }
      out.printf("data: [%s] %s%n", p, s);
   }   

   public void writeContent(List<String> path, char[] buf, 
                            int start, int length)
   {
      writeContent(path, new String(buf, start, length));
   }

   /**
    * Flushes the underlying PrintStream.
    */
   public void flush()
   {
      out.flush();
   }
   
   /**
    * Prints a summary of the number of choices and amount of data
    * written to the stream.
    */
   public void close()
   {
      out.printf("bits: %d choices.%n", choices);
      out.printf("data: %d strings, %d characters.%n", strings, chars);
      flush();
   }


}
