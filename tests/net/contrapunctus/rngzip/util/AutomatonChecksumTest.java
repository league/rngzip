package net.contrapunctus.rngzip.util;

import java.util.LinkedList;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AutomatonChecksumTest
{
  public static final String TEST_DIR = "tests/cases/";
  
  @Parameterized.Parameters
  public static LinkedList<Object[]> cases()
  {
    LinkedList<Object[]> l = new LinkedList<Object[]>();
    l.add( new Object[] { 0x86e012c2L, "alt-at.rng" } );
    l.add( new Object[] { 0x82600b3fL, "alt.rng" } );
    l.add( new Object[] { 0x532a1ba0L, "alt02.rng" } );
    // jarx01 checksum not consistent, flipped between 
    // b3595eb and 94f95eb.  md5 sums of the schema:
    // 6f20aeedfde21809be849bf4926456c1  jarx01.rnc
    // 00f1fee8e6f65388704f6f1d7e6ef168  jarx01.rng
    l.add( new Object[] { 0x0b3595ebL, "jarx01.rng" } );
    l.add( new Object[] { 0xf39410f5L, "minimal.rng" } );
    l.add( new Object[] { 0x8d3f12ddL, "opt.rng" } );
    l.add( new Object[] { 0x369a0ed8L, "opt02.rng" } );
    l.add( new Object[] { 0xf9050eb1L, "opt03.rng" } );
    l.add( new Object[] { 0xab060cd9L, "opt04.rng" } );
    return l;
  }

  String filename;
  long sum;

  public AutomatonChecksumTest(long _sum, String _filename)
  {
    sum = _sum;
    filename = TEST_DIR + _filename;
  }

  @Test
  public void run() throws Exception
  {
    BaliAutomaton ba = BaliAutomaton.fromRNG(new File(filename));
    long s = ba.checksum();
    assert sum == s
      : "checksum mismatch: " + filename + ": " 
      + Long.toString(s, 16) + " != " + Long.toString(sum, 16);
  }

}
