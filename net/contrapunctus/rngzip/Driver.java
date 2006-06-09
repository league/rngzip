package net.contrapunctus.rngzip;

import java.util.LinkedList;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import net.contrapunctus.rngzip.io.InteractiveInput;
import net.contrapunctus.rngzip.io.RNGZInputInterface;
import net.contrapunctus.rngzip.io.RNGZInputStream;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;
import net.contrapunctus.rngzip.io.RNGZOutputStream;
import net.contrapunctus.rngzip.io.RNGZSettings;
import net.contrapunctus.rngzip.io.VerboseOutput;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.ErrorReporter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import sax.Writer;

public class Driver
{
   public static void main(String[] args) throws Exception
   {
      new Driver("rngzip").run(args);
   }

   protected Driver(String name)
   {
      myname = name;
   }

   protected boolean requireSchema()
   {
      return true;
   }

   protected Compressor makeCompressor
      (BaliAutomaton au,
       ErrorReporter er,
       RNGZOutputInterface ro)
   {
      return new GenericCompressor(au, er, ro);
   }
   
   protected Decompressor makeDecompressor
      (BaliAutomaton au,
       RNGZInputInterface ri,
       ContentHandler ch)
      throws IOException, SAXException
   {
      return new GenericDecompressor(au, ri, ch);
   }

   private final String myname;
   private boolean stdout_p;     // write to standard output
   private boolean decompress_p; // decompress instead of compress
   private boolean debug_p;      // trace compressor; replaces normal output
   private boolean force_p;      // force overwrite of output files
   private boolean keep_p;       // do not remove input files
   private boolean pretty_p;     // line-break and indent decompressed output
   private boolean timings_p;    // output timing information
   private int pretty_indent = 2;
   private int verbosity = 1;    // 0=errors only, 1=warnings, 2=stats&info

   private String suffix = ".rnz";
   private RNGZSettings settings = new RNGZSettings();
   private String schema;
   private BaliAutomaton automaton;

   private Getopt opt;
   private int curopt, errcount = 0;
   private static PrintStream err = System.err;

   private void run(String[] args) throws Exception
   {
      int i = processOptions(args);
      if(requireSchema())
        {
          if(i < args.length)
            {
               long start = 0, elapsed;
               schema = args[i++];
               if(timings_p) 
                  {
                     info("building automaton:       ");
                     start = System.currentTimeMillis();
                  }
              automaton = BaliAutomaton.fromRNG(new File(schema));
              if(timings_p)
                 {
                    elapsed = System.currentTimeMillis() - start;
                    info("%5dms%n", elapsed);
                 }
            }
          else fatal("schema must be specified");
        }
      if(i < args.length)
        {
           for( ; i < args.length; i++) frobFile(args[i]);
        }
      else
        {
          if(decompress_p) decompress(System.in, System.out);
          else compress(new InputSource(System.in), System.out);
        }
   }

   private int processOptions(String[] args) throws IOException
   {
      opt = new Getopt(myname, args, shortopts, longopts);
      for(curopt = opt.getopt();  curopt != -1;  curopt = opt.getopt()) {
         switch(curopt) {
         case '?': errcount++; break;
         case 'c': stdout_p = true; break;
         case 'D': debug_p = true; break;
         case 'd': decompress_p = true; break;
         case 'E': treeEncoder(); break;
         case 'f': force_p = true; break;
         case 'h': showHelp(System.out); System.exit(0); break;
         case 'k': keep_p = true; break;
         case 'p': prettyPrint(); break;
         case 'q': verbosity--; break;
         case 'S': suffix = opt.getOptarg(); break; 
         case 'T': treeCompressor(); break;
         case 't': timings_p = true; break;
         case 'V': showVersion(System.out); System.exit(0); break;
         case 'v': verbosity++; break;
         case 'Z': dataCompressor(); break;
         default: assert false : curopt;
         }
      }
      return opt.getOptind();
   }

   private static final String shortopts = "cDdE:fhkp::qS:T:tVvZ:";

