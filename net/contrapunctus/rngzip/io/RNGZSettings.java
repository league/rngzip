package net.contrapunctus.rngzip.io;

import com.colloquial.arithcode.ArithCodeInputStream;
import com.colloquial.arithcode.ArithCodeOutputStream;
import com.colloquial.arithcode.PPMModel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.contrapunctus.rngzip.util.*;
import org.apache.commons.compress.bzip2.CBZip2InputStream;
import org.apache.commons.compress.bzip2.CBZip2OutputStream;

/**
 * This class represents the various configurable settings for a
 * compressed XML stream.  To compress a stream, these settings are
 * specified on the command line (or otherwise set by the client), but
 * to decompress a stream they must be extracted from the stream
 * itself.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class RNGZSettings 
{
   /**
    * This enumeration represents the different ways to encode choice
    * points as bits.
    */
   public enum BitCoding
   {
      /**
       * Uses a fixed-length representation.  That is, if a particular
       * choice point has 6 possible choices, we will use 3 bits to
       * represent them: 000 for choice 0, 100 for choice 4, 101 for
       * choice 5, etc.
       * @see SimpleChoiceFactory
       */
      FIXED, 
         
      /**
       * Uses an adaptive Huffman algorithm.  More frequently traveled
       * paths through each choice point will eventually use
       * proportionally fewer bits.
       * @see HuffmanChoiceFactory
       */
      HUFFMAN,

      /**
       * Uses a full byte for each choice point.  Requires that there
       * are 256 or fewer choices.
       */
      BYTE;
   }

   /**
    * This enumeration represents the different ways to compress
    * sub-streams within the compressed XML format.
    */
   public enum DataCompression
   {
      /**
       * Does not apply any compression to the stream.
       */
      NONE, 

      /**
       * Applies GZIP compression to the stream.
       * @see GZIPOutputStream
       */
        GZ,

      /**
       * Applies BZip2 compression to the stream.
       * @see CBZip2OutputStream
       */
        BZ2,
        PPM4, PPM5,
        HPM4, HPM5
   }

   private static final BitCoding[] BitCoding_values = BitCoding.values();
   private static final DataCompression[] DataCompression_values =
      DataCompression.values();

   public static final BitCoding DEFAULT_CODER = BitCoding.HUFFMAN;
   public static final DataCompression DEFAULT_COMPRESSOR = DataCompression.GZ;

   /**
    * The strategy used to encode choice points as bits.  The default
    * is <code>HUFFMAN</code>.
    */
   protected BitCoding coding = DEFAULT_CODER;

   /**
    * The type of compression applied to the bit stream representing
    * the XML tree structure.  The default is <code>GZ</code>.
    */
   protected DataCompression treeCompr = DEFAULT_COMPRESSOR;

   /**
    * The type of compression applied to the character data from the
    * XML document.  The default is <code>GZ</code>.
    */
   protected DataCompression dataCompr = DEFAULT_COMPRESSOR;

   /**
    * Default constructor, creates an object that represents
    * (initially) all the default settings.
    */
   public RNGZSettings() { }

   /**
    * This constructor takes parameters to specify the bit coding
    * strategy and the types of compression used.
    * @param bc the strategy used to encode choice points as bits.
    * @param tc the type of compression applied to the bit stream
    * representing the XML tree structure.
    * @param dc the type of compression applied to the character data
    * from the XML document.
    */
   public RNGZSettings(BitCoding bc, 
                       DataCompression tc, 
                       DataCompression dc)
   {
      coding = bc;
      treeCompr = tc;
      dataCompr = dc;
   }

   /**
    * Adjusts the strategy used to encode choice points as bits.
    * @see #coding
    */
   public void setBitCoder(BitCoding bc)
   {
      coding = bc;
   }

   /**
    * Interpret the string parameter as a strategy used to encode
    * choice points as bits.  
    * @param bc a case-insensitive representation of the {@link
    * BitCoding} strategy, such as “fixed” or “Huffman”.
    */
   public void setBitCoder(String bc)
   {
      setBitCoder(BitCoding.valueOf(bc.toUpperCase()));
   }

   /**
    * Sets the type of compression used on the tree representation.
    * @see #treeCompr
    */
   public void setTreeCompressor(DataCompression tc) 
   {
      treeCompr = tc;
   }
   
   /**
    * Sets the type of compression used on the tree representation.
    * @param tc a case-insensitive representation of the {@link
    * DataCompression} type, such as “none” or “gz”.
    */
   public void setTreeCompressor(String tc)
   {
      setTreeCompressor(DataCompression.valueOf(tc.toUpperCase()));
   }
   
   /**
    * Sets the type of compression used on the character data.
    * @see #dataCompr
    */
   public void setDataCompressor(DataCompression dc) 
   {
      dataCompr = dc;
   }
   
   /**
    * Sets the type of compression used on the character data.
    * @param dc a case-insensitive representation of the {@link
    * DataCompression} type, such as “none” or “gz”.
    */
   public void setDataCompressor(String dc)
   {
      setDataCompressor(DataCompression.valueOf(dc.toUpperCase()));
   }

   /**
    * Provides a brief, human-readable representation of these
    * settings.
    */
   public String toString()
   {
      return coding + "/" + treeCompr + "/" + dataCompr;
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

  private static Object externalInstance
    (String nm, Class ty, Object arg)
    throws IOException
  {
    try {
      Class c = Class.forName(nm);
      Constructor k = c.getConstructor(ty);
      return k.newInstance(arg);
    }
    catch(Exception x) {
      throw new IOException(x.getMessage());
    }
  }

   /**
    * Filter an output stream through a compressor, as specified by
    * ‘cm’.  That is, if ‘cm’ is <code>GZ</code>, this method will
    * return <code>new GZIPOutputStream(out)</code>.
    */
   public static OutputStream wrapOutput
      (OutputStream out, DataCompression cm)
      throws IOException
   {
      switch(cm) {
      case NONE: break;
      case GZ: out = new GZIPOutputStream(out); break;
      case BZ2: out = new CBZip2OutputStream(out); break; 
      case PPM4:
      case HPM4:
        out = new ArithCodeOutputStream(out, new PPMModel(4));
        break;
      case PPM5:
      case HPM5:
        out = new ArithCodeOutputStream(out, new PPMModel(5));
        break;

        // here's how it would work for external stuff:
        //out = (OutputStream) externalInstance
        //  ("org.apache.commons.compress.bzip2.CBZip2OutputStream",
        //   OutputStream.class, out);
        //break;
      default: assert false;
      }
      return out;
   }

   /**
    * Record a representation of these settings onto the designated
    * stream.  This representation is sufficient for reconstructing
    * the settings upon decompressing.
    */
   public void writeTo(MultiplexOutputStream mux, int stream) 
      throws IOException
   {
      /* For future compatibility, the config stream tells how many
         other embedded streams will exist, then gives the compression
         scheme for each one.  For now, that is:

         zz  encoder id (one byte, 0=FIXED, 1=HUFFMAN, 2=BYTE)
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
      out.write(treeCompr.ordinal());
      out.write(dataCompr.ordinal());
   }

   /** 
    * Construct a <code>BitOutputStream</code> on the multiplexed
    * stream, using the tree compressor specified by these settings.
    * @see #treeCompr
    */
   protected BitOutputStream newBitOutput(MultiplexOutputStream mux, 
                                          int stream)
      throws IOException
   {
      return mux.open
         (stream, new OutputStreamFilter<BitOutputStream>() {
            public BitOutputStream wrap (OutputStream out) throws IOException {
               return new BitOutputStream(wrapOutput(out, treeCompr));
            }
         });
   }

   /**
    * Construct a <code>DataOutputStream</code> which is compressed
    * according to the settings.
    * @see #dataCompr
    */
  protected ContextualOutputStream newDataOutput
    (MultiplexOutputStream mux, int stream)
    throws IOException
  {
    return mux.open
      (stream, new OutputStreamFilter<ContextualOutputStream>() {
        public ContextualOutputStream wrap (OutputStream out) 
          throws IOException {
          switch( dataCompr ) {
          case HPM4:
            return new PPMContextOutputStream(out, 4);
          case HPM5:
            return new PPMContextOutputStream(out, 5);
          default:
            return new ContextFreeOutputStream(wrapOutput(out, dataCompr));
          }
        }});
  }

   /**
    * Construct a <code>ChoiceCoder</code> according to these
    * settings.
    * @see #coding
    */
   protected ChoiceCoder makeChoiceCoder(int limit, Object id)
   {
      if(limit < 1) {
         throw new IllegalArgumentException("limit < 1");
      }
      else if(limit == 1) {
         return TrivialChoiceCoder.instance;
      }
      else if(limit > 2 && coding == BitCoding.HUFFMAN) {
         return new HuffmanChoiceCoder(limit, id);
      }
      else if(coding == BitCoding.BYTE) {
         return new ByteChoiceCoder(limit, id);
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

   /**
    * Return the magic value.  This is used to validate the input
    * stream during decompression.  It is a protected method rather
    * than just a constant, so that if you subclass this class you can
    * provide a different magic number.
    * @see #MAGIC
    */
   protected int magic() 
   {
      return MAGIC;
   }

   /**
    * Reconstitute the settings from a given stream.
    */
   protected RNGZSettings fromStream(MultiplexInputStream mux, 
                                     int stream)
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
         treeCompr = DataCompression_values[config.read()];
         dataCompr = DataCompression_values[config.read()];
      }
      catch(IndexOutOfBoundsException x) {
         throw new RNGZFormatException("unknown coding");
      }
      return this;
   }

   /** 
    * Create a decompressing input stream, according to the value of
    * ‘cm’. 
    */
   public static InputStream wrapInput
      (InputStream in, DataCompression cm)
      throws IOException
   {
      switch(cm) {
      case NONE: break;
      case GZ: in = new GZIPInputStream(in); break;
      case BZ2: in = new CBZip2InputStream(in); break;
      case PPM4: 
      case HPM4:
        in = new ArithCodeInputStream(in, new PPMModel(4));
        break;
      case PPM5: 
      case HPM5:
        in = new ArithCodeInputStream(in, new PPMModel(5));
        break;
        //in = (InputStream) externalInstance
        //  ("org.apache.commons.compress.bzip2.CBZip2InputStream",
        //   InputStream.class, in);
        //break;
      default: assert false;
      }
      return in;
   }

   /**
    * Create a decompressing bit input stream, according to these
    * settings.
    * @see #treeCompr
    */
   protected BitInputStream newBitInput(MultiplexInputStream mux,
                                        int stream)
      throws IOException
   {
      return new BitInputStream(wrapInput(mux.open(stream), treeCompr));
   }
   
   /**
    * Create a decompressing data input stream, according to these
    * settings.
    * @see #dataCompr
    */
   protected ContextualInputStream newDataInput
     (MultiplexInputStream mux, int stream)
      throws IOException
   {
     InputStream in = mux.open(stream);
     switch( dataCompr ) {
     case HPM4:
       return new PPMContextInputStream(in, 4);
     case HPM5:
       return new PPMContextInputStream(in, 5);
     default:
       return new ContextFreeInputStream(wrapInput(in, dataCompr));
     }
   }
}
