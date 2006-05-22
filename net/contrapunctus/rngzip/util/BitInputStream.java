package net.contrapunctus.rngzip.util;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is used to read individual bits from an input stream.
 * It is compatible with BitOutputStream.
 *
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 *
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 * @see BitOutputStream
 */
public final class BitInputStream implements Closeable
{
   /* To be compatible with BitOutputStream, we must do the opposite:
    * we fill the buffer from MOST significant to LEAST.  The ‘mask’
    * has exactly one bit set, which is the next one to load.  When
    * ‘mask’ becomes zero, that means the buffer is empty and we need
    * to read a new byte from the underlying stream.
    */
   private byte buf;
   private int mask;
   private InputStream in;
   private boolean close_p;
   
   /* We set ‘in’ to null when the stream is closed. */
   private void check()
   {
      if(in == null)
         throw new IllegalStateException("Stream already closed.");
   }

   /**
    * Construct a BitInputStream on top of the provided InputStream.
    *
    * @param in The underlying binary input stream.
    * @param close_p Determines whether closing this stream also
    * closes the underlying stream.
    * @throws IllegalArgumentException if ‘in’ is null.
    */
   public BitInputStream(InputStream in, boolean close_p)
   {
      if(in == null)
         throw new IllegalArgumentException("Input stream was null");
      this.in = in;
      this.close_p = close_p;
      this.buf = 0;
      this.mask = 0;            // start with empty buffer
   }

   /** 
    * Convenience constructor for the common case where the underlying
    * stream <i>should</i> be closed when this stream is closed.
    * Equivalent to {@code BitInputStream(in, true)}.
    * 
    * @param in The underlying binary input stream.
    * @throws IllegalArgumentException if ‘in’ is null.
    */
   public BitInputStream(InputStream in)
   {
      this(in, true);
   }
   
   /**
    * Read a single bit from the input stream.
    *
    * @return the bit read, as a boolean value.
    * @throws IOException if there is a failure reading the underlying
    * stream.
    * @throws EOFException if there is an end-of-file condition on the
    * underlying stream.
    * @throws IllegalStateException if the stream is already closed.
    */
   public boolean readBit() throws IOException
   {
      check();
      if(mask == 0) {
         /* Buffer is empty */
         int r = in.read();
         if(r < 0) throw new EOFException();
         assert r >= 0 && r <= 255 : r;
         buf = (byte) r;
         mask = 0x80;
      }
      int r = buf & mask;
      mask >>>= 1;
      return r != 0;
   }

   /**
    * Read ‘n’ bits from the input stream, and return them in the
    * least-significant bits of a long integer value.
    *
    * @param n The number of bits to read.
    * @return A long integer value where the ‘n’ least-significant
    * bits come from the stream, and any other bits are zero.
    * @throws IllegalArgumentException if ‘n’ is negative or &gt;64.
    * @throws IOException if there is a failure reading the underlying
    * stream.
    * @throws EOFException if there is an end-of-file condition on the
    * underlying stream.
    * @throws IllegalStateException if the stream is already closed.
    */
   public long readBits(int n) throws IOException
   {
      if(n < 0 || n > 64)
         throw new IllegalArgumentException("n must be 0..64");
      long r = 0;
      /* We keep this simple by just calling readBit() repeatedly. */
      for(int i = 0;  i < n;  i++) {
         r <<= 1;
         if(readBit()) r |= 1;
      }
      return r;
   }

   /**
    * It is not strictly necessary to close a BitInputStream (unlike a
    * BitOutputStream), but if ‘close_p’ was set to true, this will
    * close the underlying stream. 
    *
    * @throws IllegalStateException if the stream is already closed.
    * @throws IOException if the underlying stream complains during
    * closing.
    */
   public void close() throws IOException
   {
      check();
      if(close_p) {
         in.close();
      }
      in = null;
   }
}
