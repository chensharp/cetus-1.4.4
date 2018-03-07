package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CheckCheckpointedProcedures
  extends ProcedureWalker
{
  private static String passName = "[CheckCheckpointedProcedures]";
  private ArrayList<Procedure> notCheckpointed;
  
  private CheckCheckpointedProcedures(Program program)
  {
    super(program);
    this.notCheckpointed = new ArrayList();
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    CheckCheckpointedProcedures transform = new CheckCheckpointedProcedures(
      program);
    transform.start();
    transform.addTransitivity();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    BreadthFirstIterator iter = new BreadthFirstIterator(procedure);
    iter.pruneOn(Expression.class);
    while (iter.hasNext()) {
      try
      {
        CppcCheckpointPragma pragma = (CppcCheckpointPragma)
          iter.next(CppcCheckpointPragma.class);
        setCheckpointed(procedure);
        return;
      }
      catch (NoSuchElementException e)
      {
        this.notCheckpointed.add(procedure);
      }
    }
  }
  
  private void setCheckpointed(Procedure procedure)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)procedure.getName());
    if (c == null)
    {
      System.err.println("BUG: THIS PROCEDURE IS NOT REGISTERED(" + 
        procedure.getName() + ")");
      System.exit(0);
    }
    c.setCheckpointed(true);
  }


  private void addTransitivity() {
    boolean modified = true;
    final ArrayList<Procedure> removeFromList = new ArrayList<Procedure>(this.notCheckpointed.size());
    while (this.notCheckpointed.size() != 0 && modified) {
      modified = false;
      for (final Procedure procedure : this.notCheckpointed) {
        boolean isCheckpointed = false;
        final DepthFirstIterator procIter = new DepthFirstIterator((Traversable)procedure);
        procIter.pruneOn((Class)Expression.class);
        while (procIter.hasNext() && !isCheckpointed) {
          try {
            final FunctionCall call = (FunctionCall)procIter.next((Class)FunctionCall.class);
            final Identifier fName = (Identifier)call.getName();
            if (!CppcRegisterManager.isRegistered(fName)) {
              continue;
            }
            final ProcedureCharacterization calledChar = CppcRegisterManager.getCharacterization(fName);
            if (!calledChar.getCheckpointed()) {
              continue;
            }
            this.setCheckpointed(procedure);
            removeFromList.add(procedure);
            modified = true;
            isCheckpointed = true;
          }
          catch (NoSuchElementException ex) {}
        }
      }
      for (final Procedure checkpointed : removeFromList) {
        this.notCheckpointed.remove(checkpointed);
      }
      removeFromList.clear();
    }
  }

  /*
  private void addTransitivity()
  {
    boolean modified = true;
    ArrayList<Procedure> removeFromList = new ArrayList(
      this.notCheckpointed.size());
    while ((this.notCheckpointed.size() != 0) && (modified))
    {
      modified = false;
      boolean isCheckpointed;
      DepthFirstIterator procIter;
      for (Iterator localIterator = this.notCheckpointed.iterator(); localIterator.hasNext(); (procIter.hasNext()) && (!isCheckpointed) )
      {
        Procedure procedure = (Procedure)localIterator.next();
        
        isCheckpointed = false;
        
        procIter = new DepthFirstIterator(procedure);
        procIter.pruneOn(Expression.class);
        continue;
        try
        {
          FunctionCall call = (FunctionCall)procIter.next(
            FunctionCall.class);
          Identifier fName = (Identifier)call.getName();
          if (CppcRegisterManager.isRegistered(fName))
          {
            ProcedureCharacterization calledChar = 
              CppcRegisterManager.getCharacterization(fName);
            if (calledChar.getCheckpointed())
            {
              setCheckpointed(procedure);
              
              removeFromList.add(procedure);
              modified = true;
              isCheckpointed = true;
            }
          }
        }
        catch (NoSuchElementException localNoSuchElementException) {}
      }
      for (Procedure checkpointed : removeFromList) {
        this.notCheckpointed.remove(checkpointed);
      }
      removeFromList.clear();
    }
  }*/


}
