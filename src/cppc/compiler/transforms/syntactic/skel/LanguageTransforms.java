package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.Program;
import cetus.hir.Tools;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import java.lang.reflect.Method;

public abstract class LanguageTransforms
  extends ProcedureWalker
{
  private static String passName = "[LanguageTransforms]";
  
  protected LanguageTransforms(Program program)
  {
    super(program);
  }
  
  private static final LanguageTransforms getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption("CPPC/Utils/LanguageTransforms/ClassName");
    try
    {
      Class theClass = Class.forName(className);
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod("getTransformInstance", param);
      return (LanguageTransforms)instancer.invoke(null, new Object[] { program });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    return null;
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    LanguageTransforms transform = getTransformInstance(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
}