   private static LongOpt[] longopts = new LongOpt[] {
      new LongOpt("stdout",          LongOpt.NO_ARGUMENT,       null, 'c'),
      new LongOpt("debug",           LongOpt.NO_ARGUMENT,       null, 'D'),
      new LongOpt("decompress",      LongOpt.NO_ARGUMENT,       null, 'd'),
      new LongOpt("tree-encoder",    LongOpt.REQUIRED_ARGUMENT, null, 'E'),
      new LongOpt("force",           LongOpt.NO_ARGUMENT,       null, 'f'),
      new LongOpt("help",            LongOpt.NO_ARGUMENT,       null, 'h'),
      new LongOpt("keep",            LongOpt.NO_ARGUMENT,       null, 'k'),
      new LongOpt("pretty-print",    LongOpt.OPTIONAL_ARGUMENT, null, 'p'),
      new LongOpt("quiet",           LongOpt.NO_ARGUMENT,       null, 'q'),
      new LongOpt("suffix",          LongOpt.REQUIRED_ARGUMENT, null, 'S'),
      new LongOpt("tree-compressor", LongOpt.REQUIRED_ARGUMENT, null, 'T'),
      new LongOpt("timings",         LongOpt.NO_ARGUMENT,       null, 't'),
      new LongOpt("version",         LongOpt.NO_ARGUMENT,       null, 'V'),
      new LongOpt("verbose",         LongOpt.NO_ARGUMENT,       null, 'v'),
      new LongOpt("data-compressor", LongOpt.REQUIRED_ARGUMENT, null, 'Z')
   };

   private void showHelp(PrintStream ps) throws IOException
   {
      ps.printf("usage: %s [options] %s[file ...]%n", myname,
                requireSchema()? "schema.rng " : "");
      InputStream help = Driver.class.getResourceAsStream("help.txt");
      assert help != null;
      copy(help, ps);
   }

   private static void copy (InputStream source, OutputStream sink)
      throws IOException
   {
      int k, SIZE = 128;
      byte[] buf = new byte [SIZE];
      while ((k = source.read (buf, 0, SIZE)) != -1) {
         sink.write (buf, 0, k);
      }
      source.close();
   }

   private void showVersion(PrintStream ps)
   {
      ps.printf("%s 0.1%n", myname);
   }

   private void prettyPrint()
   {
      pretty_p = true;
      if(opt.getOptarg() != null) {
         try {
            pretty_indent = Integer.parseInt(opt.getOptarg());
         }
         catch(NumberFormatException x) {
            invalid("requires an integer");
            errcount++;
         }
      }
   }
   
   private void treeEncoder()
   {
      try {
         settings.setBitCoder(opt.getOptarg());
      }
      catch(IllegalArgumentException x) {
         enumError(RNGZSettings.BitCoding.values());
      }
   }
   
   private void treeCompressor()
   {
      try {
         settings.setTreeCompressor(opt.getOptarg());
      }
      catch(IllegalArgumentException x) {
         enumError(RNGZSettings.DataCompression.values());
      }
   }
   
   private void dataCompressor()
   {
      try {
         settings.setDataCompressor(opt.getOptarg());
      }
      catch(IllegalArgumentException x) {
         enumError(RNGZSettings.DataCompression.values());
      }
   }
   
   private String longOptOf(int o)
   {
      for(LongOpt lo : longopts) {
         if(lo.getVal() == o) {
            return lo.getName();
         }
      }
      return null;
   }

   private void reportInvalid()
   {
      System.err.printf("%s: invalid parameter: --%s (-%c) ",
                        myname, longOptOf(curopt), curopt);
   }
  
  private void invalid(String why)
  {
    System.err.printf("%s: invalid argument to --%s (-%c): %s",
                      myname, longOptOf(curopt), curopt, why);
  }
   
   private int intOption()
   {
      try {
         return Integer.parseInt(opt.getOptarg());
      }
      catch(NumberFormatException x) {
         reportInvalid();
         System.err.println("requires an integer");
         errcount++;
         return -1;
      }
   }

   private <T> void enumError(T[] vs) 
   {
      reportInvalid();
      System.err.print("requires one of: ");
      for(T v : vs) {
         System.err.print(v.toString().toLowerCase());
         System.err.print(' ');
      }
      System.err.println();
      errcount++;
   }

