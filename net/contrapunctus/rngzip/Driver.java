package net.contrapunctus.rngzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import net.contrapunctus.rngzip.io.InteractiveInput;
import net.contrapunctus.rngzip.io.RNGZInputInterface;
import net.contrapunctus.rngzip.io.RNGZInputStream;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;
import net.contrapunctus.rngzip.io.RNGZOutputStream;
import net.contrapunctus.rngzip.io.RNGZSettings;
import net.contrapunctus.rngzip.io.VerboseOutput;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.ErrorReporter;
import net.contrapunctus.rngzip.util.SchemaFormatException;
import net.contrapunctus.rngzip.util.SimpleXMLWriter;
import net.contrapunctus.rngzip.util.PrettyXMLWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Driver
{
   public static void main(String[] args) throws Exception
   {
      new Driver().run(args);
   }

   private static final String myname = "rngzip";
   Options opt = new Options(myname);
   private BaliAutomaton automaton;
   private long checksum;
   private static PrintStream err = System.err;

   void run(String[] args) throws Exception
   {
      int first = opt.process(args);
      if( opt.identify_p ) {
         for( int i = first; i < args.length; i++) {
            identify(args[i]);
         }
      }
      if( opt.compress_p || opt.decompress_p ) {
         if( opt.schema != null ) {
            loadAutomaton(opt.schema);
         }
         Task task = opt.compress_p? new Zip() : new Unzip();
         if( first < args.length ) {
            for( int i = first;  i < args.length;  i++ ) {
               applyToFile(task, new File(args[i]));
            }
         }
         else {
            applyToStdIO(task);
         }
      }
   }

   private HashMap<URL, BaliAutomaton> autoMap = 
      new HashMap<URL, BaliAutomaton>();

   private void loadAutomaton (URL url) throws SchemaFormatException
   {
      automaton = autoMap.get(url);
      if( automaton == null ) {
         info("loading %s%n", url);
         info("building automaton... ");
         long start = 0;
         if( opt.timings_p ) { start = System.currentTimeMillis(); }
         automaton = BaliAutomaton.fromRNG(url);
         checksum = automaton.checksum();
         if( opt.timings_p ) {
            long elapsed = System.currentTimeMillis() - start;
            err.printf("%5d%s", elapsed, 
                       opt.verbosity > 1? "ms\n" : ",");
         }
         else {
            info("done%n");
         }
         autoMap.put(url, automaton);
      }
   }

   private void loadAutomaton (File file) 
      throws FileNotFoundException, SchemaFormatException
   {
      if(!file.exists()) 
         throw new FileNotFoundException(file.toString());
      URL url = null;
      try { url = file.toURI().toURL(); }
      catch(MalformedURLException x) { assert false : x; }
      loadAutomaton(url);
   }

   private void loadAutomaton (String spec)
      throws FileNotFoundException, SchemaFormatException
   {
      try {
         loadAutomaton(new URL(spec));
      }
      catch(MalformedURLException x) {
         loadAutomaton(new File(spec));
      }
   }

   private void identify(String name)
   {
      RNGZInputStream zin = null;
      long len = -1;
      try {
         File file = new File(name);
         len = file.length();
         FileInputStream in = new FileInputStream(file);
         zin = new RNGZInputStream(in, opt.settings);
      }
      catch(FileNotFoundException x) {
         System.out.println(x.getMessage());
      }
      catch(IOException x) {
         System.out.printf("%s (%s)%n", name, x.getMessage());
      }
      if( zin != null ) {
         System.out.printf("%s: %d bytes %s", name, len, opt.settings);
         URL url = zin.getSchemaURL();
         if( opt.verbosity > 1 && url != null ) {
            System.out.printf(" %s(%016x)%n", url, zin.getSchemaSum());
         }
         else {
            System.out.println();
         }
         try { zin.close(); }
         catch(IOException x) { }
      }
   }

   private void error(String fmt, Object... args)
   {
      if(opt.verbosity >= 0) {
         System.err.printf("%s: error: ", myname);
         System.err.printf(fmt, args);
         System.err.println();
      }
   }

   private void info(String fmt, Object... args)
   {
      if(opt.verbosity > 1) {
         System.err.printf(fmt, args);
      }
   }

   private void applyToStdIO (Task task)
      throws IOException, SAXException
   {
      task.setInput(System.in);
      task.setOutput(System.out);
      task.prepare();
      task.run();
   }

   private void applyToFile (Task task, File infile)
      throws IOException, SAXException
   {
      task.setInput(infile);
      if( opt.stdout_p ) {
         task.setOutput(System.out);
         task.prepare();
         task.run();
         if( opt.timings_p ) {
           err.printf("%5d%s", task.getTime(),
                      opt.verbosity > 1? "ms\n" : ",");
         }
         return;
      }
      File outfile = task.getOutput();
      try {
         if(!outfile.createNewFile() && !opt.force_p) {
            error("%s already exists; use --force (-f) to overwrite.",
                  outfile);
            return;
         }
      }
      catch(IOException x) {
         error("cannot create %s: %s", outfile, x.getMessage());
         return;
      }
      OutputStream outstream = new FileOutputStream(outfile);
      task.setOutput(outstream);
      task.prepare();
      info("%-25s ", infile);
      task.run();
      task.closeInput();
      outstream.close();
      if( opt.timings_p ) {
        err.printf("%5d%s", task.getTime(),
                   opt.verbosity > 1? "ms\n" : ",");
      }
      float ratio = task.computeRatio();
      boolean del_p = false;    // did we delete the input file?
      if( !opt.keep_p ) {
         del_p = infile.delete();
      }
      info("%.2f%% -- %s %s%n",
           ratio, del_p? "replaced with" : "created", outfile);
      if(!opt.keep_p && !del_p) {
         error("could not remove %s", infile);
      }
   }

   private abstract class Task {
      protected File infile;
      protected File outfile;
      protected OutputStream outstream;
      private long elapsed;
      abstract void setInput (File file) throws FileNotFoundException;
      abstract void setInput (InputStream in);
      abstract File getOutput ();
      abstract void execute() throws IOException, SAXException;
      abstract float computeRatio (long insize, long outsize);
      void setOutput (OutputStream out) {
         outstream = out;
      }
      float computeRatio() {
         assert infile != null && outfile != null;
         long insize = infile.length();
         long outsize = outfile.length();
         return computeRatio (insize, outsize);
      }
      void prepare() throws IOException { }
      void run() throws IOException, SAXException {
         long start = System.currentTimeMillis();
         execute();
         elapsed = System.currentTimeMillis() - start;
      }
      void closeInput() throws IOException { }
      long getTime() { return elapsed; }
   }  // end class Task

   private class Zip extends Task {
      private InputSource insource;
      void setInput (File file) {
         infile = file;
         insource = new InputSource(file.getPath());
      }
      void setInput (InputStream in) {
         insource = new InputSource(in);
      }
      File getOutput() {
         assert infile != null;
         String name = infile.getPath() + opt.suffix;
         outfile = new File (name);
         return outfile;
      }
      float computeRatio (long insize, long outsize) {
         return (insize - outsize) / (float)insize * 100;
      }
      void execute() throws IOException, SAXException {
         assert insource != null && outstream != null;
         RNGZOutputInterface rnz = opt.debug_p?
            new VerboseOutput(System.err) :
            new RNGZOutputStream(outstream, opt.settings, automaton);
         ErrorReporter err = new ErrorReporter();
         GenericCompressor gc = new GenericCompressor(automaton, err, rnz);
         XMLReader xr = XMLReaderFactory.createXMLReader();
         xr.setFeature("http://xml.org/sax/features/validation", false);
         xr.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
         xr.setContentHandler(gc);
         xr.setErrorHandler(err);
         xr.parse(insource);
         rnz.close();
      }
   }  // end class Zip

   private class Unzip extends Task {
      private InputStream instream;
      private RNGZInputInterface zin;
      private ContentHandler ch;
      void setInput (File file) throws FileNotFoundException {
         infile = file;
         instream = new FileInputStream(infile);
      }
      void setInput (InputStream in) {
         instream = in;
      }
      File getOutput() {
         assert infile != null;
         String name = infile.getPath();
         if( opt.suffix.length() > 0 && name.endsWith(opt.suffix) ) {
            name = name.substring(0, name.length() - opt.suffix.length());
         }
         else {
            name += ".xml";
         }
         outfile = new File (name);
         return outfile;
      }
      float computeRatio (long insize, long outsize) {
         return (outsize - insize) / (float)outsize * 100;
      }
      void prepare() throws IOException {
         assert instream != null && outstream != null;
         if( opt.debug_p ) {
            zin = new InteractiveInput(instream, System.err);
         }
         else {
            RNGZInputStream zis = new RNGZInputStream(instream, opt.settings);
            if( zis.getSchemaURL() != null ) {
               // input stream has schema ref embedded
               if( opt.schema == null ) {
                  loadAutomaton(zis.getSchemaURL());
               }
               if( zis.getSchemaSum() != checksum ) {
                  error("MISMATCH %08X <> %08X", zis.getSchemaSum(), checksum);
                  error("Schema was %s", zis.getSchemaURL());
                  throw new IOException("SUMS DO NOT MATCH"); // FIX
               }
            }
            else if( opt.schema == null ) {
               throw new IOException("NO SCHEMA SPECIFIED"); // FIX
            }
            zin = zis;
         }
         ch = (opt.pretty_p?
               new PrettyXMLWriter(outstream, opt.pretty_tab) :
               new SimpleXMLWriter(outstream));
      }
      void execute() throws IOException, SAXException {
         assert zin != null && ch != null;
         new GenericDecompressor(automaton, zin, ch);
         outstream.write('\n');
      }
      void closeInput() throws IOException {
         instream.close();
      }
   }  // end class Unzip
}
