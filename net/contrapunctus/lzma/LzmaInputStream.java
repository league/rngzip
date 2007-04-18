package net.contrapunctus.lzma;

import SevenZip.Compression.LZMA.Encoder;
import SevenZip.Compression.LZMA.Decoder;
import java.io.*;

public class LzmaInputStream extends FilterInputStream
{
    protected DecoderThread dth;

    private static final PrintStream dbg = System.err;
    private static final boolean DEBUG =
        System.getProperty("DEBUG_LzmaStreams") != null;

    public LzmaInputStream( InputStream _in )
    {
        super( null );
        dth = new DecoderThread( _in );
        in = new ConcurrentBufferInputStream( dth.q );
        if(DEBUG) dbg.printf("%s << %s (%s)%n", this, in, dth.q);
        dth.start( );
    }

    public void close( ) throws IOException
    {
        if(DEBUG) dbg.printf("%s closed%n", this);
        super.close( );
    }

    public String toString( )
    {
        return String.format("lzmaIn@%x", hashCode());
    }

    public static void main( String[] args ) throws IOException
    {
        // first compress
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream( baos );
        ps.print("I will try decoding this text.");
        ps.close();
        byte[] buf = baos.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream( buf );
        baos = new ByteArrayOutputStream();
        Encoder enc = new Encoder();
        enc.SetEndMarkerMode(true);
        enc.WriteCoderProperties( baos );
        enc.Code( bis, baos, -1, -1, null );
        buf = baos.toByteArray();

        // then decompress traditionally
        bis = new ByteArrayInputStream( buf );
        Decoder dec = new Decoder();
        byte[] props = new byte[5];
        bis.read( props, 0, 5 );
        dec.SetDecoderProperties( props );
        dec.Code( bis, System.out, -1 );
        System.out.println();

        // and using stream
        bis = new ByteArrayInputStream( buf );
        LzmaInputStream is = new LzmaInputStream( bis );
        //for( int k = is.read();  k != -1;  k = is.read() )
        //    {
        //        System.out.printf("%02x %c%n", k, (char)k);
        //    }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        System.out.println(br.readLine());
    }
}
