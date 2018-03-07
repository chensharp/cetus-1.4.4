package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CheckPragmedProcedures
  extends ProcedureWalker
{
  private static String passName = "[CheckPragmedProcedures]";
  private ArrayList<Procedure> notPragmed;
  
  private CheckPragmedProcedures(Program program)
  {
    super(program);
    this.notPragmed = new ArrayList();
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    CheckPragmedProcedures transform = new CheckPragmedProcedures(program);
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
        Statement stmt = (Statement)iter.next(Statement.class);
        if ((stmt instanceof CppcLabel))
        {
          setPragmed(procedure);
          return;
        }
        if ((stmt instanceof CppcPragmaStatement))
        {
          setPragmed(procedure);
          return;
        }
      }
      catch (NoSuchElementException e)
      {
        this.notPragmed.add(procedure);
      }
    }
  }
  
  private void setPragmed(Procedure procedure)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)procedure.getName());
    if (c == null) {
      throw new InternalError("Procedure " + procedure.getName() + 
        " not registered");
    }
    if (!c.getPragmed())
    {
      c.setPragmed(true);
      LanguageAnalyzerFactory.getLanguageAnalyzer().checkIncludes(procedure);
    }
  }
  
  /*
  private void addTransitivity()
  {
    boolean modified = true;
    ArrayList<Procedure> removeFromList = new ArrayList(
      this.notPragmed.size());
    while ((this.notPragmed.size() != 0) && (modified))
    {
      modified = false;
      boolean isPragmed;
      DepthFirstIterator procIter;
      for (Iterator localIterator = this.notPragmed.iterator(); localIterator.hasNext(); (procIter.hasNext()) && (!isPragmed))
      {
        Procedure procedure = (Procedure)localIterator.next();
        
        isPragmed = false;
        
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
            if (calledChar.getPragmed())
            {
              setPragmed(procedure);
              
              removeFromList.add(procedure);
              modified = true;
              isPragmed = true;
            }
          }
        }
        catch (NoSuchElementException localNoSuchElementException) {}
      }
      for (Procedure pragmed : removeFromList) {
        this.notPragmed.remove(pragmed);
      }
      removeFromList.clear();
    }
  }*/

  private void addTransitivity() {
    boolean modified = true;
    final ArrayList<Procedure> removeFromList = new ArrayList<Procedure>(this.notPragmed.size());
    while (this.notPragmed.size() != 0 && modified) {
      modified = false;
      for (final Procedure procedure : this.notPragmed) {
        boolean isPragmed = false;
        final DepthFirstIterator procIter = new DepthFirstIterator((Traversable)procedure);
        procIter.pruneOn((Class)Expression.class);
        while (procIter.hasNext() && !isPragmed) {
          try {
            final FunctionCall call = (FunctionCall)procIter.next((Class)FunctionCall.class);
            final Identifier fName = (Identifier)call.getName();
            if (!CppcRegisterManager.isRegistered(fName)) {
              continue;
            }
            final ProcedureCharacterization calledChar = CppcRegisterManager.getCharacterization(fName);
            if (!calledChar.getPragmed()) {
              continue;
            }
            this.setPragmed(procedure);
            removeFromList.add(procedure);
            modified = true;
            isPragmed = true;
          }
          catch (NoSuchElementException ex) {}
        }
      }
      for (final Procedure pragmed : removeFromList) {
        this.notPragmed.remove(pragmed);
      }
      removeFromList.clear();
    }
  }


}
