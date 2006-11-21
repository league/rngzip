package net.contrapunctus.rngzip.util;

import java.io.IOException;
import java.util.List;

public interface ContextualInputStream
{
  public long readLong(List<String> cx) throws IOException;
  public String readUTF(List<String> cx) throws IOException;
}
