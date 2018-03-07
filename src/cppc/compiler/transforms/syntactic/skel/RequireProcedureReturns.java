package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.ReturnStatement;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.transforms.shared.ProcedureWalker;
import java.util.List;

public class RequireProcedureReturns
  extends ProcedureWalker
{
  private static String passName = "[RequireProcedureReturns]";
  
  private RequireProcedureReturns(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    RequireProcedureReturns transform = new RequireProcedureReturns(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    CompoundStatement statementList = procedure.getBody();
    List children = statementList.getChildren();
    Statement lastStatement = (Statement)children.get(children.size() - 1);
    if ((lastStatement instanceof ReturnStatement)) {
      return;
    }
    List returnType = procedure.getReturnType();
    if (returnType.contains(Specifier.VOID)) {
      return;
    }
    ReturnStatement returnStatement = new ReturnStatement(new IntegerLiteral(0L));
    statementList.addStatementAfter(lastStatement, returnStatement);
  }
}
