package net.contrapunctus.rngzip.io;

/**
 * This class creates encoders and decoders using a straightforward
 * fixed-length two’s complement representation.  That is, if a
 * particular choice point has 6 possible choices, we will use 3 bits
 * to represent them: 000 for choice 0, 100 for choice 4, 101 for
 * choice 5, etc.  In this example, two bit sequences are considered
 * invalid, and are wasted: 110 (representing 6) and 111 (representing
 * 7).
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class SimpleChoiceFactory implements ChoiceEncoderFactory, 
                                            ChoiceDecoderFactory
{
   public ChoiceDecoder makeChoiceDecoder(int limit, Object id)
   {
      if(limit < 1) throw new IllegalArgumentException("limit < 1");
      else if(limit == 1) return TrivialChoiceCoder.instance;
      else return new SimpleChoiceCoder(limit, id);
   }
   
   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      if(limit < 1) throw new IllegalArgumentException("limit < 1");
      else if(limit == 1) return TrivialChoiceCoder.instance;
      else return new SimpleChoiceCoder(limit, id);
   }   
}

