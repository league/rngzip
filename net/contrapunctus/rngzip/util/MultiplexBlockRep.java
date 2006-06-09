package net.contrapunctus.rngzip.util;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

/**
 * This class encapsulates the encoding format used to represent block
 * headers in a multiplexed stream.  It’s probably more complicated
 * than necessary, but saves a few bytes if your stream IDs and block
 * sizes are small.
 *
 * <p>If the stream ID is zero, we use one of the following
 * representations for the block header.  The high bit of the first
 * byte must be zero.  The <i>x</i> bits represent the size of the
 * block.  There is <b>not</b> a unique representation for each block
 * size: if your block size fits within 14 bits, you may use any of
 * these.  BitOutputStream, however, always chooses the shortest
 * representation.
 * 
 * <pre>
 *   0xxxxxxx 0xxxxxxx                    14 free bits [a]
 *   0xxxxxxx 1xxxxxxx 0xxxxxxx           21 free bits [b]
 *   0xxxxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx  29 free bits [c]
 * </pre>
 * 
 * <p>If the stream ID is in the range 1–4 (inclusive), we use one of
 * the following representations.  The two highest bits of the first
 * byte must be “10”, and the next two <i>y</i> bits represent the
 * stream ID (00=stream 1, 01=stream 2, 10=stream 3, 11=stream 4).
 * 
 * <pre>
 *   10yyxxxx 0xxxxxxx                    11 free bits [d]
 *   10yyxxxx 1xxxxxxx 0xxxxxxx           18 free bits [e]
 *   10yyxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx  26 free bits [f]
 * </pre>
 *
 * <p>Finally, as long as the stream ID is less than 64, we can use
 * one of the following representations.  The two highest bits must be
 * “11” and the remaining six bits of the first byte encode the stream
 * ID.
 * 
 * <pre>
 *   11yyyyyy xxxxxxxx 0xxxxxxx           15 free bits [g]
 *   11yyyyyy xxxxxxxx 1xxxxxxx xxxxxxxx  23 free bits [h]
 * </pre>
 *
 * <p>Since we may need to read/write compatible streams from other
 * languages, here are some sample encodings that exercise each case.
 * Stream IDs and lengths are in decimal, but the bytes are in
 * hexadecimal.
 *
 * <pre>
 *    ID  Block length  Bytes
 *    ==  ============  ===========
 *     0        12,121  5E 59        using [a], or
 *                      00 DE 59     using [b], or                
 *                      00 80 AF 59  using [c], or
 *                      C0 5E 59     using [g], or
 *                      C0 00 AF 59  using [h].
 *
 *     0     1,350,449  52 B6 31     using [b], or
 *                      00 A9 9B 31  using [c], or
 *                      C0 29 9B 31  using [h].
 *
 *     0     6,723,355  01 CD 97 1B  using [c], or
 *                      C0 CD 97 1B  using [h].
 *
 *     3         2,000  AF 50        using [d], or
 *                      A0 8F 50     using [e], or
 *                      A0 80 87 D0  using [f], or
 *                      C3 0F 50     using [g], or
 *                      C3 00 87 D0  using [h].
 *
 *     2       250,042  9F A1 3A     using [e], or
 *                      90 87 D0 BA  using [f], or
 *                      C2 07 D0 BA  using [h].
 *
 *     4     8,385,630  B1 FF F4 5E  using [f], or
 *                      C4 FF F4 5E  using [h].
 *
 *    42        10,222  EA 4F 6E     using [g], or
 *                      EA 00 A7 EE  using [h].
 *
 *    60     4,874,941  FC 94 E2 BD  using [h].
 * </pre>
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see MultiplexOutputStream
 * @see MultiplexInputStream
 */
public final class MultiplexBlockRep
{
   /**
    * The largest possible block size, in bytes, is {@value} (8 MB).
    */
   public static final int MAX_BLOCK_SIZE = 1 << 23; // 8M

   /**
    * Stream identifiers start from zero and must be less than this
    * constant, {@value}.
    */
   public static final int MAX_STREAM_ID = 1 << 6; // 64

   /**
    * This method validates that the stream ID ‘sid’ is within range.
    * It must be non-negative and less than or equal to
    * <code>MAX_STREAM_ID</code>.
    * @throws IllegalArgumentException if ‘sid’ is out of range.
    */
   public static void checkStreamID(int sid)
   {
      if(sid < 0) {
         throw new IllegalArgumentException("streamID may not be negative");
      }
      if(sid >= MAX_STREAM_ID) {
         throw new IllegalArgumentException
            ("streamID was "+sid+"; it should not exceed "+MAX_STREAM_ID);
      }
   }

   int streamID;
   private int size;
   private OutputStream os;
   private InputStream is;

   /**
    * Construct a block representation for encoding.  Headers will be
    * output to the stream ‘os’ each time <code>encode</code> is
    * called.
    * @throws AssertionError if ‘os’ is null, or if the ‘streamID’ is
    * out of range (assuming assertions are enabled).
    */
   public MultiplexBlockRep(OutputStream os, int streamID)
   {
      assert os != null;
      assert streamID >= 0 && streamID < MAX_STREAM_ID : streamID;
      this.os = os;
      this.streamID = streamID;
   }

   /**
    * Construct a block representation for decoding.  A header will be
    * read from the stream ‘is’ each time <code>decode</code> is
    * called.
    * @throws AssertionError if ‘is’ is null (assuming assertions are
    * enabled).
    */
   public MultiplexBlockRep(InputStream is)
   {
      assert is != null;
      this.is = is;
   }  

