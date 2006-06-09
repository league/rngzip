package net.contrapunctus.rngzip.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;

/**
 * This class interleaves multiple logical streams of data within a
 * single underlying output stream.  Each embedded stream is
 * identified by a small integer.  To begin writing to an embedded
 * stream, you must first open it.
 *
 * <p>The file format consists of a 4-byte magic/version number (see
 * {@link #MAGIC}), followed by a 4-byte magic/version number for your
 * application (provided to the constructor).  After that, we simply
 * alternate blocks of data from the embedded streams.  Each block
 * begins with an 2, 3, or 4-byte header encoding the stream ID and
 * the length of the block.  The block header format is described in
 * the class {@link MultiplexBlockRep}.  If your application needs
 * to embed further configuration or version data, just do so by
 * allocating a particular embedded stream for that information.
 *
 * <p>Although it may make sense to use this stream abstraction in a
 * multi-threaded environment, this implementation is currently <b>not
 * thread-safe.</b>
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see MultiplexInputStream
 */
public final class MultiplexOutputStream implements Closeable, Flushable
{
   /* The underlying data stream, and a boolean flag to say whether
      we’re responsible for closing it.  After this stream is closed,
      we set ‘out’ to null and forbid any further operations on this
      or any embedded streams. */
   private DataOutputStream out;
   private boolean close_p;
   
   /** 
    * The first four bytes output to the stream are given by this
    * ‘magic’ number, in the Java-standard big-endian byte order.
    * They are the bytes for “MuX” in ASCII (4D 75 58 in hexadecimal)
    * followed by a one-byte version number, currently 01.  The next
    * four bytes are a magic number determined by the client and
    * provided to the constructor.
    */
   public static final int MAGIC = 0x4D755801;

   /* A collection of all the streams we’ve handed out; INCLUDING any
      FilterOutputStreams they have been wrapped in.  We must close
      these outer streams before we can close; otherwise, any buffered
      data (such as in a BufferedOutputStream or GZIPOutputStream)
      will be lost.  See open() and close(). */
   private LinkedList<Closeable> streams = new LinkedList<Closeable>();

   /* Here is where we buffer all the output data.  Each embedded
      stream holds a byte buffer, and membership in the queue implies
      that the buffer is non-empty.  The total number of bytes in all
      queued buffers is ‘total’.  Once this number reaches ‘capacity’,
      the queue is dumped to the underlying stream. */
   private LinkedList<EmbeddedOS> queue = new LinkedList<EmbeddedOS>();
   private int total, capacity = 32768;

   private static final boolean DEBUG = false;
   private static final PrintStream dbg = System.err;

   /* Some invariant-checking code. */
   private final void check()
   {
      if(out == null) 
         throw new IllegalStateException("Stream already closed.");
      assert invariants();
   }
   private final boolean invariants()
   {
      int n = 0;
      for(EmbeddedOS e : queue) {
         assert e.queued;
         int k = e.buf.size();
         assert k > 0;
         n += k;
      }
      assert n == total;
      return true;
   }

   /** 
    * The primary constructor for objects of this class.
    *
    * @param out the underlying data output stream.  Embedded streams
    * opened with this class are interleaved here.
    * @param close_p determines whether closing this stream also
    * closes the underlying stream.
    * @param magic a four-byte word that the client can use as a
    * combined magic and version number.
    * @throws IllegalArgumentException if ‘out’ is null.
    * @throws IOException if there is a problem writing magic numbers
    * to the underlying stream.
    */
   public MultiplexOutputStream(DataOutputStream out, 
                                  boolean close_p,
                                  int magic)
      throws IOException
   {
      if(out == null)
         throw new IllegalArgumentException("Output stream was null.");
      this.out = out;
      this.close_p = close_p;
      out.writeInt(MAGIC);
      out.writeInt(magic);
   }

   /**
    * Convenience constructor for a normal output stream.  It wraps
    * ‘out’ in a DataOutputStream for you.
    */
   public MultiplexOutputStream(OutputStream out, boolean close_p, int magic)
      throws IOException
   {
      this(new DataOutputStream(out), close_p, magic);
   }

   /**
    * Convenience constructor for when ‘close_p’ is true (meaning that
    * this class <i>is</i> responsible for closing ‘out’).
    */
   public MultiplexOutputStream(DataOutputStream out, int magic)
      throws IOException
   {
      this(out, true, magic);
   }

   /**
    * Convenience constructor for a normal output stream that also
    * automatically sets ‘close_p’ to true.
    */
   public MultiplexOutputStream(OutputStream out, int magic)
      throws IOException
   {
      this(new DataOutputStream(out), true, magic);
   }

   /**
    * Retrieve the current buffer capacity of this stream.  Once the
    * buffers reach this capacity, they are all dumped as blocks to
    * the underlying stream.
    */
   public int capacity() 
   { 
      return capacity; 
   }

