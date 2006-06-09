package net.contrapunctus.rngzip.io;

/**
 * This class creates encoders and decoders using an adaptive Huffman
 * algorithm.  More frequently traveled paths through this choice
 * point will eventually use proportionally fewer bits in their
 * representation.  An important observation about this approach is
 * that different choices will be represented by different bit
 * sequences at different times.  So the bit representation depends
 * not only on the choice, but on the entire past history of choices.
 *
 * <p>One other nice thing about the Huffman encoding is that, because
 * it is based on a binary tree, <i>every</i> valid bit sequence
 * decodes to a permissible choice.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class HuffmanChoiceFactory 
  implements ChoiceEncoderFactory, ChoiceDecoderFactory
{
   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      if(limit < 1) throw new IllegalArgumentException("limit < 1");
      else if(limit == 1) return TrivialChoiceCoder.instance;
      else if(limit == 2) return new SimpleChoiceCoder(limit, id);
      else return new HuffmanChoiceCoder(limit, id);
   }
   
   public ChoiceDecoder makeChoiceDecoder(int limit, Object id)
   {
      if(limit < 1) throw new IllegalArgumentException("limit < 1");
      else if(limit == 1) return TrivialChoiceCoder.instance;
      else if(limit == 2) return new SimpleChoiceCoder(limit, id);
      else return new HuffmanChoiceCoder(limit, id);
   }
}
