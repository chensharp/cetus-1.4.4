package cppc.compiler.cetus;

import cetus.hir.Identifier;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CppcUnregisterPragma
  extends CppcPragmaStatement
{
  private List<Identifier> unregisters;
  
  public CppcUnregisterPragma()
  {
    this.unregisters = new ArrayList();
    registerPrintMethod();
  }
  
  public CppcUnregisterPragma(List<Identifier> unregisters)
  {
    this.unregisters = unregisters;
    registerPrintMethod();
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcUnregisterPragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcUnregisterPragma.class.getMethod(
        "classPrintMethod", params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcUnregisterPragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public List<Identifier> getUnregisters()
  {
    return this.unregisters;
  }
  
  public void addUnregister(Identifier id)
  {
    if (!this.unregisters.contains(id)) {
      this.unregisters.add(id);
    }
  }
  
  public static void classPrintMethod(CppcUnregisterPragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    
    p.print("#pragma ");
    for (int i = 0; 
          i < GlobalNamesFactory.getGlobalNames().UNREGISTER_PRAGMA().length; i++) {
      p.print(GlobalNamesFactory.getGlobalNames().UNREGISTER_PRAGMA()[i] + 
        " ");
    }
    p.print("( ");
    
    Iterator<Identifier> iter = statement.unregisters.iterator();
    while (iter.hasNext())
    {
      p.print(iter.next());
      if (iter.hasNext()) {
        p.print(", ");
      }
    }
    p.print(" )");
  }
}
