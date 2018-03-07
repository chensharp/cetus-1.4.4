package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.TranslationUnit;
import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class AddCheckpointPragmas extends ProcedureWalker
{
  private static final String passName = "[AddCheckpointPragmas]";
  private long programSize;
  private int programStatements;
  
  //内部类
  private final class LoopNode
  {
    private CppcStatement loop;
    private Set<Identifier> calledProcedures;
    private Set<Identifier> callerProcedures;
    private boolean checkpoint;
    private boolean manualCheckpoint;
    private LoopNode parent;
    private List<LoopNode> children;
    
    LoopNode(Set<Identifier> loop, Set<Identifier> calledProcedures, boolean callerProcedures)
    {
      this.loop = loop;
      this.calledProcedures = calledProcedures;
      this.callerProcedures = callerProcedures;
      this.checkpoint = false;
      this.manualCheckpoint = manualCheckpoint;
      this.parent = null;
      this.children = new ArrayList();
    }
    
    CppcStatement getLoop()
    {
      return this.loop;
    }
    
    Set<Identifier> getCalledProcedures()
    {
      return this.calledProcedures;
    }
    
    void setCalledProcedures(Set<Identifier> calledProcedures)
    {
      this.calledProcedures = calledProcedures;
    }
    
    Set<Identifier> getCallerProcedures()
    {
      return this.callerProcedures;
    }
    
    boolean getCheckpoint()
    {
      return this.checkpoint;
    }
    
    void setCheckpoint(boolean checkpoint)
    {
      this.checkpoint = checkpoint;
    }
    
    boolean getManualCheckpoint()
    {
      return this.manualCheckpoint;
    }
    
    void setManualCheckpoint(boolean manualCheckpoint)
    {
      this.manualCheckpoint = manualCheckpoint;
    }
    
    double costFunction()
    {
      double sFactor = this.loop.statementCount / 
        AddCheckpointPragmas.this.programStatements;
      double wFactor = this.loop.getWeight() / AddCheckpointPragmas.this.programSize;
      if (wFactor == 0.0D) {
        return -1.0D;
      }
      return -Math.log10(sFactor * wFactor);
    }
    
    LoopNode getParent()
    {
      return this.parent;
    }
    
    void setParent(LoopNode p)
    {
      this.parent = p;
    }
    
    List<LoopNode> getChildren()
    {
      return this.children;
    }
    
    void add(LoopNode n)
    {
      n.setParent(this);
      for (int i = 0; i < this.children.size(); i++) {
        if (((LoopNode)this.children.get(i)).costFunction() > n.costFunction())
        {
          this.children.add(i, n);
          return;
        }
      }
      this.children.add(n);
    }
  }
  
  private List<LoopNode> loopCatalog = new ArrayList();
  
  public AddCheckpointPragmas(Program program)
  {
    super(program);
    
    Procedure main = ObjectAnalizer.findMainProcedure(program);
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization( (Identifier)main.getName());
    this.programSize = c.getWeight();
    this.programStatements = c.statementCount;
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[AddCheckpointPragmas] begin", 1);
    if (ConfigurationManager.hasOption("DisableCkptAnalysis")) {
      return;
    }
    AddCheckpointPragmas transform = new AddCheckpointPragmas(program);
    transform.start();
    
    Tools.printlnStatus("[AddCheckpointPragmas] end", 1);
  }
  
  //输出全部循环
  void printLoops(List<LoopNode> loops, int tabs)
  {
    for (LoopNode n : loops)
    {
      if (tabs == 0) {
        System.out.println();
      }
      System.out.println(n.costFunction());
      printLoops(n.getChildren(), tabs + 1);
    }
  }
  
  //开始入口
  protected void start()
  {
    super.start();
    
    List<LoopNode> orphanLoops = new ArrayList();//
    Map<CppcStatement, LoopNode> processedLoops =  new HashMap();//处理过的循环
    
    SetOperations<Identifier> setOps = new SetOperations();
    
    for (LoopNode l : this.loopCatalog) {//遍历全部
      if (l.getParent() == null)
      {
        boolean added = false;
        for (LoopNode ll : this.loopCatalog) {
          if (l != ll)
          {
            if (ll.getCalledProcedures().contains(l.getLoop().getProcedure().getName()))
            {
              ll.add(l);
              added = true;
              break;
            }
            if (!setOps.setIntersection(l.getCallerProcedures(), ll.getCalledProcedures()).isEmpty())
            {
              ll.add(l);
              added = true;
            }
          }
        }
        if ((added) || 
        
          (l.getCallerProcedures().size() != 1) || 
          (ObjectAnalizer.isMainProcedure(l.getLoop().getProcedure())))
        {
          if (!added) {
            for (int i = 0; i < orphanLoops.size(); i++) {
              if (((LoopNode)orphanLoops.get(i)).costFunction() > l.costFunction())
              {
                orphanLoops.add(i, l);
                added = true;
                break;
              }
            }
          }
          if (!added) {
            orphanLoops.add(l);
          }
        }
      }
    }

    if (orphanLoops.isEmpty()) {
      return;
    }
    int n = orphanLoops.size();
    double[] f = new double[n];
    for (int i = 0; i < n; i++) {
      f[i] = ((LoopNode)orphanLoops.get(i)).costFunction();
    }
    double[] D = new double[n];
    double a = f[(n - 1)] - f[0];
    double b = 1 - n;
    double c = -f[0] * b;
    double d = Math.sqrt(a * a + b * b);
    int pos = 0;
    for (int i = 0; i < n; i++)
    {
      D[i] = (Math.abs(a * i + b * f[i] + c) / d);
      if (D[i] > D[pos]) {
        pos = i;
      }
    }

    int threshold = -1;
    if (pos != 0)
    {
      double[] df = new double[pos + 1];
      double[] d2f = new double[pos + 1];
      double[] sum = new double[pos + 1];
      boolean[] ismax = new boolean[pos + 1];
      df[0] = (d2f[0] = sum[0] = 0.0D);
      ismax[0] = false;
      for (int i = 1; i < pos + 1; i++)
      {
        f[i] -= f[(i - 1)];
        df[i] -= df[(i - 1)];
        sum[i] = (sum[(i - 1)] + df[i]);
      }
      for (int i = 1; i < pos; i++) {
        ismax[i] = ((d2f[i] > d2f[(i - 1)]) && (d2f[i] > d2f[(i + 1)]) ? 1 : false);
      }
      ismax[pos] = (d2f[pos] > d2f[(pos - 1)] ? 1 : false);
      for (int i = 0; i < pos + 1; i++) {
        if ((ismax[i] != 0) && 
          (sum[i] / sum[pos] > 0.5D))
        {
          threshold = i - 1;
          break;
        }
      }
      if (threshold == -1) {
        for (int i = pos; i >= 0; i--) {
          if (ismax[i] != 0)
          {
            threshold = i - 1;
            break;
          }
        }
      }
    }
    else
    {
      threshold = 0;
    }
    for (int i = 0; i < threshold + 1; i++) {
      placeCheckpoint((LoopNode)orphanLoops.get(i));
    }
  }
  
  private double pushCheckpoint(LoopNode p, double sum, double sumThreshold, double costThreshold)
  {
    List<LoopNode> children = p.getChildren();
    for (LoopNode n : children)
    {
      if (n.costFunction() > costThreshold) {
        return sum;
      }
      if (n.costFunction() < p.costFunction()) {
        break;
      }
      double delta = n.costFunction() - p.costFunction();
      if (sum - delta > sumThreshold)
      {
        sum -= delta;
        p.setCheckpoint(false);
        n.setCheckpoint(true);
        sum = pushCheckpoint(n, sum, sumThreshold, costThreshold);
      }
      else
      {
        return sum;
      }
    }
    return sum;
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure.getBody());
    iter.pruneOn(Expression.class);
    iter.pruneOn(Loop.class);
    while (iter.hasNext()) {
      try
      {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        if ((cppcStatement.getStatement() instanceof Loop)) {
          processLoop(cppcStatement, null);
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
  
  private void processLoop(CppcStatement cppcStatement, LoopNode parent)
  {
    Loop l = (Loop)cppcStatement.getStatement();
    DepthFirstIterator iter = new DepthFirstIterator(l.getBody());
    iter.pruneOn(Expression.class);
    iter.pruneOn(Loop.class);
    Set<Identifier> calledFunctions = new HashSet();
    boolean manualCheckpoint = false;
    
    Identifier thisProc = (Identifier)cppcStatement.getProcedure().getName();
    Set<Identifier> thisCallers = getCallers(thisProc);
    
    LoopNode node = new LoopNode(cppcStatement, null, thisCallers, false);
    
    if (node.costFunction() == -1.0D) {
      return;
    }
    
    if (parent != null) {
      node.setParent(parent);
    }
    
    while (iter.hasNext()) {
      try
      {
        CppcStatement innerStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        if ((innerStatement.getStatement() instanceof Loop)) {
          processLoop(innerStatement, node);
        }

        if ((innerStatement.getStatement() instanceof CppcCheckpointLoopPragma))
        {
          innerStatement.detach();
          manualCheckpoint = true;
        }

        if ((innerStatement.getStatement() instanceof ExpressionStatement))
        {
          DepthFirstIterator stmtIter = new DepthFirstIterator(
            innerStatement);
          while (stmtIter.hasNext()) {
            try
            {
              FunctionCall call = (FunctionCall)stmtIter.next(
                FunctionCall.class);
              calledFunctions.add((Identifier)call.getName());
            }
            catch (NoSuchElementException localNoSuchElementException) {}
          }
        }
        
      }
      catch (NoSuchElementException localNoSuchElementException1) {}
    }
    node.setCalledProcedures(calledFunctions);
    node.setManualCheckpoint(manualCheckpoint);
    if (parent != null) {
      parent.add(node);
    }
    this.loopCatalog.add(node);
  }
  
  private Set<Identifier> getCallers(Identifier proc)
  {
    if (!CppcRegisterManager.isRegistered(proc)) {
      return null;
    }
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      proc);
    Set<Identifier> callers = new HashSet();
    Set<Identifier> add = new HashSet(c.getCalledFrom());
    callers.add(proc);
    while (add.size() > 0)
    {
      Set<Identifier> newAdd = new HashSet();
      for (Identifier name : add)
      {
        callers.add(name);
        if (CppcRegisterManager.isRegistered(name))
        {
          ProcedureCharacterization nc = 
            CppcRegisterManager.getCharacterization(name);
          
          newAdd.addAll(nc.getCalledFrom());
        }
      }
      newAdd.removeAll(callers);
      add = newAdd;
    }
    return callers;
  }
  
  private void placeCheckpoint(LoopNode ckpt)
  {
    BreadthFirstIterator iter = new BreadthFirstIterator(
      ((Loop)ckpt.getLoop().getStatement()).getBody());
    iter.next();
    iter.pruneOn(Expression.class);
    while (iter.hasNext()) {
      try
      {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        if ((cppcStatement.getSafePoint()) && 
          (!(cppcStatement.getStatement() instanceof DeclarationStatement)))
        {
          TranslationUnit tunit = (TranslationUnit)
            ckpt.getLoop().getProcedure().getParent();
          System.out.println("Placing checkpoint at: " + 
            tunit.getInputFilename() + ": " + 
            cppcStatement.where());
          
          addCheckpointPragma(cppcStatement);
          
          return;
        }
      }
      catch (NoSuchElementException e)
      {
        TranslationUnit tunit = (TranslationUnit)
          ckpt.getLoop().getProcedure().getParent();
        System.err.println(tunit.getInputFilename() + ": " + 
          ckpt.getLoop().where() + ": " + 
          "no suitable safe point for inserting checkpoint");
      }
    }
  }
  
  private void addCheckpointPragma(Statement stmt)
  {
    CppcCheckpointPragma pragma = new CppcCheckpointPragma();
    CppcStatement cppcStatement = new CppcStatement(pragma);
    pragma.setParent(cppcStatement);
    
    CompoundStatement statementList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass(stmt, CompoundStatement.class);
    statementList.addStatementBefore(stmt, cppcStatement);
  }
}
