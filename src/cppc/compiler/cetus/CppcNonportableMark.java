package cppc.compiler.cetus;

import cetus.hir.Expression;

public class CppcNonportableMark extends CppcPragmaStatement
{
  private Expression expr;
  
  public CppcNonportableMark(Expression expr)
  {
    this.expr = expr;
  }
  
  public Expression getExpression()
  {
    return this.expr;
  }
}
