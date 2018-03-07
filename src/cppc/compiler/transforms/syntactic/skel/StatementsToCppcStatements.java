package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.ProcedureWalker;
import java.util.NoSuchElementException;

public class StatementsToCppcStatements
  extends ProcedureWalker
{
  private static String passName = "[StatementsToCppcStatements]";
  
  private StatementsToCppcStatements(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    StatementsToCppcStatements transform = new StatementsToCppcStatements(
      program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator procIter = new DepthFirstIterator(statementList);
    procIter.next();
    while (procIter.hasNext())
    {
      Statement statement = null;
      try
      {
        statement = (Statement)procIter.next(Statement.class);
      }
      catch (NoSuchElementException e)
      {
        return;
      }
      if (!(statement instanceof CppcStatement))
      {
        CppcStatement cppcStatement = new CppcStatement(statement);
        statement.swapWith(cppcStatement);
        statement.setParent(cppcStatement);
      }
    }
  }
}
