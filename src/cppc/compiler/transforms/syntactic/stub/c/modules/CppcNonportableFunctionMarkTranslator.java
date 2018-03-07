package cppc.compiler.transforms.syntactic.stub.c.modules;

import cetus.hir.Declarator;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.VariableDeclaration;
import java.util.List;

public class CppcNonportableFunctionMarkTranslator
  extends cppc.compiler.transforms.syntactic.skel.modules.CppcNonportableFunctionMarkTranslator
{
  protected void furtherModifyRegisterForCallImage(FunctionCall call, VariableDeclaration vd)
  {
    call.addArgument(getIsStatic(vd.getDeclarator(0)));
  }
  
  private Identifier getIsStatic(Declarator declarator)
  {
    if (declarator.getSpecifiers().size() == 0) {
      return new Identifier("CPPC_STATIC");
    }
    return new Identifier("CPPC_DYNAMIC");
  }
}
