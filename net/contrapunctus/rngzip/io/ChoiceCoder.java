package net.contrapunctus.rngzip.io;

/**
 * A class implementing this interface represents a choice point in an
 * automaton; it can convert back and forth between transitions and
 * bit sequences.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public interface ChoiceCoder
  extends ChoiceEncoder, ChoiceDecoder
{ }
