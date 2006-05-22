package org.kohsuke.bali;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.automaton.builder.*;
import org.kohsuke.bali.datatype.DatatypeLibraryImpl;
import org.kohsuke.bali.optimizer.AttributeReorder;
import org.kohsuke.bali.optimizer.InterleaveStrengthReducer;
import org.kohsuke.bali.optimizer.Unifier;
import org.kohsuke.bali.optimizer.ZeroOrMoreAttributeExpander;
import org.kohsuke.bali.writer.AutomatonDumper;
import org.kohsuke.bali.writer.AutomatonVisualizer;
import org.kohsuke.bali.writer.AutomatonWriter;
import org.kohsuke.bali.writer.CSharpWriter;
import org.kohsuke.bali.writer.Interpreter;
import org.kohsuke.bali.writer.StringBasedEncoder;
import org.kohsuke.bali.writer.JavaWriter;
import org.kohsuke.bali.writer.MultiWriter;
import org.kohsuke.bali.writer.NullWriter;
import org.kohsuke.bali.writer.WinCppWriter;
import org.relaxng.datatype.DatatypeLibrary;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.msv.driver.textui.DebugController;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.util.ExpressionWalker;
import com.sun.msv.reader.Controller;
import com.sun.msv.reader.dtd.DTDReader;
import com.sun.msv.reader.trex.ng.RELAXNGReader;
import com.sun.msv.reader.util.GrammarLoader;
import com.sun.msv.scanner.dtd.DTDParser;
import com.sun.msv.verifier.jaxp.SAXParserFactoryImpl;
import com.sun.msv.writer.relaxng.RELAXNGWriter;

