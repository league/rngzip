package net.contrapunctus.rngzip.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
   
/**
 * This is an input interface that asks the user for input whenever it
 * needs to make a choice or produce character data.  This can be
 * useful for tracing and debugging; it provides a way to guide a
 * decompressor interactively through the automaton.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class InteractiveInput implements RNGZInputInterface
{
   private BufferedReader in;
   private PrintStream out;

   /**
    * Create an interactive input stream that prints prompts to ‘out’
    * and reads responses from ‘in’.
    */
   public InteractiveInput(BufferedReader in, PrintStream out)
   {
      this.in = in;
      this.out = out;
   }

   /**
    * Convenience constructor that automatically creates a
    * BufferedReader for the given InputStream.
    */
   public InteractiveInput(InputStream in, PrintStream out)
   {
      this(new BufferedReader(new InputStreamReader(in)), out);
   }

   /**
    * Convenience constructor that selects ‘System.err’ for prompting
    * and ‘System.in’ for input.
    */
   public InteractiveInput()
   {
      this(System.in, System.err);
   }

   /**
    * @return a choice decoder that should only be used with the
    * ‘readChoice’ in this class.  It asks for input on the streams
    * provided to the constructor.
    */
   public ChoiceDecoder makeChoiceDecoder(int limit, Object id)
   {
      return new InteractiveChoiceDecoder(in, out, limit, id);
   }   

   /**
    * @param dec should be a choice decoder returned by the
    * ‘makeChoiceDecoder’ in this class.
    */
   public int readChoice(ChoiceDecoder dec) throws IOException
   {
      return dec.decode(null);
   }
   
   public String readContent(List<String> path) throws IOException
   {
      out.printf("%s %% ", path.get(path.size()-1));
      out.flush();
      return in.readLine();
   }

   /**
    * Does nothing; does not even close the underlying
    * <code>PrintStream</code>.
    */
   public void close() 
   { }

}
