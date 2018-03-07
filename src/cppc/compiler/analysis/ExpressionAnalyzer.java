package cppc.compiler.analysis;

import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BooleanLiteral;
import cetus.hir.CharLiteral;
import cetus.hir.CommaExpression;
import cetus.hir.Declarator;
import cetus.hir.EscapeLiteral;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;

import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.DoubleLiteral;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;

//import cppc.compiler.fortran.FortranArraySpecifier;
//import cppc.compiler.fortran.FortranDoLoop;

import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import cppc.util.dispatcher.FunctionDispatcher;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ExpressionAnalyzer
  extends FunctionDispatcher<Expression>
{
  private static Class instanceClass;
  private static ExpressionAnalyzer instance;
  protected static CppcStatement currentStatement = null;
  
  static
  {
    try
    {
      instanceClass = Class.forName(ConfigurationManager.getOption(
        "CPPC/Analysis/ExpressionAnalyzer/ClassName"));
      instance = (ExpressionAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static Set<Identifier> analyzeExpression(CppcStatement s, Expression expr)
  {
    Method m = instance.dispatch(expr, "analyzeExpression");
    if (m == null)
    {
      System.err.println("WARNING: cppc.compiler.analysis.ExpressionAnalyzer.analyzeExpression not implemented for " + 
      
        expr.getClass());
      
      return createEmptySet();
    }
    CppcStatement oldStatement = currentStatement;
    currentStatement = s;
    try
    {
      return (Set)m.invoke(instance, new Object[] { expr });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    finally
    {
      currentStatement = oldStatement;
    }
    return createEmptySet();
  }
  
  protected static Set<Identifier> createEmptySet()
  {
    return new HashSet(0);
  }
  
  protected Set<Identifier> analyzeExpression(ArrayAccess expr)
  {
    long currentWeight = currentStatement.getWeight();
    
    Set<Identifier> consumed = analyzeExpression(currentStatement, 
      expr.getIndex(0));
    for (int i = 1; i < expr.getNumIndices(); i++) {
      consumed.addAll(analyzeExpression(currentStatement, 
        expr.getIndex(i)));
    }
    currentStatement.setWeight(currentWeight);
    currentStatement.getConsumed().addAll(consumed);
    
    return analyzeExpression(currentStatement, expr.getArrayName());
  }
  
  protected Set<Identifier> analyzeExpression(AssignmentExpression expr)
  {
    currentStatement.getConsumed().addAll(analyzeExpression(currentStatement, 
      expr.getRHS()));
    
    long currentWeight = currentStatement.getWeight();
    Set<Identifier> generated = analyzeExpression(currentStatement, 
      expr.getLHS());
    
    currentStatement.setWeight(currentWeight);
    
    currentStatement.getPartialGenerated().addAll(generated);
    checkGeneratedArrays(generated);
    currentStatement.getGenerated().addAll(generated);
    if (!expr.getOperator().equals(AssignmentOperator.NORMAL)) {
      currentStatement.getConsumed().addAll(generated);
    }
    return createEmptySet();
  }
  
  private static void checkGeneratedArrays(Set<Identifier> gen)
  {
    Set<Identifier> remove = new HashSet(gen.size());
    for (Identifier id : gen)
    {
      VariableDeclaration vd = null;
      try
      {
        vd = 
          LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
          currentStatement, id);
      }
      catch (SymbolNotDefinedException e)
      {
        remove.add(id);
        continue;
      }
      catch (SymbolIsNotVariableException e)
      {
        remove.add(id);
      }
      Declarator declarator = ObjectAnalizer.getDeclarator(vd, id);
      if ((declarator.getArraySpecifiers().size() != 0) || 
        (declarator.getSpecifiers().size() != 0)) {
        if ((!checkFullyGeneratedArray(id)) && 
          (!checkLoopGeneratedArray(id))) {
          remove.add(id);
        }
      }
    }
    gen.removeAll(remove);
  }
  
  private static boolean checkFullyGeneratedArray(Identifier id)
  {
    ArrayAccess access = (ArrayAccess)ObjectAnalizer.getParentOfClass(id, 
      ArrayAccess.class);
    
    return access == null;
  }
  
  //该函数含有 fortran 支持相关，
  private static boolean checkLoopGeneratedArray(Identifier id)
  {
    ArrayAccess access = (ArrayAccess)ObjectAnalizer.getParentOfClass(id, 
      ArrayAccess.class);
    Statement stmt = 
      (Statement)ObjectAnalizer.getParentOfClass(id, 
      Statement.class);
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass(
      stmt, SymbolTable.class);
    if (!LanguageAnalyzerFactory.getLanguageAnalyzer().insideLoop(stmt)) {
      return false;
    }
    FortranDoLoop doLoop = 
      (FortranDoLoop)ObjectAnalizer.getParentOfClass(
      stmt, FortranDoLoop.class);
    if (doLoop == null) {
      return false;
    }
    ArrayList<FortranDoLoop> loops = 
      new ArrayList(
      access.getNumIndices());
    while (doLoop != null)
    {
      loops.add(doLoop);
      doLoop = 
        (FortranDoLoop)ObjectAnalizer.getParentOfClass(
        doLoop.getParent(), FortranDoLoop.class);
    }
    if (loops.size() != access.getNumIndices()) {
      return false;
    }
    int i = access.getNumIndices() - 1;
    for (FortranDoLoop l : loops)
    {
      if (!l.getLoopVar().equals(access.getIndex(i))) {
        return false;
      }
      VariableDeclaration vd = (VariableDeclaration)symbolTable.findSymbol(
        id);
      Declarator declarator = ObjectAnalizer.getDeclarator(vd, id);
      if (declarator.getArraySpecifiers().size() == 0) {
        return false;
      }
      if ((l.getStep() != null) && 
        (!l.getStep().equals(new IntegerLiteral(1L)))) {
        return false;
      }

      List arraySpecs = declarator.getArraySpecifiers();
      FortranArraySpecifier spec =  (FortranArraySpecifier)arraySpecs.get(i--);
      if (!spec.getLowerBound().equals(l.getStart())) {
        return false;
      }
      if (!spec.getUpperBound().equals(l.getStop())) {
        return false;
      }
    }
    return true;
  }
  
  protected Set<Identifier> analyzeExpression(BinaryExpression expr)
  {
    Set<Identifier> resultSet = analyzeExpression(currentStatement, 
      expr.getLHS());
    resultSet.addAll(analyzeExpression(currentStatement, 
      expr.getRHS()));
    
    return resultSet;
  }
  
  protected Set<Identifier> analyzeExpression(CharLiteral charLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(CommaExpression expr)
  {
    Set<Identifier> resultSet = createEmptySet();
    Iterator iter = expr.getChildren().iterator();
    while (iter.hasNext()) {
      resultSet.addAll(analyzeExpression(currentStatement, 
        (Expression)iter.next()));
    }
    return resultSet;
  }
  
  protected Set<Identifier> analyzeExpression(DoubleLiteral doubleLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(EscapeLiteral escapeLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(FloatLiteral floatLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(FunctionCall functionCall)
  {
    if (!CppcRegisterManager.isRegistered((Identifier)functionCall.getName()))
    {
      Procedure procedure = functionCall.getProcedure();
      if (procedure == null) {
        registerNullFunctionCall(functionCall);
      } else {
        ProcedureAnalyzer.analyzeProcedure(procedure);
      }
    }
    if (functionCall.getProcedure() != null) {
      CppcRegisterManager.getCharacterization((Identifier)currentStatement.getProcedure().getName()).addCall((Identifier)
        functionCall.getName());
    }
    ProcedureCharacterization characterization = 
      CppcRegisterManager.getCharacterization(
      (Identifier)functionCall.getName());
    if (!characterization.isNull()) {
      characterization.addCalledFrom((Identifier)
        currentStatement.getProcedure().getName());
    }
    Set<ProcedureParameter> localConsumed = characterization.getConsumed();
    Set<ProcedureParameter> localGenerated = characterization.getGenerated();
    for (int i = 0; i < functionCall.getNumArguments(); i++)
    {
      Expression argument = functionCall.getArgument(i);
      Set<Identifier> ids = analyzeExpression(currentStatement, argument);
      ProcedureParameter aux = new ProcedureParameter(i);
      boolean found = false;
      if (localConsumed.contains(aux))
      {
        for (Identifier id : ids) {
          if (!currentStatement.getGenerated().contains(id)) {
            currentStatement.getConsumed().add(id);
          }
        }
        found = true;
      }
      if (localGenerated.contains(aux))
      {
        currentStatement.getPartialGenerated().addAll(ids);
        checkGeneratedArrays(ids);
        currentStatement.getGenerated().addAll(ids);
        found = true;
      }
      if (!found)
      {
        if (localConsumed.contains(ProcedureParameter.VARARGS)) {
          currentStatement.getConsumed().addAll(ids);
        }
        if (localGenerated.contains(ProcedureParameter.VARARGS))
        {
          currentStatement.getGenerated().addAll(ids);
          currentStatement.getPartialGenerated().addAll(ids);
        }
      }
    }
    currentStatement.getGlobalConsumed().addAll(
      characterization.getGlobalConsumed());
    currentStatement.getGlobalGenerated().addAll(
      characterization.getGlobalGenerated());
    if (characterization.getProcedure() != null) {
      currentStatement.setWeight(characterization.getWeight());
    }
    currentStatement.statementCount = characterization.statementCount;
    
    return createEmptySet();
  }
  
  protected static void registerNullFunctionCall(FunctionCall functionCall)
  {
    if (!functionCall.getName().toString().startsWith("_"))
    {
      System.out.println("WARNING: Taking a conservative approach over " + 
        functionCall.getName() + " function.");
      System.out.println("\tReason  : Code not found.");
      System.out.println("\tApproach: All parameters are consumed, none generated.");
      
      System.out.println("\tSolution: Implement or issue a petition over a semantic module for this function's family.");
    }
    HashSet<ProcedureParameter> generated = 
      new HashSet(0);
    HashSet<ProcedureParameter> consumed = new HashSet(1);
    
    consumed.add(ProcedureParameter.VARARGS);
    
    ProcedureCharacterization characterization = new ProcedureCharacterization(
      (Identifier)functionCall.getName());
    characterization.setGenerated(generated);
    characterization.setConsumed(consumed);
    characterization.setNull(true);
    characterization.statementCount = 1;
    characterization.setWeight(functionCall.getNumArguments());
    
    CppcRegisterManager.addProcedure((Identifier)functionCall.getName(), 
      characterization);
  }
  
  static Map knownTrash = new HashMap();
  
  protected Set<Identifier> analyzeExpression(Identifier identifier)
  {
    Set<Identifier> resultSet = new HashSet(1);
    resultSet.add(identifier);
    
    currentStatement.setWeight(currentStatement.getWeight() + 1L);
    
    return resultSet;
  }
  
  protected Set<Identifier> analyzeExpression(BooleanLiteral booleanLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(IntegerLiteral integerLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(StringLiteral stringLiteral)
  {
    return createEmptySet();
  }
  
  protected Set<Identifier> analyzeExpression(UnaryExpression expr)
  {
    Expression expression = expr.getExpression();
    UnaryOperator operator = expr.getOperator();
    if ((operator.equals(UnaryOperator.ADDRESS_OF)) || 
      (operator.equals(UnaryOperator.DEREFERENCE)) || 
      (operator.equals(UnaryOperator.MINUS)) || 
      (operator.equals(UnaryOperator.PLUS)) || 
      (operator.equals(UnaryOperator.LOGICAL_NEGATION)) || 
      (operator.equals(UnaryOperator.BITWISE_COMPLEMENT))) {
      return analyzeExpression(currentStatement, expression);
    }
    if ((operator.equals(UnaryOperator.POST_INCREMENT)) || 
      (operator.equals(UnaryOperator.POST_DECREMENT)) || 
      (operator.equals(UnaryOperator.PRE_INCREMENT)) || 
      (operator.equals(UnaryOperator.PRE_DECREMENT)))
    {
      Set<Identifier> resultSet = analyzeExpression(currentStatement, 
        expression);
      currentStatement.getConsumed().addAll(resultSet);
      currentStatement.getGenerated().addAll(resultSet);
      currentStatement.getPartialGenerated().addAll(resultSet);
      return createEmptySet();
    }
    if (operator.equals(UnaryOperator.LOGICAL_NEGATION)) {
      return analyzeExpression(currentStatement, expression);
    }
    System.err.println("WARNING: cppc.compiler.analysis.ExpressionAnalyzer.analyzeExpression( CppcStatement, UnaryExpression ) not implemented for UnaryOperator: " + 
    
      operator);
    
    return createEmptySet();
  }
}
