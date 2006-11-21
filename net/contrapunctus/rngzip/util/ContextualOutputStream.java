package net.contrapunctus.rngzip.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface ContextualOutputStream extends Closeable
{
  public void writeLong(List<String> cx, long lo) throws IOException;
  public void writeUTF(List<String> cx, String st) throws IOException;
  public void flush() throws IOException;
}
