package net.contrapunctus.rngzip.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;

/**
 * This class represents choice points in an automaton using an
 * adaptive Huffman algorithm.  More frequently traveled paths through
 * this choice point will eventually use proportionally fewer bits in
 * their representation.  An important observation about this approach
 * is that different choices will be represented by different bit
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
public final class HuffmanChoiceCoder implements ChoiceCoder
{
   private static final boolean DEBUG = false;
   private static PrintStream dbg;
   private int limit;
   private Object id;
   private Node root;
   private Leaf[] leaves;
   
   static {
      if(DEBUG) {
         try {
            dbg = new PrintStream(new java.io.FileOutputStream("huffman.log"));
         }
         catch(java.io.FileNotFoundException e) {
            dbg = System.err;
         }
      }
   }
   
   /**
    * Construct a huffman encoder representing a choice point with up
    * to ‘limit’ possible choices.  Huffman is a waste of effort
    * unless there are more than two choices, so ‘limit’ must be
    * greater than 2.  You probably want to construct objects using the
    * {@link HuffmanChoiceFactory} instead.
    * 
    * @param limit the number of choices at this choice point, which
    * must be greater than 2.
    *
    * @param id this object is just used to represent the choice point
    * for debugging purposes—it may be null.  If non-null, only its
    * ‘toString’ method will be called.
    * 
    * @throws AssertionError if ‘limit’ is not greater than 2.
    */
   public HuffmanChoiceCoder(int limit, Object id)
   {
      /* Huffman is a waste of time unless there are more than two
         choices.  For just two choices, the factory should have
         delegated to the SimpleChoiceCoder. */
      assert limit > 2;
      this.limit = limit;
      this.id = id;
      leaves = new Leaf[limit];
      LinkedList<AbstractNode> q = new LinkedList<AbstractNode>();
      for(int i = 0;  i < limit;  i++) {
         leaves[i] = new Leaf(i);
         q.offer(leaves[i]);
      }
      while(q.size() > 1) {
         AbstractNode n1 = q.remove();
         AbstractNode n2 = q.remove();
         Node np = new Node(n1, n2);
         q.offer(np);
      }
      root = (Node) q.remove();
   }
   
   private abstract class AbstractNode
   {
      protected Node parent;
      protected boolean first_p; /* true if I am first child of my parent */
      protected int frequency;
      protected abstract Leaf asLeaf();
      protected abstract int decode(BitInputStream in) throws IOException;
      protected void update() { }
      protected void encode(BitOutputStream bo) throws IOException
      {
         if(parent != null) {
            parent.encode(bo);
            bo.writeBit(first_p);
            if(DEBUG) {
               dbg.print(first_p? 1 : 0);
            }
         }
      }
      protected void tick()
      {
         frequency++;
         if(parent == null) return;
         Node gp = parent.parent;
         if(gp == null) {
            parent.frequency++;
            return;
         }
         AbstractNode uncle = parent.first_p? gp.second : gp.first;
         if(frequency > uncle.frequency) {
            if(DEBUG) {
               dbg.printf("  -- rotating %s with %s%n", this, uncle);
               dbg.printf("  -- starting from %s%n", gp);
            }
            boolean uncle_first_p = uncle.first_p;
            uncle.parent = parent;
            if(first_p) parent.first = uncle;
            else parent.second = uncle;
            uncle.first_p = first_p;

            parent = gp;
            if(uncle_first_p) gp.first = this;
            else gp.second = this;
            first_p = uncle_first_p;

            if(first_p) gp.second.update();
            else gp.first.update();
            
            gp.tick();
               
            if(DEBUG) {
               dbg.printf("  -- result is %s%n", gp);
            }
         }
         else {
            parent.tick();
         }
      }
   }
   
   private class Leaf extends AbstractNode
   {
      protected int choice;
      protected Leaf(int ch)
      {
         choice = ch;
      }
      protected Leaf asLeaf()
      {
         return this;
      }
      protected int decode(BitInputStream in)
      {
         if(DEBUG) {
            dbg.print(" -> "+choice);
         }
         return choice;
      }
      public String toString()
      {
         return "("+frequency+" #"+choice+")";
      }
   }
   
   private class Node extends AbstractNode
   {
      protected AbstractNode first, second;
      protected Node(AbstractNode n1, AbstractNode n2)
      {
         first = n1;
         second = n2;
         n1.first_p = true;
         n2.first_p = false;
         n1.parent = this;
         n2.parent = this;
      }
      protected Leaf asLeaf()
      {
         return null;
      }
      protected int decode(BitInputStream in) throws IOException
      {
         boolean b = in.readBit();
         if(DEBUG) {
            dbg.print(b? 1 : 0);
         }
         return (b? first : second).decode(in);
      }
      public String toString()
      {
         return "("+frequency+" "+first+" "+second+")";
      }      
      protected void update()
      {
         frequency = first.frequency + second.frequency;
      }
   }
   
   public void encode(int choice, BitOutputStream bo) 
      throws IOException
   {
      if(bo == null) 
         throw new IllegalArgumentException("bo cannot be null");
      if(choice < 0 || choice >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds for choice point "+this);
      if(DEBUG) dbg.printf("%s: %d/%d -> ", this, choice, limit);
      leaves[choice].encode(bo);
      if(DEBUG) dbg.println();
      leaves[choice].tick();
   }
   
   public int decode(BitInputStream bi) throws IOException
   {
      if(bi == null)
         throw new IllegalArgumentException("bi cannot be null");
      if(DEBUG) {
         dbg.printf("%s: ", this);
      }
      int ch = root.decode(bi);
      /* the huffman decoder should NEVER produce an invalid choice,
         as long as there are enough bits on the stream. */
      assert ch >= 0 && ch < limit;
      if(DEBUG) {
         dbg.printf("/%d%n", limit);
      }
      leaves[ch].tick();
      return ch;
   }
   
   /**
    * Identifies this choice point using the ‘id’ object provided to
    * the constructor (if it was non-null).
    */
   public String toString()
   {
      if(id == null) return super.toString();
      else return id.toString();
   }
}
