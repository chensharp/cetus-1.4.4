package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.Identifier;
import cetus.hir.Label;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.ReturnStatement;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import java.util.List;

public class AddExitLabels
  extends ProcedureWalker
{
  private static String passName = "[AddExitLabels]";
  
  private AddExitLabels(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    AddExitLabels transform = new AddExitLabels(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    if (ObjectAnalizer.isMainProcedure(procedure)) {
      return;
    }
    if (!ObjectAnalizer.isCppcProcedure(procedure)) {
      return;
    }
    CompoundStatement statementList = procedure.getBody();
    
    Label enterLabel = new Label(new Identifier(
      GlobalNamesFactory.getGlobalNames().ENTER_FUNCTION_LABEL()));
    Statement ref = ObjectAnalizer.findLastDeclaration(procedure);
    if (ref == null)
    {
      procedure.getBody().getChildren().add(0, enterLabel);
      enterLabel.setParent(procedure.getBody());
    }
    else
    {
      procedure.getBody().addStatementAfter(ref, enterLabel);
    }
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter(enterLabel, jump);
    
    CppcLabel exitLabel = new CppcLabel(new Identifier(
      GlobalNamesFactory.getGlobalNames().EXIT_FUNCTION_LABEL()));
    
    Statement last = ObjectAnalizer.findLastStatement(procedure);
    if ((last instanceof ReturnStatement))
    {
      statementList.addStatementBefore(last, exitLabel);
    }
    else
    {
      statementList.addStatement(exitLabel);
      statementList.addStatement(new ReturnStatement());
    }
  }
}
