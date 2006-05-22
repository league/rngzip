package net.contrapunctus.rngzip;
        
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.contrapunctus.rngzip.io.RNGZInputStream;
import net.contrapunctus.rngzip.io.RNGZOutputStream;
import net.contrapunctus.rngzip.io.RNGZSettings;
import net.contrapunctus.rngzip.util.ErrorReporter;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Test suite for a generic compress-decompress round-trip, using 
 * files in tests/ sub-directory.
 */
public class GenericTest extends junit.framework.TestCase
{
   static final String TESTDIR = "tests";
   String xfile, rfile;
   
   public GenericTest(String name)
   {
      super(name);
      name = TESTDIR+File.separatorChar+name;
      xfile = name;
      rfile = name.replaceFirst("\\..*\\.xml", ".rng");
   }

   public void runTest() throws Exception
   {
      /* first compress */
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      RNGZOutputStream ro = new RNGZOutputStream(bo, new RNGZSettings());
      ErrorReporter err = new ErrorReporter() {
            protected void report(String kind, SAXParseException exn) { }
         };
      GenericCompressor gc = new GenericCompressor(rfile, err, ro);
      XMLReader xr = XMLReaderFactory.createXMLReader();
      xr.setContentHandler(gc);
      xr.setErrorHandler(err);
      xr.parse(xfile);
      ro.close();
      /* report */
      byte[] buf = bo.toByteArray();
      //long n = new File(xfile).length();
      //float pct = (n - buf.length) / (float)n * 100;
      //System.err.printf("%s: %d -> %d (%.1f%%)%n", xfile, n,
      //                  buf.length, pct);                 
      /* then decompress */
      ByteArrayInputStream bi = new ByteArrayInputStream(buf);
      RNGZInputStream ri = new RNGZInputStream(bi, new RNGZSettings());
      new GenericDecompressor(rfile, ri, new DefaultHandler());
      ri.close();
   }
   
   private static TestSuite suite = new TestSuite();
   public static Test suite() 
   {
      File testdir = new File(TESTDIR);
      String[] tests = testdir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
               return name.matches(".*\\..*\\.xml");
            }
         });
      for(String t : tests) {
         suite.addTest(new GenericTest(t));
      }
      return suite;
   }
}

