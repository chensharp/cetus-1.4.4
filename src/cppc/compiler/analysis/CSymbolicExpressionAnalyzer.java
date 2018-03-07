package cppc.compiler.analysis;

import cetus.hir.ArrayAccess;
import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.SizeofExpression;
import cetus.hir.Statement;
import cetus.hir.Typecast;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cppc.compiler.cetus.MultiExpression;
import cppc.compiler.cetus.UnfoldedExpression;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSymbolicExpressionAnalyzer
  extends SymbolicExpressionAnalyzer
{
  protected Expression analyzeExpression(AssignmentExpression expr)
  {
    if (expr.getOperator().equals(AssignmentOperator.NORMAL)) {
      return super.analyzeExpression(expr);
    }
    BinaryOperator operator = getEquivalentOperator(expr.getOperator());
    BinaryExpression virtualExpr = new BinaryExpression(
      (Expression)expr.getLHS().clone(), operator, 
      (Expression)expr.getRHS().clone());
    Expression folded = analyzeExpression(virtualExpr);
    if ((expr.getLHS() instanceof Identifier))
    {
      List<Expression> value = new ArrayList(1);
      value.add(folded);
      SymbolicAnalyzer.addKnownSymbol(knownSymbols, (Identifier)expr.getLHS(), 
        value);
    }
    AssignmentExpression newExpr = new AssignmentExpression(expr.getLHS(), 
      AssignmentOperator.NORMAL, folded);
    newExpr.setParens(false);
    
    return super.analyzeExpression(newExpr);
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
    if (operator.equals(BinaryOperator.BITWISE_INCLUSIVE_OR)) {
      return new BinaryExpression(foldedLHS, expr.getOperator(), foldedRHS);
    }
    if (operator.equals(BinaryOperator.SHIFT_LEFT)) {
      return new BinaryExpression(foldedLHS, expr.getOperator(), foldedRHS);
    }
    return super.analyzeExpression(expr);
  }
  
  protected Expression analyzeExpression(SizeofExpression sizeofExpression)
  {
    if (sizeofExpression.getExpression() != null) {
      return analyzeExpression(sizeofExpression.getExpression(), 
        knownSymbols);
    }
    return (Expression)sizeofExpression.clone();
  }
  
  protected Expression analyzeExpression(Typecast typecast)
  {
    return analyzeExpression((Expression)typecast.getChildren().get(0), 
      knownSymbols);
  }
  
  protected Expression analyzeExpression(UnaryExpression expr)
  {
    UnaryOperator operator = expr.getOperator();
    if (operator.equals(UnaryOperator.ADDRESS_OF)) {
      return (Expression)expr.clone();
    }
    Expression folded = analyzeExpression(expr.getExpression(), 
      knownSymbols);
    if (!(folded instanceof Literal)) {
      return new UnaryExpression(operator, folded);
    }
    if (operator.equals(UnaryOperator.BITWISE_COMPLEMENT)) {
      return new UnaryExpression(operator, folded);
    }
    if (operator.equals(UnaryOperator.PRE_DECREMENT))
    {
      BinaryExpression virtualExpr = new BinaryExpression(folded, 
        BinaryOperator.SUBTRACT, new IntegerLiteral(1L));
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr);
      virtualAssignment.setParent(expr.getParent());
      folded = analyzeExpression(virtualAssignment, knownSymbols);
      
      folded.setParent(null);
      return folded;
    }
    if (operator.equals(UnaryOperator.PRE_INCREMENT))
    {
      BinaryExpression virtualExpr = new BinaryExpression(folded, 
        BinaryOperator.ADD, new IntegerLiteral(1L));
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr);
      virtualAssignment.setParent(expr.getParent());
      folded = analyzeExpression(virtualAssignment, knownSymbols);
      
      folded.setParent(null);
      return folded;
    }
    if (operator.equals(UnaryOperator.POST_DECREMENT))
    {
      BinaryExpression virtualExpr = new BinaryExpression(folded, 
        BinaryOperator.SUBTRACT, new IntegerLiteral(1L));
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr);
      virtualAssignment.setParent(expr.getParent());
      analyzeExpression(virtualAssignment, knownSymbols);
      
      folded.setParent(null);
      return folded;
    }
    if (operator.equals(UnaryOperator.POST_INCREMENT))
    {
      BinaryExpression virtualExpr = new BinaryExpression(folded, 
        BinaryOperator.ADD, new IntegerLiteral(1L));
      AssignmentExpression virtualAssignment = new AssignmentExpression(
        expr.getExpression(), AssignmentOperator.NORMAL, virtualExpr);
      virtualAssignment.setParent(expr.getParent());
      analyzeExpression(virtualAssignment, knownSymbols);
      
      folded.setParent(null);
      return folded;
    }
    return super.analyzeExpression(expr);
  }
  
  protected Expression foldArrayValue(ArrayAccess expr)
  {
    Expression name = expr.getArrayName();
    if (!(name instanceof Identifier)) {
      return null;
    }
    Identifier base = (Identifier)expr.getArrayName();
    if (knownSymbols.containsKey(base))
    {
      List values = (List)knownSymbols.get(base);
      List<Expression> indexes = foldArrayIndexes(expr);
      if (indexes == null) {
        return null;
      }
      for (int i = 0; i < indexes.size() - 1; i++)
      {
        Expression index = (Expression)indexes.get(i);
        
        int iindex = (int)((IntegerLiteral)index).getValue();
        values = ((Initializer)values.get(iindex)).getChildren();
      }
      Expression index = (Expression)indexes.get(indexes.size() - 1);
      if ((index instanceof IntegerLiteral))
      {
        int iindex = (int)((IntegerLiteral)index).getValue();
        Expression folded = (Expression)values.get(iindex);
        return folded;
      }
    }
    return null;
  }
  
  protected Expression applyLogicalAnd(Expression lhs, Expression rhs)
  {
    if ((!(lhs instanceof IntegerLiteral)) || 
      (!(rhs instanceof IntegerLiteral)))
    {
      System.out.println("WARNING: line " + lhs.getStatement().where() + 
        " trying to fold logical and applied to non-boolean operands.");
      return new BinaryExpression(lhs, BinaryOperator.LOGICAL_AND, rhs);
    }
    IntegerLiteral safeLhs = (IntegerLiteral)lhs;
    IntegerLiteral safeRhs = (IntegerLiteral)rhs;
    if ((safeLhs.getValue() == 1L) && 
      (safeRhs.getValue() == 1L)) {
      return new IntegerLiteral(1L);
    }
    return new IntegerLiteral(0L);
  }
  
  protected Expression applyLogicalNegation(Expression expr)
  {
    if (!(expr instanceof IntegerLiteral))
    {
      System.out.println("WARNING: line " + expr.getStatement().where() + 
        " trying to fold logical negation applied to non-integer operand.");
      return expr;
    }
    IntegerLiteral safeExpr = (IntegerLiteral)expr;
    return buildBooleanLiteral(safeExpr.getValue() == 0L);
  }
  
  protected Expression applyLogicalOr(Expression lhs, Expression rhs)
  {
    if ((!(lhs instanceof IntegerLiteral)) || 
      (!(rhs instanceof IntegerLiteral)))
    {
      System.out.println("WARNING: line " + lhs.getStatement().where() + 
        " trying to fold logical or applied to non-boolean operands.");
      return new BinaryExpression(lhs, BinaryOperator.LOGICAL_OR, rhs);
    }
    IntegerLiteral safeLhs = (IntegerLiteral)lhs;
    IntegerLiteral safeRhs = (IntegerLiteral)rhs;
    
    return buildBooleanLiteral((safeLhs.getValue() != 0L) || 
      (safeRhs.getValue() != 0L));
  }
  
  protected Expression buildBooleanLiteral(boolean b)
  {
    if (b) {
      return new IntegerLiteral(1L);
    }
    return new IntegerLiteral(0L);
  }
  
  private BinaryOperator getEquivalentOperator(AssignmentOperator op)
  {
    if (op.equals(AssignmentOperator.ADD)) {
      return BinaryOperator.ADD;
    }
    if (op.equals(AssignmentOperator.BITWISE_AND)) {
      return BinaryOperator.BITWISE_AND;
    }
    if (op.equals(AssignmentOperator.BITWISE_EXCLUSIVE_OR)) {
      return BinaryOperator.BITWISE_EXCLUSIVE_OR;
    }
    if (op.equals(AssignmentOperator.BITWISE_INCLUSIVE_OR)) {
      return BinaryOperator.BITWISE_INCLUSIVE_OR;
    }
    if (op.equals(AssignmentOperator.DIVIDE)) {
      return BinaryOperator.DIVIDE;
    }
    if (op.equals(AssignmentOperator.MODULUS)) {
      return BinaryOperator.MODULUS;
    }
    if (op.equals(AssignmentOperator.MULTIPLY)) {
      return BinaryOperator.MULTIPLY;
    }
    if (op.equals(AssignmentOperator.SHIFT_LEFT)) {
      return BinaryOperator.SHIFT_LEFT;
    }
    if (op.equals(AssignmentOperator.SHIFT_RIGHT)) {
      return BinaryOperator.SHIFT_RIGHT;
    }
    if (op.equals(AssignmentOperator.SUBTRACT)) {
      return BinaryOperator.SUBTRACT;
    }
    return AssignmentOperator.NORMAL;
  }
  
  protected boolean initializeArrayValue(ArrayAccess access)
  {
    VariableDeclaration vd = null;
    try
    {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        access, (Identifier)access.getArrayName());
    }
    catch (SymbolIsNotVariableException e)
    {
      return false;
    }
    catch (SymbolNotDefinedException e)
    {
      return false;
    }
    VariableDeclarator vdeclarator = null;
    for (int i = 0; i < vd.getNumDeclarators(); i++) {
      if (vd.getDeclarator(i).getSymbol().equals(access.getArrayName()))
      {
        vdeclarator = (VariableDeclarator)vd.getDeclarator(i);
        break;
      }
    }
    if (vdeclarator == null) {
      return false;
    }
    List<ArraySpecifier> aspecs = 
      vdeclarator.getTrailingSpecifiers();
    if (aspecs.isEmpty()) {
      return false;
    }
    ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
    List values = createValuesList(aspec, 0, null, 0);
    if (values == null) {
      return false;
    }
    SymbolicAnalyzer.addKnownSymbol(knownSymbols, 
      (Identifier)access.getArrayName(), values);
    return true;
  }
  
  private List createValuesList(ArraySpecifier aspec, int index, List values, int valuesSize)
  {
    Expression size = aspec.getDimension(index);
    Expression foldedSize = analyzeExpression(size, knownSymbols);
    if (!(foldedSize instanceof IntegerLiteral)) {
      return null;
    }
    int intSize = (int)((IntegerLiteral)foldedSize).getValue();
    if (values == null)
    {
      values = new ArrayList(intSize);
      if (index == aspec.getNumDimensions() - 1) {
        for (int i = 0; i < intSize; i++) {
          values.add(null);
        }
      } else if (createValuesList(aspec, index + 1, values, intSize) == null) {
        return null;
      }
      return values;
    }
    for (int i = 0; i < valuesSize; i++)
    {
      List innerValues = new ArrayList(intSize);
      values.add(innerValues);
      if (index < aspec.getNumDimensions() - 1)
      {
        if (createValuesList(aspec, index + 1, values, intSize) == null) {
          return null;
        }
      }
      else {
        for (int j = 0; j < intSize; j++) {
          innerValues.add(null);
        }
      }
    }
    return values;
  }
}
