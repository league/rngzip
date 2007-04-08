package net.contrapunctus.rngzip.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPInputStream;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.util.ContextualInputStream;
import net.contrapunctus.rngzip.util.MultiplexInputStream;
import net.contrapunctus.rngzip.util.SchemaFormatException;

/**
 * This implements a compressed XML input interface by reading the
 * tree structure and data stream(s) from a single underlying input
 * stream.  The embedded streams may optionally be compressed using
 * gzip; this is specified in a configuration stream of the input.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public final class RNGZInputStream implements RNGZInputInterface
{
   private MultiplexInputStream mux;
   private RNGZSettings settings;
   private BitInputStream bits;
   private ContextualInputStream data;
   private URL schemaURL;
   private long expectedSum;

   private InputStream filter(int streamID, int kind) throws IOException
   {
      InputStream is = mux.open(streamID);
      switch(kind) {
      case 0x01: 
         is = new GZIPInputStream(is);
         break;
      }
      return is;
   }

   /**
    * Construct an input stream for compressed XML data, which reads
    * from ‘in’.  Additional configuration data is read from the
    * stream itself.
    * @throws IllegalArgumentException if ‘in’ is null.
    * @throws RNGZFormatException if the data on ‘in’ does not
    * appear to represent a valid input stream in the format expected
    * by this class.
    * @throws IOException if there is some other problem reading from
    * ‘in’.
    */
   public RNGZInputStream(InputStream in, RNGZSettings se) 
      throws IOException
   {
      mux = new MultiplexInputStream(in);
      settings = se.fromStream(mux, 1);
      bits = settings.newBitInput(mux, 0);
      data = settings.newDataInput(mux, 2);
      String s = data.readUTF(null);
      if( s.length() > 0 ) {
         schemaURL = new URL( s );
         expectedSum = data.readLong(null);
      }
   }

   public URL getSchemaURL()
   {
      return schemaURL;
   }

   public long getSchemaSum()
   {
      return expectedSum;
   }

   private final void check()
   {
      if(mux == null) {
         throw new IllegalStateException("stream already closed");
      }
   }

   public ChoiceDecoder makeChoiceDecoder(int limit, Object id)
   {
      return settings.makeChoiceCoder(limit, id);
   }
   
   /**
    * @throws IllegalStateException if the stream is already closed.
    */
   public int readChoice(ChoiceDecoder dec) throws IOException
   {
      check();
      return dec.decode(bits);
   }
   
   /**
    * @throws IllegalStateException if the stream is already closed.
    */
   public String readContent(List<String> path) throws IOException
   {
      check();
      //String elt = path.get(path.size()-1);
      return data.readUTF(path);
   }

   /**
    * Closes the multiplexed input stream, and all the embedded
    * streams; the object becomes useless after this.
    * @throws IllegalStateException if the stream is already closed.
    */
   public void close() throws IOException
   {
      check();
      mux.close();
      mux = null;
      bits = null;
      data = null;
   }
}