   /**
    * Change the current buffer capacity of this stream.  The capacity
    * is a tunable parameter.  The higher the capacity, the less
    * overhead in the output due to alternating streams.  The lower
    * the capacity, the less memory overhead is required to read and
    * write the stream.  Lowering the capacity below the current
    * buffer size does not trigger an immediate write to the
    * underlying stream, but ensures that the buffers will be flushed
    * on the next write to an embedded stream.  Very small values
    * (such as 0 and 1) are permitted, but probably not useful.
    *
    * @return this object.
    * @throws IllegalArgumentException if ‘cap’ is negative.
    */
   public MultiplexOutputStream capacity(int cap)
   {
      if(capacity < 0) throw new IllegalArgumentException();
      capacity = cap;
      return this;
   }

   /** 
    * Create a new embedded stream.  The right way to wrap up the
    * stream into a FilterOutputStream is to use an OutputStreamFilter
    * object.  This is so that we can remember the <i>outermost</i>
    * stream and close it when needed.  Otherwise, data may be lost.
    * Here is an example that opens a gzipped DataOutputStream:
    *
    * <pre>
    * OutputStreamFilter&lt;DataOutputStream&gt; factory =
    *    new OutputStreamFilter&lt;DataOutputStream&gt;() {
    *       public DataOutputStream wrap(OutputStream out)
    *       throws IOException {
    *          return new DataOutputStream(new GZIPOutputStream(out));
    *       }
    *    }
    * DataOutputStream data = mux.open(factory, 14);
    * </pre>
    *
    * @param streamID the identifier of the stream to open.
    * @param factory an object to wrap the embedded stream into a
    * filtering output stream.
    * @throws IllegalArgumentException if ‘factory’ is null, or if
    * ‘streamID’ is out of range.
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a problem creating the
    * underlying stream (reported by ‘factory’).
    * @see OutputStreamFilter
    * @see MultiplexBlockRep#MAX_STREAM_ID
    */
   public <T extends Closeable> T open (int streamID,
                                        OutputStreamFilter<T> factory)
      throws IOException
   {
      if(factory == null)
         throw new IllegalArgumentException("factory may not be null");
      MultiplexBlockRep.checkStreamID(streamID);
      check();
      
      T str = factory.wrap(new EmbeddedOS(streamID));
      streams.add(str);
      return str;
   }
   
   /**
    * Dump all the buffered data to the underlying stream.  It is
    * probably never necessary for clients to call this (it happens
    * automatically on close), but if it seems helpful for your
    * application, there’s no harm in it.
    *
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a problem writing to or flushing
    * the underlying stream.
    */
   public void flush() throws IOException
   {
      check();
      dump();
      out.flush();
   }

   private void dump() throws IOException
   {
      if(total > 0) {
         for(EmbeddedOS e : queue) {
            e.dequeue();
         }
         queue.clear();
         total = 0;
      }
   }

   private void maybeDump() throws IOException
   {
      if(total > capacity) dump();
   }

   /**
    * Close this stream.  After calling close, this object and any
    * streams it handed out are useless and should be discarded.  The
    * underlying stream is closed only if this object was created with
    * ‘close_p’ set to true.
    *
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a problem writing to or closing
    * the underlying stream.
    */
   public void close() throws IOException
   {
      check();
      for(Closeable cl : streams) {
         cl.close();
      }
      dump();
      out.flush();
      if(close_p) out.close();
      out = null;
      queue = null;
      streams = null;
   }

   private class EmbeddedOS extends OutputStream
   {
      private ByteArrayOutputStream buf = new ByteArrayOutputStream();
      private MultiplexBlockRep blk;
      private boolean queued = false;
      private EmbeddedOS(int streamID)
      {
         blk = new MultiplexBlockRep(out, streamID);
      }
      private void enqueue() 
      {
         assert !queued;
         queue.offer(this);
         queued = true;
      }
      private void dequeue() throws IOException
      {
         assert queued;
         if(DEBUG) {
            dbg.printf("*** Writing %d bytes from stream %d%n",
                       buf.size(), blk.streamID);
         }
         blk.encode(buf.size());
         buf.writeTo(out);
         buf.reset();
         queued = false;
      }
      public void write(int b) throws IOException
      {
         check();
         if(buf.size()+1 == MultiplexBlockRep.MAX_BLOCK_SIZE) {
            dump();
         }
         if(!queued) enqueue();
         buf.write(b);
         total++;
         maybeDump();
      }
      public void write(byte[] b, int off, int len) throws IOException
      {
         check();
         if(off < 0 || len < 0) throw new IndexOutOfBoundsException();
         if(len == 0) return;
         while(buf.size() + len >= MultiplexBlockRep.MAX_BLOCK_SIZE) {
            int n = MultiplexBlockRep.MAX_BLOCK_SIZE - buf.size() - 1;
            assert n > 0 && n < MultiplexBlockRep.MAX_BLOCK_SIZE : n;
            if(!queued) enqueue();
            buf.write(b, off, n);
            total += n;
            dump();
            off += n;
            len -= n;
         }
         if(!queued) enqueue();
         buf.write(b, off, len);
         total += len;
         maybeDump();
      }
   }

}
