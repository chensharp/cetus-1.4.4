package cppc.compiler.transforms.semantic.stub.c;

import cetus.hir.AccessExpression;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.PointerSpecifier;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.UnaryExpression;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.io.PrintStream;
import java.util.Hashtable;

public class AddOpenFilesControl
  extends cppc.compiler.transforms.semantic.skel.AddOpenFilesControl
{
  private static final String FD_PARAMETER = "FileDescriptor";
  private static final String PATH_PARAMETER = "Path";
  private static final String DESCRIPTOR_TYPE_PARAMETER = "DescriptorType";
  private static final String TMP_PATH_PARAMETER = "CPPC_PATH_TMP_";
  private static int TMP_PATH_COUNT = 0;
  
  private AddOpenFilesControl(Program program)
  {
    super(program);
  }
  
  public static final AddOpenFilesControl getTransformInstance(Program program)
  {
    return new AddOpenFilesControl(program);
  }
  
  private void checkPathParameter(FunctionCall call)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName());
    Hashtable<String, String> attributes = c.getSemantic("CPPC/IO/Open");
    
    String pathParameterPlace = (String)attributes.get("Path");
    int pos = new Integer(pathParameterPlace).intValue() - 1;
    Expression pathParameter = call.getArgument(pos);
    if (!(pathParameter instanceof Identifier))
    {
      Identifier newPath = new Identifier("CPPC_PATH_TMP_" + 
        TMP_PATH_COUNT++);
      call.setArgument(pos, newPath);
      
      AssignmentExpression expr = new AssignmentExpression(
        (Identifier)newPath.clone(), AssignmentOperator.NORMAL, pathParameter);
      ExpressionStatement stmt = new ExpressionStatement(expr);
      
      Statement ref = call.getStatement();
      CompoundStatement statementList = (CompoundStatement)ref.getParent();
      statementList.addStatementBefore(ref, stmt);
      
      VariableDeclarator vdecl = new VariableDeclarator(
        PointerSpecifier.UNQUALIFIED, (Identifier)newPath.clone());
      VariableDeclaration vd = new VariableDeclaration(Specifier.CHAR, vdecl);
      statementList.addDeclaration(vd);
    }
  }
  
  protected Statement getCppcOpenCall(FunctionCall call)
  {
    checkPathParameter(call);
    
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName());
    Hashtable<String, String> attributes = c.getSemantic("CPPC/IO/Open");
    
    String pathParameterPlace = (String)attributes.get("Path");
    Expression pathParameter = call.getArgument(new Integer(
      pathParameterPlace).intValue() - 1);
    
    String fdParameterStr = (String)attributes.get("FileDescriptor");
    Expression fdParameter = null;
    try
    {
      fdParameter = getFdParameter(fdParameterStr, call);
    }
    catch (Exception e)
    {
      System.err.println("WARNING: cannot find Expression for a descriptor in call " + 
        call);
      System.err.println("'" + e.getMessage() + " is not a variable symbol, " + 
        "or is not defined");
      System.err.println("Call ignored. The opened file will not be correctly recovered");
      
      return null;
    }
    String descriptorType = (String)attributes.get("DescriptorType");
    
    IntegerLiteral fileCode = new IntegerLiteral(
      GlobalNamesFactory.getGlobalNames().NEXT_FILE_CODE());
    
    FunctionCall cppcOpenCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().REGISTER_DESCRIPTOR_FUNCTION()));
    
    cppcOpenCall.addArgument(fileCode);
    cppcOpenCall.addArgument(fdParameter);
    cppcOpenCall.addArgument(new Identifier(descriptorType));
    cppcOpenCall.addArgument((Expression)pathParameter.clone());
    if ((fdParameter instanceof UnaryExpression)) {
      return new ExpressionStatement(cppcOpenCall);
    }
    AssignmentExpression assignment = new AssignmentExpression(
      (Expression)fdParameter.clone(), AssignmentOperator.NORMAL, 
      cppcOpenCall);
    
    return new ExpressionStatement(assignment);
  }
  
  private Expression getFdParameter(String stringValue, FunctionCall call)
    throws SymbolNotDefinedException, SymbolIsNotVariableException
  {
    Expression fdParam = null;
    if (stringValue.equals("return"))
    {
      AssignmentExpression expr = 
        (AssignmentExpression)ObjectAnalizer.getParentOfClass(call, 
        AssignmentExpression.class);
      if (expr != null)
      {
        fdParam = expr.getLHS();
      }
      else
      {
        VariableDeclarator vdecl = 
          (VariableDeclarator)ObjectAnalizer.getParentOfClass(call, 
          VariableDeclarator.class);
        fdParam = vdecl.getSymbol();
      }
    }
    else
    {
      try
      {
        fdParam = call.getArgument(new Integer(stringValue).intValue() - 1);
      }
      catch (NumberFormatException localNumberFormatException) {}
    }
    if (fdParam != null)
    {
      if ((!(fdParam instanceof Identifier)) && 
        (!(fdParam instanceof AccessExpression)))
      {
        System.err.println("BUG: fdParam is not an instance of cetus.hir.Identifier");
        
        System.err.println("in " + getClass());
        System.err.println("Method getFdParameter()");
        System.exit(0);
      }
      Identifier fdIdentifier = null;
      if ((fdParam instanceof AccessExpression)) {
        fdIdentifier = (Identifier)((AccessExpression)fdParam).getLHS();
      } else {
        fdIdentifier = (Identifier)fdParam;
      }
      SymbolTable table = ObjectAnalizer.getSymbolTable(fdIdentifier);
      Declaration decl = table.findSymbol(fdIdentifier);
      if (decl == null) {
        throw new SymbolNotDefinedException(fdIdentifier.toString());
      }
      if (!(decl instanceof VariableDeclaration)) {
        throw new SymbolIsNotVariableException(fdIdentifier.toString());
      }
      VariableDeclaration varDecl = (VariableDeclaration)decl;
      Declarator declarator = null;
      for (int i = 0; i < varDecl.getNumDeclarators(); i++) {
        if (varDecl.getDeclarator(i).getSymbol().equals(fdIdentifier))
        {
          declarator = varDecl.getDeclarator(i);
          break;
        }
      }
      return LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
        declarator);
    }
    System.err.println("ERROR:  Unable to extract FD information for function call " + 
      call);
    System.err.println("\tMethod cppc.compiler.transforms.semantic.stub.c.AddOpenFilesControl.getFdParameter needs to be extended for dealing with " + 
    
      call.getName() + "()");
    System.exit(0);
    
    return null;
  }
  
  protected Statement getCppcCloseCall(FunctionCall call)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName());
    Hashtable<String, String> attributes = c.getSemantic("CPPC/IO/Close");
    
    String fdParameterStr = (String)attributes.get("FileDescriptor");
    Expression fdParameter = null;
    try
    {
      fdParameter = getFdParameter(fdParameterStr, call);
    }
    catch (Exception e)
    {
      System.err.println("WARNING: cannot find Expression for a descriptor in call " + 
        call);
      System.err.println("'" + e.getMessage() + " is not a variable symbol, " + 
        "or is not defined");
      System.err.println("Call ignored. The opened file will not be correctly recovered");
      
      return null;
    }
    FunctionCall cppcCloseCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().UNREGISTER_DESCRIPTOR_FUNCTION()));
    
    cppcCloseCall.addArgument(fdParameter);
    
    return new ExpressionStatement(cppcCloseCall);
  }
}
