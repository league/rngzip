package com.colloquial.arithcode;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.*;

/** <P>An input stream which uses a statistical model and arithmetic
 * coding for decompression of encoded bytes read from an underlying
 * input stream.  Given a statistical model of a byte sequence, it
 * operates in the same way as
 * <code>java.util.zip.GZIPInputStream</code>.
 *
 * @author <a href="http://www.colloquial.com/carp/">Bob Carpenter</a>
 * @version 1.1
 * @see ArithCodeOutputStream
 * @see ArithCodeModel
 * @since 1.0
 */
public final class ArithCodeInputStream extends InputStream {

    /** Construct an arithmetic coded input stream from a specified
     * arithmetic decoder and a statistical model.
     * @param decoder Arithmetic decoder from which to read input events.
     * @param model Statistical model for arithmetic coding.
     * @throws IOException If there is an I/O exception in the underlying input stream.
     */
    public ArithCodeInputStream(ArithDecoder decoder, ArithCodeModel model) throws IOException {
	_decoder = decoder;
	_model = model;
    }

    /** Construct an arithmetic coded input stream from a specified
     * bit input and a statistical model.
     * @param bitIn Bit input from which to read bits.
     * @param model Statistical model for arithmetic coding.
     * @throws IOException If there is an I/O exception in the underlying input stream.
     */
    public ArithCodeInputStream(BitInput in, ArithCodeModel model) throws IOException {
	this(new ArithDecoder(in), model);
    }

    /** Construct an arithmetic coded input stream from a specified
     * buffered input stream and a statistical model.
     * @param in Buffered input stream from which to read coded bits.
     * @param model Statistical model for arithmetic coding.
     * @throws IOException If there is an I/O exception in the underlying input stream.
     */
    public ArithCodeInputStream(BufferedInputStream in, ArithCodeModel model) throws IOException {
	this(new BitInput(in),model);
    }

    /** Construct an arithmetic coded input stream from a specified
     * input stream and a statistical model.
     * @param in Input stream from which to read coded bits.
     * @param model Statistical model for arithmetic coding.
     * @throws IOException If there is an I/O exception in the underlying input stream.
     */
    public ArithCodeInputStream(InputStream in, ArithCodeModel model) throws IOException {
	this(new BufferedInputStream(in), model);
    }
    
    /** Closes this input stream.
     * @throws IOException If there is an exception closing the underlying input stream.
     */
    public void close() throws IOException {
	_decoder.close();
    }

    /** Not supported.
     */
    public void mark(int readLimit)  {
    }

    /** Returns <code>false</code> because marking is not supported.
     * @return <code>false</code>.
     */
    public boolean markSupported() {
	return false;
    }

    /** Read an array of bytes into the specified byte array, returning
     * number of bytes read.
     * @param bs Byte array into which to read the bytes.
     * @return Number of bytes read.
     * @throws IOException If there is an I/O exception reading from the underlying stream.
     */
    public int read(byte[] bs) throws IOException {
	return read(bs,0,bs.length);
    }

    /** Read the specified number of bytes into the array, beginning from the position
     * specified by the offset. Return the total number of bytes read.  Will be less than
     * array length if the end of stream was encountered.
     * @param bs Byte array into which to read the bytes.
     * @param off Offset into byte array from which to begin writing output.
     * @param len Maximum number of bytes to read.
     * @return Number of bytes read.
     * @throws IOException If there is an I/O exception reading from the underlying stream.
     */
    public int read(byte[] bs, int off, int len) throws IOException {
	for (int i = off; i < len; ++i) {
	    int nextByte = read();
	    if (nextByte == -1) return (i - off); // eof, return length read
	    bs[i] = (byte)nextByte;
	}
	return len > 0 ? len : 0;
    }

    /* Reads the next byte from the input stream.   Returns -1 if end-of-stream
     * is encountered; otherwise result is given in the low order 8 bits of
     * the return value.
     * @return The next byte from the input stream or -1 if end of stream is encountered.
     * @throws IOException If there is an I/O exception reading from the underlying stream.
     */
    public int read() throws IOException {
	int result = decodeNextByte();
        if( result == -1 ) return -1;
        assert 0 <= result && result < 256 : result;
	return result;
    }
	
    /** Not supported.  Throws an <code>IOException</code> if called.
     * @throws IOException whenever called.
     */
    public void reset() throws IOException {
	throw new IOException("reset not supported in AdaptiveUnigramInputStream");
    }

    /** Skips the given number of bytes from the input.
     * @param n Number of bytes to skip.
     * @return Number of bytes skipped.
     * @throws IOException If there is an I/O exception reading from the underlying stream.
     */
    public long skip(long n) throws IOException {
	for (long i = 0; i < n; ++i) 
	    if (read() == -1) return i;
	return n;
    }

    /** The statistical model model on which the input stream is based.
     */
    private final ArithCodeModel _model;

    /** The arithmetic decoder used to read bytes.
     */
    private final ArithDecoder _decoder;

    /** The buffered next byte to write.  If it's equal to -1,
     * the end of stream has been reached, otherwise next byte
     * is the low order bits.
     */
    private boolean eof_p = false;

    /** Interval used for coding ranges.
     */
    private final int[] _interval = new int[3];

    /** Buffers the next byte into <code>_nextByte</code>.
     */
    private int decodeNextByte() throws IOException {
      if (eof_p || _decoder.endOfStream()) { 
        eof_p = true;
        return ArithCodeModel.EOF;
      }
	while (true) {
          int _nextByte = _model.pointToSymbol(_decoder.getCurrentSymbolCount(_model.totalCount()));
	    _model.interval(_nextByte,_interval);
	    _decoder.removeSymbolFromStream(_interval); 
	    if (_nextByte != ArithCodeModel.ESCAPE) return _nextByte;
	}
    }
    
}
