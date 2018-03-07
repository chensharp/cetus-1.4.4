package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.TranslationUnit;
import cppc.compiler.cetus.CppcPragmaStatement;
import java.io.PrintStream;
import java.lang.reflect.Method;

public abstract class TranslationModule<T extends CppcPragmaStatement>
{
  private Method translationMethod = null;
  
  public static void printErrorInTranslation(PrintStream stream, Statement s, String message)
  {
    TranslationUnit tunit = (TranslationUnit)s.getProcedure().getParent();
    stream.println(tunit.getInputFilename() + ": " + s.where() + ": " + 
      message);
  }
  
  public abstract void translate(T paramT);
  
  public abstract Class<T> getTargetClass();
  
  public Method getMethod()
  {
    if (this.translationMethod == null) {
      try
      {
        this.translationMethod = getClass().getMethod("translate", new Class[] {
          getTargetClass() });
      }
      catch (NoSuchMethodException e)
      {
        throw new InternalError("No such method: " + e.getMessage());
      }
    }
    return this.translationMethod;
  }
}