   /**
    * Output a header for a block of ‘size’ bytes.
    * @throws AssertionError if this object was not constructed with
    * an <code>OutputStream</code> or if ‘size’ is out of range.
    * @see #MultiplexBlockRep(OutputStream, int)
    * @see #MAX_BLOCK_SIZE
    */
   public void encode(int sz) throws IOException
   {
      assert os != null;
      size = sz;
      assert size >= 0 && size < MAX_BLOCK_SIZE : size;
      if(streamID == 0) encodeS0();
      else if(streamID <= 4) encodeS4();
      else encodeOther();
   }

   /**
    * Input a block header, and return the size of that block.
    * @throws AssertionError if this objects was not constructed with
    * an <code>InputStream</code>.
    * @see #MultiplexBlockRep(InputStream)
    */
   public int decode() throws IOException
   {
      assert is != null;
      int b0 = readByte();
      if((b0 & 0x80) == 0) {    // 0xxx xxxx
         return decodeS0(b0); 
      }
      else if((b0 & 0x40) == 0) { // 10yy xxxx
         return decodeS4(b0); 
      }
      else {                    // 11yy yyyy
         assert (b0 & 0xC0) == 0xC0 : b0; 
         return decodeOther(b0);
      }
   }

   private void writeBits(int k, int mask) throws IOException
   {
      os.write(mask | size >> k);
      size &= (1 << k) - 1;
   }

   private int readByte() throws IOException
   {
      int b = is.read();
      if(b == -1) throw new EOFException();
      else return b;
   }

   private int readBytes(int max) throws IOException
   {
      boolean more;
      for(more = true;  more && max > 1;  max--) {
         int b = readByte();
         if((b & 0x80) == 0) more = false; 
         else b &= 0x7F;
         size = b | size << 7;
      }
      if(more) {
         int b = readByte();
         size = b | size << 8;
      }
      return size;
   }              

   /* Use 2, 3, or 4 bytes, beginning with "0" bit:
    *  0xxxxxxx 0xxxxxxx                    14 free bits (7+7)
    *  0xxxxxxx 1xxxxxxx 0xxxxxxx           21 free bits (7+7+7)
    *  0xxxxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx  29 free bits (7+7+7+8)
    */
   private void encodeS0() throws IOException
   {
      if(size < 1 << 14) {
         writeBits(7, 0x00);
         writeBits(0, 0x00);
      }
      else if(size < 1 << 21) {
         writeBits(14, 0x00);
         writeBits( 7, 0x80);
         writeBits( 0, 0x00);
      }
      else {
         assert size < 1 << 29 : size;
         writeBits(22, 0x00);
         writeBits(15, 0x80);
         writeBits( 8, 0x80);
         writeBits( 0, 0x00);
      }
   }

   private int decodeS0(int b0) throws IOException
   {
      streamID = 0;
      size = b0;
      return readBytes(3);
   }
   
   /* Use 2, 3, or 4 bytes, encoding "10" and the stream ID in the
    * first 4 bits.
    *  10yyxxxx 0xxxxxxx                    11 free bits (4+7)
    *  10yyxxxx 1xxxxxxx 0xxxxxxx           18 free bits (4+7+7)
    *  10yyxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx  26 free bits (4+7+7+8)
    */    
   private void encodeS4() throws IOException
   {
      int mask = 0;
      switch(streamID) {
      case 1: mask = 0x80; break; // 1000
      case 2: mask = 0x90; break; // 1001
      case 3: mask = 0xA0; break; // 1010
      case 4: mask = 0xB0; break; // 1011
      default: assert false : streamID;
      }         
      if(size < 1 << 11) {
         writeBits(7, mask);
         writeBits(0, 0x00);
      }
      else if(size < 1 << 18) {
         writeBits(14, mask);
         writeBits( 7, 0x80);
         writeBits( 0, 0x00);
      }   
      else {
         assert size < 1 << 26 : size;
         writeBits(22, mask);
         writeBits(15, 0x80);
         writeBits( 8, 0x80);
         writeBits( 0, 0x00);
      }
   }

   private int decodeS4(int b0) throws IOException
   {
      switch(b0 & 0xF0) {
      case 0x80: streamID = 1; break; // 1000 
      case 0x90: streamID = 2; break; // 1001 
      case 0xA0: streamID = 3; break; // 1010 
      case 0xB0: streamID = 4; break; // 1011
      default: assert false : b0;
      }
      size = b0 & 0x0F;
      return readBytes(3);
   }
   
   /* Use 3 or 4 bytes, encoding the stream ID in the first one.
    *  11yyyyyy xxxxxxxx 0xxxxxxx           15 free bits (8+7)
    *  11yyyyyy xxxxxxxx 1xxxxxxx xxxxxxxx  23 free bits (8+7+8)
    */
   private void encodeOther() throws IOException
   {
      assert streamID < 1 << 6 : streamID;
      os.write(0xC0 | streamID);
      if(size < 1 << 15) {
         writeBits(7, 0x00);
         writeBits(0, 0x00);
      }
      else {
         assert size < 1 << 23 : size;
         writeBits(15, 0x00);
         writeBits( 8, 0x80);
         writeBits( 0, 0x00);
      }
   }

   private int decodeOther(int b0) throws IOException
   {
      streamID = b0 & 0x3F;
      int b1 = readByte();
      size = b1;
      return readBytes(2);
   }
}

