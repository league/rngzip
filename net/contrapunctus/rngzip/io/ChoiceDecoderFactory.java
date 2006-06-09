package net.contrapunctus.rngzip.io;

/**
 * This interface specifies a factory method for creating
 * <code>ChoiceDecoder</code> objects.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public interface ChoiceDecoderFactory
{
   /**
    * Create a decoder representing a choice point with up to ‘limit’
    * possible choices.  
    *
    * @param limit the number of choices at this choice point, which
    * must be strictly positive.  A limit of one is okay, although
    * it’s kind of a degenerate case: only the choice ‘0’ will ever be
    * allowed (and it should take up no space in the bit stream).
    *
    * @param id this object is just used to represent the choice point
    * for debugging purposes—it may be null.  If non-null, only its
    * ‘toString’ method will be called.
    *
    * @throws IllegalArgumentException if the limit is less than one.
    */
   ChoiceDecoder makeChoiceDecoder(int limit, Object id);
}
