package net.contrapunctus.rngzip.io;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.ContextualOutputStream;
import net.contrapunctus.rngzip.util.ErrorReporter;
import net.contrapunctus.rngzip.util.MultiplexOutputStream;
import net.contrapunctus.rngzip.util.OutputStreamFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This implements a compressed XML output interface by multiplexing
 * the tree structure and data stream(s) onto a single underlying
 * output stream.  The embedded streams may optionally be compressed
 * using gzip.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class RNGZOutputStream implements RNGZOutputInterface
{
   private MultiplexOutputStream mux;
   private RNGZSettings settings;
   private BitOutputStream bits;
   private ContextualOutputStream data;

   private final boolean STATS = false;
   private final PrintStream dbg = System.err;
   private HashMap<String, Integer> tallies;

   /**
    * Construct an output stream for compressed XML data, which writes
    * (as a multiplex stream) to ‘out’.
    * @throws IllegalArgumentException if ‘out’ is null.
    * @throws IOException if there is trouble writing to ‘out’.
    */
   public RNGZOutputStream(OutputStream out, 
                           RNGZSettings settings,
                           BaliAutomaton au)
      throws IOException
   {
      this.settings = settings;
      mux = new MultiplexOutputStream(out, settings.magic());
      settings.writeTo(mux, 1);
      bits = settings.newBitOutput(mux, 0);
      data = settings.newDataOutput(mux, 2);
      if (au != null) {
         bits.writeBit(true);
         data.writeUTF(null, au.getURL().toString());
         data.writeLong(null, au.checksum());
      }
      else {
         bits.writeBit(false);
      }
      if(STATS) {
         tallies = new HashMap<String, Integer>();
      }
   }

   private final void check()
   {
      if(mux == null) {
         throw new IllegalStateException("stream already closed");
      }
   }

   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      return settings.makeChoiceCoder(limit, id);
   }

   /**
    * @throws IllegalStateException if the stream is already closed.
    */
   public void writeChoice(ChoiceEncoder enc, int choice) throws IOException
   {
      check();
      enc.encode(choice, bits);
   }

   /**
    * @throws IllegalStateException if the stream is already closed.
    */
   public void writeContent(List<String> path, String s) throws IOException
   {
      check();
      if(STATS) {
         String elt = path.get(path.size()-1);
         assert elt.intern() == elt;
         tally(elt);
      }
      data.writeUTF(path, s);
   }
   
   /**
    * @throws IllegalStateException if the stream is already closed.
    */
   public void writeContent(List<String> path, char[] buf, int off, int len)
      throws IOException
   {
      writeContent(path, new String(buf, off, len));
   }

   /** 
    * Flushes the character data stream and underlying multiplex
    * stream.  It’s never necessary to call this, as it happens
    * automatically on close.
    * @throws IOException if there is a problem on the underlying
    * stream.
    * @throws IllegalStateException if the stream is already closed.
    */
   public void flush() throws IOException
   {
      check();
      data.flush();
      mux.flush();
   }

   /**
    * Closes the stream; after this, the object becomes useless.
    * @throws IllegalStateException if the stream is already closed.
    */
   public void close() throws IOException
   {
      check();
      mux.close();
      if(STATS) {
         for(Map.Entry<String, Integer> e : tallies.entrySet()) {
            dbg.printf("%20s %5d%n", e.getKey(), e.getValue());
         }
      }
      mux = null;
      bits = null;
      data = null;
      tallies = null;
   }   

   private void tally(String elt)
   {
      Integer i = tallies.get(elt);
      if(i == null) {
         i = 0;
      }
      tallies.put(elt, i+1);
   }

}
