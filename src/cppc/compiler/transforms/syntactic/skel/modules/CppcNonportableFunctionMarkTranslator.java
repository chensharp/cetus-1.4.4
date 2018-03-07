package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.VariableDeclaration;
import cppc.compiler.analysis.ExpressionAnalyzer;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.DataType;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.VariableSizeAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class CppcNonportableFunctionMarkTranslator
  extends TranslationModule<CppcNonportableFunctionMark>
{
  private static String SUBSTITUTION_PREFIX = "CPPC_TMP_";
  
  public Class getTargetClass()
  {
    return CppcNonportableFunctionMark.class;
  }
  
  public void translate(CppcNonportableFunctionMark pragma)
  {
    CompoundStatement statementList = (CompoundStatement)
      ObjectAnalizer.getParentOfClass(pragma, CompoundStatement.class);
    
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    CppcNonportableFunctionMark mark = null;
    while ((iter.hasNext()) && (mark != pragma)) {
      try
      {
        mark = (CppcNonportableFunctionMark)iter.next(
          CppcNonportableFunctionMark.class);
      }
      catch (NoSuchElementException e)
      {
        throw new InternalError("Cannot find current annote");
      }
    }
    FunctionCall call = null;
    try
    {
      call = (FunctionCall)iter.next(FunctionCall.class);
    }
    catch (NoSuchElementException e)
    {
      throw new InternalError("Cannot find current nonportable call");
    }
    Statement stmt = call.getStatement();
    CppcStatement cppcStatement = new CppcStatement(stmt);
    stmt.swapWith(cppcStatement);
    stmt.setParent(cppcStatement);
    ExpressionAnalyzer.analyzeExpression(cppcStatement, call);
    stmt.detach();
    cppcStatement.swapWith(stmt);
    
    boolean registers = false;
    for (Identifier id : cppcStatement.getConsumed()) {
      if (addCppcRegisterForCallImage(stmt, id, statementList)) {
        registers = true;
      }
    }
    CppcLabel jumpLabel = new CppcLabel(new Identifier(
      GlobalNamesFactory.getGlobalNames().EXECUTE_LABEL()));
    pragma.swapWith(jumpLabel);
    
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter(stmt, jump);
    if (!registers) {
      return;
    }
    FunctionCall commitCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().COMMIT_CALL_IMAGE_FUNCTION()));
    ExpressionStatement callStatement = new ExpressionStatement(commitCall);
    statementList.addStatementBefore(stmt, callStatement);
    
    addCppcCreateCallImage(jumpLabel, call, stmt, statementList);
  }
  
  private void addCppcCreateCallImage(Statement ref, FunctionCall call, Statement callStatement, CompoundStatement statementList)
  {
    FunctionCall createCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().CREATE_CALL_IMAGE_FUNCTION()));
    
    createCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      call.getName().toString()));
    
    createCall.addArgument(new IntegerLiteral(callStatement.where()));
    
    ExpressionStatement createStatement = new ExpressionStatement(createCall);
    statementList.addStatementAfter(ref, createStatement);
  }
  
  private boolean addCppcRegisterForCallImage(Statement ref, Identifier id, CompoundStatement statementList)
  {
    VariableDeclaration vd = null;
    try
    {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        ref, id);
      if (vd.getSpecifiers().contains(Specifier.CONST)) {
        return false;
      }
    }
    catch (SymbolNotDefinedException e)
    {
      return false;
    }
    catch (SymbolIsNotVariableException e)
    {
      return false;
    }
    FunctionCall call = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_FOR_CALL_IMAGE_FUNCTION()));
    
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
      ObjectAnalizer.getDeclarator(vd, id)));
    
    Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize(
      (Identifier)vd.getDeclarator(0).getSymbol(), ref);
    if (size == null) {
      size = new IntegerLiteral(1L);
    }
    call.addArgument(size);
    try
    {
      call.addArgument((Identifier)
        TypeManager.getType(vd.getSpecifiers()).getBaseType().clone());
    }
    catch (Exception e)
    {
      String message = "Warning: CPPC does not support registering objects of type: " + 
        e.getMessage() + "\n" + "\tPlease contact developers " + 
        "to issue a feature request";
      printErrorInTranslation(System.err, ref, message);
    }
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      id.toString()));
    
    furtherModifyRegisterForCallImage(call, vd);
    
    ExpressionStatement callStatement = new ExpressionStatement(call);
    statementList.addStatementBefore(ref, callStatement);
    
    return true;
  }
  
  protected abstract void furtherModifyRegisterForCallImage(FunctionCall paramFunctionCall, VariableDeclaration paramVariableDeclaration);
}
