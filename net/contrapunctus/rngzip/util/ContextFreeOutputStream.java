package net.contrapunctus.rngzip.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ContextFreeOutputStream
  implements ContextualOutputStream
{
  private DataOutputStream out;

  public ContextFreeOutputStream(OutputStream _out)
    {
      out = new DataOutputStream(_out);
    }
  public void writeLong(List<String> cx, long lo) throws IOException
  {
    out.writeLong(lo);
  }
  public void writeUTF(List<String> cx, String st) throws IOException
  {
    out.writeUTF(st);
  }
  public void flush() throws IOException
  {
    out.flush();
  }
  public void close() throws IOException
  {
    out.close();
  }
}
