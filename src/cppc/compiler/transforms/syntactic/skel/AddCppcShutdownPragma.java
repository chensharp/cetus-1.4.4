package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.ReturnStatement;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.utils.ObjectAnalizer;

public class AddCppcShutdownPragma
{
  private Program program;
  private static final String passName = "[AddCppcShutdownPragma]";
  
  public AddCppcShutdownPragma(Program program)
  {
    this.program = program;
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[AddCppcShutdownPragma] begin", 1);
    
    AddCppcShutdownPragma transform = new AddCppcShutdownPragma(program);
    transform.start();
    
    Tools.printlnStatus("[AddCppcShutdownPragma] end", 1);
  }
  
  private void start()
  {
    Procedure mainProc = ObjectAnalizer.findMainProcedure(this.program);
    
    Statement lastStmt = ObjectAnalizer.findLastStatement(mainProc);
    
    addCppcShutdownPragma(lastStmt);
  }
  
  private void addCppcShutdownPragma(Statement ref)
  {
    CppcShutdownPragma pragma = new CppcShutdownPragma();
    
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(ref.getParent(), 
      CompoundStatement.class);
    if ((ref instanceof ReturnStatement)) {
      statementList.addStatementBefore(ref, pragma);
    } else {
      statementList.addStatementAfter(ref, pragma);
    }
  }
}
