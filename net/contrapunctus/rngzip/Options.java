package net.contrapunctus.rngzip;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import net.contrapunctus.rngzip.io.RNGZSettings;

public class Options
{
  private static final String shortopts = "cDdE:fhikp::qS:s:T:tVvZ:";

  private static LongOpt[] longopts = new LongOpt[] {
    new LongOpt("stdout",          LongOpt.NO_ARGUMENT,       null, 'c'),
    new LongOpt("debug",           LongOpt.NO_ARGUMENT,       null, 'D'),
    new LongOpt("decompress",      LongOpt.NO_ARGUMENT,       null, 'd'),
    new LongOpt("tree-encoder",    LongOpt.REQUIRED_ARGUMENT, null, 'E'),
    new LongOpt("force",           LongOpt.NO_ARGUMENT,       null, 'f'),
    new LongOpt("help",            LongOpt.NO_ARGUMENT,       null, 'h'),
    new LongOpt("identify",        LongOpt.NO_ARGUMENT,       null, 'i'),
    new LongOpt("ignore-checksum", LongOpt.NO_ARGUMENT,       null,  2 ),
    new LongOpt("keep",            LongOpt.NO_ARGUMENT,       null, 'k'),
    new LongOpt("pretty-print",    LongOpt.OPTIONAL_ARGUMENT, null, 'p'),
    new LongOpt("quiet",           LongOpt.NO_ARGUMENT,       null, 'q'),
    new LongOpt("suffix",          LongOpt.REQUIRED_ARGUMENT, null, 'S'),
    new LongOpt("schema",          LongOpt.REQUIRED_ARGUMENT, null, 's'),
    new LongOpt("tree-compressor", LongOpt.REQUIRED_ARGUMENT, null, 'T'),
    new LongOpt("timings",         LongOpt.NO_ARGUMENT,       null, 't'),
    new LongOpt("version",         LongOpt.NO_ARGUMENT,       null, 'V'),
    new LongOpt("exact-version",   LongOpt.NO_ARGUMENT,       null,  3 ),
    new LongOpt("verbose",         LongOpt.NO_ARGUMENT,       null, 'v'),
    new LongOpt("data-compressor", LongOpt.REQUIRED_ARGUMENT, null, 'Z')
  };

  private Getopt opt;
  private int curopt, errcount;
  private static PrintStream err = System.err;
  private final String myname;    // identifier for this program
  private RNGZSettings settings   // records tree-encoder, tree-compressor,
    = new RNGZSettings();         //   and data-compressor settings
  private boolean stdout_p;       // write to standard out; don't touch files
  private boolean debug_p;        // trace compressor; replaces normal output
  private boolean decompress_p;   // decompress instead of compress
  private boolean force_p;        // force overwrite of output files
  private boolean identify_p;     // print information about .rnz files
  private boolean ignore_sum_p;   // decompress even if schema changed
  private boolean keep_p;         // do not remove input files
  private boolean pretty_p;       // line-break and indent XML output
  private int pretty_tab = 2;     //   how far to indent?
  private int verbosity = 1;      // 0=errors only, 1=warnings, 2=stats&info
  private String suffix = ".rnz"; // use this suffix on compressed files
  private String schema;          // use this schema (required to compress)
  private boolean timings_p;      // output timing information

  public Options (String myname)
  {
    this.myname = myname;
  }

  public int process (String[] args)
  {
    opt = new Getopt(myname, args, shortopts, longopts);
    curopt = opt.getopt();
    while( curopt != -1 ) 
      {
        handleCurrentOpt();
        curopt = opt.getopt();
      }
    return opt.getOptind();
  }

  protected void handleCurrentOpt()
  {
    switch( curopt ) 
      {
      case '?': errcount++;               break;
      case 'c': stdout_p = true;          break;
      case 'D': debug_p = true;           break;
      case 'd': decompress_p = true;      break;
      case 'E': handleTreeEncoder();      break;
      case 'f': force_p = true;           break;
      case 'h': showHelp(System.out);     break;
      case 'i': identify_p = true;        break;
      case  2 : ignore_sum_p = true;      break;
      case 'k': keep_p = true;            break;
      case 'p': handlePretty();           break;
      case 'q': verbosity--;              break;
      case 'S': suffix = opt.getOptarg(); break;
      case 's': schema = opt.getOptarg(); break;
      case 'T': handleTreeCompressor();   break;
      case 't': timings_p = true;         break;
      case 'V': showVersion(System.out);  break;
      case  3 : showContext(System.out);  break;
      case 'v': verbosity++;              break;
      case 'Z': handleDataCompressor();   break;
      default: assert false : curopt;
      }
  }

  void handleTreeEncoder()
  {
    try {
      settings.setBitCoder(opt.getOptarg());
    }
    catch(IllegalArgumentException x) {
      enumError(RNGZSettings.BitCoding.values());
    }
  }

  protected <T> void enumError(T[] vs) 
  {
    reportInvalid();
    err.print(" requires one of: ");
    for(T v : vs) {
      err.print(v.toString().toLowerCase());
      err.print(' ');
    }
    err.println();
  }
  
  protected void reportInvalid()
  {
    errcount++;
    err.printf("%s: invalid parameter: --%s",
               myname, longOptOf(curopt));
    if( Character.isLetterOrDigit(curopt) ) 
      {
        err.printf(" (-%c)", curopt);
      }
  } 

  protected void invalid(String why)
  {
    reportInvalid();
    err.printf(": %s%n", why);
  }

  protected String longOptOf (int o)
  {
    for( LongOpt lo : longopts ) 
      {
        if( lo.getVal() == o )
          {
            return lo.getName();
          }
      }
    return null;
  }

  protected void handleTreeCompressor()
  {
    try {
      settings.setTreeCompressor(opt.getOptarg());
    }
    catch(IllegalArgumentException x) {
      enumError(RNGZSettings.DataCompression.values());
    }
  }

  protected void handleDataCompressor()
  {
    try {
      settings.setDataCompressor(opt.getOptarg());
    }
    catch(IllegalArgumentException x) {
      enumError(RNGZSettings.DataCompression.values());
    }
  }

  protected void handlePretty()
  {
    pretty_p = true;
    if(opt.getOptarg() != null) {
      try {
        pretty_tab = Integer.parseInt(opt.getOptarg());
      }
      catch(NumberFormatException x) {
        invalid("requires an integer");
      }
    }
  }

  protected void showHelp(PrintStream out)
  {
    out.printf("usage: %s [options] [file ...]%n", myname);
    copyResource("help.txt", out);
    System.exit(0);
  }

  protected static void copyResource (String name, OutputStream sink)
  {
    InputStream src = Options.class.getResourceAsStream(name);
    if( src == null ) {
      err.printf("error: %s is missing!%n", name);
    }
    else try {
      copyStream(src, sink);
    }
    catch(IOException x) {
      err.printf("error printing %s: %s%n", name, x);
    }
  }

  private static void copyStream (InputStream source, OutputStream sink)
    throws IOException
  {
    int k, SIZE = 128;
    byte[] buf = new byte [SIZE];
    while ((k = source.read (buf, 0, SIZE)) != -1) {
      sink.write (buf, 0, k);
    }
    source.close();
  }

  protected void showVersion(PrintStream out)
  {
    copyResource("version.txt", out);
    System.exit(0);
  }

  protected void showContext(PrintStream out)
  {
    copyResource("context.txt", out);
    System.exit(0);
  }

}
