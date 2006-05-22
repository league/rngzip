package net.contrapunctus.rngzip.util;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This cless reads multiple logical streams of data from a single
 * underlying input stream.  Each embedded stream is identified by a
 * small integer.  To begin reading an embedded stream, you must first
 * open it.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 */
public final class MultiplexInputStream implements Closeable
{
   /* The underlying data stream. After this stream is closed, we set
      ‘in’ to null and forbid any rfurther operations on this or any
      embedded streams. */
   private DataInputStream in;
   private boolean close_p;

   private final void check()
   {
      if(in == null)
         throw new IllegalStateException("Stream already closed.");
   }

   /* The application-defined magic/version number read from the
      stream. */
   private int magic;

   /* Used for decoding block headers. */
   private MultiplexBlockRep block;

   /* A map from stream ID to an embedded input stream.  If it’s not
      in this map, it hasn’t been opened yet. */
   private HashMap<Integer,EmbeddedIS> map = 
      new HashMap<Integer,EmbeddedIS>();

   private static final boolean DEBUG = false;
   private static final PrintStream dbg = System.err;

   /**
    * The primary constructor for objects of this class.
    * 
    * @param in The underlying data input streams.  Embedded streams
    * opened with this class are read from ‘in’.
    * @param close_p Determines whether closing this stream also
    * closes the underlying stream.
    * @throws IllegalArgumentException if ‘in’ is null.
    * @throws IOException if there is a problem reading header
    * information from the underlying stream.
    * @throws MultiplexFormatException if this does not appear to be a
    * valid multiplex input stream.
    */   
   public MultiplexInputStream(DataInputStream in, boolean close_p)
      throws IOException
   {
      this.in = in;
      this.close_p = close_p;
      this.block = new MultiplexBlockRep(in);
      if(in.readInt() != MultiplexOutputStream.MAGIC)
         throw MultiplexFormatException.badMagic();
      magic = in.readInt();
   }

   /**
    * Convenience constructor for a normal output stream.  It
    * constructs a DataInputStream around ‘in’ for you.
    */
   public MultiplexInputStream(InputStream in, boolean close_p)
      throws IOException
   {
      this(new DataInputStream(in), close_p);
   }

   /**
    * Convenience constructor for when ‘close_p’ is true (meaning that
    * this class <i>is</i> responsible for closing ‘in’.
    */
   public MultiplexInputStream(DataInputStream in) throws IOException
   {
      this(in, true);
   }
   
   /**
    * Convenience constructor for a normal output stream that also
    * automatically sets ‘close_p’ to true.
    */
   public MultiplexInputStream(InputStream in) throws IOException
   {
      this(new DataInputStream(in), true);
   }
   
   /**
    * Retrieve the application-level magic/version number read from
    * the stream.  This is the 4-byte ‘magic’ value provided to the
    * {@link MultiplexOutputStream} class when it was created.
    */
   public int magic() 
   {
      return magic;
   }

   /**
    * Open a new embedded stream.  Unlike its counterpart {@link
    * MultiplexOutputStream#open(int, OutputStreamFilter)}, clients
    * may wrap the InputStream in whatever filters are needed without
    * informing this class.
    *
    * @param streamID The identifier of the stream to open.
    * @return An input stream that will read only those blocks marked
    * with ‘streamID’.
    * @throws IllegalArgumentException if ‘streamID’ is out of range.
    * @throws IllegalStateException if the stream is already closed.
    * @see MultiplexBlockRep#MAX_STREAM_ID
    */   
   public InputStream open(int streamID)
   {
      MultiplexBlockRep.checkStreamID(streamID);
      check();
      return getStream(streamID);
   }

   /* Use the map to fetch a particular stream, or create it if it
      doesn’t exist yet. */
   private EmbeddedIS getStream(int streamID)
   {
      EmbeddedIS eis = map.get(streamID);
      if(eis == null) {
         eis = new EmbeddedIS(streamID);
         map.put(streamID, eis);
      }
      return eis;
   }

   /**
    * Close the multiplex stream.  This closes the underlying input
    * stream if this object was created with ‘close_p’ set to true.
    * No further operations on this object will be permitted.
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a problem closing the underlying
    * stream.
    */
   public void close() throws IOException
   {
      check();
      if(close_p) in.close();
      in = null;
      block = null;
      map.clear();
      map = null;
   }

   private final class EmbeddedIS extends InputStream
   {
      private int streamID;
      /* A queue of blocks read from the underlying stream. */
      private LinkedList<byte[]> queue = new LinkedList<byte[]>();
      /* The current block is wrapped in this input stream: */
      private ByteArrayInputStream bin;

      /* Get ready to read data, or return false if nothing remains. */ 
      private boolean prepare() throws IOException
      {
         if(bin != null && bin.available() > 0) {
            /* Some bytes still remain in ‘bin’. */
            return true;
         }
         /* bin is empty; get more data from the queue. */
         if(queue.size() < 1) {
            /* queue is empty, read from underlying stream */
            readMore(streamID);
         }
         /* Now queue should be ready */
         byte[] bs = queue.poll();
         if(bs == null || bs.length == 0) {
            /* There was nothing left for this stream. */
            bin = null;
            return false;
         }
         bin = new ByteArrayInputStream(bs);
         return true;
      }
      
      private EmbeddedIS(int streamID)
      {
         this.streamID = streamID;
      }

      /* This is the InputStream method that reads a single byte,
       * or returns -1 on EOF.
       */
      public int read() throws IOException
      {
         if(prepare()) return bin.read();
         else return -1; // EOF
      }

      /* Returns the number of bytes that can be read “without
       * blocking”.  Here, we interpret this as the number of bytes
       * KNOWN to be available (in ‘bin’ and ‘queue’) without going
       * down to the underlying stream.
       */
      public int available() throws IOException
      {
         int n = 0;
         if(bin != null) {
            n = bin.available();
         }
         for(byte[] bs : queue) {
            n += bs.length;
         }
         return n;
      }

      /* Subclasses of InputStream are encouraged to override the
       * following methods with more efficient versions... so here
       * they are.  To read an array, we are happy reading up as much
       * as possible from ‘bin’.  Further data in ‘queue’ or in the
       * underlying stream will wait for the next call.
       */
      public int read(byte[] b, int off, int len) throws IOException
      {
         if(prepare()) return bin.read(b, off, len);
         else return -1;
      }
      
      public long skip(long n) throws IOException
      {
         if(prepare()) return bin.skip(n);
         else return 0;
      }  
   }

   /* We need to read blocks from the underlying stream.  As they are
    * read, queue them onto the appropriate embedded stream queues.
    * Stop when we hit a block from streamID, which initiated the
    * call.
    */
   private void readMore(int streamID) throws IOException
   {
      check();
      if(DEBUG) {
         dbg.printf("*** Asked to read more from stream %d%n", streamID);
      }
      do {
         int sz;
         try { sz = block.decode(); }
         catch(EOFException x) { return; } // this EOF is okay
         assert sz > 0;
         if(DEBUG) {
            dbg.printf("*** Found stream %d with %d bytes.%n", 
                       block.streamID, sz);
         }
         byte buf[] = new byte[sz];
         try { in.readFully(buf); }
         catch(EOFException x) { // this EOF indicates an error
            throw MultiplexFormatException.endOfStream(sz);
         }
         getStream(block.streamID).queue.offer(buf);
      } while(block.streamID != streamID);
   }
}
