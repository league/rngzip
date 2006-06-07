package net.contrapunctus.rngzip;

import org.junit.Test;
import org.xml.sax.SAXException;

public class DecompressorTest extends Decompressor
{
  @Test
  public void runMe() throws SAXException
  {
    GenericTest.EventRecorder er = new GenericTest.EventRecorder();
    initialize(er);
    startElement("top");
    addAttribute("id");
    chars("42");
    epsilon(); // @id
    startElement("bar");
    chars("hello");
    epsilon(); // bar
    addAttribute("foo");
    chars("world");
    epsilon(); // @foo
    startElement("goo");
    epsilon(); // goo
    addAttribute("q");
    chars("a");
    epsilon(); // @q
    epsilon(); // top
    epsilon(); // doc
  }

  public static void main(String[] args) throws SAXException
  {
    new DecompressorTest().runMe();
  }
}
