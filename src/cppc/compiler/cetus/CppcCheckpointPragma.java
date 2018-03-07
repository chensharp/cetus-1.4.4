package cppc.compiler.cetus;

import java.io.OutputStream;
import java.io.PrintStream;


public class CppcCheckpointPragma extends CppcPragmaStatement
{
  public CppcCheckpointPragma()
  {
    registerPrintMethod();
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcCheckpointPragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcCheckpointPragma.class.getMethod("classPrintMethod", 
        params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcCheckpointPragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public static void classPrintMethod(CppcCheckpointPragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    p.print("#pragma cppc checkpoint");
  }
}
