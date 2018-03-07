package cppc.compiler.cetus;

import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CppcRegisterPragma
  extends CppcPragmaStatement
{
  private List<CppcRegister> registers;
  
  public CppcRegisterPragma()
  {
    this.registers = new ArrayList();
    registerPrintMethod();
  }
  
  public CppcRegisterPragma(List<CppcRegister> registers)
  {
    this.registers = registers;
    registerPrintMethod();
  }
  
  private void registerPrintMethod()
  {
    Class[] params = { CppcRegisterPragma.class, OutputStream.class };
    try
    {
      setPrintMethod(CppcRegisterPragma.class.getMethod("classPrintMethod", 
        params));
    }
    catch (NoSuchMethodException e)
    {
      System.err.println("BUG: NoSuchMethodException raised in " + 
        CppcRegisterPragma.class + ". Method: registerPrintMethod()");
      System.exit(0);
    }
  }
  
  public List<CppcRegister> getRegisters()
  {
    return this.registers;
  }
  
  public void addRegister(CppcRegister register)
  {
    if (!this.registers.contains(register)) {
      this.registers.add(register);
    }
  }
  
  public static void classPrintMethod(CppcRegisterPragma statement, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    
    p.print("#pragma ");
    for (int i = 0; 
          i < GlobalNamesFactory.getGlobalNames().REGISTER_PRAGMA().length; i++) {
      p.print(GlobalNamesFactory.getGlobalNames().REGISTER_PRAGMA()[i] + " ");
    }
    p.print("( ");
    
    Iterator<CppcRegister> iter = statement.registers.iterator();
    while (iter.hasNext())
    {
      CppcRegister register = (CppcRegister)iter.next();
      register.print(stream);
      if (iter.hasNext()) {
        p.print(", ");
      }
    }
    p.print(" )");
  }
}
