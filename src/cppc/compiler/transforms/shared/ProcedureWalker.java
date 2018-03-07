package cppc.compiler.transforms.shared;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.Procedure;
import cetus.hir.Program;
import java.util.NoSuchElementException;


//函数便利器，遍历全部函数。
public abstract class ProcedureWalker
{
  private Program program;
  
  protected ProcedureWalker(Program program)
  {
    this.program = program;
  }
  
  protected void start()
  {
    BreadthFirstIterator iter = new BreadthFirstIterator(this.program);
    iter.pruneOn(Procedure.class);
    while (iter.hasNext())
    {
      Procedure procedure = null;
      try
      {
        procedure = (Procedure)iter.next(Procedure.class);
      }
      catch (NoSuchElementException e)
      {
        return;
      }
      walkOverProcedure(procedure);
    }
  }
  
  protected abstract void walkOverProcedure(Procedure paramProcedure);
}
