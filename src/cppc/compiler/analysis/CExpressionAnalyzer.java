package cppc.compiler.analysis;

import cetus.hir.AccessExpression;
import cetus.hir.ConditionalExpression;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.SizeofExpression;
import cetus.hir.Typecast;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CExpressionAnalyzer
  extends ExpressionAnalyzer
{
  protected Set<Identifier> analyzeExpression(AccessExpression expr)
  {
    return ExpressionAnalyzer.analyzeExpression(currentStatement, 
      expr.getLHS());
  }
  
  protected Set<Identifier> analyzeExpression(ConditionalExpression expr)
  {
    Set<Identifier> resultSet = analyzeExpression(currentStatement, 
      expr.getCondition());
    resultSet.addAll(analyzeExpression(currentStatement, 
      expr.getTrueExpression()));
    resultSet.addAll(analyzeExpression(currentStatement, 
      expr.getFalseExpression()));
    
    return resultSet;
  }
  
  protected Set<Identifier> analyzeExpression(SizeofExpression sizeofExpression)
  {
    if (sizeofExpression.getExpression() != null) {
      return ExpressionAnalyzer.analyzeExpression(currentStatement, 
        sizeofExpression.getExpression());
    }
    return new HashSet(0);
  }
  
  protected Set<Identifier> analyzeExpression(Typecast typecast)
  {
    return analyzeExpression(currentStatement, 
      (Expression)typecast.getChildren().get(0));
  }
}
