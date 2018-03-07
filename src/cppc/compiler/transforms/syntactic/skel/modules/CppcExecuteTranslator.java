package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.CompoundStatement;
import cetus.hir.Identifier;
import cetus.hir.Statement;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

public class CppcExecuteTranslator
  extends TranslationModule<CppcExecutePragma>
{
  public Class getTargetClass()
  {
    return CppcExecutePragma.class;
  }
  
  public void translate(CppcExecutePragma pragma)
  {
    CppcLabel jumpLabel = new CppcLabel(new Identifier(
      GlobalNamesFactory.getGlobalNames().EXECUTE_LABEL()));
    Statement begin = pragma.getBegin();
    CompoundStatement beginList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass(pragma, CompoundStatement.class);
    beginList.addStatementBefore(begin, jumpLabel);
    
    CppcConditionalJump jump = new CppcConditionalJump();
    Statement end = pragma.getEnd();
    CompoundStatement endList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass(end, CompoundStatement.class);
    endList.addStatementAfter(end, jump);
    
    pragma.detach();
  }
}
