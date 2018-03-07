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

public class CppcStatementsToStatements
  extends ProcedureWalker
{
  private static String passName = "[CppcStatementsToStatements]";
  
  private CppcStatementsToStatements(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    CppcStatementsToStatements transform = new CppcStatementsToStatements(
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
      if ((statement instanceof CppcStatement))
      {
        CppcStatement cppcStatement = (CppcStatement)statement;
        Statement reference = cppcStatement.getStatement();
        cppcStatement.swapWith(reference);
      }
    }
  }
}
