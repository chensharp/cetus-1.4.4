package cppc.compiler.transforms.syntactic.stub.c.modules;

import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import java.util.List;

public class CppcUnregisterTranslator
  extends cppc.compiler.transforms.syntactic.skel.modules.CppcUnregisterTranslator
{
  protected Expression getReference(Declarator declarator)
  {
    if ((declarator.getSpecifiers().size() == 0) && 
      (declarator.getArraySpecifiers().size() == 0)) {
      return new UnaryExpression(UnaryOperator.ADDRESS_OF, 
        (Identifier)declarator.getSymbol().clone());
    }
    return (Identifier)declarator.getSymbol().clone();
  }
}
