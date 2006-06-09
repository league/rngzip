package net.contrapunctus.rngzip.util;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.IOException;

/**
 * This simple interface describes factories for building output
 * stream filters, such as DataOutputStream, GZIPOutputStream, and
 * others.  The type of the resulting stream is given by the parameter
 * ‘T’.  This interface is primarily used in conjunction with the open
 * method of MultiplexOutputStream.  In this usage, the return type
 * need only to implement Closeable—it is not necessarily a subclass
 * of OutputStream.<p>
 * 
 * In this example, we build an anonymous class that specifies a
 * factory for building a gzipped PrintStream:
 * 
 *   <pre> 
 *   new OutputStreamFilter&lt;PrintStream&gt;() {
 *      public PrintStream wrap (OutputStream out)
 *      throws IOException {
 *         return new PrintStream(new GZIPOutputStream(out));
 *      }
 *   }
 *   </pre>
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see MultiplexOutputStream#open(int, OutputStreamFilter)
 * @see java.io.FilterOutputStream
 */
public interface OutputStreamFilter<T extends Closeable>
{
   /** 
    * The main factory method, to wrap the provided output stream
    * within a filtering stream (or a chain of them).
    * 
    * @return the new stream.
    * @throws java.io.IOException if there is a problem opening the
    * filtering stream.
    */
   T wrap(OutputStream out) throws IOException;

}
