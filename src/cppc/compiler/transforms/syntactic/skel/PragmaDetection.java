package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.syntactic.skel.modules.TranslationModule;
import cppc.compiler.utils.ConfigurationManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.NoSuchElementException;

public abstract class PragmaDetection
  extends ProcedureWalker
{
  private static String passName = "[PragmaDetection]";
  private static HashMap<Class<? extends CppcPragmaStatement>, TranslationModule<? extends CppcPragmaStatement>> translators = new HashMap();
  
  protected PragmaDetection(Program program)
  {
    super(program);
    registerAllModules();
  }
  
  protected abstract void registerAllModules();
  
  private static final PragmaDetection getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption(
      "CPPC/Transforms/PragmaDetection/ClassName");
    try
    {
      Class theClass = Class.forName(className);
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod("getTransformInstance", param);
      return (PragmaDetection)instancer.invoke(null, new Object[] {
        program });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    return null;
  }
  
  protected static void registerModule(TranslationModule<? extends CppcPragmaStatement> module)
  {
    translators.put(module.getTargetClass(), module);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    PragmaDetection transform = getTransformInstance(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    iter.pruneOn(Expression.class);
    while (iter.hasNext()) {
      try
      {
        CppcPragmaStatement pragma = (CppcPragmaStatement)iter.next(
          CppcPragmaStatement.class);
        TranslationModule<? extends CppcPragmaStatement> module = 
          (TranslationModule)translators.get(pragma.getClass());
        if (module != null) {
          try
          {
            module.getMethod().invoke(module, new Object[] { pragma });
          }
          catch (Exception e)
          {
            throw new InternalError(e.getMessage());
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
}
