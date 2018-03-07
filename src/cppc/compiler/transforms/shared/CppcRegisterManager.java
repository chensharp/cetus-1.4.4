package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;
import cppc.compiler.utils.ConfigurationManager;
import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

//注册管理器，
public final class CppcRegisterManager
{
  private static HashMap<Identifier, ProcedureCharacterization> procedures = new HashMap();//pc是啥？待研究 ，，函数数组
  
  static
  {
    String indexFileName = ConfigurationManager.getOption("FunctionGrimoire");//不理解该值在何处设置？
    if (indexFileName != null)
    {
      File file = new File(indexFileName);
      if (file == null) {
        System.err.println("WARNING: cannot access file " + indexFileName + " for reading");
      } else {
        CppcRegisterParser.parse(file);//处理文件
      }
    }
  }
  
  //添加一个
  public static final boolean addProcedure(Identifier procedure, ProcedureCharacterization characterization)
  {
    if (procedures.containsKey(procedure)) {
      return false;
    }
    procedures.put(procedure, characterization);
    
    return true;
  }
  
  //检查是否已经注册过了
  public static final boolean isRegistered(Identifier procedure)
  {
    return procedures.containsKey(procedure);
  }
  
  //通过p 查找pc
  public static final ProcedureCharacterization getCharacterization(Identifier procedure)
  {
    if (!isRegistered(procedure)) {
      return null;
    }
    return (ProcedureCharacterization)procedures.get(procedure);
  }
  
  //通过role找到一组名字返回，
  public static final HashSet<Identifier> getProceduresWithRole(String role)
  {
    HashSet<Identifier> procs = new HashSet();
    
    Iterator<ProcedureCharacterization> i = procedures.values().iterator();
    while (i.hasNext())
    {
      ProcedureCharacterization c = (ProcedureCharacterization)i.next();
      
      if (c.hasSemantic(role)) {
        procs.add(c.getName());
      }
    }
    return procs;
  }
}
