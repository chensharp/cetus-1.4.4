package cppc.compiler.analysis;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.ContinueStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DoLoop;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.GotoStatement;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.Label;
import cetus.hir.NullStatement;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.hir.WhileLoop;
import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.utils.ConfigurationManager;
import cppc.util.dispatcher.FunctionDispatcher;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class StatementAnalyzer
  extends FunctionDispatcher<Statement>
{
  private static Class instanceClass;
  private static StatementAnalyzer instance;
  protected static CppcStatement currentStatement = null;
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption(
        "CPPC/Analysis/StatementAnalyzer/ClassName"));
      instance = (StatementAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }


    public static void analyzeStatement(final CppcStatement cppcStatement) {
        cppcStatement.statementCount = 1;
        final Method m = StatementAnalyzer.instance.dispatch(cppcStatement.getStatement(), "analyzeStatement");
        if (m == null) {
            System.err.println("WARNING: cppc.compiler.analysis.StatementAnalyzer.analyzeStatement() not implemented for " + cppcStatement.getStatement().getClass());
            return;
        }
        final CppcStatement oldStatement = StatementAnalyzer.currentStatement;
        StatementAnalyzer.currentStatement = cppcStatement;
        try {
            m.invoke(StatementAnalyzer.instance, cppcStatement.getStatement());
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        finally {
            StatementAnalyzer.currentStatement = oldStatement;
        }
        StatementAnalyzer.currentStatement = oldStatement;
    }
  
 
  
  protected void analyzeStatement(CppcLabel label)
  {
    currentStatement.setWeight(0L);
    currentStatement.statementCount = 0;
  }
  
  protected void analyzeStatement(CompoundStatement compoundStatement)
  {
    BreadthFirstIterator iter = new BreadthFirstIterator(compoundStatement);
    iter.next();
    iter.pruneOn(Statement.class);
    
    long totalWeight = 0L;
    int statementCount = 0;
    while (iter.hasNext()) {
      try
      {
        CppcStatement next = (CppcStatement)iter.next(CppcStatement.class);
        analyzeStatement(next);
        totalWeight += next.getWeight();
        statementCount += next.statementCount;
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    currentStatement.setWeight(totalWeight);
    currentStatement.statementCount = statementCount;
  }
  
  protected void analyzeStatement(ContinueStatement stmt)
  {
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcCheckpointPragma pragma)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcCheckpointLoopPragma pragma)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcConditionalJump pragma)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcExecutePragma pragma)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcNonportableFunctionMark s)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  protected void analyzeStatement(CppcShutdownPragma pragma)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
  }
  
  private static void analyzeInitializer(Initializer init)
  {
    List values = init.getChildren();
    if (values != null)
    {
      Iterator children = values.iterator();
      while (children.hasNext())
      {
        Object obj = children.next();
        if ((obj instanceof Expression)) {
          currentStatement.getConsumed().addAll(
            ExpressionAnalyzer.analyzeExpression(currentStatement, 
            (Expression)obj));
        } else {
          analyzeInitializer((Initializer)obj);
        }
      }
    }
  }
  
  protected void analyzeStatement(DeclarationStatement s)
  {
    currentStatement.statementCount = 0;
    currentStatement.setWeight(0L);
    Declaration declaration = s.getDeclaration();
    if (!(declaration instanceof VariableDeclaration)) {
      return;
    }
    VariableDeclaration vd = (VariableDeclaration)declaration;
    for (int i = 0; i < vd.getNumDeclarators(); i++)
    {
      Declarator declarator = vd.getDeclarator(i);
      if ((declarator instanceof VariableDeclarator))
      {
        if (declarator.getInitializer() != null)
        {
          currentStatement.getGenerated().add(
            (Identifier)declarator.getSymbol().clone());
          currentStatement.getPartialGenerated().add(
            (Identifier)declarator.getSymbol().clone());
          analyzeInitializer(declarator.getInitializer());
        }
        List trailingSpecs = 
          ((VariableDeclarator)declarator).getTrailingSpecifiers();
        for (int j = 0; j < trailingSpecs.size(); j++) {
          currentStatement.getConsumed().addAll(
            SpecifierAnalyzer.analyzeSpecifier(currentStatement, 
            (Specifier)trailingSpecs.get(j)));
        }
        currentStatement.getPartialGenerated().add(
          (Identifier)declarator.getSymbol());
      }
    }
  }
  
  protected void analyzeStatement(DoLoop l)
  {
    CppcStatement body = (CppcStatement)l.getBody();
    analyzeStatement(body);
    
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(currentStatement, 
      l.getCondition()));
    
    currentStatement.setWeight(currentStatement.getWeight() + 
      body.getWeight());
    currentStatement.statementCount += body.statementCount;
  }
  
  protected void analyzeStatement(ExpressionStatement s)
  {
    Expression expr = s.getExpression();
    
    currentStatement.getConsumed().addAll(ExpressionAnalyzer.analyzeExpression(
      currentStatement, expr));
  }
  
  protected void analyzeStatement(GotoStatement stmt)
  {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(currentStatement, 
      stmt.getValue()));
  }
  
  protected void analyzeStatement(IfStatement s)
  {
    currentStatement.getConsumed().addAll(ExpressionAnalyzer.analyzeExpression(
      currentStatement, s.getControlExpression()));
    
    long weight = currentStatement.getWeight();
    int statements = 0;
    
    analyzeStatement((CppcStatement)s.getThenStatement());
    
    weight += ((CppcStatement)s.getThenStatement()).getWeight();
    statements = ((CppcStatement)s.getThenStatement()).statementCount;
    if (s.getElseStatement() != null)
    {
      analyzeStatement((CppcStatement)s.getElseStatement());
      
      weight += ((CppcStatement)s.getElseStatement()).getWeight();
      statements += ((CppcStatement)s.getElseStatement()).statementCount;
    }
    currentStatement.setWeight((int)Math.ceil(weight / 2.0D));
    currentStatement.statementCount = ((int)Math.ceil(statements / 2.0D));
  }
  
  protected void analyzeStatement(WhileLoop l)
  {
    currentStatement.getConsumed().addAll(
      ExpressionAnalyzer.analyzeExpression(currentStatement, 
      l.getCondition()));
    
    analyzeStatement((CppcStatement)l.getBody());
    currentStatement.setWeight(((CppcStatement)l.getBody()).getWeight());
    currentStatement.statementCount += 
      ((CppcStatement)l.getBody()).statementCount;
  }
  
  protected void analyzeStatement(Label label)
  {
    currentStatement.setWeight(0L);
    currentStatement.statementCount = 0;
  }
  
  protected void analyzeStatement(NullStatement nullStatement) {}
}
