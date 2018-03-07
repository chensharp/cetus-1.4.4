package cppc.compiler.cetus;

import cetus.hir.Statement;
import java.io.OutputStream;
import java.io.PrintStream;

public class CppcExecutePragma extends CppcPragmaStatement
{
  private Statement begin;
  private Statement end;
  
  public CppcExecutePragma(Statement begin, Statement end)
  {
    registerPrintMethod();
    
    this.begin = begin;
    this.end = end;
  }
  
  public Statement getBegin()
  {
    return this.begin;
  }
  
  public Statement getEnd()
  {
    return this.end;
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcExecutePragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcExecutePragma.class.getMethod("classPrintMethod", 
        params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcExecutePragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public static void classPrintMethod(CppcExecutePragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    
    p.print("#pragma cppc execute");
  }
}
