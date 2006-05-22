package net.contrapunctus.rngzip.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;

final class HuffmanChoiceCoder implements ChoiceCoder
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
   
   protected abstract class AbstractNode
   {
      protected Node parent;
      protected boolean first_p; /* true if I am first child of my parent */
      protected int frequency;
      protected abstract Leaf asLeaf();
      protected abstract int decode(BitInputStream in) throws IOException;
      protected void update() { }
      protected void encode(BitOutputStream out) throws IOException
      {
         if(parent != null) {
            parent.encode(out);
            out.writeBit(first_p);
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
   
   protected class Leaf extends AbstractNode
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
   
   protected class Node extends AbstractNode
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
   
   public void encode(int ch, BitOutputStream out) throws IOException
   {
      if(out == null) 
         throw new IllegalArgumentException("out cannot be null");
      if(ch < 0 || ch >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+ch+" is out of bounds for choice point "+this);
      if(DEBUG) dbg.printf("%s: %d/%d -> ", this, ch, limit);
      leaves[ch].encode(out);
      if(DEBUG) dbg.println();
      leaves[ch].tick();
   }
   
   public int decode(BitInputStream in) throws IOException
   {
      if(in == null)
         throw new IllegalArgumentException("in cannot be null");
      if(DEBUG) {
         dbg.printf("%s: ", this);
      }
      int ch = root.decode(in);
      /* the huffman decoder should NEVER produce an invalid choice,
         as long as there are enough bits on the stream. */
      assert ch >= 0 && ch < limit;
      if(DEBUG) {
         dbg.printf("/%d%n", limit);
      }
      leaves[ch].tick();
      return ch;
   }
   
   public String toString()
   {
      if(id == null) return super.toString();
      else return id.toString();
   }
}
