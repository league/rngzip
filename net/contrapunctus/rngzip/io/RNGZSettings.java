package net.contrapunctus.rngzip.io;

import com.colloquial.arithcode.ArithCodeInputStream;
import com.colloquial.arithcode.ArithCodeOutputStream;
import com.colloquial.arithcode.PPMModel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.MultiplexInputStream;
import net.contrapunctus.rngzip.util.MultiplexOutputStream;
import net.contrapunctus.rngzip.util.OutputStreamFilter;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

public class RNGZSettings 
{
   public enum BitCoding
   {
      FIXED, HUFFMAN; 
   }

   private static final int ppmOrder = 4;
   
   public enum DataCompression
   {
     NONE, GZ, BZ2, PPM;
   }

   private static final BitCoding[] BitCoding_values = BitCoding.values();
   private static final DataCompression[] DataCompression_values =
      DataCompression.values();

   protected BitCoding coding = BitCoding.HUFFMAN;
   protected DataCompression bit_cm = DataCompression.BZ2;
   protected DataCompression dat_cm = DataCompression.BZ2;

   public RNGZSettings() { }
   public RNGZSettings(BitCoding bc, DataCompression bcm, DataCompression dcm)
   {
      coding = bc;
      bit_cm = bcm;
      dat_cm = dcm;
   }

   public void setBitCoder(BitCoding bc)
   {
      coding = bc;
   }

   public void setBitCoder(String bc)
   {
      setBitCoder(BitCoding.valueOf(bc.toUpperCase()));
   }

   public void setBitCompressor(DataCompression bcm) 
   {
      bit_cm = bcm;
   }
   
   public void setBitCompressor(String bcm)
   {
      setBitCompressor(DataCompression.valueOf(bcm.toUpperCase()));
   }
   
   public void setDataCompressor(DataCompression dcm) 
   {
      dat_cm = dcm;
   }
   
   public void setDataCompressor(String dcm)
   {
      setDataCompressor(DataCompression.valueOf(dcm.toUpperCase()));
   }

   public String toString()
   {
      return coding + "/" + bit_cm + "/" + dat_cm;
   }

   /**
    * Bytes 5–8 of the stream are given by this ‘magic’ number, in
    * Java-standard big-endian byte order.  They follow the four bytes
    * of {@link MultiplexOutputStream#MAGIC}.  These bytes represent
    * the letters “rnZ” in ASCII (72 6E 5A in hexadecimal) followed by
    * a one-byte version number, currently 01.
    */
   public static final int MAGIC = 0x726E5A01;

   /* ----------------------------------------------------------------
    *                       COMPRESSOR INTERFACE
    * ----------------------------------------------------------------
    */   
   protected OutputStream wrapOutput(OutputStream out, DataCompression cm)
      throws IOException
   {
      switch(cm) {
      case NONE: break;
      case GZ: out = new GZIPOutputStream(out); break;
      case BZ2: out = new CBZip2OutputStream(out); break;
      case PPM: 
        out = new ArithCodeOutputStream(out, new PPMModel(ppmOrder)); 
        break;
      default: assert false;
      }
      return out;
   }

   protected void writeTo(MultiplexOutputStream mux, int stream) 
      throws IOException
   {
      /* For future compatibility, the config stream tells how many
         other embedded streams will exist, then gives the compression
         scheme for each one.  For now, that is:

         zz  encoder id (one byte, 0=FIXED, 1=HUFFMAN)
         02  number of streams
         xx  compression for bit stream (0=NONE, 1=GZ, 2=BZ2, 3=PPM)
         yy  compression for data stream (same)
      */
      DataOutputStream out = mux.open
         (stream, new OutputStreamFilter<DataOutputStream>() {
            public DataOutputStream wrap(OutputStream out)
            throws IOException {
               return new DataOutputStream(out);
            }
         });
      out.write(coding.ordinal());
      out.write(2);
      out.write(bit_cm.ordinal());
      out.write(dat_cm.ordinal());
   }

   protected int magic() 
   {
      return MAGIC;
   }

   protected BitOutputStream newBitOutput(MultiplexOutputStream mux, 
                                          int stream)
      throws IOException
   {
      return mux.open
         (stream, new OutputStreamFilter<BitOutputStream>() {
            public BitOutputStream wrap (OutputStream out) throws IOException {
               return new BitOutputStream(wrapOutput(out, bit_cm));
            }
         });
   }

   protected DataOutputStream newDataOutput(MultiplexOutputStream mux, 
                                            int stream)
      throws IOException
   {
      return mux.open
         (stream, new OutputStreamFilter<DataOutputStream>() {
            public DataOutputStream wrap (OutputStream out) throws IOException {
               return new DataOutputStream(wrapOutput(out, dat_cm));
            }
         });
   }
   
   protected ChoiceCoder makeChoiceCoder(int limit, Object id)
   {
      if(limit < 1) {
         throw new IllegalArgumentException("limit < 1");
      }
      else if(limit == 1) {
         return new TrivialChoiceCoder();
      }
      else if(limit > 2 && coding == BitCoding.HUFFMAN) {
         return new HuffmanChoiceCoder(limit, id);
      }
      else {
         assert coding == BitCoding.FIXED || limit == 2;
         return new SimpleChoiceCoder(limit, id);
      }
   }

   /* ----------------------------------------------------------------
    *                      DECOMPRESSOR INTERFACE
    * ----------------------------------------------------------------
    */   

   protected RNGZSettings fromStream(MultiplexInputStream mux, int stream)
      throws IOException
   {
      if(mux.magic() != magic()) {
         throw new RNGZFormatException("bad magic");
      }
      DataInputStream config = new DataInputStream(mux.open(stream));
      try { 
         coding = BitCoding_values[config.read()]; 
         if(config.read() != 2) {
            throw new RNGZFormatException("invalid config data");
         }
         bit_cm = DataCompression_values[config.read()];
         dat_cm = DataCompression_values[config.read()];
      }
      catch(IndexOutOfBoundsException x) {
         throw new RNGZFormatException("unknown coding");
      }
      return this;
   }

   protected InputStream wrapInput(InputStream in, DataCompression cm)
      throws IOException
   {
      switch(cm) {
      case NONE: break;
      case GZ: in = new GZIPInputStream(in); break;
      case BZ2: in = new CBZip2InputStream(in); break;
      case PPM: 
        in = new ArithCodeInputStream(in, new PPMModel(ppmOrder));
        break;
      default: assert false;
      }
      return in;
   }

   protected BitInputStream newBitInput(MultiplexInputStream mux,
                                        int stream)
      throws IOException
   {
      return new BitInputStream(wrapInput(mux.open(stream), bit_cm));
   }
   
   protected DataInputStream newDataInput(MultiplexInputStream mux,
                                          int stream)
      throws IOException
   {
      return new DataInputStream(wrapInput(mux.open(stream), dat_cm));
   }

}
