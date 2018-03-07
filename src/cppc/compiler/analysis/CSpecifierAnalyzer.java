package cppc.compiler.analysis;

import cetus.hir.ArraySpecifier;
import cetus.hir.Identifier;
import java.util.Set;

public class CSpecifierAnalyzer
  extends SpecifierAnalyzer
{
  protected Set<Identifier> analyzeSpecifier(ArraySpecifier spec)
  {
    Set<Identifier> ret = ExpressionAnalyzer.analyzeExpression(
      currentStatement, spec.getDimension(0));
    for (int i = 1; i < spec.getNumDimensions(); i++) {
      ret.addAll(ExpressionAnalyzer.analyzeExpression(currentStatement, 
        spec.getDimension(i)));
    }
    return ret;
  }
}
