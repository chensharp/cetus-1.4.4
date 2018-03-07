package cppc.compiler.transforms.syntactic.stub.c.modules;

import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.Declaration;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import java.util.List;

public class CppcRegisterTranslator
  extends cppc.compiler.transforms.syntactic.skel.modules.CppcRegisterTranslator
{
  private Expression getSize(String sizeStr, SymbolTable symbolTable)
  {
    try
    {
      Integer sizeValue = new Integer(sizeStr);
      return new IntegerLiteral(sizeValue.intValue());
    }
    catch (NumberFormatException localNumberFormatException)
    {
      Declaration decl = symbolTable.findSymbol(new Identifier(sizeStr));
      try
      {
        if (decl != null)
        {
          if (!(decl instanceof VariableDeclaration)) {
            throw new SymbolIsNotVariableException(sizeStr);
          }
          return new Identifier(sizeStr);
        }
        throw new SymbolNotDefinedException(sizeStr);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    return null;
  }
  
  protected void furtherModifyRegisterCall(FunctionCall call, VariableDeclarator varDeclarator)
  {
    call.addArgument(getIsStatic(varDeclarator));
  }
  
  protected Statement getRegisterCallStatement(VariableDeclarator declarator, FunctionCall call)
  {
    if (getIsStaticAsBoolean(declarator)) {
      return new ExpressionStatement(call);
    }
    AssignmentExpression assignment = new AssignmentExpression(
      (Identifier)declarator.getSymbol().clone(), AssignmentOperator.NORMAL, 
      call);
    return new ExpressionStatement(assignment);
  }
  
  private Identifier getIsStatic(VariableDeclarator declarator)
  {
    if (getIsStaticAsBoolean(declarator)) {
      return new Identifier("CPPC_STATIC");
    }
    return new Identifier("CPPC_DYNAMIC");
  }
  
  private boolean getIsStaticAsBoolean(VariableDeclarator declarator)
  {
    return declarator.getSpecifiers().size() == 0;
  }
}
