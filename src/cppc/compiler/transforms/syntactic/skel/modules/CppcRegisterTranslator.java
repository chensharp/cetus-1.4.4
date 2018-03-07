package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.exceptions.TypeNotSupportedException;
import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.transforms.shared.DataType;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.transforms.shared.TypedefDataType;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

public abstract class CppcRegisterTranslator
  extends TranslationModule<CppcRegisterPragma>
{
  private static final String STRING_LENGTH = "cppc_string_length_";
  private static int stringsFound = 0;
  
  public Class getTargetClass()
  {
    return CppcRegisterPragma.class;
  }
  
  public void translate(CppcRegisterPragma pragma)
  {
    CppcLabel jumpLabel = new CppcLabel(new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_LABEL()));
    pragma.swapWith(jumpLabel);
    jumpLabel.setLineNumber(pragma.where());
    
    Statement after = jumpLabel;
    for (CppcRegister register : pragma.getRegisters()) {
      try
      {
        after = addCppcRegisterCall(after, register);
      }
      catch (SymbolNotDefinedException localSymbolNotDefinedException) {}catch (SymbolIsNotVariableException localSymbolIsNotVariableException) {}catch (TypeNotSupportedException e)
      {
        String message = "Warning: CPPC does not support registering objects of type: " + 
          e.getMessage() + "\n" + 
          "\tPlease contact developers to issue a feature request";
        printErrorInTranslation(System.err, jumpLabel, message);
      }
    }
    CompoundStatement statementList = 
      (CompoundStatement)jumpLabel.getParent();
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter(after, jump);
  }
  
  protected abstract void furtherModifyRegisterCall(FunctionCall paramFunctionCall, VariableDeclarator paramVariableDeclarator);
  
  protected abstract Statement getRegisterCallStatement(VariableDeclarator paramVariableDeclarator, FunctionCall paramFunctionCall);
  
  private Statement addCppcRegisterCall(Statement after, CppcRegister register)
    throws SymbolNotDefinedException, SymbolIsNotVariableException, TypeNotSupportedException
  {
    VariableDeclaration varDecl = 
      LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
      after, register.getName());
    VariableDeclarator varDeclarator = null;
    if ((varDecl instanceof CommonBlock))
    {
      CommonBlock block = 
        (CommonBlock)varDecl;
      varDecl = block.getDeclaration(register.getName());
    }
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(after, 
      CompoundStatement.class);
    if ((register.getSize() instanceof FunctionCall))
    {
      FunctionCall call = (FunctionCall)register.getSize();
      
      Identifier sizeId = new Identifier("cppc_string_length_" + stringsFound++);
      VariableDeclaration sizeDeclaration = new VariableDeclaration(
        Specifier.INT, new VariableDeclarator(sizeId));
      statementList.addDeclaration(sizeDeclaration);
      
      BinaryExpression sizeExpr = new BinaryExpression(register.getSize(), 
        BinaryOperator.ADD, new IntegerLiteral(1L));
      sizeExpr.setParens(false);
      
      AssignmentExpression sizeAssignment = new AssignmentExpression(sizeId, 
        AssignmentOperator.NORMAL, sizeExpr);
      ExpressionStatement sizeStmt = new ExpressionStatement(sizeAssignment);
      statementList.addStatementAfter(after, sizeStmt);
      after = sizeStmt;
      
      CppcRegister sizeRegister = new CppcRegister(sizeId, 
        new IntegerLiteral(1L));
      after = addCppcRegisterCall(after, sizeRegister);
      
      register.setSize(sizeId);
      for (int i = 0; i < varDecl.getNumDeclarators(); i++) {
        if (varDecl.getDeclarator(i).getSymbol().equals(register.getName()))
        {
          varDeclarator = (VariableDeclarator)varDecl.getDeclarator(i);
          if (varDeclarator.getInitializer() != null) {
            break;
          }
          Initializer initializer = new Initializer(
            new StringLiteral(""));
          varDeclarator.setInitializer(initializer);
          
          break;
        }
      }
    }
    FunctionCall cppcRegisterCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_FUNCTION()));
    for (int i = 0; i < varDecl.getNumDeclarators(); i++) {
      if (varDecl.getDeclarator(i).getSymbol().equals(register.getName()))
      {
        varDeclarator = (VariableDeclarator)varDecl.getDeclarator(i);
        break;
      }
    }
    cppcRegisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
      varDeclarator));
    
    DataType dataType = TypeManager.getType(varDecl.getSpecifiers());
    if (dataType == null) {
      return after;
    }
    Expression registerSize = null;
    if (register.getSize() == null)
    {
      registerSize = new IntegerLiteral(dataType.size());
    }
    else
    {
      registerSize = (Expression)register.getSize().clone();
      if (dataType.size() != 1) {
        registerSize = new BinaryExpression(
          new IntegerLiteral(dataType.size()), BinaryOperator.MULTIPLY, 
          registerSize);
      }
    }
    if (((dataType instanceof TypedefDataType)) && 
      (((TypedefDataType)dataType).size() != 1))
    {
      IntegerLiteral size = new IntegerLiteral(
        ((TypedefDataType)dataType).size());
      registerSize = new BinaryExpression(registerSize, 
        BinaryOperator.MULTIPLY, size);
    }
    cppcRegisterCall.addArgument(registerSize);
    cppcRegisterCall.addArgument((Identifier)dataType.getBaseType().clone());
    cppcRegisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      register.getName().toString()));
    furtherModifyRegisterCall(cppcRegisterCall, varDeclarator);
    Statement cppcRegisterCallStatement = getRegisterCallStatement(
      varDeclarator, cppcRegisterCall);
    
    statementList.addStatementAfter(after, cppcRegisterCallStatement);
    
    return cppcRegisterCallStatement;
  }
}
