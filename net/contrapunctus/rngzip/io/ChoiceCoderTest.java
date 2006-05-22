package net.contrapunctus.rngzip.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;


/**
 * Test suite for choice encoders and decoders.
 */
public class ChoiceCoderTest extends junit.framework.TestCase
{
   private static final int MIN = 1;
   private static final int MAX = 20;
   private ChoiceEncoder[] es;
   private ChoiceDecoder[] ds;
   
   private void setupEncoders (ChoiceEncoderFactory ef)
   {
      es = new ChoiceEncoder[MAX];
      for(int i = MIN;  i < MAX;  i++) {
         es[i] = ef.makeChoiceEncoder(i, i);
      }
   }
   
   private void setupDecoders (ChoiceDecoderFactory df)
   {
      ds = new ChoiceDecoder[MAX];
      for(int i = MIN;  i < MAX;  i++) {
         ds[i] = df.makeChoiceDecoder(i, i);
      }
   }
   
   private void setupSimple()
   {
      SimpleChoiceFactory scf = new SimpleChoiceFactory();
      setupEncoders(scf);
      setupDecoders(scf);
   }
   
   private void setupHuffman()
   {
      HuffmanChoiceFactory hcf = new HuffmanChoiceFactory();
      setupEncoders(hcf);
      setupDecoders(hcf);
   }
   
   /* test wether decode(encode()) is identity */
   private void runIdentity() throws IOException
   {
      final int ITER = 5;
      ByteArrayOutputStream ao = new ByteArrayOutputStream();
      BitOutputStream bo = new BitOutputStream(ao);
      /* do a bunch of encoding */
      for(int k = 0;  k < ITER;  k++) {
         for(int i = MIN;  i < MAX;  i++) {
            es[i].encode(0, bo);
            for(int j = 0;  j < i;  j++) {
               es[i].encode(j, bo);
            }
            es[i].encode(0, bo);
            es[i].encode(i-1, bo);
         }
      }
      /* extract the bytes */
      bo.close();
      byte[] buf = ao.toByteArray();
      //System.err.printf("%d bytes.%n", buf.length);
      ByteArrayInputStream ai = new ByteArrayInputStream(buf);
      BitInputStream bi = new BitInputStream(ai);
      /* now do decoding */
      for(int k = 0;  k < ITER;  k++) {
         for(int i = MIN;  i < MAX;  i++) {
            assertEquals(0, ds[i].decode(bi));
            for(int j = 0;  j < i;  j++) {
               assertEquals(j, ds[i].decode(bi));
            }
            assertEquals(0, ds[i].decode(bi));
            assertEquals(i-1, ds[i].decode(bi));
         }
      }
   }

   public void testIdentitySimple() throws IOException
   {
      setupSimple();
      runIdentity();
   }

   public void testIdentityHuffman() throws IOException
   {
      setupHuffman();
      runIdentity();
   }
   
}
