package net.contrapunctus.rngzip.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import net.contrapunctus.rngzip.util.MultiplexBlockTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * Test suite for RNGZ input and output streams.
 */
@RunWith(Parameterized.class)
public class RNGZIOTest 
{
  @Parameterized.Parameters 
  public static LinkedList<Object[]> data() {
    LinkedList<Object[]> list = new LinkedList<Object[]>();
    for(RNGZSettings.BitCoding x 
          : RNGZSettings.BitCoding.values()) {
      for(RNGZSettings.DataCompression y 
            : RNGZSettings.DataCompression.values()) {
        for(RNGZSettings.DataCompression z 
              : RNGZSettings.DataCompression.values()) {
          list.add( new Object[] { new RNGZSettings(x, y, z) } );
        }
      }
    }
    return list;
  }

  public RNGZIOTest( RNGZSettings s )
  {
    settings = s;
  }

  public String toString() 
  {
    return settings.toString();
  }

  private RNGZSettings settings;
  private final int MIN = 1;
  private final int MAX = 5;
  private ChoiceCoder[] cc = new ChoiceCoder[MAX];

  @Before
   public void setupCoders() 
   {
      for(int i = MIN;  i < MAX;  i++) {
         cc[i] = settings.makeChoiceCoder(i, i);
      }
   }

  @Test
  public void checkIO() throws Exception
  {
    try 
      {
        runTest(); 
      }
    catch( Throwable x ) 
      {
        throw new Error(this + ":  " + x.getMessage());
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
