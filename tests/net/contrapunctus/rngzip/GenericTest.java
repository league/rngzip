package net.contrapunctus.rngzip;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import net.contrapunctus.rngzip.io.RNGZInputStream;
import net.contrapunctus.rngzip.io.RNGZOutputStream;
import net.contrapunctus.rngzip.io.RNGZSettings;
import net.contrapunctus.rngzip.util.Bali;
import net.contrapunctus.rngzip.util.ErrorReporter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.writer.*;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import runtime.ValidateletImpl;

/**
 * Test suite for a generic compress-decompress round-trip, using
 * files in tests/cases/ sub-directory.  Not only do we look for
 * exceptions and assertion failures in the process, but we ensure
 * that the ultimate output is identical (in terms of SAX events) to
 * the original input.
 */
@RunWith(Parameterized.class)
public class GenericTest 
{
  public static final String TEST_DIR = "tests/cases";

  private XMLReader xmlReader;
  private ErrorReporter errorReporter;
  private ByteArrayOutputStream errorBytes;
  private PrintStream errorStream;
  private RNGZSettings settings;

  private String origFileName;    // ---.---.xml
  private String schemaFileName;  // ---.rng
  private byte[] compressedBytes;
  private EventRecorder origSax, newSax;

  @Before
  public void setup() throws Exception 
  {
    errorBytes = new ByteArrayOutputStream();
    errorStream = new PrintStream(errorBytes);
    errorReporter = new ErrorReporter(errorStream);
    xmlReader = XMLReaderFactory.createXMLReader();
    xmlReader.setErrorHandler(errorReporter);
    settings = new RNGZSettings();
  }

