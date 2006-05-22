package org.kohsuke.bali.writer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.datatype.DatatypeImpl;
import org.kohsuke.bali.datatype.ValidationContextImpl;
import org.kohsuke.bali.datatype.Value;
import org.relaxng.datatype.Datatype;

/**
 * Produces Java validatelet source code.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JavaWriter extends StringBasedEncoder {
    public JavaWriter( String packageName, String className, File outDir ) throws IOException {
        this.packageName = packageName;
        this.className = className;
        
        while( packageName.length()!=0 ) {
            int idx = packageName.indexOf('.');
            if(idx==-1) idx = packageName.length();
            
            String s = packageName.substring(0,idx);
            outDir = new File(outDir,s);
            outDir.mkdir();
            
            // move to the next part
            packageName = packageName.substring(idx);
            if(packageName.length()>0)  packageName=packageName.substring(1);
        }
        
        targetDir = outDir;
        out = new FileWriter( new File( outDir, className+".java" ) );
    }

    private final String packageName;
    private final String className;
    
    /** Writer connected to the generated validatelet. */
    private final Writer out;
    
    /** The directory to which all the generated files will be placed. */
    private final File targetDir;


    /**
     * Gets the package declaration as a string if the generated file
     * should belong to a package.
     */
    private String getPackageDeclaration() {
        return packageName.length()==0?"":("package "+packageName+";");
    }
    
    
    public void write(TreeAutomaton automaton) throws IOException {
        System.err.println("producing a Java validatelet");
        super.write(automaton);
    }
    protected void write(
        String encNameCodes,
        int defaultNameCode,
        String encStates,
        String encATr,
        String encDTr,
        String encETr,
        String encITr,
        String encLTr,
        String encNTr,
        String encVTr,
        DatatypeImpl[] datatypes,
        Value[] values ) throws IOException {



        out.write(format("preamble", getPackageDeclaration(), className ));

        out.write(Util.toJavaString(encNameCodes));
        out.write("/*name literals*/,\n");
        out.write(Integer.toString(defaultNameCode));
        out.write("/*default name code*/,\n");
        
        out.write(compress(encStates));
        out.write("/*states*/,\n");
        out.write(compress(encATr));
        out.write("/*a-tr*/,\n");
        out.write(compress(encDTr));
        out.write("/*d-tr*/,\n");
        out.write(compress(encETr));
        out.write("/*e-tr*/,\n");
        out.write(compress(encITr));
        out.write("/*i-tr*/,\n");
        out.write(compress(encLTr));
        out.write("/*l-tr*/,\n");
        out.write(compress(encNTr));
        out.write("/*n-tr*/,\n");
        out.write(compress(encVTr));
        out.write("/*v-tr*/,\n");
        out.write("new Object[]{");
        for( int i=0; i<datatypes.length; i++ ) {
            if(i!=0)    out.write(',');
            out.write(encode(datatypes[i]));
        }
        out.write("}/*datatypes*/,\n");
        out.write("new Object[]{");
        for( int i=0; i<values.length; i++ ) {
            if(i!=0)    out.write(',');
            out.write(Util.toJavaString(values[i].value));
            out.write(',');
            out.write(encodeContext(values[i].context));
        }
        out.write("}/*values*/");
            
        out.write(format("epilogue", className ));
        
        out.close();
        
        // copy other files necessary to run the generated code
        copy("AttributesSet.java");
        copy("BuiltinDatatypeLibrary.java");
        copy("NameCodeMap.java");
        copy("Schema.java");
        copy("State.java");
        copy("StateFactory.java");
        copy("Transition.java");
        copy("ValidateletImpl.java");
    }

    /**
     * Copies the resource file with the given file name to the target directory.
     * Used to produce runtime code the target directory.
     */
    private void copy( String fileName ) throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader(
            this.getClass().getResourceAsStream("/runtime/"+fileName) ));
        PrintWriter writer = new PrintWriter(new FileWriter( new File( targetDir, fileName ) ));
        
        // replace the package declaration
        reader.readLine();
        writer.write( getPackageDeclaration()+"\n" );
        
        // copy the rest of the lines
        String line;
        while( (line=reader.readLine())!=null )
            writer.println(line);
        
        reader.close();
        writer.close();       
    }
    
    private String compress( String s ) {
        if( s.length()<10000 )
            return escape(s);   // no need for compression
        
        try {
            // compress by GZip
            OutputStream os = new StringOutputStream();
            Writer w = new OutputStreamWriter(new GZIPOutputStream(os),"UTF-8");
            w.write(s);
            w.close();
            
            StringBuffer out = new StringBuffer();
            out.append("Schema.decompress(");
            out.append(escape(os.toString()));
            out.append(")");
            
            return out.toString();
        } catch( IOException e ) {
            // impossible
            throw new InternalError();
        }
    }
    
    protected String escape(String str) {
        return Util.toJavaString(str);
    }

}
