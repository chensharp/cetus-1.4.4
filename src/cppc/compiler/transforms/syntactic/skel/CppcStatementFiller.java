package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;
import cppc.compiler.analysis.ProcedureAnalyzer;
import cppc.compiler.transforms.shared.ProcedureWalker;

public class CppcStatementFiller
  extends ProcedureWalker
{
  private static final String passName = "[CppcStatementFiller]";
  
  private CppcStatementFiller(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[CppcStatementFiller] begin", 2);
    
    CppcStatementFiller transform = new CppcStatementFiller(program);
    transform.start();
    
    Tools.printlnStatus("[CppcStatementFiller] end", 2);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    ProcedureAnalyzer.analyzeProcedure(procedure);
  }
}
