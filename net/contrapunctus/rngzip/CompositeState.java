package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

abstract class CompositeState
{
   public abstract CompositeState 
      initialize( SingletonState s0 );

   public abstract CompositeState 
      start( int element, Map<Integer,String> att ) throws IOException;

   protected abstract CompositeState
      attrs( Map<Integer,String> att ) throws IOException;

   public abstract CompositeState 
      chars( char[] buf, int start, int length ) throws IOException;

   public abstract CompositeState 
      end( ) throws IOException;

   public abstract void 
      show(PrintWriter ps);

   public String toString() {
      StringWriter sw = new StringWriter(132);
      show(new PrintWriter(sw));
      return sw.toString();
   }
}
