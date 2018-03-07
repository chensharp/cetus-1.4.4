package cppc.compiler.utils;

import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.SizeofExpression;
import cetus.hir.Specifier;
import cetus.hir.SymbolTable;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


//错误记录： 51行，


//
public class CVariableSizeAnalizer implements VariableSizeAnalizer
{
  private static final String STRLEN_FUNCTION_NAME = "strlen";
  
  public Expression getSize(Identifier id, Traversable reference)
  {
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass(reference, SymbolTable.class);
    
    Declaration declaration = symbolTable.findSymbol(id);
    if (declaration == null)
    {
      System.out.println("BUG: Symbol " + id + " not found in symbol table in " + CVariableSizeAnalizer.class + ". Method: getSize()");
      System.exit(0);
    }
    if (!(declaration instanceof VariableDeclaration))
    {
      System.err.println("BUG: Symbol " + id + " is not a variable. In " + CVariableSizeAnalizer.class + ". Method: getSize()");
      System.exit(0);
    }

    VariableDeclaration vd = (VariableDeclaration)declaration;
    for (int i = 0; i < vd.getNumDeclarators(); i++ )
    {
      Declarator declarator = vd.getDeclarator(i);
      //Identifier tempid = 
      Identifier declId = (Identifier)declarator.getSymbol();//!!! 这里需要语义检查，否则无法通过。
      if (declId.equals(id))
      {
        List arraySpecifiers = declarator.getArraySpecifiers();
        List pointerSpecifiers = declarator.getSpecifiers();
        if ((arraySpecifiers.size() == 0) && 
          (pointerSpecifiers.size() == 0)) {
          return null;
        }
        if (pointerSpecifiers.size() == 0) {
          return getSizeOfArray(arraySpecifiers);
        }
        if (arraySpecifiers.size() == 0) {
          return getSizeOfPointer(id, declarator);
        }
      }
    }
    System.out.println("WARNING: cppc.compiler.utils.CVariableSizeAnalizer.getSize(): cannot handle declaration " + declaration);
    return null;
  }
  
  private Expression getSizeOfArray(List arraySpecifiers)
  {
    Iterator iter = arraySpecifiers.iterator();
    Expression expr = new IntegerLiteral(1L);
    //ArraySpecifier spec;
    //int i;
    //for (; iter.hasNext(); i < spec.getNumDimensions())
    //{
    //  spec = (ArraySpecifier)iter.next();
    //  i = 0; continue;
    //  expr = new BinaryExpression(expr, BinaryOperator.MULTIPLY, 
    //    spec.getDimension(i));i++;
    //}

    while (iter.hasNext()) {
      final ArraySpecifier spec = (ArraySpecifier) iter.next();
      for (int i = 0; i < spec.getNumDimensions(); ++i) {
        expr = (Expression)new BinaryExpression(expr, BinaryOperator.MULTIPLY, spec.getDimension(i));
      }
    }

    return expr;
  }
  
  private Expression getSizeOfPointer(Identifier id, Declarator declarator)
  {
    VariableDeclaration vd = (VariableDeclaration)ObjectAnalizer.getParentOfClass(declarator, VariableDeclaration.class);
    Procedure procedure = (Procedure)ObjectAnalizer.getParentOfClass(vd, Procedure.class);
    if (procedure == null)
    {
      Program program = (Program)ObjectAnalizer.getParentOfClass(vd, Program.class);
      procedure = ObjectAnalizer.findMainProcedure(program);
    }
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    while (iter.hasNext())
    {
      FunctionCall functionCall = null;
      try
      {
        functionCall = (FunctionCall)iter.next(FunctionCall.class);
        Identifier procedureName = (Identifier)functionCall.getName();
        if (procedureName.equals(new Identifier("malloc")))
        {
          AssignmentExpression assignment = (AssignmentExpression)ObjectAnalizer.getParentOfClass(functionCall, AssignmentExpression.class);
          if (assignment != null)
          {
            Identifier lhs = (Identifier)assignment.getLHS();
            if (lhs.equals(id))
            {
              Expression mallocParameter = functionCall.getArgument(0);
              
              SizeofExpression sizeofExpr = new SizeofExpression(vd.getSpecifiers());
              return new BinaryExpression((Expression)mallocParameter.clone(), BinaryOperator.DIVIDE, sizeofExpr);
            }
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    if (vd.getSpecifiers().contains(Specifier.CHAR))
    {
      FunctionCall functionCall = new FunctionCall(new Identifier("strlen"));
      functionCall.addArgument((Identifier)id.clone());
      
      return functionCall;
    }
    return new IntegerLiteral(1L);
  }
}
