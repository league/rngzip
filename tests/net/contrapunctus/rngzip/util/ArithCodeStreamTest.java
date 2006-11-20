package net.contrapunctus.rngzip.util;

import com.colloquial.arithcode.*;
import java.io.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArithCodeStreamTest
{
  PPMModel po;
  ByteArrayOutputStream bos;
  ArithCodeOutputStream aos;
  DataOutputStream dos;
  boolean verbose = false;

  @Before
    public void setUp()
  {
    po = new PPMModel(4);
    bos = new ByteArrayOutputStream();
    aos = new ArithCodeOutputStream(bos, po);
    dos = new DataOutputStream(aos);
  }

  @Test
    public void dataRoundTrip() throws IOException
  {
    dos.writeUTF("Hello, world!");
    dos.writeInt(0xCAFEBABE);
    dos.writeUTF("Over & out.");
    dos.close();
    //    aos.close(); // important?
    byte[] bs = bos.toByteArray();
    if(verbose) {
      for(int i = 0;  i < bs.length;  i++) {
        System.out.printf("%02x ", bs[i]);
        if(i%16 == 15) System.out.println();
      }
      System.out.println();
    }
    PPMModel pi = new PPMModel(4);
    ByteArrayInputStream bis = new ByteArrayInputStream(bs);
    ArithCodeInputStream ais = new ArithCodeInputStream(bis, pi);
    DataInputStream dis = new DataInputStream(ais);
    String s1 = dis.readUTF();
    assert s1.equals("Hello, world!") : s1;
    int i1 = dis.readInt();
    assert 0xCAFEBABE == i1 : i1;
    String s2 = dis.readUTF();
    assert s2.equals("Over & out.") : s2;
    int i2 = dis.read();
    assert -1 == i2 : i2;
  }

  public static void main(String[] args) throws IOException
  {
    ArithCodeStreamTest t = new ArithCodeStreamTest();
    t.verbose = true;
    t.setUp();
    t.dataRoundTrip();
  }
}
