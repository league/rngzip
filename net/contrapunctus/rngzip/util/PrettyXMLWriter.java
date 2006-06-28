package net.contrapunctus.rngzip.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

public class PrettyXMLWriter implements ContentHandler
{
  private PrintStream out;
  private int tab;
  private Stack<LinkedList<Node>> stack;

  private abstract class Node {
    abstract void write(int depth);
    boolean isText() { return false; }
  } // class Node

  private class Element extends Node {
    String qname;
    AttributesImpl atts;
    LinkedList<Node> children;
    Element(String qn, Attributes at) {
      qname = qn;
      atts = new AttributesImpl(at); // must copy
      children = new LinkedList<Node>();
    }
    void write(int depth) {
      indent(depth);
      out.print('<');
      out.print(qname);
      SimpleXMLWriter.outputAttrs(out, atts);
      if(children.isEmpty()) {
        out.print("/>");
      }
      else {
        out.print('>');
        int d = adjust(depth);
        for(Node n : children) {
          n.write(d);
        }
        if( d >= 0 ) indent(depth);
        out.print("</");
        out.print(qname);
        out.print('>');
      }
    }
    void indent(int depth) {
      if(depth >= 0) {
        out.println();
        for(int i = 0;  i < depth*tab;  i++) {
          out.print(' ');
        }
      }
    }
    int adjust(int depth) {
      for(Node n : children) {
        if(n.isText()) return -1;
      }
      return depth+1;
    }
  } // class Element

  private class Text extends Node {
    char[] ch;
    int start, len;
    Text(char[] ch, int start, int len) {
      this.ch = ch;
      this.start = start;
      this.len = len;
    }
    void write(int depth) {
      SimpleXMLWriter.quotedOutput(out, ch, start, len, false);
    }
    boolean isText() { return true; }
  } // class Text

  public PrettyXMLWriter(OutputStream os, int tab)
  {
    this(new PrintStream(os), tab);
  }

  public PrettyXMLWriter(PrintStream out, int tab)
  {
    this.out = out;
    this.tab = tab;
  }

  // Receive notification of the beginning of a document.
  public void startDocument() 
  {
    stack = new Stack<LinkedList<Node>>();
    stack.add(new LinkedList<Node>());
  }

  // Receive notification of the end of a document.
  public void endDocument()
  {
    out.print("<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
    LinkedList<Node> list = stack.pop();
    assert stack.empty();
    assert list.size() == 1;
    list.getFirst().write(0);
    out.println();
    out.flush();
  }

  // Receive notification of character data.
  public void characters(char[] ch, int start, int len)
  {
    stack.peek().add(new Text(ch, start, len));
  }

  // Receive notification of the beginning of an element.
  public void startElement(String uri, String localName, String qName, 
                           Attributes atts) 
  {
    Element e = new Element(qName, atts);
    stack.peek().add(e);
    stack.push(e.children);
  }

  // Receive notification of the end of an element.
  public void endElement(String uri, String localName, String qName) 
  {
    stack.pop();
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
