package cppc.compiler.cetus;

import java.io.OutputStream;
import java.io.PrintStream;

public class CppcConditionalJump  extends CppcPragmaStatement
{
  private int leap;
  
  public CppcConditionalJump()
  {
    registerPrintMethod();
    
    this.leap = 1;
  }
  
  public int getLeap()
  {
    return this.leap;
  }
  
  public void setLeap(int leap)
  {
    this.leap = leap;
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcConditionalJump.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcConditionalJump.class.getMethod("classPrintMethod", 
        params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcConditionalJump.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public static void classPrintMethod(CppcConditionalJump statement, OutputStream stream) {}
}
