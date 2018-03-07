package cppc.compiler.cetus;

import java.io.OutputStream;
import java.io.PrintStream;

public class CppcShutdownPragma
  extends CppcPragmaStatement
{
  public CppcShutdownPragma()
  {
    registerPrintMethod();
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcShutdownPragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcShutdownPragma.class.getMethod("classPrintMethod", 
        params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcShutdownPragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public static void classPrintMethod(CppcShutdownPragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    
    p.print("#pragma cppc shutdown");
  }
}
