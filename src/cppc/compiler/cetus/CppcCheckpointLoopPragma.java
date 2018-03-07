package cppc.compiler.cetus;

import java.io.OutputStream;
import java.io.PrintStream;

public class CppcCheckpointLoopPragma
  extends CppcPragmaStatement
{
  public CppcCheckpointLoopPragma()
  {
    registerPrintMethod();
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcCheckpointLoopPragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcCheckpointLoopPragma.class.getMethod(
        "classPrintMethod", params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcCheckpointLoopPragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public static void classPrintMethod(CppcCheckpointLoopPragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    p.print("#pragma cppc checkpoint loop");
  }
}
