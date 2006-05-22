package org.kohsuke.bali.writer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encodes byte stream into a string by just using lower 8 bits
 * of each character. Resulting string will be UTF-8 safe.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class StringOutputStream extends OutputStream {

    private StringBuffer buf = new StringBuffer();
    
    public void write(int b) throws IOException {
        buf.append((char)b);
    }
    
    public String toString() {
        return buf.toString();
    }
}
