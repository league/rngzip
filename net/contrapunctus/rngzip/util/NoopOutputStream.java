package net.contrapunctus.rngzip.util;

import java.io.OutputStream;

/**
 * This is the most trivial <code>OutputStream</code> imaginable; it
 * simply discards all bytes sent to it.  In other words, compared to
 * <code>OutputStream</code>, it just provides a non-abstract
 * <code>write</code> method.  Why is this useful?  It can be passed
 * to a <code>CheckedOutputStream</code> if you want to compute the
 * checksum but don't want to worry about writing or storing the
 * actual bytes output anywhere.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see java.util.zip.CheckedOutputStream
 */
public class NoopOutputStream extends OutputStream
{
  /**
   * Does nothing.
   */
  public void write (int b)
  {}
}
