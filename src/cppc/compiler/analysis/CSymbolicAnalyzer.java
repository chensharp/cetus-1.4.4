package cppc.compiler.analysis;

import cetus.hir.BreakStatement;
import cetus.hir.Case;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.ReturnStatement;
import cetus.hir.SwitchStatement;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.MultiExpression;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CSymbolicAnalyzer
  extends SymbolicAnalyzer
{
  protected void analyzeStatement(BreakStatement s) {}
  
  protected void analyzeStatement(Case stmt)
  {
    SymbolicExpressionAnalyzer.analyzeExpression(stmt.getExpression(), 
      knownSymbols);
  }
  
  private Identifier getForLoopVar(ForLoop forLoop)
  {
    CppcStatement cppcStatement = (CppcStatement)forLoop.getParent();
    Set<Identifier> generated = cppcStatement.getGenerated();
    if (generated.size() == 1) {
      return (Identifier)generated.iterator().next();
    }
    return null;
  }
  
  protected void enterLoop(ForLoop forLoop)
  {
    analyzeStatement((CppcStatement)forLoop.getInitialStatement(), 
      knownSymbols);
    
    removeLoopCarriedSymbols(forLoop);
    
    Identifier loopVar = getForLoopVar(forLoop);
    if (loopVar != null)
    {
      Map<Identifier, List> localKnown = new HashMap(
        knownSymbols);
      analyzeStatement(
        (CppcStatement)forLoop.getInitialStatement(), localKnown);
      if (localKnown.containsKey(loopVar))
      {
        List values = (List)localKnown.get(loopVar);
        if (values.size() == 1)
        {
          Expression startValue = (Expression)values.get(0);
          if ((startValue instanceof IntegerLiteral))
          {
            List<Expression> iterationValues = new ArrayList();
            
            Expression currentValue = startValue;
            Expression truthValue = 
              SymbolicExpressionAnalyzer.analyzeExpression(
              forLoop.getCondition(), localKnown);
            Expression booleanTrue = 
              SymbolicExpressionAnalyzer.instance.buildBooleanLiteral(true);
            while (truthValue.equals(booleanTrue))
            {
              iterationValues.add((Expression)currentValue.clone());
              
              SymbolicExpressionAnalyzer.analyzeExpression(forLoop.getStep(), 
                localKnown);
              values = (List)localKnown.get(loopVar);
              currentValue = (Expression)values.get(0);
              
              truthValue = SymbolicExpressionAnalyzer.analyzeExpression(
                forLoop.getCondition(), localKnown);
            }
            MultiExpression mexpr = new MultiExpression(loopVar);
            for (Expression iter : iterationValues) {
              mexpr.addExpression(iter, iter);
            }
            if (!mexpr.getChildren().isEmpty())
            {
              values = new ArrayList(1);
              values.add(mexpr);
              addKnownSymbol(knownSymbols, loopVar, values, forLoop);
            }
          }
        }
      }
    }
    else
    {
      SymbolicExpressionAnalyzer.analyzeExpression(forLoop.getCondition(), 
        knownSymbols);
      
      SymbolicExpressionAnalyzer.analyzeExpression(forLoop.getStep(), 
        knownSymbols);
    }
  }
  
  protected void exitLoop(ForLoop forLoop)
  {
    Identifier loopVar = getForLoopVar(forLoop);
    if (knownSymbols.containsKey(loopVar))
    {
      List<Expression> values = (List)knownSymbols.get(loopVar);
      if (values.size() == 1)
      {
        Expression value = (Expression)values.get(0);
        if ((value instanceof MultiExpression))
        {
          Expression lastChild = (Expression)value.getChildren().get(
            value.getChildren().size() - 1);
          values.clear();
          values.add(lastChild);
        }
      }
      else
      {
        knownSymbols.remove(loopVar);
      }
    }
  }
  
  protected void analyzeStatement(ForLoop forLoop) {}
  
  protected void analyzeStatement(ReturnStatement returnStatement)
  {
    SymbolicExpressionAnalyzer.analyzeExpression(
      returnStatement.getExpression(), knownSymbols);
  }
  
  protected void analyzeStatement(SwitchStatement stmt)
  {
    SymbolicExpressionAnalyzer.analyzeExpression(stmt.getExpression(), 
      knownSymbols);
    
    analyzeStatement((CppcStatement)stmt.getChildren().get(1), 
      knownSymbols);
  }
}
