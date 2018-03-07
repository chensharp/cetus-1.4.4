package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.Procedure;
import cetus.hir.Program;

public class LanguageTransforms
  extends cppc.compiler.transforms.syntactic.skel.LanguageTransforms
{
  private LanguageTransforms(Program program)
  {
    super(program);
  }
  
  protected void walkOverProcedure(Procedure procedure) {}
  
  public static final LanguageTransforms getTransformInstance(Program program)
  {
    return new LanguageTransforms(program);
  }
}
