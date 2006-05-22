package org.kohsuke.bali.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;

import org.kohsuke.bali.datatype.DatatypeImpl;
import org.kohsuke.bali.datatype.Value;

/**
 * Generate C# source code.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class CSharpWriter extends StringBasedEncoder {
    
    private final PrintWriter out;
    
    private final String packageName;
    
    private final String className;
    
    private final File outDir;
    
    /**
     * @param _packageName
     *      package name or null to put the code into the root package.
     */
    public CSharpWriter( String _packageName, String _className, File _outDir ) throws IOException {
        this.packageName = _packageName;
        this.className = _className;
        this.out = new PrintWriter(new FileWriter(new File(_outDir,className+".cs")));
        this.outDir = _outDir;
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
        Value[] values)
        throws IOException {
            
        out.println("// GENERATED CODE --- DO NOT MODIFY");
        out.println();
        out.println("using System;");
        out.println("using System.Xml;");
        out.println("using org.relaxng.datatype;");
//        out.println("using Org.Kohsuke.Bali");
        out.println();
        
        if( packageName!=null ) {
            out.println("namespace "+packageName);
            out.println("{");
        }
        
        out.println("public class "+className+" : DomValidatelet");
        out.println("{");
        out.println(TAB+"///<summary>");
        out.println(TAB+"///Create a new instance of <c>Validatelet</c> preconfigured for this schema.");
        out.println(TAB+"///</summary>");
        out.println(TAB+"public "+className+"() : base("+className+".Schema) {}");
        out.println();
        out.println();
        out.println(TAB+"///<summary>");
        out.println(TAB+"///Default instance of the compiled schema.");
        out.println(TAB+"///Compiled with default datatype libraries.");
        out.println(TAB+"///</summary>");
        out.println(TAB+"public static readonly Schema Schema = CreateSchema();");
        out.println();
        out.println();
        out.println(TAB+"///<summary>");
        out.println(TAB+"///Main method for quick testing");
        out.println(TAB+"///</summary>");
        out.println(TAB+"public static int Main( string[] args ) {");
        out.println(TAB+TAB+"XmlDocument dom = new XmlDocument();");
        out.println(TAB+TAB+"dom.PreserveWhitespace = true;");
        out.println(TAB+TAB+"DomValidatelet validatelet = new "+className+"();");
        out.println(TAB+TAB+"try {");
        out.println(TAB+TAB+TAB+"foreach( string arg in args ) {");
        out.println(TAB+TAB+TAB+TAB+"Console.WriteLine(\"validating \"+arg);");
        out.println(TAB+TAB+TAB+TAB+"dom.Load(arg);");
        out.println(TAB+TAB+TAB+TAB+"validatelet.Validate(dom);");
        out.println(TAB+TAB+TAB+"}");
        out.println(TAB+TAB+"} catch( DomValidationException e ) {");
        out.println(TAB+TAB+TAB+"Console.WriteLine(e.Message);");
        out.println(TAB+TAB+TAB+"return -1;");
        out.println(TAB+TAB+"}");
        out.println(TAB+TAB+"Console.WriteLine(\"validated\");");
        out.println(TAB+TAB+"return 0;");
        out.println(TAB+"}");
        out.println();
        out.println();
        out.println(TAB+"///<summary>");
        out.println(TAB+"///Obtain a compiled schema by using default datatype libraries.");
        out.println(TAB+"///</summary>");
        out.println(TAB+"public static Schema CreateSchema() {");
        out.println(TAB+TAB+"return CreateSchema(new DefaultDatatypeLibraryLoader());");
        out.println(TAB+"}");
        out.println();
        out.println();
        out.println(TAB+"///<summary>");
        out.println(TAB+"///Obtain a compiled schema by using a custom datatype library loader.");
        out.println(TAB+"///</summary>");
        out.println(TAB+"public static Schema CreateSchema( DatatypeLibraryFactory datatypeFactory) {");
        out.println(TAB+TAB+"return new Schema(");

        out.write(Util.toCSharpString(encNameCodes));
        out.write("/*name literals*/,\n");
        out.write(Integer.toString(defaultNameCode));
        out.write("/*default name code*/,\n");
        
        out.write(escape(encStates));
        out.write("/*states*/,\n");
        out.write(escape(encATr));
        out.write("/*a-tr*/,\n");
        out.write(escape(encDTr));
        out.write("/*d-tr*/,\n");
        out.write(escape(encETr));
        out.write("/*e-tr*/,\n");
        out.write(escape(encITr));
        out.write("/*i-tr*/,\n");
        out.write(escape(encLTr));
        out.write("/*l-tr*/,\n");
        out.write(escape(encNTr));
        out.write("/*n-tr*/,\n");
        out.write(escape(encVTr));
        out.write("/*v-tr*/,\n");

        out.write("new object[]{");
        for( int i=0; i<datatypes.length; i++ ) {
            if(i!=0)    out.write(',');
            out.write(encode(datatypes[i]));
        }
        out.write("}/*datatypes*/,\n");
        out.write("new object[]{");
        for( int i=0; i<values.length; i++ ) {
            if(i!=0)    out.write(',');
            out.write(Util.toJavaString(values[i].value));
            out.write(',');
            out.write(encodeContext(values[i].context));
        }
        out.write("}/*values*/,\n");

        out.println(TAB+TAB+"datatypeFactory);");
        out.println(TAB+"}");
        out.println("}");
        
        if( packageName!=null )
            out.println("}");
            
        out.close();
        
        copy("BaliRuntime.cs");
        copy("DomValidatelet.cs");
    }

    /**
     * Copies the resource file with the given file name to the target directory.
     * Used to produce runtime code the target directory.
     */
    private void copy( String fileName ) throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader(
            this.getClass().getResourceAsStream("/csharp/"+fileName) ));
        PrintWriter writer = new PrintWriter(new FileWriter( new File( outDir, fileName ) ));
        
        // copy the rest of the lines
        String line;
        while( (line=reader.readLine())!=null ) {
            if( line.startsWith("namespace ") ) {
                // replace it with the user-specified namespace
                if( packageName==null )
                    continue;
                else
                    line = "namespace "+packageName + " {";
            }
            if( line.startsWith("}//end namespace") && packageName==null )
                continue;   // skip the end mark if necessary
            writer.println(line);
        }
        
        reader.close();
        writer.close();       
    }

    protected String escape(String str) {
        return Util.toCSharpString(str);
    }

    private static final String TAB = "\t";
}
