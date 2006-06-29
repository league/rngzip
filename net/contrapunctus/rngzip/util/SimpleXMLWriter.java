package net.contrapunctus.rngzip.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

public class SimpleXMLWriter implements ContentHandler
{
  private PrintStream out;

  public SimpleXMLWriter(OutputStream os)
  {
    try {
      out = new PrintStream(os, false, "UTF-8");
    }
    catch(UnsupportedEncodingException x) {
      assert false : x;      // all implementations must support UTF-8
    }
  }

  public SimpleXMLWriter(PrintStream ps)
  {
    out = ps;
  }

  // Receive notification of the beginning of a document.
  public void startDocument() 
  {
    out.println("<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
  }

  // Receive notification of the end of a document.
  public void endDocument() 
  {
    out.println();
    out.flush();
  }

  // Receive notification of character data.
  public void characters(char[] ch, int start, int length) 
  {
    quotedOutput(out, ch, start, length, false);
  }

  // Receive notification of the beginning of an element.
  public void startElement(String uri, String localName, String qname, 
                           Attributes atts) 
  {
    out.print('<');
    out.print(qname);
    outputAttrs(out, atts);
    out.print('>');
  }

  // Receive notification of the end of an element.
  public void endElement(String uri, String localName, String qname) 
  {
    out.print("</");
    out.print(qname);
    out.print('>');
  }

  static void outputAttrs(PrintStream out, Attributes atts)
  {
    if (atts != null) {
      int len = atts.getLength();
      for (int i = 0; i < len; i++) {
        out.print(' ');
        out.print(atts.getQName(i));
        out.print("=\"");
        quotedOutput(out, atts.getValue(i), true);
        out.print('"');
      }
    }
  }

  static void quotedOutput(PrintStream out, char[] ch, int start, 
                           int len, boolean attr_p)
  {
    for(int i = 0;  i < len;  i++) {
      quotedOutput(out, ch[start+i], attr_p);
    }
  }

  static void quotedOutput(PrintStream out, String s, boolean attr_p)
  {
    if(s != null) {
      for(int i = 0;  i < s.length();  i++) {
        quotedOutput(out, s.charAt(i), attr_p);
      }
    }
  }

  static void quotedOutput(PrintStream out, char c, boolean attr_p)
  {
    switch(c) {
    case '<': out.print("&lt;"); break;
    case '>': out.print("&gt;"); break;
    case '&': out.print("&amp;"); break;
    case '"': out.print(attr_p? "&quot;" : "\""); break;
    default:  
      if (((c >= 0x01 && c <= 0x1F && c != 0x09 && c != 0x0A) 
           || (c >= 0x7F && c <= 0x9F) || c == 0x2028)
          || attr_p && (c == 0x09 || c == 0x0A)) {
        out.print("&#x");
        out.print(Integer.toHexString(c).toUpperCase());
        out.print(";");
      }
      else {
        out.print(c);
      }        
    }
  }

  // Begin the scope of a prefix-URI Namespace mapping.  
  public void startPrefixMapping(String prefix, String uri) 
  {
    throw new UnsupportedOperationException
      ("SimpleXMLWriter.startPrefixMapping");
  }

  // End the scope of a prefix-URI mapping.
  public void endPrefixMapping(String prefix) 
  {
    // error already issued in startPrefixMapping
  }

  // Receive notification of ignorable whitespace in element content.
  public void ignorableWhitespace(char[] ch, int start, int length) 
  {
    // ignore it
  }

  // Receive notification of a processing instruction.
  public void processingInstruction(String target, String data) 
  {
    throw new UnsupportedOperationException
      ("SimpleXMLWriter.processingInstruction");
  }

  // Receive an object for locating the origin of SAX document events.
  public void setDocumentLocator(Locator locator) 
  {
    // ignore it
  }

  // Receive notification of a skipped entity.
  public void skippedEntity(String name) 
  {
    // ignore it
  }


}
