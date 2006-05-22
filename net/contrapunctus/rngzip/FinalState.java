package net.contrapunctus.rngzip;

import org.xml.sax.Attributes;
import java.io.IOException;

public class FinalState extends SingletonState
{
   public void end(SequentialStates st) throws IOException
   {
      st.pop();
   }

   public String toString()
   {
      return "$";
   }
}
