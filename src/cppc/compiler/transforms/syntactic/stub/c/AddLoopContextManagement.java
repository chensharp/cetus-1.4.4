package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.AssignmentExpression;
import cetus.hir.ExpressionStatement;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.Program;
import cetus.hir.Statement;
import cppc.compiler.utils.ObjectAnalizer;

public class AddLoopContextManagement
  extends cppc.compiler.transforms.syntactic.skel.AddLoopContextManagement
{
  private AddLoopContextManagement(Program program)
  {
    super(program);
  }
  
  public static final AddLoopContextManagement getTransformInstance(Program program)
  {
    return new AddLoopContextManagement(program);
  }
  
  protected Statement testInsideLoop(Statement ref)
  {
    ForLoop loop = (ForLoop)ObjectAnalizer.getParentOfClass(ref, 
      ForLoop.class);
    
    return loop;
  }
  
  protected Identifier getLoopIndex(Statement loop)
  {
    ForLoop forLoop = (ForLoop)loop;
    
    Statement initStmt = forLoop.getInitialStatement();
    
    AssignmentExpression assignment = (AssignmentExpression)
      ((ExpressionStatement)initStmt).getExpression();
    Identifier lhs = (Identifier)assignment.getLHS();
    
    return lhs;
  }
}
