package net.contrapunctus.rngzip;

import java.io.File;
import net.contrapunctus.rngzip.io.RNGZSettings;
import org.junit.Test;

public class Benchmarks
{
    @Test
    public void nothing() { }

    private static Driver dr;
    private static String[] args;
    
    public static void main( String[] args ) throws Exception
    {
        File xmlFile = new File( args[0] );
        File rngFile = new File( xmlFile.getParentFile(), "schema.rng" );
        Benchmarks.args = args;
        dr = new Driver( );
        Driver.err = System.out;
        dr.opt.schema = rngFile.toString( );
        dr.opt.keep_p = true;
        dr.opt.force_p = true;
        dr.opt.verbosity = 2;
        dr.opt.timings_p = true;
        doBitCoding( RNGZSettings.BitCoding.FIXED );
        doBitCoding( RNGZSettings.BitCoding.HUFFMAN );
        doBitCoding( RNGZSettings.BitCoding.BYTE );
    }

    public static void doBitCoding( RNGZSettings.BitCoding bc )
        throws Exception
    {
        doTreeCmp( bc, RNGZSettings.DataCompression.NONE );
        doTreeCmp( bc, RNGZSettings.DataCompression.GZ );
        doTreeCmp( bc, RNGZSettings.DataCompression.LZMA );
        doTreeCmp( bc, RNGZSettings.DataCompression.BZ2 );
        doTreeCmp( bc, RNGZSettings.DataCompression.PPMX );
    }

    public static void doTreeCmp( RNGZSettings.BitCoding bc, 
                                  RNGZSettings.DataCompression tc )
        throws Exception
    {
        doDataCmp( bc, tc, RNGZSettings.DataCompression.GZ );
        doDataCmp( bc, tc, RNGZSettings.DataCompression.LZMA );
        doDataCmp( bc, tc, RNGZSettings.DataCompression.BZ2 );
        doDataCmp( bc, tc, RNGZSettings.DataCompression.PPMX );
    }

    public static void doDataCmp( RNGZSettings.BitCoding bc,
                                  RNGZSettings.DataCompression tc,
                                  RNGZSettings.DataCompression dc )
        throws Exception
    {
        dr.opt.settings = new RNGZSettings( bc, tc, dc );
        dr.opt.suffix = "." + dr.opt.settings.toString( );
        dr.run( args );
    }
}
