package cppc.compiler.analysis;

import cetus.hir.Identifier;
import cetus.hir.Specifier;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.ConfigurationManager;
import cppc.util.dispatcher.FunctionDispatcher;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public abstract class SpecifierAnalyzer
  extends FunctionDispatcher<Specifier>
{
  private static Class instanceClass;
  private static SpecifierAnalyzer instance;
  protected static CppcStatement currentStatement = null;
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption(
        "CPPC/Analysis/SpecifierAnalyzer/ClassName"));
      instance = (SpecifierAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static Set<Identifier> analyzeSpecifier(CppcStatement s, Specifier spec)
  {
    Method m = instance.dispatch(spec, "analyzeSpecifier");
    if (m == null)
    {
      System.err.println("WARNING: cppc.compiler.analysis.ExpressionAnalyzer.analyzeSpecifier not implemented for " + 
      
        spec.getClass());
      
      return createEmptySet();
    }
    CppcStatement oldStatement = currentStatement;
    currentStatement = s;
    try
    {
      return (Set)m.invoke(instance, new Object[] { spec });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    finally
    {
      currentStatement = oldStatement;
    }
    return createEmptySet();
  }
  
  protected static Set<Identifier> createEmptySet()
  {
    return new HashSet(0);
  }
}