import runtime.ValidateletImpl;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Driver {

    private static int usage( String msg ) {
        if(msg!=null)
            System.err.println(msg);
        
        System.err.println(
            "Usage: bali <schema file name> [<instance1> <instance2> ...]\n"+
            "\n"+
            "Input Options (by default file extension is used)\n"+
            "  -dtd: input schema file is DTD\n"+
            "  -rng: input schema file is RELAX NG\n"+
            "Optimization Options (all turned on by default. append '-' to turn off (e.g., -u-) \n"+
            "  -u  : compress the automaton by minimizing the input grammar\n"+
            "  -ir : compress the automaton by reducing <interleave>s to <group>s if possible\n"+
            "  -ia : compress ignorable attributes\n"+
            "  -ra : reorder attributes\n"+
            "  -ss : compress transition table by sharing them across states\n"+
            "  -mx : improve the runtim efficiency of <mixed>\n"+
            "  -za : massage zero-or-more attributes\n"+
            "Output Options:\n"+
            "  -oa <format> :\n"+
            "        dump automaton into a graphic file (gif/ps/png)\n"+
            "  -oj <target dir> <fully qualified class name> :\n"+
            "        output Java validatelet\n"+
            "  -ocs <target dir> <fully qualified class name> :\n"+
            "        output .NET C# validatelet\n"+
            "  -ot :\n"+
            "        dump automaton to a text file\n"+
            "  -ow <target dir> <namespace> <class name>\n"+
            "        output Visual C++ validatelet for Win32\n"+
            "        <namespace> can be '-' to indicate no namespace\n"+
            "Other Options:\n"+
            "  -ne : just encode names in the grammar and quit\n"+
            "  -v  : be verbose\n"+
            "\n"+
            "  instance documents are validated with the interpreter if specified\n");
        return -1;
    }
    
    public static void main(String[] args) {
        System.exit(doMain(args));
    }
    
    // schema language constants
    private static final int SCHEMALANGUAGE_AUTODETECT  = 0;
    private static final int SCHEMALANGUAGE_RELAXNG     = 1;
    private static final int SCHEMALANGUAGE_DTD         = 2;
    
    
    /**
     * Executes the program and returns the exit code.
     * 
     * This method will never call <code>System.exit</code>.
     */
    public static int doMain(String[] args) {

        String grammarName = null;
        AutomatonWriter writer = new NullWriter();
        
        // set to non-null if we need an interpreter.
        Interpreter interpreter = null;
        
        boolean unify = true;
        boolean interleaveReduction = true;
        boolean ignorableAttributeOptimization = true;
        boolean nameEncodingTest = false;
        boolean useEpsilonTransition = true;
        boolean optimizeMixed = true;
        boolean expandZeroOrMoreAtts = true;
        boolean reorderAttributes = true;
        boolean verbose = false;
        
        int schemaLanguage = SCHEMALANGUAGE_AUTODETECT;
        
        ArrayList instances = new ArrayList();

        TreeAutomaton automaton;
        

        try {
            try {
                for( int i=0; i<args.length; i++ ) {
                    String arg = args[i];
                    
                    if("-/".indexOf(arg.charAt(0))!=-1) {
                        String opt = arg.substring(1);
                        
                        if(opt.equals("oa")) {
                            String type = args[++i];
                            writer = new MultiWriter( writer,
                                new AutomatonVisualizer( type,  System.out ));
                            continue;
                        }
                        if(opt.equals("oj")) {
                            File target = new File(args[++i]);
                            if( !target.isDirectory() )
                                return usage("Cowardly refuse to output to a non-existent directory: "+target);
                            
                            String fullClassName = args[++i];
                            
                            int idx = fullClassName.lastIndexOf('.');
                            
                            if(idx==-1)
                                writer = new MultiWriter( writer,
                                    new JavaWriter( "", fullClassName, target ));
                            else
                                writer = new MultiWriter( writer,
                                    new JavaWriter(
                                        fullClassName.substring(0,idx),
                                        fullClassName.substring(idx+1),
                                        target ));
                            
                            continue;
                        }
                        if(opt.equals("ocs")) {
                            File target = new File(args[++i]);
                            if( !target.isDirectory() )
                                return usage("Cowardly refuse to output to a non-existent directory: "+target);
                            
                            String fullClassName = args[++i];
                            
                            int idx = fullClassName.lastIndexOf('.');
                            
                            if(idx==-1)
                                writer = new MultiWriter( writer,
                                    new CSharpWriter( null, fullClassName, target ));
                            else
                                writer = new MultiWriter( writer,
                                    new CSharpWriter(
                                        fullClassName.substring(0,idx),
                                        fullClassName.substring(idx+1),
                                        target ));
                            
                            continue;
                        }
                        if(opt.equals("ot")) {
                            writer = new MultiWriter( writer,
                                new AutomatonDumper(System.out));
                            continue;
                        }
                        if(opt.equals("ow")) {
                            File target = new File(args[++i]);
                            if( !target.isDirectory() )
                                return usage("Cowardly refuse to output to a non-existent directory: "+target);
                                
                            String namespace = args[++i];
                            String className = args[++i];
                            
                            if(namespace.equals("-"))   namespace=null;
                            
                            writer = new MultiWriter( writer,
                                new WinCppWriter( namespace, className, target ) );
                            continue;
                        }
                        if(opt.equals("u")) {
                            unify = true;
                            continue;
                        }
                        if(opt.equals("u-")) {
                            unify = false;
                            continue;
                        }
                        if(opt.equals("ir")) {
                            interleaveReduction = true;
                            continue;
                        }
                        if(opt.equals("ir-")) {
                            interleaveReduction = false;
                            continue;
                        }
                        if(opt.equals("ia")) {
                            ignorableAttributeOptimization = true;
                            continue;
                        }
                        if(opt.equals("ia-")) {
                            ignorableAttributeOptimization = false;
                            continue;
                        }
                        if(opt.equals("ss")) {
                            useEpsilonTransition = true;
                            continue;
                        }
                        if(opt.equals("ss-")) {
                            useEpsilonTransition = false;
                            continue;
                        }
                        if(opt.equals("ra")) {
                            reorderAttributes = true;
                            continue;
                        }
                        if(opt.equals("ra-")) {
                            reorderAttributes = false;
                            continue;
                        }
                        if(opt.equals("mx")) {
                            optimizeMixed = true;
                            continue;
                        }
                        if(opt.equals("mx-")) {
                            optimizeMixed = false;
                            continue;
                        }
                        if(opt.equals("za")) {
                            expandZeroOrMoreAtts = true;
                            continue;
                        }
                        if(opt.equals("za-")) {
                            expandZeroOrMoreAtts = false;
                            continue;
                        }
                        if(opt.equals("ne")) {
                            nameEncodingTest = true;
                            continue;
                        }
                        if(opt.equals("v")) {
                            verbose = true;
                            continue;
                        }
                        if(opt.equals("dtd")) {
                            schemaLanguage = SCHEMALANGUAGE_DTD;
                            continue;
                        }
                        if(opt.equals("rng")) {
                            schemaLanguage = SCHEMALANGUAGE_RELAXNG;
                            continue;
                        }
                            
                        return usage("Unknown option: "+arg);
                    }
                    
                    if( grammarName!=null ) {
                        if( interpreter==null ) {
                            // make sure that the interpreter will run.
                            interpreter = new Interpreter();
                            writer = new MultiWriter( writer, interpreter );
                        }
                        instances.add(arg);
                        continue;
                    }
                        
                    grammarName = arg;
                }
            } catch( ArrayIndexOutOfBoundsException e ) {
                // incorrect command line parameters can cause this error
                return usage("incorrect parameter arity");
            }
        
        
            if( grammarName==null )
                return usage("no grammar is given");
            
            if( schemaLanguage==SCHEMALANGUAGE_AUTODETECT ) {
                if( grammarName.substring(Math.max(0,grammarName.length()-4)).equals(".rng") )
                    schemaLanguage = SCHEMALANGUAGE_RELAXNG;
                else
                if( grammarName.substring(Math.max(0,grammarName.length()-4)).equals(".dtd") )
                    schemaLanguage = SCHEMALANGUAGE_DTD;
            }
        
        
            System.err.println("parsing the grammar");
            Grammar grammar=null;
            URL grammarUrl = new File(grammarName).toURL();
            try {
                switch( schemaLanguage ) {
                case SCHEMALANGUAGE_RELAXNG:
                    grammar = loadRELAXNGGrammar(grammarUrl);
                    break;
                case SCHEMALANGUAGE_DTD:
                    grammar = loadDTDGrammar(grammarUrl);
                    break;
                default:
                    grammar = loadOtherGrammar(grammarUrl);
                    break;
                }
            } catch( IOException e ) {
                e.printStackTrace();
            } catch( SAXException e ) {
                ;   // the error should have been reported already
            } catch( ParserConfigurationException e ) {
                e.printStackTrace();
            }
            
            if(grammar==null) {
                System.err.println("failed to parse a grammar");
                return -2;
            }
            
            if( unify ) {
                System.err.println("compacting the grammar");
                grammar = Unifier.unify(grammar);
            }
            
            if( expandZeroOrMoreAtts ) {
                System.err.println("massaging zero or more attributes");
                grammar = ZeroOrMoreAttributeExpander.optimize(grammar);
            }
            
            if( interleaveReduction ) {
                System.err.println("reducing <interleave>s");
                grammar = InterleaveStrengthReducer.optimize(grammar);
            }
            
            if( reorderAttributes ) {
                System.err.println("reordering attributes");
                grammar = AttributeReorder.optimize(grammar);
            }
            
            if(nameEncodingTest) {
                testNameEncoding(grammar);
                return 0;
            }
            
            System.err.println("building the automaton");
            if( ignorableAttributeOptimization )
                System.err.println("  + optimize ignorable optional attributes");
            if( useEpsilonTransition )
                System.err.println("  + compress transition table");
            if( optimizeMixed )
                System.err.println("  + optimize <mixed>");
                
            automaton = TreeAutomatonBuilder.build(
                grammar,
                ignorableAttributeOptimization,
                useEpsilonTransition,
                optimizeMixed);
            System.err.println(automaton.countStates()+" states and "+automaton.countTransitions()+" transitions");
            
            // convert this automaton into whatever form the user wants.
            writer.write(automaton);
            
//        } catch( SAXException e ) {
//            ; // should have been reported
//            e.printStackTrace();
//            return -2;
        } catch( IOException e ) {
            printException(e,verbose);
            return -2;
        } catch( TooComplicatedException e ) {
            System.out.println("the grammar is too big");
            return -2;
        }


        if( instances.isEmpty() )
            return 0; // quit here

        //
        // run interpreter
        //
        try {
            // create a configured SAX parser factory
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            
            
            XMLReader reader = spf.newSAXParser().getXMLReader();
            ValidateletImpl v = interpreter.createValidatelet();
            reader.setContentHandler(v);
            
            for (Iterator itr = instances.iterator(); itr.hasNext();) {
                String name = (String) itr.next();
                System.out.println("validating "+name);
                
                reader.parse(new File(name).toURL().toExternalForm());
                System.out.println("valid");
            }
        } catch( SAXException e ) {
            printException(e,verbose);
            return 0;
        } catch( IOException e ) {
            printException(e,verbose);
            return 0;
        } catch( ParserConfigurationException e ) {
            printException(e,verbose);
            return 0;
        }
        
        return 0;
    }
    
    private static void printException( Exception e, boolean verbose ) {
        if( verbose )
            e.printStackTrace();
        else
            System.err.println("ERROR: "+e.getMessage());
    }

    /**
     * Changes the file extension to a new one.
     * 
     * @param newExt
     *      something like "abc" without a dot.
     */
    private static String replaceExtension( String pathName, String newExt ) {
        int idx1 = pathName.lastIndexOf(File.separatorChar);
        int idx2 = pathName.lastIndexOf('.');
        if( idx1<idx2 )
            return pathName.substring(0,idx2)+'.'+newExt;
        else
            return pathName+"."+newExt;
    }


    /**
     * Encode all names in the grammar and dumps it to the screen.
     */
    public static void testNameEncoding( Grammar g ) throws TooComplicatedException {
        NameClassEncoder e = NameClassEncoder.build(g);
        e.dumpSigntures();
    }
    
    /**
     * Loads a RELAX NG grammar.
     * 
     * @return null
     *      If failed to parse a schema
     */
    public static Grammar loadRELAXNGGrammar( URL url ) {
        RELAXNGReader reader = createRELAXNGReader();        
        reader.parse( url.toExternalForm() );
        return reader.getResult();
    }

    public static RELAXNGReader createRELAXNGReader() {
        SAXParserFactory spf = new SAXParserFactoryImpl(RELAXNGReader.getRELAXNGSchema4Schema());
        spf.setNamespaceAware(true);
        spf.setValidating(false);
        
        RELAXNGReader reader = new RELAXNGReader(
            new DebugController(true),
            spf,
            new RELAXNGReader.StateFactory() {
                // return our DatatypeLibrary implementation
                public DatatypeLibrary resolveDataTypeLibrary( String namespaceURI ) {
                    return new DatatypeLibraryImpl(namespaceURI);
                }
            },
            new ExpressionPool() );
        
        return reader;
    }
    
    /**
     * Loads a DTD grammar
     * 
     * @return null
     *      If failed to parse a schema
     */
    public static Grammar loadDTDGrammar( URL url ) throws IOException, SAXException {
        DebugController controller = new DebugController(true);
        DTDReader reader = new DTDReader(controller,new ExpressionPool());
        reader.setDatatypeLibrary(new DatatypeLibraryImpl("http://www.w3.org/2001/XMLSchema-datatypes"));
        DTDParser parser = new DTDParser();
        parser.setDtdHandler(reader);
        parser.setEntityResolver(controller);
        parser.parse(url.toExternalForm());
    
        return reader.getResult();
    }
    
    /**
     * Uses RELAXNGWriter to transform other grammars into RELAX NG.
     * 
     * @return null
     *      If failed to parse a schema
     */
    public static Grammar loadOtherGrammar( URL url ) throws IOException, ParserConfigurationException, SAXException {
        // parse it once
        DebugController controller = new DebugController(true);
        GrammarLoader loader = new GrammarLoader();
        loader.setController(controller);
        loader.setStrictCheck(true);
        Grammar grammar = loader.parse(url.toExternalForm());
        if(grammar==null)   return null;    // failed to parse
        
        // then write it as RELAX NG, and re-parse it as RELAX NG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RELAXNGWriter writer = new RELAXNGWriter();
        // removing dependence on org.apache.xml.serialize.*
        throw new UnsupportedOperationException("loadOtherGrammar");
        /*
        writer.setDocumentHandler(new XMLSerializer( baos,
                new OutputFormat("xml",null,true) ) );
        writer.write(grammar);
        baos.close();
        
        // re-parse it as RELAX NG grammar
        RELAXNGReader reader = createRELAXNGReader();
        reader.parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())));
        
        return reader.getResult();
        */
    }
}
