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
  private final String myname;  // identifier for this program
  RNGZSettings settings =  new RNGZSettings();
  boolean settings_p;     // are any RNGZSettings changed from the defaults?
  boolean stdout_p;       // write to standard out; don't touch files
  boolean debug_p;        // trace compressor; replaces normal output
  boolean decompress_p;   // decompress instead of compress
  boolean compress_p;
  boolean force_p;        // force overwrite of output files
  boolean identify_p;     // print information about .rnz files
  boolean ignore_sum_p;   // decompress even if schema changed
  boolean keep_p;         // do not remove input files
  boolean pretty_p;       // line-break and indent XML output
  int pretty_tab = 2;     //   how far to indent?
  int verbosity = 1;      // 0=errors only, 1=warnings, 2=stats&info
  String suffix = ".rnz"; // use this suffix on compressed files
  String schema;          // use this schema (required to compress)
  boolean timings_p;      // output timing information

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
    int i = opt.getOptind();
    check(i, args.length);
    if( errcount > 0 )
      {
        System.exit(1);
      }
    opt = null;
    return i;
  }

  protected void check(int i, int n)
  {
    if( !decompress_p && !identify_p ) { // We ARE compressing
      compress_p = true;
      if( schema == null ) {
        err.printf("%s: error: schema must be specified when compressing%n",
                   myname);
        errcount++;
      }
    }
    if( stdout_p && n > i+1 ) {
      err.printf("%s: error: specify at most one file "+
                 "at a time when using --stdout (-c)%n", myname);
      errcount++;
    }
    if( verbosity < 1 ) return;
    if( decompress_p || identify_p ) { // We are not compressing
      if( settings_p ) {
        err.printf("%s: warning: not compressing, so "+
                   "-E,-T,-Z will be ignored%n", myname);
      }
    }
    if( !decompress_p ) {       // We are not decompressing
      final String msg = "%s: warning: not decompressing, so ";
      if( ignore_sum_p ) {
        err.printf(msg+"--ignore-checksum is irrelevant%n", myname);
      }
      if( pretty_p ) {
        err.printf(msg+"--pretty-print (-p) is irrelevant%n", myname);
      }
    }
    if( identify_p && !decompress_p ) { // We are identifying ONLY
      final String msg = "%s: warning: in identification mode (-i), ";
      if( schema != null ) {
        err.printf(msg+"--schema (-s) is irrelevant%n", myname);
      }
      if( force_p ) {
        err.printf(msg+"--force (-f) is irrelevant%n", myname);
      }
      if( timings_p ) {
        err.printf(msg+"--timings (-t) is irrelevant%n", myname);
      }
    }
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
    settings_p = true;
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
    settings_p = true;
  }

  protected void handleDataCompressor()
  {
    try {
      settings.setDataCompressor(opt.getOptarg());
    }
    catch(IllegalArgumentException x) {
      enumError(RNGZSettings.DataCompression.values());
    }
    settings_p = true;
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
    out.printf("Usage: %s [options] [file ...]%n", myname);
    copyResource("help.txt", out);
    out.print("Coders: ");
    enumOptions(out, settings.DEFAULT_CODER,
                RNGZSettings.BitCoding.values());
    out.print("Compressors: ");
    enumOptions(out, settings.DEFAULT_COMPRESSOR,
                RNGZSettings.DataCompression.values());
    System.exit(0);
  }

  protected <T> void enumOptions(PrintStream out, T def, T[] vs)
  {
    for(T v : vs) {
      out.printf("%s%s ", v == def? "*" : "",
                 v.toString().toLowerCase());
    }
    out.println();
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
