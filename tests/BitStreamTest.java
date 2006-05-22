package net.contrapunctus.rngzip.util;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test suite for {@link BitInputStream} and {@link BitOutputStream}.
 */
public class BitStreamTest extends junit.framework.TestCase
{
   ByteArrayOutputStream a;
   BitOutputStream b;
   
   public void setUp()
   {
      a = new ByteArrayOutputStream();
      b = new BitOutputStream(a);
   }

   /** Tests that {@code writeBit(b)} is equivalent to {@code
    * writeBit(b? 1 : 0)}. 
    */
   public void testWriteBool() throws IOException
   {
      b.writeBit(true);
      b.writeBit(false);
      b.writeBit(3);
      b.writeBit(true);
      b.writeBit(-1);
      b.writeBit(0);
      b.writeBit(false);        // 1011 100 0 <- last bit is padding
      b.close();                // 0xB8
      byte[] bs = a.toByteArray();
      assertEquals(1, bs.length);
      assertEquals((byte)0xB8, bs[0]);
   }

   /** The inverse of {@link #testWriteBool()}. */
   public void testReadBool() throws IOException
   {
      byte[] bs = new byte[] {(byte)0xB8};
      BitInputStream c = new BitInputStream(new ByteArrayInputStream(bs));
      assertEquals(true,  c.readBit());
      assertEquals(false, c.readBit());
      assertEquals(true,  c.readBit());
      assertEquals(true,  c.readBit());
      assertEquals(true,  c.readBit());
      assertEquals(false, c.readBit());
      assertEquals(false, c.readBit());
   }  

   /** Tests writing more than one byte */
   public void testRollover() throws IOException
   {
      b.writeBit(true);         // 1101
      b.writeBit(true);
      b.writeBit(false);
      b.writeBit(true);
      b.writeBit(false);        // 0001
      b.writeBit(false);
      b.writeBit(false);
      b.writeBit(true);
      b.writeBit(true);         // 1010
      b.writeBit(false);
      b.writeBit(true);
      b.writeBit(false);
      b.writeBit(true);         // 1{000}
      b.close();
      byte[] bs = a.toByteArray();
      assertEquals(2, bs.length);
      assertEquals((byte)0xD1, bs[0]);
      assertEquals((byte)0xA8, bs[1]);
   }

   /** The inverse of {@link #testRollover()}. */
   public void testReadRollover() throws IOException
   {
      byte[] bs = new byte[] {(byte)0xD1,
                              (byte)0xA8};
      BitInputStream c = new BitInputStream(new ByteArrayInputStream(bs));
      assertEquals(true , c.readBit()); // 1101
      assertEquals(true , c.readBit());
      assertEquals(false, c.readBit());
      assertEquals(true , c.readBit());
      assertEquals(false, c.readBit()); // 0001
      assertEquals(false, c.readBit());
      assertEquals(false, c.readBit());
      assertEquals(true , c.readBit());
      assertEquals(true , c.readBit()); // 1010
      assertEquals(false, c.readBit());
      assertEquals(true , c.readBit());
      assertEquals(false, c.readBit());
      assertEquals(true , c.readBit()); // 1{000}
   }

   /** Test multiple invocations of writeBits */
   public void testWriteBits() throws IOException
   {
      b.writeBits(0xCAFEBA, 24); // CA FE BA
      b.writeBits(0xFE8, 9);     // {111} 1 1110 1000
      b.writeBits(0xBEBF35, 24); // 1011 1110 1011 1111 0011 0101
      // total of 57 bits written, = 8 bytes  (7 bytes + 1 extra bit)
      b.close();
      // 1111 0100 0101 1111 0101 1111 1001 1010 1{000} {0000}
      //    F    4    5    F    5    F    9    A     8      0
      byte[] bs = a.toByteArray();
      assertEquals(8, bs.length);
      assertEquals((byte)0xCA, bs[0]);
      assertEquals((byte)0xFE, bs[1]);
      assertEquals((byte)0xBA, bs[2]);
      assertEquals((byte)0xF4, bs[3]);
      assertEquals((byte)0x5F, bs[4]);
      assertEquals((byte)0x5F, bs[5]);
      assertEquals((byte)0x9A, bs[6]);
      assertEquals((byte)0x80, bs[7]);
   }

   /** Inverse of testWriteBits */
   public void testReadBits() throws IOException
   {
      byte[] bs = new byte[] {(byte)0xCA,
                              (byte)0xFE,
                              (byte)0xBA,
                              (byte)0xF4,
                              (byte)0x5F,
                              (byte)0x5F,
                              (byte)0x9A,
                              (byte)0x80};
      BitInputStream c = new BitInputStream(new ByteArrayInputStream(bs));
      assertEquals(0xCAFEBA, c.readBits(24));
      assertEquals(0x1E8,    c.readBits(9));
      assertEquals(0xBEBF35, c.readBits(24));
   }

   /** It should be okay to call writeBits with ‘n’ set to zero. */
   public void testWriteNoBits() throws IOException
   {
      b.writeBits(0xCAFE, 0);
      b.close();
      byte[] bs = a.toByteArray();
      assertEquals(0, bs.length);
   }

   public void testWriteMultipleOfEight() throws IOException
   {
      b.writeBits(0xABC, 9);    // 101{0 1011 1100}
      b.writeBits(0xDEF, 7);    // 1101 1{110 1111}
      byte[] bs = a.toByteArray();
      // 0101 1110 0110 1111 = 5E 6F
      assertEquals(2, bs.length);
      assertEquals(0x5E, bs[0]);
      assertEquals(0x6F, bs[1]);
   }  
      
   /** Test the mask-making helper function. */
   public void testMask()
   {
      assertEquals(0x000, BitOutputStream.mask(0));
      assertEquals(0x00F, BitOutputStream.mask(4));
      assertEquals(0x01F, BitOutputStream.mask(5));
      assertEquals(0x3FF, BitOutputStream.mask(10));
      assertEquals(0x0FFFFFFF, BitOutputStream.mask(28));
      assertEquals(0x1FFFFFFF, BitOutputStream.mask(29));
      assertEquals(0x3FFFFFFF, BitOutputStream.mask(30));
      assertEquals(0x7FFFFFFF, BitOutputStream.mask(31));
   }
}

