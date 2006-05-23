package net.contrapunctus.rngzip.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite for the Multiplex* classes.
 */
public class MultiplexBlockTest
{
  @Test
   public void testExamples() throws IOException
   {
      testEnc(0,12121,new byte[]{(byte)0x5E,(byte)0x59});
      testDec(0,12121,new byte[]{(byte)0x5E,(byte)0x59});
      testDec(0,12121,new byte[]{(byte)0x00,(byte)0xDE,(byte)0x59});
      testDec(0,12121,new byte[]{(byte)0xC0,(byte)0x5E,(byte)0x59});
      testDec(0,12121,new byte[]{(byte)0x00,(byte)0x80,(byte)0xAF,(byte)0x59});
      testDec(0,12121,new byte[]{(byte)0xC0,(byte)0x00,(byte)0xAF,(byte)0x59});

      testEnc(0,1350449,new byte[]{(byte)0x52,(byte)0xB6,(byte)0x31});
      testDec(0,1350449,new byte[]{(byte)0x52,(byte)0xB6,(byte)0x31});
      testDec(0,1350449,new byte[]{(byte)0x00,(byte)0xA9,(byte)0x9B,(byte)0x31});
      testDec(0,1350449,new byte[]{(byte)0xC0,(byte)0x29,(byte)0x9B,(byte)0x31});

      testEnc(0,6723355,new byte[]{(byte)0x01,(byte)0xCD,(byte)0x97,(byte)0x1B});
      testDec(0,6723355,new byte[]{(byte)0x01,(byte)0xCD,(byte)0x97,(byte)0x1B});
      testDec(0,6723355,new byte[]{(byte)0xC0,(byte)0xCD,(byte)0x97,(byte)0x1B});

      testEnc(3,2000,new byte[]{(byte)0xAF,(byte)0x50});
      testDec(3,2000,new byte[]{(byte)0xAF,(byte)0x50});
      testDec(3,2000,new byte[]{(byte)0xA0,(byte)0x8F,(byte)0x50});
      testDec(3,2000,new byte[]{(byte)0xC3,(byte)0x0F,(byte)0x50});
      testDec(3,2000,new byte[]{(byte)0xA0,(byte)0x80,(byte)0x87,(byte)0xD0});
      testDec(3,2000,new byte[]{(byte)0xC3,(byte)0x00,(byte)0x87,(byte)0xD0});

      testEnc(2,250042,new byte[]{(byte)0x9F,(byte)0xA1,(byte)0x3A});
      testDec(2,250042,new byte[]{(byte)0x9F,(byte)0xA1,(byte)0x3A});
      testDec(2,250042,new byte[]{(byte)0x90,(byte)0x87,(byte)0xD0,(byte)0xBA});
      testDec(2,250042,new byte[]{(byte)0xC2,(byte)0x07,(byte)0xD0,(byte)0xBA});

      testEnc(4,8385630,new byte[]{(byte)0xB1,(byte)0xFF,(byte)0xF4,(byte)0x5E});
      testDec(4,8385630,new byte[]{(byte)0xB1,(byte)0xFF,(byte)0xF4,(byte)0x5E});
      testDec(4,8385630,new byte[]{(byte)0xC4,(byte)0xFF,(byte)0xF4,(byte)0x5E});

      testEnc(42,10222,new byte[]{(byte)0xEA,(byte)0x4F,(byte)0x6E});
      testDec(42,10222,new byte[]{(byte)0xEA,(byte)0x4F,(byte)0x6E});
      testDec(42,10222,new byte[]{(byte)0xEA,(byte)0x00,(byte)0xA7,(byte)0xEE});

      testEnc(60,4874941,new byte[]{(byte)0xFC,(byte)0x94,(byte)0xE2,(byte)0xBD});
      testDec(60,4874941,new byte[]{(byte)0xFC,(byte)0x94,(byte)0xE2,(byte)0xBD});
   }

   private void testEnc(int sid, int size, byte[] bs) throws IOException
   {
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      MultiplexBlockRep ro = new MultiplexBlockRep(bo, sid);
      ro.encode(size);
      byte[] rs = bo.toByteArray();
      assert(bs.length == rs.length);
      for(int i = 0;  i < bs.length;  i++) {
         assert(bs[i] == rs[i]);
      }
   }

