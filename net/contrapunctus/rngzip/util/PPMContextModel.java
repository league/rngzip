package net.contrapunctus.rngzip.util;

import com.colloquial.arithcode.ArithCodeModel;
import com.colloquial.arithcode.PPMModel;
import java.util.List;

class PPMContextModel
{
  protected PPMModel model;
  private int[] range = new int[3];
  
  PPMContextModel(int len)
  {
    model = new PPMModel(len);
  }
  protected void noteContext(List<String> cx)
  {
    if( cx != null ) {
      String e = cx.get(cx.size()-1);
      int h = e.hashCode() & 0xFF;
      //System.err.printf("injecting %s (%02x)\n", e, h);
      while( model.escaped(h) ) {
        //System.err.print(".");
        model.interval(ArithCodeModel.ESCAPE, range);
      }
      model.interval(h, range);
    }
  }
}
