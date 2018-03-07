package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.IfStatement;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.Traversable;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AddRestartJumps
  extends ProcedureWalker
{
  private static String passName = "[AddRestartJumps]";
  
  protected AddRestartJumps(Program program)
  {
    super(program);
  }
  
  private static final AddRestartJumps getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption("CPPC/Transforms/AddRestartJumps/ClassName");
    try
    {
      Class theClass = Class.forName(className);
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod("getTransformInstance", param);
      return (AddRestartJumps)instancer.invoke(null, new Object[] { program });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    return null;
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    AddRestartJumps transform = getTransformInstance(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected abstract void addCppcVariables(Procedure paramProcedure, List<CppcLabel> paramList);
  
  protected abstract void addConditionalJump(CppcConditionalJump paramCppcConditionalJump, List<CppcLabel> paramList);
  
  protected void walkOverProcedure(Procedure procedure)
  {
    if (!ObjectAnalizer.isCppcProcedure(procedure)) {
      return;
    }
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    iter.pruneOn(Expression.class);
    try
    {
      while (iter.hasNext())
      {
        CppcConditionalJump jump = (CppcConditionalJump)iter.next(CppcConditionalJump.class);
        if (iter.hasNext())
        {
          Statement stmt = (Statement)iter.next(Statement.class);
          if ((stmt instanceof CppcLabel))
          {
            jump.detach();
            stmt.detach();
          }
        }
      }
    }
    catch (NoSuchElementException e) {}
    iter.reset();
    iter.next();
    while (iter.hasNext()) {
      try
      {
        IfStatement ifStmt = (IfStatement)iter.next(IfStatement.class);
        instrumentIfStatement(ifStmt);
      }
      catch (NoSuchElementException e) {}
    }
    iter.reset();
    iter.next();
    while (iter.hasNext()) {
      try
      {
        Loop loop = (Loop)iter.next(Loop.class);
        instrumentLoop(loop);
      }
      catch (NoSuchElementException e) {}
    }
    iter.reset();
    ArrayList<CppcLabel> orderedLabels = new ArrayList();
    try
    {
      while (iter.hasNext()) {
        orderedLabels.add((CppcLabel)iter.next(CppcLabel.class));
      }
    }
    catch (NoSuchElementException e) {}
    if (orderedLabels.size() == 0) {
      return;
    }
    addCppcVariables(procedure, orderedLabels);
    
    iter.reset();
    try
    {
      while (iter.hasNext())
      {
        CppcConditionalJump jump = (CppcConditionalJump)iter.next(CppcConditionalJump.class);
        
        addConditionalJump(jump, orderedLabels);
      }
    }
    catch (NoSuchElementException e) {}
  }
  
  private void instrumentIfStatement(IfStatement ifStmt)
  {
    Statement thenPart = ifStmt.getThenStatement();
    Statement elsePart = ifStmt.getElseStatement();
    CompoundStatement thenList = null;
    CompoundStatement elseList = null;
    
    int thenLabels = executesInList(thenPart);
    int elseLabels = executesInList(elsePart);
    if ((thenLabels == 0) && (elseLabels == 0)) {
      return;
    }
    if (!(thenPart instanceof CompoundStatement))
    {
      CompoundStatement newThen = new CompoundStatement();
      ifStmt.setThenStatement(newThen);
      newThen.addStatement(thenPart);
      thenList = newThen;
    }
    else
    {
      thenList = (CompoundStatement)thenPart;
    }
    if (elsePart == null)
    {
      elsePart = new CompoundStatement();
      ifStmt.setElseStatement(elsePart);
    }
    if (!(elsePart instanceof CompoundStatement))
    {
      CompoundStatement newElse = new CompoundStatement();
      ifStmt.setElseStatement(newElse);
      newElse.addStatement(elsePart);
      elseList = newElse;
    }
    else
    {
      elseList = (CompoundStatement)elsePart;
    }
    CppcConditionalJump thenJump = new CppcConditionalJump();
    CppcConditionalJump elseJump = new CppcConditionalJump();
    
    Statement firstExecutable = ObjectAnalizer.findFirstExecutable(thenList);
    if (firstExecutable == null) {
      thenList.addStatement(thenJump);
    } else {
      thenList.addStatementBefore(firstExecutable, thenJump);
    }
    firstExecutable = ObjectAnalizer.findFirstExecutable(elseList);
    if (firstExecutable == null) {
      elseList.addStatement(elseJump);
    } else {
      elseList.addStatementBefore(firstExecutable, elseJump);
    }
    if (elseLabels != 0)
    {
      CppcConditionalJump lastJump = findLastJump(thenList);
      lastJump.setLeap(elseLabels + 1);
    }
    if (thenLabels != 0)
    {
      CppcConditionalJump firstJump = findFirstJump(elseList);
      firstJump.setLeap(thenLabels + 1);
    }
  }
  
  private void instrumentLoop(Loop loop)
  {
    BreadthFirstIterator iter = new BreadthFirstIterator(loop.getBody());
    try
    {
      iter.next(CppcConditionalJump.class);
    }
    catch (Exception e)
    {
      return;
    }
    CppcConditionalJump jump = new CppcConditionalJump();
    CompoundStatement statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass((Traversable)loop, CompoundStatement.class);
    statementList.addStatementAfter((Statement)loop, jump);
    
    int loopLabels = executesInList((Statement)loop);
    jump.setLeap(loopLabels + 1);
  }
  
  private int executesInList(Statement statementList)
  {
    if (statementList == null) {
      return 0;
    }
    int currentExecutes = 0;
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    iter.pruneOn(Expression.class);
    while (iter.hasNext()) {
      try
      {
        Statement next = (Statement)iter.next(Statement.class);
        if ((next instanceof CppcLabel))
        {
          currentExecutes++;
        }
        else if ((next instanceof CompoundStatement))
        {
          int insideCount = executesInList((CompoundStatement)next);
          currentExecutes += insideCount;
        }
      }
      catch (NoSuchElementException e) {}
    }
    return currentExecutes;
  }
  
  private CppcConditionalJump findFirstJump(Statement statementList)
  {
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    iter.pruneOn(Statement.class);
    while (iter.hasNext()) {
      try
      {
        return (CppcConditionalJump)iter.next(CppcConditionalJump.class);
      }
      catch (NoSuchElementException e) {}
    }
    return null;
  }
  
  private CppcConditionalJump findLastJump(Statement statementList)
  {
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    iter.pruneOn(Statement.class);
    
    CppcConditionalJump last = null;
    while (iter.hasNext()) {
      try
      {
        last = (CppcConditionalJump)iter.next(CppcConditionalJump.class);
      }
      catch (NoSuchElementException e) {}
    }
    return last;
  }
}
