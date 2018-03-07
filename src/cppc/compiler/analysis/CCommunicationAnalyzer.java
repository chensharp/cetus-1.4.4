package cppc.compiler.analysis;

import cetus.hir.ForLoop;
import cetus.hir.SwitchStatement;
import cppc.compiler.cetus.CppcStatement;
import java.util.List;

public class CCommunicationAnalyzer
  extends CommunicationAnalyzer
{
  protected void analyzeStatement(ForLoop forLoop)
  {
    analyzeStatement(forLoop);
  }
  
  protected void analyzeStatement(SwitchStatement stmt)
  {
    analyzeStatement((CppcStatement)stmt.getChildren().get(1));
  }
}
