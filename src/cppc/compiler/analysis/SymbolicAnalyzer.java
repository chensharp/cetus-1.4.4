package cppc.compiler.analysis;

import cetus.hir.CompoundStatement;
import cetus.hir.ContinueStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Label;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.NullStatement;
import cetus.hir.Procedure;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.hir.WhileLoop;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.cetus.CppcShutdownPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.util.dispatcher.FunctionDispatcher;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public abstract class SymbolicAnalyzer
  extends FunctionDispatcher<Statement>
{
  private static Class instanceClass;
  private static SymbolicAnalyzer instance;
  protected static Map knownSymbols = null;
  private static List<CppcStatement> remove = new ArrayList();
  private static int callDepth = 0;
  protected static Map<Expression, Set<Identifier>> removeUponLabel = new Hashtable();
  protected static Stack<Identifier> idMask = new Stack();
  protected static Stack<Expression> valueMask = new Stack();
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption(
        "CPPC/Analysis/SymbolicAnalyzer/ClassName"));
      instance = (SymbolicAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static void analyzeStatement(final CppcStatement cppcStatement, final Map knownSymbols) {
        final Statement stmt = cppcStatement.getStatement();
        final Method m = SymbolicAnalyzer.instance.dispatch(stmt, "analyzeStatement");
        if (m == null) {
            System.err.println("WARNING: cppc.compiler.analysis.SymbolicAnalyzer.analyzeStatement() not implemented for " + stmt.getClass());
            return;
        }
        Map oldSymbols = null;
        if (SymbolicAnalyzer.knownSymbols != knownSymbols) {
            oldSymbols = SymbolicAnalyzer.knownSymbols;
            SymbolicAnalyzer.knownSymbols = knownSymbols;
        }
        try {
            ++SymbolicAnalyzer.callDepth;
            m.invoke(SymbolicAnalyzer.instance, stmt);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        finally {
            if (oldSymbols != null) {
                SymbolicAnalyzer.knownSymbols = oldSymbols;
            }
            --SymbolicAnalyzer.callDepth;
            if (SymbolicAnalyzer.callDepth == 0) {
                for (final CppcStatement s : SymbolicAnalyzer.remove) {
                    final CompoundStatement parent = (CompoundStatement)s.getParent();
                    if (parent != null) {
                        parent.removeChild((Traversable)s);
                    }
                }
                SymbolicAnalyzer.remove.clear();
            }
        }
        if (oldSymbols != null) {
            SymbolicAnalyzer.knownSymbols = oldSymbols;
        }
        --SymbolicAnalyzer.callDepth;
        if (SymbolicAnalyzer.callDepth == 0) {
            for (final CppcStatement s : SymbolicAnalyzer.remove) {
                final CompoundStatement parent = (CompoundStatement)s.getParent();
                if (parent != null) {
                    parent.removeChild((Traversable)s);
                }
            }
            SymbolicAnalyzer.remove.clear();
        }
    }


  
  public static void enterLoop(Loop l, Map knownSymbols)
  {
    Method m = instance.dispatch((Statement)l, "enterLoop");
    if (m == null)
    {
      System.err.println("WARNING: cppc.compiler.analysis.SymbolicAnalyzer.enterLoop() not implemented for " + 
        l.getClass());
      
      return;
    }
    knownSymbols = knownSymbols;
    try
    {
      m.invoke(instance, new Object[] { l });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static void exitLoop(Loop l)
  {
    Method m = instance.dispatch((Statement)l, "exitLoop");
    if (m == null)
    {
      System.err.println("WARNING: cppc.compiler.analysis.SymbolicAnalyzer.exitLoop() not implemented for " + 
        l.getClass());
      
      return;
    }
    try
    {
      m.invoke(instance, new Object[] { l });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  protected void analyzeStatement(CppcConditionalJump jump) {}
  
  protected void analyzeStatement(CppcExecutePragma pragma) {}
  
  protected void analyzeStatement(CppcLabel label) {}
  
  protected void analyzeStatement(CppcShutdownPragma shutdown) {}
  
  protected void analyzeStatement(CompoundStatement compoundStatement)
  {
    Set<Identifier> commVariables = 
      CommunicationAnalyzer.getCurrentCommunicationVariables();
    if ((commVariables != null) && (!commVariables.isEmpty()))
    {
      SetOperations<Identifier> setOps = new SetOperations();
      Iterator iter = compoundStatement.getChildren().iterator();
      while (iter.hasNext())
      {
        CppcStatement next = (CppcStatement)iter.next();
        if (!setOps.setIntersection(next.getPartialGenerated(), commVariables).isEmpty()) {
          analyzeStatement(next, knownSymbols);
        }
      }
    }
  }
  
  protected void analyzeStatement(ContinueStatement stmt) {}
  
  protected void analyzeStatement(DeclarationStatement stmt)
  {
    Declaration declaration = stmt.getDeclaration();
    if ((declaration instanceof VariableDeclaration))
    {
      VariableDeclaration vd = (VariableDeclaration)declaration;
      for (int i = 0; i < vd.getNumDeclarators(); i++)
      {
        VariableDeclarator vdeclarator = 
          (VariableDeclarator)vd.getDeclarator(i);
        if (vdeclarator.getInitializer() != null)
        {
          List<Expression> initializers = 
            vdeclarator.getInitializer().getChildren();
          List<Expression> folded = new ArrayList(
            initializers.size());
          for (int j = 0; j < initializers.size(); j++) {
            folded.add(SymbolicExpressionAnalyzer.analyzeExpression(
              (Expression)initializers.get(j), knownSymbols));
          }
          Identifier id = (Identifier)vdeclarator.getSymbol();
          addKnownSymbol(knownSymbols, id, folded);
        }
      }
    }
  }
  
  protected void analyzeStatement(ExpressionStatement s)
  {
    SymbolicExpressionAnalyzer.analyzeExpression(s.getExpression(), 
      knownSymbols);
  }
  
  protected void analyzeStatement(GotoStatement s)
  {
    SymbolicExpressionAnalyzer.analyzeExpression(s.getValue(), knownSymbols);
    
    Expression target = s.getValue();
    DepthFirstIterator iter = new DepthFirstIterator(
      s.getProcedure().getBody());
    iter.next();
    iter.pruneOn(Expression.class);
    while (iter.hasNext())
    {
      Object obj = iter.next();
      if (((obj instanceof Label)) && 
        (target.equals(((Label)obj).getName()))) {
        return;
      }
      if (((obj instanceof GotoStatement)) && 
        (obj == s)) {
        break;
      }
    }
    Set<Identifier> removeInc = new HashSet();
    while (iter.hasNext()) {
      try
      {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        if (((cppcStatement.getStatement() instanceof Label)) && 
          (target.equals(((Label)cppcStatement.getStatement()).getName()))) {
          break;
        }
        for (Identifier id : cppcStatement.getGenerated()) {
          removeInc.add(id);
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    Set<Identifier> oldRemove = (Set)removeUponLabel.get(s.getValue());
    if (oldRemove == null) {
      removeUponLabel.put(s.getValue(), removeInc);
    } else {
      oldRemove.addAll(removeInc);
    }
  }
  
  protected void analyzeStatement(IfStatement s)
  {
    if (CommunicationAnalyzer.symbolicAnalysis) {
      return;
    }
    Expression control = SymbolicExpressionAnalyzer.analyzeExpression(
      s.getControlExpression(), knownSymbols);
    if ((control instanceof MultiExpression))
    {
      MultiExpression safeControl = (MultiExpression)control;
      
      idMask.push(safeControl.getVar());
      Statement thenBranch = s.getThenStatement();
      Statement elseBranch = s.getElseStatement();
      for (Expression key : safeControl.getValueSet())
      {
        Expression controlValue = safeControl.getValue(key);
        
        valueMask.push(safeControl.getKeyOf(controlValue));
        
        IfStatement virtual = new IfStatement((Expression)
          controlValue.clone(), new NullStatement());
        virtual.setThenStatement(thenBranch);
        if (elseBranch != null) {
          virtual.setElseStatement(elseBranch);
        }
        virtual.setParent(s.getParent());
        
        analyzeStatement(virtual);
        valueMask.pop();
      }
      s.setThenStatement(thenBranch);
      if (elseBranch != null) {
        s.setElseStatement(elseBranch);
      }
      idMask.pop();
      return;
    }
    if ((control instanceof Literal))
    {
      Expression trueLiteral = 
        SymbolicExpressionAnalyzer.instance.buildBooleanLiteral(true);
      CppcStatement branch = null;
      if (control.equals(trueLiteral)) {
        branch = (CppcStatement)s.getThenStatement();
      } else {
        branch = (CppcStatement)s.getElseStatement();
      }
      if (branch != null) {
        analyzeStatement(branch, knownSymbols);
      }
      return;
    }
    Map symbolsCopyThen = new HashMap(knownSymbols);
    Map symbolsCopyElse = new HashMap(knownSymbols);
    Map oldSymbols = knownSymbols;
    
    analyzeStatement((CppcStatement)s.getThenStatement(), 
      symbolsCopyThen);
    if (s.getElseStatement() != null) {
      analyzeStatement((CppcStatement)s.getElseStatement(), 
        symbolsCopyElse);
    }
    knownSymbols = oldSymbols;
    
    Set<Identifier> thenSymbols = symbolsCopyThen.keySet();
    Object elseSymbols = symbolsCopyElse.keySet();
    
    SetOperations<Identifier> setOps = new SetOperations();
    Set<Identifier> thenRemoved = setOps.setMinus(
      knownSymbols.keySet(), thenSymbols);
    Set<Identifier> elseRemoved = setOps.setMinus(
      knownSymbols.keySet(), (Set)elseSymbols);
    
    Set<Identifier> remove = new HashSet(knownSymbols.size());
    remove.addAll(thenRemoved);
    remove.addAll(elseRemoved);
    for (Identifier id : symbolsCopyThen.keySet()) {
      if ((knownSymbols.containsKey(id)) && 
        (!knownSymbols.get(id).equals(symbolsCopyThen.get(id)))) {
        if (symbolsCopyThen.get(id).equals(symbolsCopyElse.get(id)))
        {
          knownSymbols.put(id, symbolsCopyThen.get(id));
          symbolsCopyElse.remove(id);
        }
        else
        {
          remove.add(id);
          symbolsCopyElse.remove(id);
        }
      }
    }
    for (Identifier id : symbolsCopyElse.keySet()) {
      if ((knownSymbols.containsKey(id)) && 
        (!knownSymbols.get(id).equals(symbolsCopyThen.get(id)))) {
        remove.add(id);
      }
    }
    for (Identifier id : remove) {
      knownSymbols.remove(id);
    }
  }
  
  protected Set<Identifier> removeLoopCarriedSymbols(Loop l)
  {
    Statement body = l.getBody();
    DepthFirstIterator iter = new DepthFirstIterator(body);
    iter.pruneOn(Loop.class);
    iter.pruneOn(Expression.class);
    
    CppcStatement cppcStatement = null;
    Set<Identifier> consumed = new HashSet();
    Set<Identifier> generated = new HashSet();
    SetOperations<Identifier> setOps = new SetOperations();
    while (iter.hasNext())
    {
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e)
      {
        continue;
      }
      Statement stmt = cppcStatement.getStatement();
      if (!(stmt instanceof Loop))
      {
        DepthFirstIterator stmtIter = new DepthFirstIterator(stmt);
        stmtIter.next();
        stmtIter.pruneOn(CppcStatement.class);
        while (stmtIter.hasNext()) {
          try
          {
            FunctionCall call = (FunctionCall)stmtIter.next(FunctionCall.class);
            if (!CppcRegisterManager.isRegistered((Identifier)call.getName())) {
              for (int i = 0; i < call.getNumArguments(); i++)
              {
                Expression arg = call.getArgument(i);
                if (((arg instanceof Identifier)) && 
                  (knownSymbols.containsKey((Identifier)arg))) {
                  knownSymbols.remove((Identifier)arg);
                }
              }
            }
          }
          catch (NoSuchElementException localNoSuchElementException1) {}
        }
        consumed.addAll(setOps.setMinus(cppcStatement.getConsumed(), 
          generated));
        generated.addAll(cppcStatement.getGenerated());
      }
    }
    Set<Identifier> carried = setOps.setIntersection(
      consumed, generated);
    Set<Identifier> keys = knownSymbols.keySet();
    Set<Identifier> carriedSymbols = setOps.setIntersection(carried, keys);
    
    return carriedSymbols;
  }
  
  protected void calculateCarriedSymbols(Identifier loopVar, IfStatement s, Set<Identifier> carriedSymbols)
  {
    Expression control = SymbolicExpressionAnalyzer.analyzeExpression(
      s.getControlExpression(), knownSymbols);
    Set<Identifier> generatedInIf = new HashSet();
    
    DepthFirstIterator iter = new DepthFirstIterator(s);
    boolean relevant = false;
    while ((iter.hasNext()) && (!relevant)) {
      try
      {
        CppcStatement cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        for (Identifier id : cppcStatement.getGenerated()) {
          if (carriedSymbols.contains(id)) {
            generatedInIf.add(id);
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    if (generatedInIf.isEmpty()) {
      return;
    }
    Expression trueLiteral = 
      SymbolicExpressionAnalyzer.instance.buildBooleanLiteral(true);
    if ((control instanceof MultiExpression))
    {
      MultiExpression safeControl = (MultiExpression)control;
      Object generatedEvolution = new HashMap();
      for (Identifier id : generatedInIf)
      {
        MultiExpression mexpr = new MultiExpression(safeControl.getVar());
        if (knownSymbols.containsKey(id))
        {
          Expression firstIterationValue = 
            safeControl.getKeyOf(
            (Expression)safeControl.getChildren().get(0));
          mexpr.addExpression(firstIterationValue, (Expression)
            ((Expression)((List)knownSymbols.get(id)).get(0)).clone());
          ((Map)generatedEvolution).put(id, mexpr);
        }
        else
        {
          generatedInIf.remove(id);
        }
      }
      Map localKnown = new HashMap(knownSymbols);
      Map oldKnown = knownSymbols;
      knownSymbols = localKnown;
      Expression controlValue;
      for (int i = 0; i < control.getChildren().size() - 1; i++)
      {
        controlValue = (Expression)control.getChildren().get(i);
        Statement branch = null;
        if (!(controlValue instanceof Literal)) {
          return;
        }
        if (trueLiteral.equals(controlValue)) {
          branch = s.getThenStatement();
        } else {
          branch = s.getElseStatement();
        }
        if (branch != null) {
          analyzeStatement((CppcStatement)branch, localKnown);
        }
        for (Identifier id : generatedInIf)
        {
          MultiExpression idEvolution = 
            (MultiExpression)((Map)generatedEvolution).get(id);
          Expression value = (Expression)((List)localKnown.get(id)).get(0);
          Expression iterationValue = safeControl.getKeyOf(
            (Expression)control.getChildren().get(i + 1));
          idEvolution.addExpression(iterationValue, value);
        }
      }
      knownSymbols = oldKnown;
      for (Identifier id : generatedInIf)
      {
        List values = new ArrayList(1);
        values.add(((Map)generatedEvolution).get(id));
        knownSymbols.put(id, values);
        System.out.println("Added induction variable " + id + ": " + values);
      }
    }
    else
    {
      if (!(control instanceof Literal)) {
        return;
      }
      Statement branch = null;
      if (trueLiteral.equals(control)) {
        branch = s.getThenStatement();
      } else {
        branch = s.getElseStatement();
      }
      if (branch != null) {
        calculateCarriedSymbols(loopVar, branch, generatedInIf);
      }
    }
  }
  
  protected void calculateCarriedSymbols(Identifier loopVar, Statement body, Set<Identifier> carriedSymbols)
  {
    DepthFirstIterator bodyIter;
    for (Iterator localIterator1 = carriedSymbols.iterator(); localIterator1.hasNext(); bodyIter.hasNext())
    {
      Identifier carried = (Identifier)localIterator1.next();
      bodyIter = new DepthFirstIterator(body);
      bodyIter.pruneOn(IfStatement.class);
      continue;
      try
      {
        CppcStatement cppcStatement = (CppcStatement)bodyIter.next(
          CppcStatement.class);
        if ((cppcStatement.getStatement() instanceof IfStatement))
        {
          calculateCarriedSymbols(loopVar, 
            (IfStatement)cppcStatement.getStatement(), carriedSymbols);
        }
        else if (cppcStatement.getGenerated().contains(carried))
        {
          Map localKnown = new HashMap();
          for (Identifier consumed : cppcStatement.getConsumed()) {
            if (knownSymbols.containsKey(consumed)) {
              localKnown.put(consumed, knownSymbols.get(consumed));
            }
          }
          if ((knownSymbols.containsKey(loopVar)) && 
            (((List)knownSymbols.get(carried)).size() == 1))
          {
            Expression values = simulateIteration(
              (MultiExpression)((List)knownSymbols.get(loopVar)).get(0), localKnown, 
              loopVar, carried, cppcStatement);
            Object value = new ArrayList(1);
            ((List)value).add(values);
            knownSymbols.put(carried, value);
          }
          else
          {
            knownSymbols.remove(carried);
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
  
  private Expression simulateIteration(MultiExpression iterValues, Map localKnown, Identifier loopVar, Identifier carried, CppcStatement statement)
  {
    MultiExpression carriedMexpr = new MultiExpression(loopVar);
    Expression firstIterationValue = 
      (Expression)((List)localKnown.get(carried)).get(0);
    
    carriedMexpr.addExpression(iterValues.getKeyOf(
      (Expression)iterValues.getChildren().get(0)), 
      (Expression)firstIterationValue.clone());
    for (int i = 1; i < iterValues.getChildren().size(); i++)
    {
      Expression expr = (Expression)iterValues.getChildren().get(i);
      if ((expr instanceof MultiExpression))
      {
        List values = new ArrayList(1);
        values.add(expr);
        localKnown.put(loopVar, values);
        analyzeStatement(statement, localKnown);
        
        Expression value = simulateIteration((MultiExpression)expr, 
          localKnown, loopVar, carried, statement);
        carriedMexpr.addExpression(expr, (Expression)value.clone());
      }
      else
      {
        List values = new ArrayList(1);
        values.add(expr);
        localKnown.put(loopVar, values);
        analyzeStatement(statement, localKnown);
        Expression finalValue = 
          (Expression)((List)localKnown.get(carried)).get(0);
        
        carriedMexpr.addExpression(expr, (Expression)finalValue.clone());
      }
    }
    return carriedMexpr;
  }
  
  protected void analyzeStatement(WhileLoop l)
  {
    removeLoopCarriedSymbols(l);
    SymbolicExpressionAnalyzer.analyzeExpression(l.getCondition(), 
      knownSymbols);
    analyzeStatement((CppcStatement)l.getBody(), knownSymbols);
  }
  
  protected static void addKnownSymbol(Map knownSymbols, Identifier id, List values)
  {
    addKnownSymbol(knownSymbols, id, values, id);
  }
  
  private static Expression convertToInt(Expression value)
  {
    if (value == null) {
      return null;
    }
    if ((value instanceof MultiExpression))
    {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression(safeValue.getVar());
      for (Expression key : safeValue.getValueSet()) {
        mexpr.addExpression(key, convertToInt(
          safeValue.getValue(key)));
      }
      return mexpr;
    }
    if ((value instanceof DoubleLiteral)) {
      return new IntegerLiteral((int)((DoubleLiteral)value).getValue());
    }
    if ((value instanceof FloatLiteral)) {
      return new IntegerLiteral((int)((FloatLiteral)value).getValue());
    }
    return (Expression)value.clone();
  }
  
  private static List convertToInt(List values)
  {
    List convertedValues = new ArrayList(values.size());
    for (int i = 0; i < values.size(); i++) {
      if ((values.get(i) instanceof List)) {
        convertedValues.add(convertToInt(
          (List)values.get(i)));
      } else {
        convertedValues.add(convertToInt(
          (Expression)values.get(i)));
      }
    }
    return convertedValues;
  }
  
  private static Expression convertToFloat(Expression value)
  {
    if (value == null) {
      return null;
    }
    if ((value instanceof MultiExpression))
    {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression(safeValue.getVar());
      for (Expression key : safeValue.getValueSet()) {
        mexpr.addExpression(key, convertToFloat(
          safeValue.getValue(key)));
      }
      return mexpr;
    }
    if ((value instanceof IntegerLiteral)) {
      return new FloatLiteral(((IntegerLiteral)value).getValue());
    }
    if ((value instanceof DoubleLiteral)) {
      return new FloatLiteral(((DoubleLiteral)value).getValue());
    }
    return (Expression)value.clone();
  }
  
  private static List convertToFloat(List values)
  {
    List convertedValues = new ArrayList(values.size());
    for (int i = 0; i < values.size(); i++) {
      if ((values.get(i) instanceof List)) {
        convertedValues.add(convertToFloat(
          (List)values.get(i)));
      } else {
        convertedValues.add(convertToFloat(
          (Expression)values.get(i)));
      }
    }
    return convertedValues;
  }
  
  private static Expression convertToDouble(Expression value)
  {
    if (value == null) {
      return null;
    }
    if ((value instanceof MultiExpression))
    {
      MultiExpression safeValue = (MultiExpression)value;
      MultiExpression mexpr = new MultiExpression(safeValue.getVar());
      for (Expression key : safeValue.getValueSet()) {
        mexpr.addExpression(key, convertToDouble(
          safeValue.getValue(key)));
      }
      return mexpr;
    }
    if ((value instanceof IntegerLiteral)) {
      return new DoubleLiteral(((IntegerLiteral)value).getValue());
    }
    if ((value instanceof FloatLiteral)) {
      return new DoubleLiteral(((FloatLiteral)value).getValue());
    }
    return (Expression)value.clone();
  }
  
  private static List convertToDouble(List values)
  {
    List convertedValues = new ArrayList(values.size());
    for (int i = 0; i < values.size(); i++) {
      if ((values.get(i) instanceof List)) {
        convertedValues.add(convertToDouble(
          (List)values.get(i)));
      } else {
        convertedValues.add(convertToDouble(
          (Expression)values.get(i)));
      }
    }
    return convertedValues;
  }
  
  protected static void addKnownSymbol(Map knownSymbols, Identifier id, List values, Traversable ref)
  {
    VariableDeclaration vd = null;
    try
    {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        ref, id);
    }
    catch (SymbolNotDefinedException e)
    {
      return;
    }
    catch (SymbolIsNotVariableException e)
    {
      return;
    }
    if (vd == null) {
      return;
    }
    List symbolValues = values;
    if (vd.getSpecifiers().contains(Specifier.INT)) {
      symbolValues = convertToInt(values);
    }
    if (vd.getSpecifiers().contains(Specifier.FLOAT)) {
      symbolValues = convertToFloat(values);
    }
    if (vd.getSpecifiers().contains(Specifier.DOUBLE)) {
      symbolValues = convertToDouble(values);
    }
    putKnownSymbol(knownSymbols, id, symbolValues);
  }
  
  private static boolean checkRecurrency(Identifier id, List values)
  {
    for (int i = 0; i < values.size(); i++) {
      if ((values.get(i) instanceof List))
      {
        if (checkRecurrency(id, (List)values.get(i))) {
          return true;
        }
      }
      else
      {
        Expression expr = (Expression)values.get(i);
        if (expr != null)
        {
          DepthFirstIterator iter = new DepthFirstIterator(expr);
          while (iter.hasNext()) {
            try
            {
              Identifier innerId = (Identifier)iter.next(Identifier.class);
              if (innerId.equals(id))
              {
                System.out.println("Avoiding register of " + id + " = " + 
                  values);
                return true;
              }
            }
            catch (NoSuchElementException localNoSuchElementException) {}
          }
        }
      }
    }
    return false;
  }
  
  private static void putMaskedSymbol(MultiExpression mexpr, Expression value, int depth)
  {
    if (!mexpr.getVar().equals(idMask.elementAt(depth)))
    {
      System.err.println("ERROR: IDMASK STACK BUG");
      System.exit(0);
    }
    Expression key = (Expression)valueMask.elementAt(depth);
    if (depth == idMask.size() - 1)
    {
      mexpr.addExpression(key, value);
      return;
    }
    Expression nextLevel = mexpr.getValue(key);
    if (!(nextLevel instanceof MultiExpression))
    {
      nextLevel = new MultiExpression(
        (Identifier)idMask.elementAt(depth + 1));
      mexpr.addExpression(key, nextLevel);
    }
    putMaskedSymbol((MultiExpression)nextLevel, value, 
      ++depth);
  }
  
  private static void putKnownSymbol(Map knownSymbols, Identifier id, List values)
  {
    if (idMask.isEmpty())
    {
      knownSymbols.put(id, values);
      return;
    }
    List currentValue = (List)knownSymbols.get(id);
    if ((currentValue != null) && 
      (currentValue.size() == 1))
    {
      Expression value = (Expression)currentValue.get(0);
      if ((value instanceof MultiExpression))
      {
        putMaskedSymbol((MultiExpression)value, 
          (Expression)((Expression)values.get(0)).clone(), 0);
        return;
      }
    }
    MultiExpression mexpr = new MultiExpression(
      (Identifier)idMask.elementAt(0));
    putMaskedSymbol(mexpr, 
      (Expression)((Expression)values.get(0)).clone(), 0);
    
    values = new ArrayList(1);
    values.add(mexpr);
    knownSymbols.put(id, values);
  }
  
  protected void analyzeStatement(Label label)
  {
    if (removeUponLabel.containsKey(label.getName()))
    {
      for (Identifier id : (Set)removeUponLabel.get(label.getName())) {
        knownSymbols.remove(id);
      }
      removeUponLabel.remove(label.getName());
    }
    Identifier target = label.getName();
    Procedure proc = label.getProcedure();
    DepthFirstIterator iter = new DepthFirstIterator(proc);
    iter.pruneOn(Expression.class);
    
    CppcStatement cppcStatement = null;
    boolean stop = false;
    while (!stop) {
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
        if (cppcStatement.getStatement() == label) {
          stop = true;
        }
      }
      catch (NoSuchElementException e)
      {
        System.err.println("ERROR: Matching label not found. At cppc.compiler.analysis.SymbolicAnalyzer.analyzeStatement( cetus.hir.Label )");
        
        System.exit(0);
      }
    }
    Set<Identifier> remove = new HashSet();
    Set<Identifier> removeInc = new HashSet(knownSymbols.size());
    while (iter.hasNext()) {
      try
      {
        cppcStatement = (CppcStatement)iter.next(
          CppcStatement.class);
        for (Identifier id : cppcStatement.getGenerated()) {
          if (knownSymbols.containsKey(id)) {
            removeInc.add(id);
          }
        }
        if ((cppcStatement.getStatement() instanceof GotoStatement))
        {
          GotoStatement gotoStmt = (GotoStatement)cppcStatement.getStatement();
          if (gotoStmt.getValue().equals(target))
          {
            remove.addAll(removeInc);
            removeInc.clear();
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException1) {}
    }
    for (Identifier id : remove) {
      knownSymbols.remove(id);
    }
  }
  
  protected void analyzeStatement(NullStatement nullStatement) {}
  
  protected void analyzeStatement(CppcNonportableFunctionMark s) {}
}
