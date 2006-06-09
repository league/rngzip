package net.contrapunctus.rngzip.util;

import java.io.IOException;
import java.net.URL;

/** 
 * Signals an error reading a Relax NG schema from an XML file.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see BaliAutomaton#fromRNG
 */
public class SchemaFormatException extends IOException
{
  SchemaFormatException(String s)
  {
    super(s);
  }

  private static final long serialVersionUID = 1L;
}
