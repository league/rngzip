package org.kohsuke.bali.writer;

/**
 * Utility methods for writing source code.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Util {

    /**
     * Escape characters unallowed in the C# source files.
     */
    public static String toCSharpString( String buf ) {
        return escape(buf,"\\u");
    }

    /**
     * Escape characters unallowed in the Java source files.
     */
    public static String toJavaString( String buf ) {
        return escape(buf,"\\u");
    }
    
    private static String escape( String buf, String hexEscape ) {
        int len = buf.length();
        
        StringBuffer result = new StringBuffer(len);
        
        result.append('\"');
        for( int i=0; i<len; i++ ) {
            char ch = buf.charAt(i);
            int esc = "\r\t\n\"\\".indexOf(ch);
            if(esc!=-1) {
                result.append('\\');
                result.append("rtn\"\\".charAt(esc));
            } else
            if(ch<0x20 || ch>=0x80) {
                // non-ascii character. Print as \\uXXXX
                result.append(hexEscape);
                result.append( Integer.toHexString( (ch>>12)&15 ) );
                result.append( Integer.toHexString( (ch>> 8)&15 ) );
                result.append( Integer.toHexString( (ch>> 4)&15 ) );
                result.append( Integer.toHexString( (ch>> 0)&15 ) );
            } else {
                // printable ascii character
                result.append(ch);
            }
//            result.append(',');  debug
        }
        result.append('\"');
        
        return result.toString();
    }
    
    public static String toCppString( String buf ) {
        return escape(buf,"\\x");
    }
    
    /**
     * Increases the length of the string to 'w' by padding extra
     * whitespace at the end.
     */
    public static String padr( String s, int w ) {
        StringBuffer buf = new StringBuffer(s);
        while( buf.length()<w )
            buf.append(' ');
        return buf.toString();
    }
    
    /**
     * Increases the length of the string to 'w' by padding extra
     * whitespace at the end.
     */
    public static String padl( String s, int w ) {
        StringBuffer buf = new StringBuffer();
        
        int len = w-s.length();
        for( int i=0; i<len; i++ )
            buf.append(' ');
        return buf.toString()+s;
    }
    
    /**
     * Capitalizes the first character.
     */
    public static String capitalizeFirst( String s ) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
