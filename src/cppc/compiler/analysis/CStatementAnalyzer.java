package cppc.compiler.analysis;

import cetus.hir.BreakStatement;
import cetus.hir.Case;
import cetus.hir.Default;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.ReturnStatement;
import cetus.hir.SwitchStatement;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.SetOperations;
import java.util.List;
import java.util.Set;

public class CStatementAnalyzer
  extends StatementAnalyzer
{
  protected void analyzeStatement(BreakStatement s)
  {
    currentStatement.setWeight(0L);
    currentStatement.statementCount = 0;
  }
  
  protected void analyzeStatement(Case stmt)
  {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(currentStatement, 
      stmt.getExpression()));
  }
  
  protected void analyzeStatement(Default stmt) {}
  
  protected void analyzeStatement(ForLoop forLoop)
  {
    CppcStatement initializer = (CppcStatement)forLoop.getInitialStatement();
    analyzeStatement(initializer);
    
    Set<Identifier> consumedByInit = initializer.getConsumed();
    
    Set<Identifier> consumed = currentStatement.getConsumed();
    consumed.addAll(consumedByInit);
    
    Set<Identifier> generated = currentStatement.getGenerated();
    
    SetOperations<Identifier> setOps = new SetOperations();
    
    ExpressionStatement expressionStatement = new ExpressionStatement(
      (Expression)forLoop.getCondition().clone());
    CppcStatement conditionStatement = new CppcStatement(
      expressionStatement);
    conditionStatement.setParent(forLoop);
    analyzeStatement(conditionStatement);
    
    currentStatement.getConsumed().addAll(setOps.setMinus(
      conditionStatement.getConsumed(), currentStatement.getGenerated()));
    
    currentStatement.getGenerated().addAll(
      conditionStatement.getGenerated());
    currentStatement.getPartialGenerated().addAll(
      conditionStatement.getPartialGenerated());
    
    ExpressionStatement stepExpression = new ExpressionStatement(
      (Expression)forLoop.getStep().clone());
    CppcStatement stepStatement = new CppcStatement(stepExpression);
    stepStatement.setParent(forLoop);
    analyzeStatement(stepStatement);
    
    currentStatement.getConsumed().addAll(setOps.setMinus(
      stepStatement.getConsumed(), currentStatement.getGenerated()));
    
    currentStatement.getGenerated().addAll(stepStatement.getGenerated());
    currentStatement.getPartialGenerated().addAll(
      stepStatement.getPartialGenerated());
    
    analyzeStatement((CppcStatement)forLoop.getBody());
    currentStatement.setWeight(
      ((CppcStatement)forLoop.getBody()).getWeight());
    currentStatement.statementCount += 
      ((CppcStatement)forLoop.getBody()).statementCount;
  }
  
  protected void analyzeStatement(ReturnStatement returnStatement)
  {
    if (returnStatement.getExpression() != null) {
      currentStatement.getConsumed().addAll(
        ExpressionAnalyzer.analyzeExpression(
        currentStatement, returnStatement.getExpression()));
    }
  }
  
  protected void analyzeStatement(SwitchStatement stmt)
  {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(currentStatement, 
      stmt.getExpression()));
    
    analyzeStatement((CppcStatement)stmt.getChildren().get(1));
    currentStatement.setWeight(
      ((CppcStatement)stmt.getChildren().get(1)).getWeight());
    currentStatement.statementCount = 
      ((CppcStatement)stmt.getChildren().get(1)).statementCount;
  }
}