   private void warn(String fmt, Object... args)
   {
      if(verbosity > 0) {
         System.err.printf("%s: warning: ", myname);
         System.err.printf(fmt, args);
         System.err.println();
      }
   }

   private void error(String fmt, Object... args)
   {
      if(verbosity >= 0) {
         System.err.printf("%s: error: ", myname);
         System.err.printf(fmt, args);
         System.err.println();
      }
      errcount++;
   }

   private void fatal(String fmt, Object... args)
   {
      error(fmt, args);
      System.exit(1);
   }

   private void info(String fmt, Object... args)
   {
      if(verbosity > 1) {
         System.err.printf(fmt, args);
      }
   }

   private void compress(InputSource in, OutputStream out)
      throws IOException, MalformedURLException, SAXException
   {
      RNGZOutputInterface rnz = debug_p?
         new VerboseOutput(System.err) :
        new RNGZOutputStream(out, settings, automaton);
      ErrorReporter err = new ErrorReporter();
      GenericCompressor gc = new GenericCompressor(automaton, err, rnz);
      XMLReader xr = XMLReaderFactory.createXMLReader();
      xr.setFeature("http://xml.org/sax/features/validation", false);
      xr.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      xr.setContentHandler(gc);
      xr.setErrorHandler(err);
      xr.parse(in);
      rnz.close();
   }

   private void frobFile(String name)
      throws IOException, MalformedURLException, SAXException
   {
      // set up input streams
      File infile = new File(name);
      InputSource insource = null;      // for compression
      InputStream instream = null;      // for decompression
      if(decompress_p) instream = new FileInputStream(infile);
      else insource = new InputSource(name);
      // if going to standard output, the rest is easy:
      if(stdout_p) {
         if(decompress_p) decompress(instream, System.out);
         else compress(insource, System.out);
         return;
      }
      long insize = infile.length(); // for size stats
      // determine new filename, by adding or peeling off the .rnz suffix
      String outname;
      if(decompress_p) {
         if(suffix.length() > 0 && name.endsWith(suffix)) { 
            outname = name.substring(0, name.length() - suffix.length());
         }
         else {
            outname = name + ".xml";
         }
      }
      else {
         outname = name+suffix;
      }
      // open output file
      File outfile = new File(outname);
      try {
         if(!outfile.createNewFile() && !force_p) {
            error("%s already exists; use --force (-f) to overwrite.",
                  outname);
            return;
         }
      }
      catch(IOException x) {
         error("cannot create %s: %s", outname, x.getMessage());
         return;
      }
      info("%-25s ", name);
      long start = 0, elapsed;  // for timing stats
      if(timings_p) {
        start = System.currentTimeMillis();
      }
      FileOutputStream outstream = new FileOutputStream(outfile);
      if(decompress_p) {
         decompress(instream, outstream);
         instream.close();
      }
      else {
         compress(insource, outstream);
      }
      outstream.close();
      if(timings_p) {           // output timing stats
        elapsed = System.currentTimeMillis() - start;
        info("%5dms, ", elapsed);
      }
      long outsize = outfile.length(); // output size stats
      float ratio;
      if(decompress_p) {
         ratio = (outsize - insize) / (float)outsize * 100;
      }
      else {
         ratio = (insize - outsize) / (float)insize * 100;
      }
      boolean del_p = false;    // did we delete the file?
      if(!keep_p) {
         del_p = infile.delete();
      }
      info("%.2f%% -- %s %s%n", 
           ratio, 
           del_p? "replaced with" : "created", 
           outname);
      if(!keep_p && !del_p) {
         error("could not remove %s", name);
      }
   }

   private void decompress(InputStream in, OutputStream out)
      throws IOException, SAXException
   {
      RNGZInputInterface zin;
      if( debug_p ) {
         zin = new InteractiveInput(in, System.err);
      }
      else {
         RNGZInputStream zis = new RNGZInputStream(in, settings);
         automaton = zis.readSchema(automaton);
         zin = zis;
      }
      Writer wr = pretty_p?
         null :
         new Writer();
      wr.setOutput(out, null);
      new GenericDecompressor(automaton, zin, wr);
      out.write('\n');
   }

}
