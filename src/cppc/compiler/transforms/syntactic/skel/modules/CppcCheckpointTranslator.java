package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.CompoundStatement;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

public class CppcCheckpointTranslator
  extends TranslationModule<CppcCheckpointPragma>
{
  public void translate(CppcCheckpointPragma pragma)
  {
    CppcLabel jumpLabel = new CppcLabel(new Identifier(
      GlobalNamesFactory.getGlobalNames().CHECKPOINT_LABEL()));
    pragma.swapWith(jumpLabel);
    
    FunctionCall cppcCheckpointCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().CHECKPOINT_FUNCTION()));
    cppcCheckpointCall.addArgument(new IntegerLiteral(
      GlobalNamesFactory.getGlobalNames().CURRENT_CHKPT_CODE()));
    ExpressionStatement cppcCheckpointCallStatement = new ExpressionStatement(
      cppcCheckpointCall);
    
    CompoundStatement statementList = (CompoundStatement)jumpLabel.getParent();
    statementList.addStatementAfter(jumpLabel, cppcCheckpointCallStatement);
    
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter(cppcCheckpointCallStatement, jump);
  }
  
  public Class getTargetClass()
  {
    return CppcCheckpointPragma.class;
  }
}
