package net.contrapunctus.rngzip;

import java.io.File;
import net.contrapunctus.rngzip.io.RNGZSettings;
import org.junit.Test;

public class Benchmarks
{
    @Test
    public void nothing() { }
    
    public static void main( String[] args ) throws Exception
    {
        File xmlFile = new File( args[0] );
        File rngFile = new File( xmlFile.getParentFile(), "schema.rng" );
        Driver dr = new Driver( );
        dr.opt.schema = rngFile.toString( );
        dr.opt.keep_p = true;
        dr.opt.force_p = true;
        dr.opt.verbosity = 2;
        dr.opt.timings_p = true;
        for(RNGZSettings.BitCoding x 
                : RNGZSettings.BitCoding.values()) {
            for(RNGZSettings.DataCompression y 
                    : RNGZSettings.DataCompression.values()) {
                for(RNGZSettings.DataCompression z 
                        : RNGZSettings.DataCompression.values()) {
                    dr.opt.settings = new RNGZSettings(x, y, z);
                    dr.opt.suffix = "." + dr.opt.settings.toString( );
                    dr.run( args );
                }
            }
        }
    }
}
