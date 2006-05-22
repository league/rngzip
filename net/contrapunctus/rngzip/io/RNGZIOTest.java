package net.contrapunctus.rngzip.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.contrapunctus.rngzip.util.MultiplexBlockTest;

/**
 * Test suite for RNGZ input and output streams.
 */
public class RNGZIOTest extends junit.framework.TestCase
{
   public static Test suite()
   {
      TestSuite ts = new TestSuite();
      for(RNGZSettings.BitCoding x : RNGZSettings.BitCoding.values()) {
         for(RNGZSettings.DataCompression y : RNGZSettings.DataCompression.values()) {
            for(RNGZSettings.DataCompression z : RNGZSettings.DataCompression.values()) {
               ts.addTest(new RNGZIOTest(new RNGZSettings(x,y,z)));
            }
         }
      }
      return ts;
   }

   private RNGZSettings settings;
   private final int MIN = 1;
   private final int MAX = 5;
   private ChoiceCoder[] cc = new ChoiceCoder[MAX];
   private RNGZIOTest(RNGZSettings s)
   {
      super(s.toString());
      settings = s;
   }

   private void setupCoders() 
   {
      for(int i = MIN;  i < MAX;  i++) {
         cc[i] = settings.makeChoiceCoder(i, i);
      }
   }
   
   public void runTest() throws Exception
   {
      // first write
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      RNGZOutputStream zo = new RNGZOutputStream(bo, settings);
      LinkedList<String> path = new LinkedList<String>();
      path.add("yo");
      setupCoders();
      zo.writeChoice(cc[3], 2);
      zo.writeChoice(cc[3], 1);
      zo.writeContent(path, "Yup");
      zo.writeChoice(cc[4], 0);
      zo.writeChoice(cc[2], 1);
      zo.writeChoice(cc[4], 0);
      zo.writeContent(path, "Foo");
      zo.writeContent(path, "ABRACADABRA".toCharArray(), 3, 5);
      zo.close();
      // then read 
      byte[] buf = bo.toByteArray();
      MultiplexBlockTest.hexdump(buf, System.err);
      ByteArrayInputStream bi = new ByteArrayInputStream(buf);
      RNGZInputStream zi = new RNGZInputStream(bi, new RNGZSettings());
      setupCoders();
      assertEquals(2, zi.readChoice(cc[3]));
      assertEquals(1, zi.readChoice(cc[3]));
      assertEquals("Yup", zi.readContent(path));
      assertEquals(0, zi.readChoice(cc[4]));
      assertEquals(1, zi.readChoice(cc[2]));
      assertEquals(0, zi.readChoice(cc[4]));
      assertEquals("Foo", zi.readContent(path));
      assertEquals("ACADA", zi.readContent(path));
      zi.close();
   }
}
