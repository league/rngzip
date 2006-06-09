package net.contrapunctus.rngzip.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is used to write individual bits to an output stream.
 * It is compatible with BitInputStream.  Internally, it keeps a
 * buffer of bits, and outputs them to the underlying stream once the
 * buffer is full.  At this time, the buffer holds exactly 8 bits, but
 * it could reasonably be any multiple of 8, such as 32.
 * 
 * <p> A non-full buffer is flushed only upon calling {@link #close()
 * close()}, which <i>optionally</i> closes the underlying stream.  We
 * do not provide a ‘flush()’ method, because it is nonsensical to
 * flush a non-full buffer if there is more output to come.  Such a
 * stream could not be read back correctly by BitInputStream.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see BitInputStream
 */
public final class BitOutputStream implements Closeable
{
   /* The ‘pos’ field represents which bit (0–7) we will read next.
      The bits are pushed into the least-significant side of ‘buf’. */
   private static final int BUFSIZE = 8;
   private short buf, pos;
   private OutputStream out;
   private boolean close_p;

   /* As soon as ‘pos’ reaches 8, the buffer should be dumped to the
      stream, so pos should always be between 0 and 7 (inclusive). */
   private boolean invariants()
   {
      assert pos >= 0 && pos < BUFSIZE : pos;
      return true;
   }

   /* We set the ‘out’ to null to indicate that the stream has been
      closed.  This is useful for checking such conditions on entry to
      each public method. */
   private void check() 
   {
      if(out == null) 
         throw new IllegalStateException("Stream already closed.");
      assert invariants();
   }
   
   /** 
    * Construct a BitOutputStream on top of the provided OutputStream.
    * 
    * @param out the underlying binary output stream.
    * @param close_p determines whether closing this stream also
    * closes the underlying stream.
    * @throws IllegalArgumentException if ‘out’ is null.
    */
   public BitOutputStream(OutputStream out, boolean close_p)
   {
      if(out == null) 
         throw new IllegalArgumentException("Output stream was null.");
      this.out = out;
      this.close_p = close_p;
      this.buf = 0;
      this.pos = 0;
   }

   /** 
    * Convenience constructor for the common case where the underlying
    * stream <i>should</i> be closed when this stream is closed.
    * Equivalent to {@code BitOutputStream(out, true)}.
    * 
    * @param out the underlying binary output stream.
    * @throws IllegalArgumentException if ‘out’ is null.
    */
   public BitOutputStream(OutputStream out)
   {
      this(out, true);
   }

   /** 
    * Write a single bit to the output stream.  The expression {@code
    * writeBit(b)} is equivalent to {@code writeBit(b? 1 : 0)}.
    * 
    * @param b the bit to write: true represents a 1, while false
    * represents 0.
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a failure writing to the
    * underlying stream.
    */
   public void writeBit(boolean b) throws IOException
   {
      check();
      pos++;
      buf <<= 1;
      if(b) buf |= 1;
      maybeDumpByte();
   }

   /* Check whether the bit buffer is full; dump its if needed. */
   private void maybeDumpByte() throws IOException
   {
      if(pos == BUFSIZE) dumpByte();
      else assert invariants();
   }

   /* Dump the contents of the bit buffer and reset it. */
   private void dumpByte() throws IOException
   {
      out.write(buf);
      buf = 0;
      pos = 0;
      /* invariants trivially hold here */
   }
   
   /** 
    * Write a single bit to the output stream.  The expression {@code
    * writeBit(x)} is equivalent to {@code writeBit(x != 0)}.
    * 
    * @param x the bit to write: any non-zero value represents a 1.
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a failure writing to the
    * underlying stream.
    */
   public void writeBit(int x) throws IOException
   {
      writeBit(x != 0);
   }

   /** 
    * Write the ‘n’ least-significant bits of ‘x’ to the stream.
    * 
    * @param x the source of the bits.  The ‘n’ least-significant bits
    * are used, but they are written in order from most- to
    * least-significant.
    * @param n the number of bits to write.  This method has no effect
    * if ‘n’ is zero.
    *
    * @throws IllegalArgumentException if ‘n’ is negative or &gt;64.
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if there is a failure writing to the
    * underlying stream.
    */
   public void writeBits(long x, int n) throws IOException
   {
      if(n < 0 || n > 64)
         throw new IllegalArgumentException("n must be 0..64");
      check();
      /* Determine how many free bits remain in the buffer; there 
         will be at least one. */
      int capacity = BUFSIZE - pos;
      assert capacity > 0;
      while(n > capacity) {
         /* We cannot fit all ‘n’ bits; just write ‘capacity’ bits to
            fill the buffer, and do the rest later. */
         n -= capacity;
         pos += capacity;
         buf <<= capacity;
         buf |= ((x >>> n) & mask(capacity));
         dumpByte();
         capacity = BUFSIZE;
      }
      /* The remaining ‘n’ bits in ‘x’ now fit within the buffer. */
      pos += n;
      buf <<= n;
      buf |= x & mask(n);
      maybeDumpByte();
   }
   
   /* Construct a bit mask with ‘k’ ones in the least-significant
      positions.  That is, mask(4)==0x0F, mask(5)==0x1F, mask(0)==0.
      ‘k’ must be non-negative. */
   static int mask(int k)
   {
      assert k >= 0 && k < 32;
      return (1 << k) - 1;
   }

   /**
    * Flushes any remaining buffered bits to the stream (padding any
    * unused bits with zeros), then closes the stream.  If this stream
    * was created with ‘close_p’ set to true, this also closes the
    * underlying stream.
    *
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if underlying stream complains during
    * flushing or closing.
    */
   public void close() throws IOException
   {
      check();
      flush();
      if(close_p) {
         out.close();
      }
      /* stream may no longer be used after close() */
      out = null;
   }
   
   /* Here is how we flush the stream.  This is not exported publicly
      because flushing before the end of the stream will corrupt the
      output.  (It would not be readable by BitInputStream.) */
   private void flush() throws IOException
   {
      if(pos > 0) {
         buf <<= (BUFSIZE - pos);     // pad with zeros
         dumpByte();
      }
      out.flush();
   }
}