  @Parameterized.Parameters 
  public static LinkedList<String[]> cases() 
  {
    File testdir = new File(TEST_DIR);
    String[] tests = testdir.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.matches(".*\\..*\\.xml");
        }
      });
    LinkedList<String[]> args = new LinkedList<String[]>();
    for(String t : tests) 
      {
        args.add( new String[] { t } );
      }
    return args;
  }

  public GenericTest(String name)
  {
    origFileName = TEST_DIR + File.separatorChar + name;
    schemaFileName = origFileName.replaceFirst("\\..*\\.xml", ".rng");
  }

  @Test
  public void roundTrip() throws Exception
  {
    try
      {
        validateOnly();
        recordOriginal();
        compress();
        decompress();
        origSax.assertEqual(newSax);
        // If we get here, there were no exceptions.
        // But was there any output on the stream?
        errorStream.flush();
        if( errorBytes.size() > 0 )
          throw new Exception("error stream");
      }
    catch( Throwable th )
      {
        maybeSaveRNZ();
        maybeSaveNewXML();
        String msg = composeErrorMessage(th.getMessage());
        throw new Error( msg, th );
      }
  }

  private void validateOnly() throws Exception
  {
    AutomatonWriter writer = new NullWriter();    
    Interpreter interpreter = new Interpreter();
    writer = new MultiWriter(writer, interpreter);
    TreeAutomaton automaton = Bali.buildAutomatonFromRNG(schemaFileName);
    writer.write(automaton);
    ValidateletImpl v = interpreter.createValidatelet();
    xmlReader.setContentHandler(v);
    try {
      xmlReader.parse(origFileName);
    }
    catch( SAXParseException exn ) {
      errorStream.println("WARNING: Bali COULD NOT VALIDATE THIS TEST CASE");
    }
  }

  private void recordOriginal() throws Exception
  {
    origSax = new EventRecorder();
    xmlReader.setContentHandler(origSax);
    xmlReader.parse(origFileName);
  }

  private void compress() throws Exception
  {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    RNGZOutputStream ro = new RNGZOutputStream(bo, settings);
    GenericCompressor gc = new GenericCompressor
      (schemaFileName, errorReporter, ro);
    xmlReader.setContentHandler(gc);
    xmlReader.parse(origFileName);
    ro.close();
    compressedBytes = bo.toByteArray();
  }

  private void decompress() throws Exception
  {
    ByteArrayInputStream bi = new ByteArrayInputStream(compressedBytes);
    RNGZInputStream ri = new RNGZInputStream(bi, settings);
    newSax = new EventRecorder();
    new GenericDecompressor(schemaFileName, ri, newSax);
    ri.close();
  }

  private void maybeSaveRNZ() throws Exception
  {
    if( compressedBytes != null )
      {
        String rnz = origFileName.replaceFirst("\\.xml$", ".rnz");
        FileOutputStream fos = new FileOutputStream(rnz);
        fos.write(compressedBytes);
        fos.close();
        errorStream.println("Saved " + rnz + " (" +
                            compressedBytes.length +
                            " bytes)");
      }
  }

  private void maybeSaveNewXML() throws IOException
  {
    if( newSax != null )
      {
        String xin = origFileName.replaceFirst("\\.xml$", ".xin");
        String xout = origFileName.replaceFirst("\\.xml$", ".xout");
        origSax.saveTo(xin);
        newSax.saveTo(xout);
        errorStream.println("Saved " + xin + " and " + xout);
      }
  }

  private String composeErrorMessage( String exnMesg )
  {
    errorStream.close();
    StringBuilder buf = new StringBuilder();
    buf.append(exnMesg);
    buf.append("\nFailure in ");
    buf.append(origFileName);
    buf.append("\n");
    if( errorBytes.size() > 0 )
      {
        buf.append("Details follow:\n");
        buf.append(new String(errorBytes.toByteArray()));
      }
    return buf.toString();
  }

  /**
   * To compare the input against the output, we don't do it at the
   * character level; there are too many variations possible: spacing
   * and indentation, <foo/> vs. <foo></foo>, id="3" vs. id='3', etc.
   * So this ContentHandler will accumulate a history of events, for
   * comparison to another history.
   */
  static class EventRecorder extends DefaultHandler
  {
    /* We store events as strings, the first character indicating the
     * kind of event, so that the document
     *    <foo id='78' href='abc'>Yes!<bar/>No</foo>
     * becomes
     *
     *   "+foo", "@href", "=abc",     // attributes in alphabetical order
     *   "@id", "=78", "$Yes!",
     *   "+bar", "-bar", "$No", "-foo"
     * 
     * Character events made entirely of white space are assumed to be
     * ignorable, and consecutive character events are merged.
     */
    private LinkedList<String> history = new LinkedList<String>();
    private TreeSet<String> keys = new TreeSet<String>();

    public void startElement( String ns, String ln, String qn,
                              Attributes at )
    {
      history.add("+" + qn);
      /* We can't count on attributes being in alpha order,
         so add keys to a sorted set. */
      keys.clear();
      for( int i = 0;  i < at.getLength();  i++ )
        {
          keys.add( at.getQName(i) );
        }
      for( String k : keys )
        {
          history.add("@" + k);
          history.add("=" + at.getValue(k));
        }
    }

    public void endElement( String ns, String ln, String qn )
    {
      history.add("-" + qn);
    }

    public void characters( char[] ch, int start, int len )
    {
      /* Check whether it's all white space. */
      boolean ignoreable = true;
      for( int i = 0;  i < len; i++ )
        {
          if( ! Character.isWhitespace(ch[start+i]) )
            {
              ignoreable = false;
              break;
            }
        }
      if( ignoreable ) return;
      String txt = new String(ch, start, len);
      /* Check whether last event was also a string. */
      String last = history.removeLast();
      if( last.charAt(0) == '$' )
        { /* Merge consecutive strings. */
          history.add(last + txt);
        }
      else
        {
          history.add(last);
          history.add('$' + txt);
        }
    }

    public int size()
    {
      return history.size();
    }

    public void saveTo( String fileName ) throws IOException
    {
      PrintStream ps = new PrintStream(new FileOutputStream(fileName));
      for( String e : history )
        {
          ps.println(e);
        }
      ps.close();
    }

    /* Check whether two event histories are the same. */
    public void assertEqual( EventRecorder that )
    {
      Iterator<String> i = this.history.iterator();
      Iterator<String> j = that.history.iterator();
      while( i.hasNext() && j.hasNext() )
        {
          String si = i.next();
          String sj = j.next();
          assert si.equals(sj) : "event mismatch: ["+si+"]["+sj+"]";
        }
      assert i.hasNext() == j.hasNext() 
        : "one event history shorter than other";
    }
  }

  /* A little test program for the EventRecorder. */
  public static void main(String[] args) throws Exception
  {
    XMLReader xr = XMLReaderFactory.createXMLReader();
    EventRecorder er1 = new EventRecorder();
    EventRecorder er2 = new EventRecorder();
    xr.setContentHandler(er1);
    xr.parse(args[0]);
    xr.setContentHandler(er2);
    xr.parse(args[1]);
    er1.assertEqual(er2);
  }
}

