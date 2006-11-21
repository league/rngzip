package net.contrapunctus.rngzip.util;

import com.colloquial.arithcode.ArithCodeModel;
import com.colloquial.arithcode.ArithCodeOutputStream;
import com.colloquial.arithcode.PPMModel;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class PPMContextOutputStream 
  extends PPMContextModel
  implements ContextualOutputStream
{
  private DataOutputStream out;
  private ArithCodeOutputStream aco;

  public PPMContextOutputStream(OutputStream _out, int len)
    {
      super(len);
      aco = new ArithCodeOutputStream(_out, model);
      out = new DataOutputStream(aco);
    }
  public void writeLong(List<String> cx, long lo) throws IOException
  {
    noteContext(cx);
    out.writeLong(lo);
  }
  public void writeUTF(List<String> cx, String st) throws IOException
  {
    noteContext(cx);
    out.writeUTF(st);
  }
  public void flush() throws IOException
  {
    out.flush();
  }
  public void close() throws IOException
  {
    out.close();
    aco.close();
  }
}