   private void testDec(int sid, int size, byte[] bs) throws IOException
   {
      ByteArrayInputStream bi = new ByteArrayInputStream(bs);
      MultiplexBlockRep ri = new MultiplexBlockRep(bi);
      int sz = ri.decode();
      assert(sid == ri.streamID);
      assert(size == sz);
   }

  @Test
   public void testBlockLimit() throws IOException
   {
      /* create a huge array */
      int size = MultiplexBlockRep.MAX_BLOCK_SIZE * 2;
      byte[] buf = new byte [size];
      buf[8] = 36;
      buf[MultiplexBlockRep.MAX_BLOCK_SIZE] = 71;
      buf[MultiplexBlockRep.MAX_BLOCK_SIZE+71] = 108;
      /* write it in big chunks */
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      MultiplexOutputStream mo = new MultiplexOutputStream(bo, 0);
      OutputStream os = mo.open(0, new OutputStreamFilter<OutputStream>() {
         public OutputStream wrap(OutputStream out) { return out; }
      });
      int off = 0, len = size;
      os.write(buf, off, 1 << 16);
      off += 1 << 16;
      len -= 1 << 16;
      os.write(buf, off, 1 << 8);
      off += 1 << 8;
      len -= 1 << 8;
      os.write(buf, off, len);
      mo.close();
   }

  @Test
   public void testStreams() throws IOException
   {
      /* first write */
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      MultiplexOutputStream mo = new MultiplexOutputStream(bo, 0xDEADBEEF);
      PrintStream pr = mo.open(0, new OutputStreamFilter<PrintStream>() {
         public PrintStream wrap(OutputStream out) throws IOException {
            return new PrintStream(out);
         }});
      DataOutputStream dat = mo.open(1, new OutputStreamFilter<DataOutputStream>() {
         public DataOutputStream wrap(OutputStream out) throws IOException {
            return new DataOutputStream(new BufferedOutputStream(out));
         }});
      pr.println("Hello, world");
      dat.writeInt(0xCAFEBABE);
      dat.writeShort(0xBEEF);
      pr.println("Here now");
      mo.flush();
      dat.writeInt(0x12345678);
      pr.println("The end.");
      mo.close();
      /* show it */
      byte[] buf = bo.toByteArray();
      hexdump(buf, System.err);
      /* then read */
      ByteArrayInputStream bi = new ByteArrayInputStream(buf);
      MultiplexInputStream mi = new MultiplexInputStream(bi);
      assert(0xDEADBEEF == mi.magic());
      DataInputStream di = new DataInputStream(mi.open(1));
      BufferedReader br = new BufferedReader(new InputStreamReader(mi.open(0)));
      assert("Hello, world".equals(br.readLine()));
      assert("Here now".equals(br.readLine()));
      assert(0xCAFEBABE == di.readInt());
      assert(0xBEEF == di.readUnsignedShort());
      assert(0x12345678 == di.readInt());
      assert("The end.".equals( br.readLine()));
      assertNull(br.readLine());
      try {
         di.readByte();
         assertTrue(false);
      }
      catch(EOFException e) {
      }          
      finally {
         //mi.close();  
      }
   }   

   public static void hexdump(byte[] buf, PrintStream out)
   {
      if(System.getProperty("VERBOSE_test") == null) return;
      byte[] asc = new byte[16];
      int n;
      /* find the next multiple of 16 */
      for(n = buf.length + 1;  n % 16 != 0;  n++)
         { }
      boolean valid;
      for(int i = 0;  i <= n;  i++) {
         valid = i < buf.length;
         if(i % 16 == 0) {
            if(i > 0) {
               out.print(" |");
               for(byte c : asc) {
                  out.print(Character.isISOControl(c)? '.' : (char)c);
               }
               out.print('|');
            }
            out.println();
            if(valid) {
               out.printf("%04X ", i);
            }
         }
         if(i % 8 == 0) {
            out.print(' ');
         }
         if(valid) {
            asc[i % 16] = buf[i];
            out.printf("%02X ", buf[i]);
         }
         else {
            asc[i % 16] = 0;
            out.print("   ");
         }
      }
      out.println();
   }
   
}
