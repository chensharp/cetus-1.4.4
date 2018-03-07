package cppc.compiler.transforms.semantic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class AddCppcExecutePragmas
  extends ProcedureWalker
{
  private Program program;
  private static final String passName = "[AddCppcExecutePragmas]";
  private static final String CPPC_NONPORTABLE_STATE_CREATOR_ROLE = "CPPC/Nonportable";
  private static final HashSet<Identifier> nonportableFunctions = initHashSet("CPPC/Nonportable");
  
  private static final HashSet<Identifier> initHashSet(String role)
  {
    return CppcRegisterManager.getProceduresWithRole(role);
  }
  
  protected AddCppcExecutePragmas(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[AddCppcExecutePragmas] begin", 1);
    
    AddCppcExecutePragmas transform = new AddCppcExecutePragmas(program);
    transform.start();
    
    Tools.printlnStatus("[AddCppcExecutePragmas] end", 1);
  }
  
  protected boolean parse(String pragmaText)
  {
    throw new IllegalAccessError();
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    while (iter.hasNext()) {
      try
      {
        FunctionCall call = (FunctionCall)iter.next(FunctionCall.class);
        if (nonportableFunctions.contains(call.getName()))
        {
          Statement stmt = (Statement)ObjectAnalizer.getParentOfClass(call, 
            Statement.class);
          
          addNonportableFunctionMark(stmt);
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
  
  private void addNonportableFunctionMark(Statement stmt)
  {
    Statement markStatement = new CppcNonportableFunctionMark();
    
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(stmt, 
      CompoundStatement.class);
    
    statementList.addStatementBefore(stmt, markStatement);
  }
}
