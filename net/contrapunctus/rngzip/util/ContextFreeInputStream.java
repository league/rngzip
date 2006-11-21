package net.contrapunctus.rngzip.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ContextFreeInputStream
  implements ContextualInputStream
{
  private DataInputStream in;

  public ContextFreeInputStream(InputStream _in)
  {
    in = new DataInputStream(_in);
  }
  public long readLong(List<String> cx) throws IOException
  {
    return in.readLong();
  }
  public String readUTF(List<String> cx) throws IOException
  {
    return in.readUTF();
  }
}
