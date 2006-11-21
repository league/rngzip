package net.contrapunctus.rngzip.util;

import com.colloquial.arithcode.ArithCodeModel;
import com.colloquial.arithcode.ArithCodeInputStream;
import com.colloquial.arithcode.PPMModel;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PPMContextInputStream 
  extends PPMContextModel
  implements ContextualInputStream
{
  private DataInputStream in;

  public PPMContextInputStream(InputStream _in, int len)
    throws IOException
  {
    super(len);
    in = new DataInputStream(new ArithCodeInputStream(_in, model));
  }
  public long readLong(List<String> cx) throws IOException
  {
    noteContext(cx);
    return in.readLong();
  }
  public String readUTF(List<String> cx) throws IOException
  {
    noteContext(cx);
    return in.readUTF();
  }
}
