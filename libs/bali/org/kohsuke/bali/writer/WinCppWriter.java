package org.kohsuke.bali.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.kohsuke.bali.automaton.Alphabet;
import org.kohsuke.bali.automaton.AttributeAlphabet;
import org.kohsuke.bali.automaton.DataAlphabet;
import org.kohsuke.bali.automaton.ElementAlphabet;
import org.kohsuke.bali.automaton.InterleaveAlphabet;
import org.kohsuke.bali.automaton.ListAlphabet;
import org.kohsuke.bali.automaton.NameSignature;
import org.kohsuke.bali.automaton.NonExistentAttributeAlphabet;
import org.kohsuke.bali.automaton.State;
import org.kohsuke.bali.automaton.TextAlphabet;
import org.kohsuke.bali.automaton.Transition;
import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.automaton.ValueAlphabet;
import org.kohsuke.bali.datatype.DatatypeImpl;
import org.kohsuke.bali.datatype.Parameter;
import org.kohsuke.bali.datatype.Value;
import org.relaxng.datatype.Datatype;

import com.sun.msv.util.StringPair;

/**
 * Produces C++ code for Windows/Visual C++ environment
 * that works with MSXML.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class WinCppWriter implements AutomatonWriter {

    public WinCppWriter( String namespaceName, String className, File outDir ) throws IOException {
        this.namespaceName = namespaceName;
        this.className = className;
        
        targetDir = outDir;
        cppFile =    new PrintWriter( new FileWriter( new File( outDir, className+".cpp" ) ) );
        headerFile = new PrintWriter( new FileWriter( new File( outDir, className+".h" ) ) );
    }

    /** Namespace to put the generated code, or null if no namespace is necessary. */
    private final String namespaceName;
    
    private final String className;
    
    /** Writer connected to the generated source file. */
    private final PrintWriter cppFile;
    
    /** Writer connected to the generated header file. */
    private final PrintWriter headerFile;
    
    /** The directory to which all the generated files will be placed. */
    private final File targetDir;
    
    /**
     * Stores objects and assign them continuous index number that 
     * starts from 0.
     * <p>
     * Used to store transitions into one array.
     */
    private class IndexMap
    {
        /** back-end storage. */
        private final HashMap core = new HashMap();
        /** storage for reverse look up */
        private final ArrayList list = new ArrayList();
        
        /** Gets the index assigned to the given object. */
        public int get( Object o ) {
            // let it cause an error if the object is not in the table
            return ((Integer)core.get(o)).intValue();
        }
        
        /** Gets the object with the specified index. */
        public Object get( int index ) {
            return list.get(index);
        }
        
        public int size() { return list.size(); }
        
        /**
         * Stores a new object into the map.
         * 
         * If the specified object is new, this method assigns a new
         * index and returns it. If it is already contained in this set,
         * just return the existing index number.
         */
        public int put( Object o ) {
            Object v = core.get(o);
            if(v!=null)     return ((Integer)v).intValue();
            
            // new object. assign a new number
            core.put( o, new Integer(index) );
            list.add( o );
            return index++;
        }
        
        public boolean contains( Object o ) {
            return core.containsKey(o);
        }
        
        private int index = 0;
    }

    
    
    
    public void write(TreeAutomaton automaton) throws IOException {
        // initialize buffers
        datatypesCode = new StringWriter();
        datatypesWriter = new PrintWriter(datatypesCode);
        datatypes = new IndexMap();
        
        nameTestsCode = new StringWriter();
        nameTestsWriter = new PrintWriter(nameTestsCode);
        
        cppFile.println("#include \"stdafx.h\"");
        cppFile.println("#include \"validateletImpl.h\"");
        cppFile.println("#include \""+className+".h\"");
        cppFile.println();

        if( namespaceName!=null )
            cppFile.println("namespace "+namespaceName+" {");
        
        cppFile.println("using namespace MSXML2;");
        cppFile.println("using namespace bali;");
        cppFile.println("using namespace bali::transition;");
        cppFile.println();
        
        cppFile.println("SingleState states[];");
        cppFile.println("Datatype* datatypes[];");
        cppFile.println("NameSignature nameTests[];");
        cppFile.println();
        
        
        // do the real work
        
        State[] states = automaton.getStates();
        
        // print attributes
        IndexMap amap = printAlphabets( automaton, AttributeAlphabet.class, "Att", "aTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    AttributeAlphabet a = (AttributeAlphabet)tr.alphabet;
                    
                    cppFile.println(MessageFormat.format(
                        "  '{' '{' {0},{1} '}',{2},states+{3},states+{4},{5} '}',",
                        new Object[] {
                            new Integer(a.name.mask),
                            new Integer(a.name.test),
                            a.repeated?"true":"false",
                            new Integer(tr.left.id),
                            new Integer(tr.right.id),
                            (previousIndex==-1)?"NULL":"aTr+"+previousIndex }));
                }
                public void printEmptyLine() {
                    cppFile.println(" { {-1,-1}, false,NULL,NULL,NULL },");
                }
            });
        
        // print datatype alphabets
        IndexMap dmap = printAlphabets( automaton, TextAlphabet.class, "Data", "dTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    if( tr.alphabet instanceof DataAlphabet ) {
                        DataAlphabet d = (DataAlphabet)tr.alphabet;
                        
                        cppFile.println(MessageFormat.format(
                            "  '{' states+{0},states+{1},datatypes+{2},{3} '}',",
                            new Object[] {
                                new Integer(tr.left.id),
                                new Integer(tr.right.id),
                                new Integer(printDatatype(d.datatype)),
                                (previousIndex==-1)?"NULL":"dTr+"+previousIndex }));
                    }
                    if( tr.alphabet instanceof ValueAlphabet ) {
                        ValueAlphabet v = (ValueAlphabet)tr.alphabet;
                        
                        cppFile.println(MessageFormat.format(
                            "  '{' NULL,states+{1},datatypes+{2},{3} '}',",
                            new Object[] {
                                null,
                                new Integer(tr.right.id),
                                new Integer(printDatatype(v)),
                                (previousIndex==-1)?"NULL":"dTr+"+previousIndex }));
                    }
                }
                public void printEmptyLine() {
                    cppFile.println(" { NULL,NULL,NULL,NULL },");
                }
            });
        
        // print elements
        IndexMap emap = printAlphabets( automaton, ElementAlphabet.class, "Element", "eTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    ElementAlphabet e = (ElementAlphabet)tr.alphabet;
                    
                    cppFile.println(MessageFormat.format(
                        "  '{' '{' {0},{1} '}',states+{2},states+{3},{4} '}',",
                        new Object[] {
                            new Integer(e.name.mask),
                            new Integer(e.name.test),
                            new Integer(tr.left.id),
                            new Integer(tr.right.id),
                            (previousIndex==-1)?"NULL":"eTr+"+previousIndex }));
                }
                public void printEmptyLine() {
                    cppFile.println(" { {-1,-1}, NULL,NULL,NULL },");
                }
            });
        
        // print interleaves
        IndexMap imap = printAlphabets( automaton, InterleaveAlphabet.class, "Interleave", "iTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    InterleaveAlphabet i = (InterleaveAlphabet)tr.alphabet;
                    
                    cppFile.println(MessageFormat.format(
                        "  '{' states+{0},states+{1},states+{2},{3},{4} '}',",
                        new Object[] {
                            new Integer(tr.left.id),
                            new Integer(tr.right.id),
                            new Integer(i.join.id),
                            i.textToLeft?"true":"false",
                            (previousIndex==-1)?"NULL":"iTr+"+previousIndex }));
                }
                public void printEmptyLine() {
                    cppFile.println(" { NULL,NULL,NULL,false,NULL },");
                }
            });
        
        // print lists
        IndexMap lmap = printAlphabets( automaton, ListAlphabet.class, "List", "lTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    ListAlphabet l = (ListAlphabet)tr.alphabet;
                    
                    cppFile.println(MessageFormat.format(
                        "  '{' states+{0},states+{1},{2} '}',",
                        new Object[] {
                            new Integer(tr.left.id),
                            new Integer(tr.right.id),
                            (previousIndex==-1)?"NULL":"lTr+"+previousIndex }));
                }
                public void printEmptyLine() {
                    cppFile.println(" { NULL,NULL,NULL },");
                }
            });
        
        // print nonAtts
        IndexMap nmap = printAlphabets( automaton, NonExistentAttributeAlphabet.class, "NoAtt", "nTr",
            new Printer() {
                public void print(Transition tr, int previousIndex) {
                    NonExistentAttributeAlphabet n = (NonExistentAttributeAlphabet)tr.alphabet;
                    
                    cppFile.println(MessageFormat.format(
                        "  '{' states+{0},nameTests+{1},{2},nameTests+{3},{4},{5} '}',",
                        new Object[] {
                            new Integer(tr.right.id),
                            printNameTests(n.negativeNameTests),
                            new Integer(n.negativeNameTests.length),
                            printNameTests(n.positiveNameTests),
                            new Integer(n.positiveNameTests.length),
                            (previousIndex==-1)?"NULL":"nTr+"+previousIndex }));
                }
                public void printEmptyLine() {
                    cppFile.println(" { NULL,NULL,0,NULL,0,NULL },");
                }
            });
        
        
        {// print datatype objects
            String str = datatypesCode.getBuffer().toString();
            if(str.length()!=0) {
                cppFile.println("static Datatype* datatypes[] = {");
                cppFile.write(str);
                cppFile.println("};");
                cppFile.println();
            }
        }
        
        {// print name signatures
            String str = nameTestsCode.getBuffer().toString();
            if(str.length()!=0) {
                cppFile.println("static NameSignature nameTests[] = {");
                cppFile.write(str);
                cppFile.println("};");
                cppFile.println();
            }
        }
        
        // print states
        cppFile.println("static StateInfo stateInfos[] = {");
        for( int i=0; i<states.length; i++ ) {
            cppFile.println(MessageFormat.format(
                "'{' {0},{1},{2},{3},{4},{5},{6},{7},{8} '}',",
                new Object[] {
                    Util.padl(Integer.toString(i),4),
                    states[i].isFinal?"true ":"false",
                    states[i].isPersistent()?"true ":"false",
                    findFirstTransition(states[i],"aTr",amap,AttributeAlphabet.class),
                    findFirstTransition(states[i],"dTr",dmap,TextAlphabet.class),
                    findFirstTransition(states[i],"eTr",emap,ElementAlphabet.class),
                    findFirstTransition(states[i],"iTr",imap,InterleaveAlphabet.class),
                    findFirstTransition(states[i],"lTr",lmap,ListAlphabet.class),
                    findFirstTransition(states[i],"nTr",nmap,NonExistentAttributeAlphabet.class) }));
        }
        cppFile.println("};");
        cppFile.println();
        cppFile.println(MessageFormat.format("static SingleState states[{0}];",
            new Object[]{ new Integer(states.length) }));
        cppFile.println();
        
        
        // print name literals
        StringPair[] literals = automaton.listNameCodes();
        if( literals.length!=0 ) {
            cppFile.println("static NameLiteral nameLiterals[] = {");
            for( int i=0; i<literals.length; i++ ) {
                String uri = literals[i].namespaceURI;
                String local = literals[i].localName;
                if( uri==TreeAutomaton.IMPOSSIBLE )     uri = "*";
                if( local==TreeAutomaton.IMPOSSIBLE )   local = "*";
                cppFile.println(MessageFormat.format("  '{' L{0},L{1},{2} '}',",
                    new Object[]{
                        Util.toCppString(uri),
                        Util.toCppString(local),
                        new Integer( automaton.getNameCode(literals[i]) ) }));
            }
            cppFile.println("};");
            cppFile.println();
        }
        
        cppFile.println("// schema object");
        cppFile.println(MessageFormat.format("Schema {0}(states,stateInfos,{1},states+0,{2},{3},{4});",
            new Object[]{
                className,
                new Integer(states.length),
                (literals.length!=0)?"nameLiterals":"NULL",
                new Integer(literals.length),
                new Integer(automaton.getNameCode(TreeAutomaton.WILDCARD)) }));
        cppFile.println();
        
        
        cppFile.println( MessageFormat.format(
            "ISAXContentHandler* create{0}SAXValidatelet() '{'\n"+
            "    ComObject<Validatelet>* p = new ComObject<Validatelet>();\n"+
            "    p->AddRef();\n"+
            "    p->setSchema(&{1});\n"+
            "    return p;\n"+
            "'}'", new Object[]{ Util.capitalizeFirst(className), className } ));

        cppFile.println( MessageFormat.format(
            "bali::IValidatelet* create{0}DOMValidatelet() '{'\n"+
            "    ComObject<Validatelet>* p = new ComObject<Validatelet>();\n"+
            "    p->AddRef();\n"+
            "    p->setSchema(&{1});\n"+
            "    return p;\n"+
            "'}'", new Object[]{ Util.capitalizeFirst(className), className } ));
        
        
        cppFile.println("}");   // close namespace
        
        
        headerFile.println(MessageFormat.format(
            "#pragma once\n"+
            "\n"+
            "\n"+
            "// for this validatelet to compile,\n"+
            "// stdafx.h needs to have the following lines.\n"+
            "/*\n"+
            "#import <msxml4.dll>\n"+
            "#include <crtdbg.h>\n"+
            "#include \"validatelet.h\"\n"+
            "*/\n"+
            "\n"+
            "\n"+
            "// create a new instance of validatelet.\n"+
            "// A validatelet will be returned with its reference count\n"+
            "// incremented to 1. Thus it's the caller's responsibility to\n"+
            "// release the object.\n"+
            "{0}\n"+
            "MSXML2::ISAXContentHandler* create{1}SAXValidatelet();\n"+
            "bali::IValidatelet* create{1}DOMValidatelet();\n"+
            "{2}\n",
            new Object[]{
                namespaceName!=null?"namespace "+namespaceName+" {":"",
                Util.capitalizeFirst(className),
                namespaceName!=null?"}":"" }));
        
        cppFile.close();
        headerFile.close();
        
        copy("validatelet.h");
        copy("validatelet.cpp");
        copy("validateletImpl.h");
    }
    
    
    /** Datatypes are generated into this string. */
    private StringWriter datatypesCode;
    /** Writer object connected to the datatypesCode. */
    private PrintWriter datatypesWriter;
    /** Maintains index for datatypes. */
    private IndexMap datatypes;

    /**
     * Generates a datatype definition and returns
     * its index number.
     */
    private int printDatatype( Datatype _dt ) {
        
        DatatypeImpl dt = (DatatypeImpl)_dt;
        
        if( datatypes.contains(dt) )    // this datatype has already been generated
            return datatypes.get(dt);
        
        int idx = datatypes.put(dt);
        
        PrintWriter w = datatypesWriter;
        w.print("  createDatatype(L");
        w.print(Util.toCppString(dt.nsURI));
        w.print(",L");
        w.print(Util.toCppString(dt.name));
        
        // TODO: context
        Parameter[] params = dt.parameters;
        for( int i=0; i<params.length; i++ ) {
            w.print(",L");
            w.print(Util.toCppString(params[i].name));
            w.print(",L");
            w.print(Util.toCppString(params[i].value));
        }
        w.println("),");
        
        return idx;
    }
    
    /**
     * Generates a value alphabet as a datatype definition
     * and returns its index.
     */
    private int printDatatype( ValueAlphabet va ) {
        
        if( datatypes.contains(va) )    // this datatype has already been generated
            return datatypes.get(va);
        
        int dtidx = printDatatype(va.datatype); // generate the underlying datatype first
        
        int idx = datatypes.put(va);
        
        datatypesWriter.println(MessageFormat.format(
            "  createValueDatatype(datatypes+{0},L{1}),",
            new Object[] {
                new Integer(dtidx),
                // TODO: context
                Util.toCppString(((Value)va.value).value) }));
        return idx;
    }
    
    private StringWriter nameTestsCode;
    private PrintWriter nameTestsWriter;
    private int nameTestsSize = 0;
    
    /**
     * Prints all the name tests in one row and returns the index
     * to the first one.
     */
    private Integer printNameTests( NameSignature[] tests ) {
        
        for( int i=0; i<tests.length; i++ )
            nameTestsWriter.println(MessageFormat.format(
                "  '{' {0},{1} '}',", new Object[] {
                    new Integer(tests[i].mask),
                    new Integer(tests[i].test) }));
        
        int r = nameTestsSize;
        nameTestsSize += tests.length;
        return new Integer(r);
    }
    
    
    
    
    
    
    
    private interface Printer {
        /**
         * Prints an alphabet.
         * 
         * @param previousIndex
         *      -1 if no previous alphabet, or its index.
         */
        public void print( Transition tr, int previousIndex );

        /**
         * Prints a dummy empty alphabet.
         */
        public void printEmptyLine();
    }
    
    private IndexMap printAlphabets( TreeAutomaton automaton,
        Class alphabetType, String typeName, String varName, Printer printer ) {

        IndexMap map = new IndexMap();

        cppFile.println(MessageFormat.format("static {0} {1}[] = '{'",
            new Object[]{typeName,varName}));
        
        boolean hasAlphabet = false;
        
        State[] states = automaton.getStates();
        for( int i=0; i<states.length; i++ ) {
            Object o = printAlphabets(alphabetType,map,states[i],printer);
            hasAlphabet |= o!=null;
        }
        
        if(!hasAlphabet)
            // if no alphabet was there, print an empty line (otherwise
            // the compiler will barf.)
            printer.printEmptyLine();
        
        cppFile.println("};");
        cppFile.println("");
        
        return map;
    }
    
    /**
     * Prints all alphabets of the given type
     * and returns the lastly printed alphabet, or null if none.
     */
    private Transition printAlphabets( Class alphabetType, IndexMap map, State st, Printer printer ) {
        Transition last;
        
        if( st.nextState!=null )
            last = printAlphabets(alphabetType,map,st.nextState,printer);
        else
            last = null;
        
        Transition[] trs = st.getTransitions();
        for( int i=0; i<trs.length; i++ ) {
            Transition tr = trs[i];
            if( alphabetType.isInstance(tr.alphabet) ) {
                
                if( map.contains(tr) )
                    return tr;   // this state has already been processed
                
                map.put(tr);   // register this new alphabet
                
                printer.print( tr, last==null?-1:map.get(last) );
                
                last = tr;
            }
        }
        
        return last;
    }
    
    /**
     * Finds the first transition of the given type of alphabet and
     * returns "[varName]+[index]". If none is found, return "NULL".
     */
    private String findFirstTransition( State st, String varName, IndexMap map, Class alphabetType ) {
        
        // note that we have to search it in the reverse order
        Transition[] trs = st.getTransitions();
        for( int i=trs.length-1; i>=0; i-- ) {
            Transition tr = trs[i];
            if( alphabetType.isInstance(tr.alphabet) )
                return varName+"+"+
                    Util.padl( Integer.toString(map.get(tr)), 3 );
        }
        
        if( st.nextState!=null )
            return findFirstTransition(st.nextState,varName,map,alphabetType);
        
        return Util.padr("NULL",7);
    }


    /**
     * Copies the resource file with the given file name to the target directory.
     * Used to produce runtime code the target directory.
     */
    public void copy( String fileName ) throws IOException {
        InputStream is = this.getClass().getResourceAsStream("/wincpp/"+fileName);
        OutputStream os = new FileOutputStream( new File( targetDir, fileName ) );
        
        // copy the data
        byte[] buf = new byte[256];
        int len;
        
        while( (len=is.read(buf)) != -1 )
            os.write(buf,0,len);
        
        is.close();
        os.close();       
    }
}
