package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

public class CppcShutdownTranslator
  extends TranslationModule<CppcShutdownPragma>
{
  public void translate(CppcShutdownPragma pragma)
  {
    Identifier cppcShutdownIdentifier = new Identifier(
      GlobalNamesFactory.getGlobalNames().SHUTDOWN_FUNCTION());
    FunctionCall functionCall = new FunctionCall(cppcShutdownIdentifier);
    
    ExpressionStatement functionCallStatement = new ExpressionStatement(
      functionCall);
    pragma.swapWith(functionCallStatement);
  }
  
  public Class getTargetClass()
  {
    return CppcShutdownPragma.class;
  }
}
