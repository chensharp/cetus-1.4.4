package cppc.compiler.analysis;

import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.BooleanLiteral;
import cetus.hir.CharLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.DepthFirstIterator;
import cetus.hir.EscapeLiteral;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.Traversable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.VariableSizeAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.util.dispatcher.FunctionDispatcher;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public abstract class SymbolicExpressionAnalyzer
  extends FunctionDispatcher<Expression>
{
  private static Class instanceClass;
  protected static SymbolicExpressionAnalyzer instance;
  protected static Map knownSymbols = null;
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption(
        "CPPC/Analysis/SymbolicExpressionAnalyzer/ClassName"));
      instance = (SymbolicExpressionAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  protected static Expression filterMultiExpression(Expression expr)
  {
    if (!(expr instanceof MultiExpression)) {
      return expr;
    }
    MultiExpression mexpr = (MultiExpression)expr;
    Expression one = (Expression)mexpr.getChildren().get(0);
    if (mexpr.getChildren().size() == 1) {
      return (Expression)one.clone();
    }
    for (Expression value : mexpr.getChildren()) {
      if (!value.equals(one)) {
        return mexpr;
      }
    }
    one.setParent(mexpr.getParent());
    return (Expression)one.clone();
  }
  
  public static Expression analyzeExpression(Expression expr, Map knownSymbols)
  {
    Method m = instance.dispatch(expr, "analyzeExpression");
    if (m == null)
    {
      System.err.println("WARNING: cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression not implemented for " + 
      
        expr.getClass());
      
      return null;
    }
    Map oldSymbols = null;
    if (knownSymbols != knownSymbols)
    {
      oldSymbols = knownSymbols;
      knownSymbols = knownSymbols;
    }
    try
    {
      Expression ret = (Expression)m.invoke(instance, new Object[] { expr });
      
      return filterMultiExpression(ret);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    finally
    {
      if (oldSymbols != null) {
        knownSymbols = oldSymbols;
      }
    }
    return null;
  }
  
  private List<List<Expression>> expandIndexes(List<List<? extends Expression>> indexes)
  {
    int indexesSize = 1;
    for (List<? extends Expression> index : indexes) {
      indexesSize *= index.size();
    }
    List<List<Expression>> expandedIndexes = 
      new ArrayList(indexesSize);
    for (int i = indexes.size() - 1; i >= 0; i--)
    {
      List<? extends Expression> index = (List)indexes.get(i);
      List<Expression> expandedIndex;
      if (expandedIndexes.isEmpty())
      {
        for (Expression indexPart : index)
        {
          expandedIndex = 
            new ArrayList(index.size());
          if ((indexPart instanceof UnfoldedExpression)) {
            expandedIndex.add(((UnfoldedExpression)indexPart).getExpression());
          } else {
            expandedIndex.add(indexPart);
          }
          expandedIndexes.add(expandedIndex);
        }
      }
      else
      {
        List<List<Expression>> masterClone = 
          (List)((ArrayList)expandedIndexes).clone();
        expandedIndexes.clear();
        Iterator localIterator3;
        for (expandedIndex = index.iterator(); expandedIndex.hasNext(); localIterator3.hasNext())
        {
          Expression indexPart = (Expression)expandedIndex.next();
          localIterator3 = masterClone.iterator(); continue;List<Expression> expandedIndex = (List)localIterator3.next();
          List<Expression> clone = (List)
            ((ArrayList)expandedIndex).clone();
          if ((indexPart instanceof UnfoldedExpression)) {
            clone.add(0, ((UnfoldedExpression)indexPart).getExpression());
          } else {
            clone.add(0, indexPart);
          }
          expandedIndexes.add(clone);
        }
      }
    }
    return expandedIndexes;
  }
  
  protected Expression analyzeExpression(ArrayAccess expr)
  {
    List<Expression> foldedIndexes = foldArrayIndexes(expr);
    if (foldedIndexes == null) {
      return (Expression)expr.clone();
    }
    ArrayAccess foldedAccess = new ArrayAccess(
      (Expression)expr.getArrayName().clone(), foldedIndexes);
    foldedAccess.setParent(expr.getParent());
    Expression folded = foldArrayValue(foldedAccess);
    if (folded != null) {
      return folded;
    }
    foldedAccess.setParent(null);
    return foldedAccess;
  }
  
  protected List<List<Expression>> foldArrayIndexes(ArrayAccess expr, List<List<Expression>> indexes)
  {
    List<List<Expression>> foldedIndexes = new ArrayList(
      indexes.size());
    for (List<Expression> index : indexes)
    {
      ArrayAccess virtual = new ArrayAccess(
        (Identifier)expr.getArrayName().clone(), 
        index);
      virtual.setParent(expr.getParent());
      foldedIndexes.add(foldArrayIndexes(virtual));
    }
    return foldedIndexes;
  }
  
  protected List<Expression> foldArrayIndexes(ArrayAccess expr)
  {
    List<Expression> foldedIndexes = new ArrayList(
      expr.getNumIndices());
    for (int i = 0; i < expr.getNumIndices(); i++)
    {
      Expression foldedIndex = analyzeExpression(expr.getIndex(i), 
        knownSymbols);
      if ((foldedIndex instanceof MultiExpression)) {
        for (int j = 0; j < foldedIndex.getChildren().size(); j++) {
          if (!(foldedIndex.getChildren().get(j) instanceof IntegerLiteral)) {
            return null;
          }
        }
      } else if (!(foldedIndex instanceof IntegerLiteral)) {
        return null;
      }
      foldedIndexes.add(foldedIndex);
    }
    return foldedIndexes;
  }
  
  protected List<List<Expression>> convertArrayIndexes(ArrayAccess expr, List<List<Expression>> indexes)
  {
    List<List<Expression>> convertedIndexes = new ArrayList(
      indexes.size());
    
    long iter = 0L;
    for (List<Expression> index : indexes)
    {
      ArrayAccess virtual = new ArrayAccess(
        (Identifier)expr.getArrayName().clone(), 
        index);
      virtual.setParent(expr.getParent());
      convertedIndexes.add(convertArrayIndexes(virtual));
      iter += 1L;
    }
    return convertedIndexes;
  }
  
  protected List<Expression> convertArrayIndexes(ArrayAccess expr)
  {
    List<Expression> convertedIndexes = new ArrayList(
      expr.getNumIndices());
    for (int i = 0; i < expr.getNumIndices(); i++) {
      convertedIndexes.add(expr.getIndex(i));
    }
    return convertedIndexes;
  }
  
  protected abstract Expression foldArrayValue(ArrayAccess paramArrayAccess);
  
  private List<ArrayAccess> unfoldAccess(ArrayAccess access)
  {
    int totalSize = 1;
    for (int i = 0; i < access.getNumIndices(); i++) {
      if ((access.getIndex(i) instanceof CommaExpression)) {
        totalSize = totalSize * ((CommaExpression)access.getIndex(i)).getChildren().size();
      }
    }
    List<ArrayAccess> virtualAccesses = new ArrayList(totalSize);
    if (totalSize != 1) {
      for (int i = 0; i < access.getNumIndices(); i++)
      {
        Expression index = access.getIndex(i);
        if ((index instanceof CommaExpression))
        {
          List<Expression> virtualIndexes = new ArrayList(
            access.getNumIndices());
          for (int j = 0; j < access.getNumIndices(); j++) {
            virtualIndexes.add(access.getIndex(j));
          }
          Iterator localIterator = ((CommaExpression)index).getChildren().iterator();
          while (localIterator.hasNext())
          {
            Expression exprIndex = (Expression)localIterator.next();
            
            virtualIndexes.set(i, exprIndex);
            ArrayAccess virtual = new ArrayAccess(
              (Identifier)access.getArrayName().clone(), virtualIndexes);
            virtualAccesses.addAll(unfoldAccess(virtual));
          }
          return virtualAccesses;
        }
      }
    }
    virtualAccesses.add(access);
    return virtualAccesses;
  }
  
  protected static List<UnfoldedExpression> unfoldMultiExpressions(Expression expr)
  {
    DepthFirstIterator iter = new DepthFirstIterator(expr);
    Identifier var = null;
    MultiExpression mexpr = null;
    while (iter.hasNext()) {
      try
      {
        mexpr = (MultiExpression)iter.next(MultiExpression.class);
        var = mexpr.getVar();
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    if (var == null) {
      return null;
    }
    List<UnfoldedExpression> returnList = new ArrayList(
      mexpr.getChildren().size());
    
    Expression clone = (Expression)expr.clone();
    clone.setParent(expr.getParent());
    iter = new DepthFirstIterator(clone);
    List<Expression> swaps = new ArrayList();
    while (iter.hasNext()) {
      try
      {
        MultiExpression innerMexpr = (MultiExpression)iter.next(
          MultiExpression.class);
        if (innerMexpr.getVar().equals(var)) {
          swaps.add(innerMexpr);
        }
      }
      catch (NoSuchElementException localNoSuchElementException1) {}
    }
    List<Expression> originalSwaps = new ArrayList(swaps);
    List<Expression> newSwaps = new ArrayList(swaps.size());
    for (Expression key : mexpr.getValueSet())
    {
      for (int i = 0; i < originalSwaps.size(); i++)
      {
        Expression swapIn = (Expression)
          ((MultiExpression)originalSwaps.get(i)).getValue(key).clone();
        Expression swap = (Expression)swaps.get(i);
        if (clone == swap) {
          clone = swapIn;
        } else {
          swap.swapWith(swapIn);
        }
        newSwaps.add(swapIn);
      }
      swaps = newSwaps;
      newSwaps = new ArrayList(swaps.size());
      Traversable parent = clone.getParent();
      clone.setParent(null);
      UnfoldedExpression unfolded = new UnfoldedExpression(var, key, 
        (Expression)clone.clone());
      clone.setParent(parent);
      unfolded.setParent(parent);
      returnList.add(unfolded);
    }
    return returnList;
  }
  
  private void assignToList(List list, Literal value)
  {
    for (int i = 0; i < list.size(); i++) {
      if ((list.get(i) instanceof List)) {
        assignToList((List)list.get(i), value);
      } else {
        list.set(i, value);
      }
    }
  }
  
  private void fastArrayAssignment(ArrayAccess access, List<Expression> indexes, Literal rhsValue)
  {
    List<List<? extends Expression>> unfoldedIndexes = 
      new ArrayList(indexes.size());
    List<UnfoldedExpression> unfoldedIndex;
    for (Expression index : indexes)
    {
      unfoldedIndex = unfoldMultiExpressions(
        index);
      if (unfoldedIndex == null)
      {
        List<Expression> thisIndex = new ArrayList(1);
        thisIndex.add(index);
        unfoldedIndexes.add(thisIndex);
      }
      else
      {
        unfoldedIndexes.add(unfoldedIndex);
      }
    }
    int indicesSize = 1;
    for (Object unfoldedIndex : unfoldedIndexes) {
      indicesSize *= ((List)unfoldedIndex).size();
    }
    Expression totalSize = VariableSizeAnalizerFactory.getAnalizer().getSize(
      (Identifier)access.getArrayName(), access);
    int intSize = 0;
    if (totalSize == null)
    {
      intSize = 1;
    }
    else
    {
      totalSize = analyzeExpression(totalSize, knownSymbols);
      if ((totalSize instanceof IntegerLiteral)) {
        intSize = (int)((IntegerLiteral)totalSize).getValue();
      }
    }
    if (indicesSize == intSize)
    {
      List values = (List)knownSymbols.get((Identifier)access.getArrayName());
      assignToList(values, rhsValue);
      return;
    }
    List<List<Expression>> indices = expandIndexes(unfoldedIndexes);
    
    List<List<Expression>> foldedIndexes = convertArrayIndexes(access, 
      indices);
    
    List values = (List)knownSymbols.get((Identifier)access.getArrayName());
    for (List<Expression> foldedIndex : foldedIndexes)
    {
      List innerValues = values;
      for (int i = 0; i < foldedIndex.size() - 1; i++)
      {
        int iindex = (int)((IntegerLiteral)foldedIndex.get(i)).getValue();
        innerValues = (List)innerValues.get(iindex);
      }
      int iindex = 
        (int)((IntegerLiteral)foldedIndex.get(foldedIndex.size() - 1)).getValue();
      innerValues.set(iindex, rhsValue);
    }
  }
  
  protected Expression analyzeExpression(AssignmentExpression expr)
  {
    Expression rhsValue = analyzeExpression(expr.getRHS(), knownSymbols);
    if ((!(rhsValue instanceof Literal)) && 
      (!(rhsValue instanceof MultiExpression))) {
      return (Expression)expr.getRHS().clone();
    }
    List<UnfoldedExpression> unfoldedExpressions;
    if ((expr.getLHS() instanceof ArrayAccess))
    {
      ArrayAccess access = (ArrayAccess)expr.getLHS();
      List<Expression> foldedIndexes = foldArrayIndexes(access);
      if (foldedIndexes == null) {
        return (Expression)expr.clone();
      }
      if (!knownSymbols.containsKey(access.getArrayName())) {
        if (!initializeArrayValue(access)) {
          return (Expression)expr.clone();
        }
      }
      if ((rhsValue instanceof Literal)) {
        for (Expression foldedIndex : foldedIndexes) {
          if ((foldedIndex instanceof MultiExpression))
          {
            fastArrayAssignment(access, foldedIndexes, 
              (Literal)rhsValue);
            return (Expression)expr.clone();
          }
        }
      }
      ArrayAccess virtualAccess = new ArrayAccess(
        (Identifier)access.getArrayName().clone(), foldedIndexes);
      
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        virtualAccess, AssignmentOperator.NORMAL, rhsValue);
      virtualAssignment.setParent(expr.getParent());
      
      unfoldedExpressions = 
        unfoldMultiExpressions(virtualAssignment);
      if (unfoldedExpressions == null)
      {
        foldedIndexes = convertArrayIndexes(virtualAccess);
        
        List values = (List)knownSymbols.get(access.getArrayName());
        for (int i = 0; i < foldedIndexes.size() - 1; i++)
        {
          int iindex = 
            (int)((IntegerLiteral)foldedIndexes.get(i)).getValue();
          try
          {
            values = (List)values.get(iindex);
          }
          catch (ClassCastException e)
          {
            return (Expression)expr.clone();
          }
        }
        int iindex = 
          (int)((IntegerLiteral)foldedIndexes.get(foldedIndexes.size() - 1)).getValue();
        values.set(iindex, rhsValue);
      }
      else
      {
        Identifier multiExpressionVar = ((UnfoldedExpression)unfoldedExpressions.get(0)).getVar();
        MultiExpression mexpr = new MultiExpression(multiExpressionVar);
        for (UnfoldedExpression unfolded : unfoldedExpressions) {
          mexpr.addExpression(unfolded.getVarValue(), analyzeExpression(
            (AssignmentExpression)unfolded.getExpression()));
        }
        if (multiExpressionVar.toString().equals("NODE"))
        {
          foldedIndexes = convertArrayIndexes(virtualAccess);
          List values = (List)knownSymbols.get(access.getArrayName());
          for (int i = 0; i < foldedIndexes.size() - 1; i++)
          {
            int iindex = 
              (int)((IntegerLiteral)foldedIndexes.get(i)).getValue();
            values = (List)values.get(iindex);
          }
          int iindex = 
            (int)((IntegerLiteral)foldedIndexes.get(foldedIndexes.size() - 1)).getValue();
          values.set(iindex, mexpr);
        }
      }
    }
    else if ((expr.getLHS() instanceof Identifier))
    {
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        (Expression)expr.getLHS().clone(), expr.getOperator(), rhsValue);
      virtualAssignment.setParent(expr.getParent());
      List<UnfoldedExpression> unfoldedExpressions = 
        unfoldMultiExpressions(virtualAssignment);
      if (unfoldedExpressions != null)
      {
        MultiExpression mexpr = new MultiExpression(
          ((UnfoldedExpression)unfoldedExpressions.get(0)).getVar());
        for (UnfoldedExpression unfolded : unfoldedExpressions) {
          mexpr.addExpression(unfolded.getVarValue(), analyzeExpression(
            (AssignmentExpression)unfolded.getExpression()));
        }
        Object values = new ArrayList(1);
        ((List)values).add(mexpr);
        SymbolicAnalyzer.addKnownSymbol(knownSymbols, 
          (Identifier)expr.getLHS(), (List)values);
      }
      else
      {
        List<Expression> values = new ArrayList(1);
        values.add(rhsValue);
        SymbolicAnalyzer.addKnownSymbol(knownSymbols, 
          (Identifier)expr.getLHS(), values);
      }
    }
    rhsValue.setParent(null);
    return rhsValue;
  }
  
  protected Expression analyzeExpression(BinaryExpression expr)
  {
    Expression foldedLHS = analyzeExpression(expr.getLHS(), 
      knownSymbols);
    
    Expression foldedRHS = analyzeExpression(expr.getRHS(), 
      knownSymbols);
    
    BinaryExpression virtual = new BinaryExpression(foldedLHS, 
      expr.getOperator(), foldedRHS);
    virtual.setParent(expr.getParent());
    
    List<UnfoldedExpression> unfoldedExpressions = unfoldMultiExpressions(
      virtual);
    if (unfoldedExpressions != null)
    {
      MultiExpression mexpr = new MultiExpression(
        ((UnfoldedExpression)unfoldedExpressions.get(0)).getVar());
      for (UnfoldedExpression unfolded : unfoldedExpressions) {
        mexpr.addExpression(unfolded.getVarValue(), analyzeExpression(
          (BinaryExpression)unfolded.getExpression()));
      }
      return mexpr;
    }
    if ((!(foldedLHS instanceof Literal)) || 
      (!(foldedRHS instanceof Literal))) {
      return new BinaryExpression(foldedLHS, expr.getOperator(), foldedRHS);
    }
    BinaryOperator operator = expr.getOperator();
    Expression newExpr = null;
    if (operator.equals(BinaryOperator.DIVIDE))
    {
      double lhsValue = numericAsDouble(foldedLHS);
      double rhsValue = numericAsDouble(foldedRHS);
      double opValue = lhsValue / rhsValue;
      
      return binaryOperationResult(foldedLHS, foldedRHS, opValue);
    }
    if (operator.equals(BinaryOperator.MULTIPLY))
    {
      double lhsValue = numericAsDouble(foldedLHS);
      double rhsValue = numericAsDouble(foldedRHS);
      double opValue = lhsValue * rhsValue;
      
      return binaryOperationResult(foldedLHS, foldedRHS, opValue);
    }
    if (operator.equals(BinaryOperator.COMPARE_EQ))
    {
      newExpr = buildBooleanLiteral(foldedLHS.equals(foldedRHS));
      return newExpr;
    }
    if (operator.equals(BinaryOperator.COMPARE_NE))
    {
      newExpr = buildBooleanLiteral(!foldedLHS.equals(foldedRHS));
      return newExpr;
    }
    if (operator.equals(BinaryOperator.COMPARE_GE))
    {
      double lhs = numericAsDouble(foldedLHS);
      double rhs = numericAsDouble(foldedRHS);
      
      newExpr = buildBooleanLiteral(lhs >= rhs);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.COMPARE_GT))
    {
      double lhs = numericAsDouble(foldedLHS);
      double rhs = numericAsDouble(foldedRHS);
      
      newExpr = buildBooleanLiteral(lhs > rhs);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.COMPARE_LE))
    {
      double lhs = numericAsDouble(foldedLHS);
      double rhs = numericAsDouble(foldedRHS);
      
      newExpr = buildBooleanLiteral(lhs <= rhs);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.COMPARE_LT))
    {
      double lhs = numericAsDouble(foldedLHS);
      double rhs = numericAsDouble(foldedRHS);
      
      newExpr = buildBooleanLiteral(lhs < rhs);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.LOGICAL_AND))
    {
      newExpr = applyLogicalAnd(foldedLHS, foldedRHS);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.LOGICAL_OR))
    {
      newExpr = applyLogicalOr(foldedLHS, foldedRHS);
      return newExpr;
    }
    if (operator.equals(BinaryOperator.ADD))
    {
      double lhsValue = numericAsDouble(foldedLHS);
      double rhsValue = numericAsDouble(foldedRHS);
      double opValue = lhsValue + rhsValue;
      
      return binaryOperationResult(foldedLHS, foldedRHS, opValue);
    }
    if (operator.equals(BinaryOperator.SUBTRACT))
    {
      double lhsValue = numericAsDouble(foldedLHS);
      double rhsValue = numericAsDouble(foldedRHS);
      double opValue = lhsValue - rhsValue;
      
      return binaryOperationResult(foldedLHS, foldedRHS, opValue);
    }
    System.err.println("WARNING: cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression( BinaryExpression ) not implemented for BinaryOperator: " + 
    
      operator);
    return (Expression)expr.clone();
  }
  
  protected Expression analyzeExpression(CharLiteral charLiteral)
  {
    return (Expression)charLiteral.clone();
  }
  
  protected Expression analyzeExpression(CommaExpression expr)
  {
    List<Expression> foldedValues = new ArrayList(
      expr.getChildren().size());
    for (int i = 0; i < expr.getChildren().size(); i++) {
      foldedValues.add(analyzeExpression(
        (Expression)expr.getChildren().get(i), knownSymbols));
    }
    return new CommaExpression(foldedValues);
  }
  
  protected Expression analyzeExpression(DoubleLiteral doubleLiteral)
  {
    return (Expression)doubleLiteral.clone();
  }
  
  protected Expression analyzeExpression(EscapeLiteral escapeLiteral)
  {
    return (Expression)escapeLiteral.clone();
  }
  
  protected Expression analyzeExpression(FloatLiteral floatLiteral)
  {
    return (Expression)floatLiteral.clone();
  }
  
  protected Expression analyzeExpression(FunctionCall functionCall)
  {
    if ((!CppcRegisterManager.isRegistered((Identifier)functionCall.getName())) || 
    
      (CppcRegisterManager.getCharacterization((Identifier)functionCall.getName()).isNull())) {
      return (Expression)functionCall.clone();
    }
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)functionCall.getName());
    Expression expr;
    for (ProcedureParameter param : c.getGenerated())
    {
      expr = functionCall.getArgument(param.getPosition());
      if ((expr instanceof Identifier)) {
        knownSymbols.remove((Identifier)expr);
      }
    }
    Set<Identifier> remove = ObjectAnalizer.globalDeclarationsToSet(
      c.getGlobalGenerated());
    for (Identifier id : remove) {
      knownSymbols.remove(id);
    }
    return (Expression)functionCall.clone();
  }
  
  protected Expression analyzeExpression(Identifier identifier)
  {
    if (knownSymbols.containsKey(identifier))
    {
      List values = (List)knownSymbols.get(identifier);
      Expression folded = null;
      if (values.size() == 1)
      {
        folded = (Expression)((Expression)values.get(0)).clone();
      }
      else
      {
        List<Expression> clonedValues = new ArrayList(
          values.size());
        for (Expression expr : values) {
          clonedValues.add((Expression)expr.clone());
        }
        folded = new CommaExpression(clonedValues);
      }
      if (!SymbolicAnalyzer.idMask.isEmpty())
      {
        int depth = 0;
        while (((folded instanceof MultiExpression)) && (
          depth < SymbolicAnalyzer.idMask.size()))
        {
          MultiExpression mexpr = (MultiExpression)folded;
          if (mexpr.getVar().equals(SymbolicAnalyzer.idMask.elementAt(depth))) {
            folded = mexpr.getValue((Expression)SymbolicAnalyzer.valueMask.elementAt(
              depth));
          }
          depth++;
        }
        if (folded != null) {
          return (Expression)folded.clone();
        }
        return (Expression)identifier.clone();
      }
      return folded;
    }
    return (Expression)identifier.clone();
  }
  
  protected Expression analyzeExpression(BooleanLiteral booleanLiteral)
  {
    return (Expression)booleanLiteral.clone();
  }
  
  protected Expression analyzeExpression(IntegerLiteral integerLiteral)
  {
    return (Expression)integerLiteral.clone();
  }
  
  protected Expression analyzeExpression(MultiExpression mexpr)
  {
    MultiExpression folded = new MultiExpression(mexpr.getVar());
    for (Expression key : mexpr.getValueSet()) {
      folded.addExpression(key, analyzeExpression(mexpr.getValue(key), 
        knownSymbols));
    }
    return folded;
  }
  
  protected Expression analyzeExpression(StringLiteral stringLiteral)
  {
    return (Expression)stringLiteral.clone();
  }
  
  protected Expression analyzeExpression(UnaryExpression expr)
  {
    UnaryOperator operator = expr.getOperator();
    
    Expression folded = analyzeExpression(expr.getExpression(), 
      knownSymbols);
    
    UnaryExpression virtual = new UnaryExpression(operator, folded);
    virtual.setParent(expr.getParent());
    
    List<UnfoldedExpression> unfoldedExpressions = unfoldMultiExpressions(
      virtual);
    if (unfoldedExpressions != null)
    {
      MultiExpression mexpr = new MultiExpression(
        ((UnfoldedExpression)unfoldedExpressions.get(0)).getVar());
      for (UnfoldedExpression unfolded : unfoldedExpressions) {
        mexpr.addExpression(unfolded.getVarValue(), analyzeExpression(
          (UnaryExpression)unfolded.getExpression()));
      }
      return mexpr;
    }
    if (!(folded instanceof Literal)) {
      return new UnaryExpression(expr.getOperator(), folded);
    }
    Expression newExpr = null;
    if (operator.equals(UnaryOperator.LOGICAL_NEGATION))
    {
      newExpr = applyLogicalNegation(folded);
      return newExpr;
    }
    if (operator.equals(UnaryOperator.MINUS))
    {
      if ((folded instanceof IntegerLiteral))
      {
        IntegerLiteral safeOp = (IntegerLiteral)folded;
        newExpr = new IntegerLiteral(-safeOp.getValue());
        return newExpr;
      }
      if ((folded instanceof DoubleLiteral))
      {
        DoubleLiteral safeOp = (DoubleLiteral)folded;
        newExpr = new DoubleLiteral(-safeOp.getValue());
        return newExpr;
      }
      if ((folded instanceof FloatLiteral))
      {
        FloatLiteral safeOp = (FloatLiteral)folded;
        newExpr = new FloatLiteral(-safeOp.getValue());
        return newExpr;
      }
    }
    if ((operator.equals(UnaryOperator.DEREFERENCE)) || 
      (operator.equals(UnaryOperator.PLUS)))
    {
      System.out.println("Check how to fold with operator: " + operator + 
        ". Line: " + expr.getStatement().where());
      return (Expression)expr.clone();
    }
    System.err.println("WARNING: cppc.compiler.analysis.SymbolicExpressionAnalyzer.analyzeExpression( UnaryExpression ) not implemented for UnaryOperator: " + 
    
      operator);
    
    return new UnaryExpression(expr.getOperator(), folded);
  }
  
  protected Expression analyzeExpression(UnfoldedExpression expr)
  {
    Expression innerAnalyzed = analyzeExpression(expr.getExpression(), 
      knownSymbols);
    
    return new UnfoldedExpression(expr.getVar(), expr.getVarValue(), 
      innerAnalyzed);
  }
  
  protected static int numericAsInteger(Expression expr)
  {
    if ((expr instanceof IntegerLiteral)) {
      return (int)((IntegerLiteral)expr).getValue();
    }
    if ((expr instanceof DoubleLiteral)) {
      return (int)((DoubleLiteral)expr).getValue();
    }
    if ((expr instanceof FloatLiteral)) {
      return (int)((FloatLiteral)expr).getValue();
    }
    return 0;
  }
  
  protected static double numericAsDouble(Expression expr)
  {
    if ((expr instanceof IntegerLiteral))
    {
      IntegerLiteral safeExpr = (IntegerLiteral)expr;
      return safeExpr.getValue();
    }
    if ((expr instanceof DoubleLiteral))
    {
      DoubleLiteral safeExpr = (DoubleLiteral)expr;
      return safeExpr.getValue();
    }
    if ((expr instanceof FloatLiteral))
    {
      FloatLiteral safeExpr = (FloatLiteral)expr;
      return safeExpr.getValue();
    }
    return 0.0D;
  }
  
  protected static Expression convertToFloat(Expression value)
  {
    if (value == null) {
      return null;
    }
    if ((value instanceof IntegerLiteral)) {
      return new FloatLiteral(((IntegerLiteral)value).getValue());
    }
    if ((value instanceof DoubleLiteral)) {
      return new FloatLiteral(((DoubleLiteral)value).getValue());
    }
    return (Expression)value.clone();
  }
  
  protected static List convertToFloat(List values)
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
  
  protected static Expression convertToDouble(Expression value)
  {
    if (value == null) {
      return null;
    }
    if ((value instanceof IntegerLiteral)) {
      return new DoubleLiteral(((IntegerLiteral)value).getValue());
    }
    if ((value instanceof FloatLiteral)) {
      return new DoubleLiteral(((FloatLiteral)value).getValue());
    }
    return (Expression)value.clone();
  }
  
  protected static List convertToDouble(List values)
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
  
  protected Expression binaryOperationResult(Expression lhs, Expression rhs, double value)
  {
    if (((lhs instanceof DoubleLiteral)) || ((rhs instanceof DoubleLiteral))) {
      return new DoubleLiteral(value);
    }
    if (((lhs instanceof FloatLiteral)) || ((rhs instanceof FloatLiteral))) {
      return new FloatLiteral(value);
    }
    return new IntegerLiteral((int)value);
  }
  
  protected abstract Expression applyLogicalAnd(Expression paramExpression1, Expression paramExpression2);
  
  protected abstract Expression applyLogicalNegation(Expression paramExpression);
  
  protected abstract Expression applyLogicalOr(Expression paramExpression1, Expression paramExpression2);
  
  protected abstract Expression buildBooleanLiteral(boolean paramBoolean);
  
  protected abstract boolean initializeArrayValue(ArrayAccess paramArrayAccess);
}
