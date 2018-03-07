package cppc.compiler.analysis;

import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.WhileLoop;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.MultiExpression;

//import cppc.compiler.transforms.shared.*;

import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.transforms.shared.comms.Communication;
import cppc.compiler.transforms.shared.comms.CommunicationBuffer;

import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.util.dispatcher.FunctionDispatcher;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

public abstract class CommunicationAnalyzer
  extends FunctionDispatcher<Statement>
{
  private static Class instanceClass;
  private static CommunicationAnalyzer instance;
  protected static CppcStatement currentStatement = null;
  protected static boolean symbolicAnalysis = true;
  protected static boolean analyzedStatement = false;
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption("CPPC/Analysis/CommunicationAnalyzer/ClassName"));
      
      instance = (CommunicationAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  private static Map<Identifier, List> knownSymbols = new HashMap();
  private static CommunicationBuffer commBuffer = new CommunicationBuffer();
  private static Map<Identifier, Set<Identifier>> commVariables = new HashMap();
  private static Set<Identifier> currentCommVariables = null;
  
  public static Set<Identifier> getCurrentCommunicationVariables()
  {
    return currentCommVariables;
  }
  
  public static void setCommunicationVariables(Map<Identifier, Set<Identifier>> commVariables)
  {
    commVariables = commVariables;
  }
  
  public static void analyzeStatement(CppcStatement cppcStatement)
  {
    Method m = instance.dispatch(cppcStatement.getStatement(), "analyzeStatement");
    if (m == null) {
      return;
    }
    boolean safePoint = !commBuffer.getPendingCommunications();
    CppcStatement oldStatement = currentStatement;
    currentStatement = cppcStatement;
    try
    {
      m.invoke(instance, new Object[] { cppcStatement.getStatement() });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      currentStatement.setSafePoint(safePoint);
      currentStatement = oldStatement;
    }
  }
  
  public static void analyze(Procedure proc)
  {
    if (proc == null) {
      return;
    }
    CommunicationBuffer procBuffer = null;
    ProcedureCharacterization c = null;
    if (CppcRegisterManager.isRegistered((Identifier)proc.getName()))
    {
      c = CppcRegisterManager.getCharacterization((Identifier)proc.getName());
      procBuffer = c.getCommunicationBuffer();
    }
    boolean optimize = true;
    if (commVariables.get((Identifier)proc.getName()) != null) {
      for (Identifier id : (Set)commVariables.get((Identifier)proc.getName())) {
        if (c.getVariableDependencies().containsKey(id))
        {
          optimize = false;
          break;
        }
      }
    }
    if ((!optimize) || (procBuffer == null))
    {
      Set<Identifier> oldCommVariables = currentCommVariables;
      currentCommVariables = (Set)commVariables.get((Identifier)proc.getName());
      if ((oldCommVariables != null) && (currentCommVariables != null)) {
        for (Identifier id : oldCommVariables) {
          if (ObjectAnalizer.isGlobal(id, proc.getBody())) {
            currentCommVariables.add(id);
          }
        }
      }
      CommunicationBuffer oldBuffer = commBuffer;
      commBuffer = new CommunicationBuffer(oldBuffer);
      
      boolean oldSymbolicAnalysis = symbolicAnalysis;
      symbolicAnalysis = true;
      
      instance.analyzeStatement(proc.getBody());
      
      symbolicAnalysis = oldSymbolicAnalysis;
      
      procBuffer = commBuffer;
      commBuffer = oldBuffer;
      
      currentCommVariables = oldCommVariables;
      if (c == null)
      {
        c = new ProcedureCharacterization((Identifier)proc.getName());
        CppcRegisterManager.addProcedure((Identifier)proc.getName(), c);
      }
      if (optimize) {
        c.setCommunicationBuffer(procBuffer);
      }
    }
    boolean safe = !commBuffer.getPendingCommunications();
    CommunicationBuffer oldBuffer = (CommunicationBuffer)commBuffer.clone();
    instance.mixBuffers(commBuffer, procBuffer);
    if ((!safe) && (!commBuffer.getPendingCommunications()))
    {
      commBuffer = oldBuffer;
      instance.analyzeStatement(proc.getBody());
    }
  }
  
  protected void analyzeStatement(CompoundStatement compoundStatement)
  {
    List children = compoundStatement.getChildren();
    for (int i = 0; i < children.size(); i++)
    {
      CppcStatement cppcStatement = (CppcStatement)children.get(i);
      
      analyzedStatement = false;
      if ((symbolicAnalysis) && 
        (currentCommVariables != null))
      {
        SetOperations<Identifier> setOps = new SetOperations();
        if (!setOps.setIntersection(cppcStatement.getPartialGenerated(), currentCommVariables).isEmpty())
        {
          SymbolicAnalyzer.analyzeStatement(cppcStatement, knownSymbols);
          analyzedStatement = true;
        }
      }
      analyzeStatement(cppcStatement);
    }
  }
  
  protected void analyzeStatement(ExpressionStatement s)
  {
    DepthFirstIterator iter = new DepthFirstIterator(s.getExpression());
    while (iter.hasNext()) {
      try
      {
        FunctionCall call = (FunctionCall)iter.next(FunctionCall.class);
        if (Communication.sendFunctions.contains(call.getName()))
        {
          Communication send = Communication.fromCall(call);
          matchSend(send, commBuffer);
          return;
        }
        if (Communication.recvFunctions.contains(call.getName()))
        {
          Communication recv = Communication.fromCall(call);
          matchRecv(recv, commBuffer);
          return;
        }
        if (Communication.waitFunctions.contains(call.getName()))
        {
          Communication wait = Communication.fromCall(call);
          matchWait(wait, commBuffer);
          return;
        }
        if (Communication.rankerFunctions.contains(call.getName()))
        {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization((Identifier)call.getName());
          
          Integer valuePos = new Integer((String)c.getSemantic("CPPC/Comm/Ranker").get("Rank"));
          
          Set<Identifier> rankIdSet = ExpressionAnalyzer.analyzeExpression(currentStatement, call.getArgument(valuePos.intValue() - 1));
          if (rankIdSet.size() != 1)
          {
            System.err.println("ERROR: RANK VARIABLE NOT IDENTIFIED");
            System.exit(0);
          }
          Identifier rankId = (Identifier)rankIdSet.iterator().next();
          Expression expr = buildNodesExpression(rankId);
          List<Expression> values = new ArrayList(1);
          values.add(expr);
          
          SymbolicAnalyzer.addKnownSymbol(knownSymbols, rankId, values);
          return;
        }
        if (Communication.sizerFunctions.contains(call.getName()))
        {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization((Identifier)call.getName());
          
          Integer valuePos = new Integer((String)c.getSemantic("CPPC/Comm/Sizer").get("Size"));
          
          Set<Identifier> sizeIdSet = ExpressionAnalyzer.analyzeExpression(currentStatement, call.getArgument(valuePos.intValue() - 1));
          if (sizeIdSet.size() != 1)
          {
            System.err.println("ERROR: SIZE VARIABLE NOT IDENTIFIED");
            System.exit(0);
          }
          Identifier sizeId = (Identifier)sizeIdSet.iterator().next();
          Expression expr = buildSizeExpression();
          List<Expression> values = new ArrayList(1);
          values.add(expr);
          
          SymbolicAnalyzer.addKnownSymbol(knownSymbols, sizeId, values);
          return;
        }
        Procedure proc = call.getProcedure();
        if (proc == null) {
          return;
        }
        Map<Identifier, List> inCallKnown = new HashMap();
        
        Set<Identifier> subCommVariables = (Set)commVariables.get(call.getName());
        SymbolTable localTable = (SymbolTable)ObjectAnalizer.getParentOfClass(s, SymbolTable.class);
        
        SymbolTable subTable = proc.getBody();
        List parameters = proc.getParameters();
        if (subCommVariables != null) {
          for (Identifier id : subCommVariables) {
            if (((ObjectAnalizer.isGlobal(id, subTable)) || (ObjectAnalizer.isGlobal(id, localTable))) && (knownSymbols.containsKey(id)))
            {
              inCallKnown.put(id, knownSymbols.get(id));
            }
            else
            {
              ProcedureParameter param = ObjectAnalizer.idToProcParam(call.getProcedure().getParameters(), id, true);
              if (param != null)
              {
                Expression expr = call.getArgument(param.getPosition());
                Expression folded = SymbolicExpressionAnalyzer.analyzeExpression(expr, knownSymbols);
                if (((folded instanceof Literal)) || ((folded instanceof MultiExpression)))
                {
                  List values = new ArrayList(1);
                  values.add(folded);
                  SymbolicAnalyzer.addKnownSymbol(inCallKnown, id, values, proc.getBody());
                }
              }
            }
          }
        }
        Map<Identifier, List> oldSymbols = knownSymbols;
        knownSymbols = inCallKnown;
        
        analyze(proc);
        
        ProcedureCharacterization c = CppcRegisterManager.getCharacterization((Identifier)call.getName());
        for (ProcedureParameter param : c.getGenerated())
        {
          Expression expr = null;
          try
          {
            expr = call.getArgument(param.getPosition());
          }
          catch (Exception e) {}
          continue;
          if ((expr instanceof Identifier))
          {
            Identifier parameterName = (Identifier)((VariableDeclaration)parameters.get(param.getPosition())).getDeclarator(0).getSymbol();
            if (knownSymbols.containsKey(parameterName)) {
              oldSymbols.put((Identifier)expr, knownSymbols.get(parameterName));
            }
          }
        }
        for (Identifier id : knownSymbols.keySet()) {
          if ((ObjectAnalizer.isGlobal(id, localTable)) || (ObjectAnalizer.isGlobal(id, subTable))) {
            if (knownSymbols.containsKey(id)) {
              oldSymbols.put(id, knownSymbols.get(id));
            }
          }
        }
        knownSymbols = oldSymbols;
      }
      catch (NoSuchElementException e) {}
    }
  }
  
  protected void analyzeStatement(IfStatement s)
  {
    CommunicationBuffer originalBuffer = commBuffer;
    Expression condition = s.getControlExpression();
    commBuffer = new CommunicationBuffer(originalBuffer);
    symbolicAnalysis = false;
    
    analyzeStatement((CppcStatement)s.getThenStatement());
    for (Communication comm : commBuffer.getAll()) {
      comm.addCondition((Expression)condition.clone());
    }
    if (s.getElseStatement() != null)
    {
      CommunicationBuffer thenBuffer = commBuffer;
      commBuffer = new CommunicationBuffer(originalBuffer);
      
      analyzeStatement((CppcStatement)s.getElseStatement());
      for (Communication comm : commBuffer.getAll()) {
        comm.addCondition(new UnaryExpression(UnaryOperator.LOGICAL_NEGATION, (Expression)condition.clone()));
      }
      mixBuffers(thenBuffer, commBuffer);
      commBuffer = thenBuffer;
    }
    mixBuffers(originalBuffer, commBuffer);
    commBuffer = originalBuffer;
    
    SymbolicAnalyzer.analyzeStatement(currentStatement, knownSymbols);
    symbolicAnalysis = true;
  }
  
  protected void analyzeStatement(WhileLoop l)
  {
    analyzeStatement(l);
  }
  
  protected void analyzeStatement(Loop l)
  {
    if (!analyzedStatement)
    {
      SymbolicAnalyzer.enterLoop(l, knownSymbols);
      analyzeStatement((CppcStatement)l.getBody());
      SymbolicAnalyzer.exitLoop(l);
    }
  }
  
  private void matchSend(Communication send, CommunicationBuffer buffer)
  {
    Communication match = null;
    
    Expression tag = send.getCallValue("Tag");
    Expression foldedTag = SymbolicExpressionAnalyzer.analyzeExpression(tag, knownSymbols);
    if ((foldedTag instanceof MultiExpression))
    {
      MultiExpression mexpr = (MultiExpression)foldedTag;
      for (Expression key : mexpr.getValueSet())
      {
        Expression value = mexpr.getValue(key);
        Communication clone = (Communication)send.clone();
        Expression cloneExpr = clone.getCallValue("Tag");
        cloneExpr.swapWith((Expression)value.clone());
        matchSend(clone, buffer);
      }
      return;
    }
    send.setExpressionProperty("Tag", foldedTag);
    try
    {
      Expression request = send.getCallValue("Request");
      Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(request, knownSymbols);
      
      send.setExpressionProperty("Request", foldedRequest);
    }
    catch (Exception e) {}
    Expression target = send.getCallValue("Destination");
    Expression foldedTarget = SymbolicExpressionAnalyzer.analyzeExpression(target, knownSymbols);
    
    send.setExpressionProperty("Destination", foldedTarget);
    for (Communication recv : buffer.getUnmatchedRecvs()) {
      if (sendRecvMatch(send, recv))
      {
        updateStatements(send, recv);
        match = recv;
        break;
      }
    }
    if (match != null)
    {
      boolean blocking = new Boolean(send.getProperty("Blocking")).booleanValue();
      
      boolean blockingMatch = new Boolean(match.getProperty("Blocking")).booleanValue();
      
      buffer.getUnmatchedRecvs().remove(match);
      
      Queue<Communication> nonblocking = new LinkedList();
      if (!blocking) {
        nonblocking.add(send);
      }
      if (!blockingMatch) {
        nonblocking.add(match);
      }
      processNonBlocking(nonblocking, buffer);
    }
    else
    {
      buffer.getUnmatchedSends().add(send);
    }
  }
  
  private void matchRecv(Communication recv, CommunicationBuffer buffer)
  {
    Communication match = null;
    
    Expression tag = recv.getCallValue("Tag");
    if (tag.equals(new Identifier("MPI_ANY_TAG")))
    {
      System.err.println("Error: line " + recv.getCall().getStatement().where() + " : " + "communication analysis does not yet support use of MPI_ANY_TAG.");
      
      System.exit(0);
    }
    Expression foldedTag = SymbolicExpressionAnalyzer.analyzeExpression(tag, knownSymbols);
    if ((foldedTag instanceof MultiExpression))
    {
      MultiExpression mexpr = (MultiExpression)foldedTag;
      for (Expression key : mexpr.getValueSet())
      {
        Expression value = mexpr.getValue(key);
        Communication clone = (Communication)recv.clone();
        Expression cloneExpr = clone.getCallValue("Tag");
        cloneExpr.swapWith((Expression)value.clone());
        matchRecv(clone, buffer);
      }
      return;
    }
    recv.setExpressionProperty("Tag", foldedTag);
    try
    {
      Expression request = recv.getCallValue("Request");
      Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(request, knownSymbols);
      
      recv.setExpressionProperty("Request", foldedRequest);
    }
    catch (Exception e) {}
    Expression source = recv.getCallValue("Source");
    if (!source.equals(new Identifier("MPI_ANY_SOURCE")))
    {
      Expression foldedSource = SymbolicExpressionAnalyzer.analyzeExpression(source, knownSymbols);
      
      recv.setExpressionProperty("Source", foldedSource);
    }
    else
    {
      recv.setExpressionProperty("Source", source);
    }
    for (Communication send : buffer.getUnmatchedSends()) {
      if (sendRecvMatch(send, recv))
      {
        updateStatements(send, recv);
        match = send;
        break;
      }
    }
    if (match != null)
    {
      boolean blocking = new Boolean(recv.getProperty("Blocking")).booleanValue();
      
      boolean blockingMatch = new Boolean(match.getProperty("Blocking")).booleanValue();
      
      buffer.getUnmatchedSends().remove(match);
      
      Queue<Communication> nonblocking = new LinkedList();
      if (!blocking) {
        nonblocking.add(recv);
      }
      if (!blockingMatch) {
        nonblocking.add(match);
      }
      processNonBlocking(nonblocking, buffer);
    }
    else
    {
      buffer.getUnmatchedRecvs().add(recv);
    }
  }
  
  private void matchWait(Communication wait, CommunicationBuffer buffer)
  {
    Expression request = wait.getCallValue("Request");
    Expression foldedRequest = SymbolicExpressionAnalyzer.analyzeExpression(request, knownSymbols);
    
    wait.setExpressionProperty("Request", foldedRequest);
    
    List<Communication> matches = new ArrayList(buffer.getUnwaitedComms().size());
    
    boolean collective = "Collective".equals(wait.getProperty("Type"));
    for (Communication comm : buffer.getUnwaitedComms()) {
      if (commWaitMatch(comm, wait, collective))
      {
        matches.add(comm);
        updateStatements(comm, wait);
      }
    }
    if (matches.isEmpty()) {
      buffer.getUnmatchedWaits().add(wait);
    } else {
      for (Communication comm : matches) {
        buffer.getUnwaitedComms().remove(comm);
      }
    }
  }
  
  private void processNonBlocking(Queue<Communication> comms, CommunicationBuffer buffer)
  {
    Set<Communication> matches = new HashSet();
    for (Communication comm : comms)
    {
      Communication match = processNonBlocking(comm, buffer);
      if (match != null) {
        matches.add(match);
      }
    }
    for (Communication match : matches) {
      buffer.getUnmatchedWaits().remove(match);
    }
  }
  
  private Communication processNonBlocking(Communication comm, CommunicationBuffer buffer)
  {
    Communication match = null;
    for (Communication wait : buffer.getUnmatchedWaits())
    {
      boolean collective = "Collective".equals(wait.getProperty("Type"));
      if (commWaitMatch(comm, wait, collective))
      {
        updateStatements(comm, wait);
        match = wait;
        break;
      }
    }
    if (match == null) {
      buffer.getUnwaitedComms().add(comm);
    }
    return match;
  }
  
  private boolean sendRecvMatch(Communication send, Communication recv)
  {
    Expression sendCommunicator = send.getCallValue("Communicator");
    Expression recvCommunicator = send.getCallValue("Communicator");
    if (!sendCommunicator.equals(recvCommunicator)) {
      return false;
    }
    Expression sendTag = send.getExpressionProperty("Tag");
    Expression recvTag = recv.getExpressionProperty("Tag");
    if (!sendTag.equals(recvTag)) {
      return false;
    }
    Expression target = send.getExpressionProperty("Destination");
    Expression source = recv.getExpressionProperty("Source");
    if (source.equals(new Identifier("MPI_ANY_SOURCE"))) {
      return true;
    }
    Expression nodes = buildNodesExpression(null);
    for (Expression i : ((MultiExpression)nodes).getValueSet()) {
      try
      {
        Expression targetProcess = ((MultiExpression)target).getValue(i);
        
        Expression sourceProcess = ((MultiExpression)source).getValue(targetProcess);
        if (!i.equals(sourceProcess)) {
          return false;
        }
      }
      catch (ClassCastException e) {}catch (Exception e) {}
    }
    return true;
  }
  
  private boolean commWaitMatch(Communication comm, Communication wait, boolean collective)
  {
    Expression waitRequest = wait.getExpressionProperty("Request");
    
    Expression commRequest = comm.getExpressionProperty("Request");
    if (!collective) {
      return waitRequest.equals(commRequest);
    }
    waitRequest = ObjectAnalizer.getBaseIdentifier(waitRequest);
    commRequest = ObjectAnalizer.getBaseIdentifier(commRequest);
    return waitRequest.equals(commRequest);
  }
  
  private void removeRedundant(CommunicationBuffer oldBuffer, CommunicationBuffer newBuffer)
  {
    CommunicationBuffer redundant = (CommunicationBuffer)oldBuffer.clone();
    redundant.getUnmatchedSends().retainAll(newBuffer.getUnmatchedSends());
    redundant.getUnmatchedRecvs().retainAll(newBuffer.getUnmatchedRecvs());
    redundant.getUnwaitedComms().retainAll(newBuffer.getUnwaitedComms());
    redundant.getUnmatchedWaits().retainAll(newBuffer.getUnmatchedWaits());
    for (Communication comm : redundant.getAll()) {
      if (comm.getConditions().size() == 0) {
        redundant.remove(comm);
      }
    }
    removeRedundant(redundant.getUnmatchedSends(), newBuffer.getUnmatchedSends());
    
    removeRedundant(redundant.getUnmatchedRecvs(), newBuffer.getUnmatchedRecvs());
    
    removeRedundant(redundant.getUnwaitedComms(), newBuffer.getUnwaitedComms());
    
    removeRedundant(redundant.getUnmatchedWaits(), newBuffer.getUnmatchedWaits());
  }
  
  private void removeRedundant(Queue<Communication> redundant, Queue<Communication> newBuffer)
  {
    List<Communication> remove = new ArrayList(redundant.size());
    for (Iterator i$ = redundant.iterator(); i$.hasNext();)
    {
      oldComm = (Communication)i$.next();
      for (Communication newComm : newBuffer) {
        if ((oldComm.equals(newComm)) && 
          (incompatible(oldComm, newComm))) {
          remove.add(newComm);
        }
      }
    }
    Communication oldComm;
    redundant.removeAll(remove);
    newBuffer.removeAll(remove);
  }
  
  private boolean incompatible(Communication lhs, Communication rhs)
  {
    for (Iterator i$ = lhs.getConditions().iterator(); i$.hasNext();)
    {
      lhsCondition = (Expression)i$.next();
      for (Expression rhsCondition : rhs.getConditions()) {
        if (incompatible(lhsCondition, rhsCondition)) {
          return true;
        }
      }
    }
    Expression lhsCondition;
    return false;
  }
  
  private boolean incompatible(Expression lhs, Expression rhs)
  {
    if ((lhs instanceof UnaryExpression))
    {
      UnaryExpression safeLhs = (UnaryExpression)lhs;
      if ((safeLhs.getOperator() == UnaryOperator.LOGICAL_NEGATION) && 
        (safeLhs.getExpression().equals(rhs))) {
        return true;
      }
    }
    if ((rhs instanceof UnaryExpression))
    {
      UnaryExpression safeRhs = (UnaryExpression)rhs;
      if ((safeRhs.getOperator() == UnaryOperator.LOGICAL_NEGATION) && 
        (safeRhs.getExpression().equals(lhs))) {
        return true;
      }
    }
    if (((lhs instanceof BinaryExpression)) && ((rhs instanceof BinaryExpression)))
    {
      BinaryExpression safeLhs = (BinaryExpression)lhs;
      BinaryExpression safeRhs = (BinaryExpression)rhs;
      if ((safeLhs.getOperator() == BinaryOperator.COMPARE_EQ) && (safeRhs.getOperator() == BinaryOperator.COMPARE_EQ))
      {
        Expression lhsValue = null;
        Expression rhsValue = null;
        if (safeLhs.getLHS().equals(safeRhs.getLHS()))
        {
          lhsValue = safeLhs.getRHS();
          rhsValue = safeRhs.getRHS();
        }
        if (safeLhs.getLHS().equals(safeRhs.getRHS()))
        {
          lhsValue = safeLhs.getRHS();
          rhsValue = safeRhs.getLHS();
        }
        if (safeLhs.getRHS().equals(safeRhs.getLHS()))
        {
          lhsValue = safeLhs.getLHS();
          rhsValue = safeRhs.getRHS();
        }
        if ((lhsValue != null) && 
          (!lhsValue.equals(rhsValue))) {
          return true;
        }
      }
    }
    return false;
  }
  
  private void mixBuffers(CommunicationBuffer oldBuffer, CommunicationBuffer newBuffer)
  {
    if (newBuffer.isEmpty()) {
      return;
    }
    removeRedundant(oldBuffer, newBuffer);
    for (Communication newSend : newBuffer.getUnmatchedSends()) {
      matchSend(newSend, oldBuffer);
    }
    for (Communication newRecv : newBuffer.getUnmatchedRecvs()) {
      matchRecv(newRecv, oldBuffer);
    }
    processNonBlocking(newBuffer.getUnwaitedComms(), oldBuffer);
    for (Communication newWait : newBuffer.getUnmatchedWaits()) {
      matchWait(newWait, oldBuffer);
    }
  }
  
  private void updateStatements(Communication source, Communication target)
  {
    CppcStatement cppcSource = (CppcStatement)source.getCall().getStatement().getParent();
    
    CppcStatement cppcTarget = (CppcStatement)target.getCall().getStatement().getParent();
    
    cppcSource.getMatchingCommunications().add(target);
    cppcTarget.getMatchingCommunications().add(source);
  }
  
  private Expression buildNodesExpression(Identifier id)
  {
    Integer procNumber = null;
    try
    {
      procNumber = new Integer(ConfigurationManager.getOption("ProcessNumber"));
    }
    catch (Exception e)
    {
      procNumber = new Integer(1);
    }
    MultiExpression mexpr = new MultiExpression(id);
    for (int i = 0; i < procNumber.intValue(); i++)
    {
      IntegerLiteral rank = new IntegerLiteral(i);
      mexpr.addExpression(rank, rank);
    }
    return mexpr;
  }
  
  private Expression buildSizeExpression()
  {
    Integer procNumber = null;
    try
    {
      procNumber = new Integer(ConfigurationManager.getOption("ProcessNumber"));
    }
    catch (Exception e)
    {
      procNumber = new Integer(1);
    }
    return new IntegerLiteral(procNumber.intValue());
  }
}
