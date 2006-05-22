package net.contrapunctus.rngzip.io;

import java.util.List;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * This interface specifies the methods needed to write a compressed
 * XML stream.  Radically different file formats are made possible by
 * having different classes implement this interface.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 */
public interface RNGZOutputInterface extends Closeable, Flushable,
                                             ChoiceEncoderFactory
{
   /**
    * Record a choice taken in the automaton.
    * @param enc indicates the choice point in the automaton; it can
    * encode the choice to a bit stream (if the implementing class so
    * desires).
    * @param choice is the choice that is taken; it must be
    * non-negative and less than the limit of the encoder.
    * @throws IndexOutOfBoundsException if ‘choice’ is out of range.
    * @throws IOException if there is a problem writing to the output
    * stream.
    */
   void writeChoice(ChoiceEncoder enc, int choice) throws IOException;

   /**
    * Record a piece of data to the output stream.  It could be the
    * value of an attribute or some element content.
    * @param path indicates the path from the root to this element in
    * the XML tree.  The last element of this list is the containing
    * element of this data.  Or, if this the value of an attribute
    * called “name”, then ‘path’ ends with the string “@name”.
    * @param s is the content to store in the output stream.
    *
    * @throws IllegalArgumentException if ‘path’ is null or an empty
    * list, or if ‘s’ is null.
    * @throws IOException if there is a problem writing to the output
    * stream.
    */
   void writeContent(List<String> path, String s) throws IOException;

   /**
    * Record a piece of data to the output stream.  This is the same
    * as {@link #writeContent(List,String)}, but the character data is
    * represented differently.  It should be equivalent to {@code
    * writeContent(path, new String(buf, start, length))}.
    *
    * @throws IllegalArgumentException if ‘path’ is null or an empty
    * list, or if ‘buf’ is null.
    * @throws IndexOutOfBoundsException if the ‘start’ and ‘length’
    * arguments are outside the bounds of the ‘buf’ array.
    * @throws IOException if there is a problem writing to the output
    * stream.
    */
   void writeContent(List<String> path, char[] buf, int start, int length) 
      throws IOException;
}
