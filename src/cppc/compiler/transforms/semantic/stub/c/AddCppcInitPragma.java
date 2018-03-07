package cppc.compiler.transforms.semantic.stub.c;

import cetus.hir.ArraySpecifier;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.FunctionCall;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.PointerSpecifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

import java.util.ArrayList;
import java.util.List;


//继承skel的虚类型，
//addInitFunctionParameters （）和 checkMainProcedure（）两个函数的实现
//
public class AddCppcInitPragma extends cppc.compiler.transforms.semantic.skel.AddCppcInitPragma
{
  private AddCppcInitPragma(Program program)
  {
    super(program);
  }
  
  public static final AddCppcInitPragma getTransformInstance(Program program)
  {
    return new AddCppcInitPragma(program);
  }
  
  protected void checkMainProcedure(Procedure mainProc)
  {
    if (mainProc.getParameters().size() == 0)
    {
      addArgsc(mainProc);
      addArgsv(mainProc);
      return;
    }

    VariableDeclaration firstParameter = (VariableDeclaration)mainProc.getParameters().get(0);
    
    if (!argscCheck(firstParameter)) {
      addArgsc(mainProc);
    }
    
    if (mainProc.getParameters().size() == 1)
    {
      addArgsv(mainProc);
      return;
    }
    
    VariableDeclaration secondParameter = (VariableDeclaration)mainProc.getParameters().get(1);
    
    if (!argsvCheck(secondParameter)) {
      addArgsv(mainProc);
    }
  }
  
  private boolean argscCheck(VariableDeclaration decl)
  {
    List declSpecifiers = decl.getSpecifiers();
    if (declSpecifiers.size() != 1) {
      return false;
    }
    Specifier specifier = (Specifier)declSpecifiers.get(0);
    return specifier.equals(Specifier.INT);
  }
  
  private boolean argsvCheck(VariableDeclaration decl)
  {
    List declSpecifiers = decl.getSpecifiers();
    if (declSpecifiers.size() != 1) {
      return false;
    }
    if (!declSpecifiers.get(0).equals(Specifier.CHAR)) {
      return false;
    }
    if (decl.getNumDeclarators() != 1) {
      return false;
    }
    Declarator declarator = decl.getDeclarator(0);
    List pointerSpecifiers = declarator.getSpecifiers();
    if (pointerSpecifiers.size() == 0) {
      return false;
    }
    if (pointerSpecifiers.size() == 1)
    {
      if (!pointerSpecifiers.get(0).equals(PointerSpecifier.UNQUALIFIED)) {
        return false;
      }
      List arraySpecifiers = declarator.getArraySpecifiers();
      if (arraySpecifiers.size() != 1) {
        return false;
      }
      if (!arraySpecifiers.get(0).equals(ArraySpecifier.UNBOUNDED)) {
        return false;
      }
      return true;
    }
    if (pointerSpecifiers.size() == 2)
    {
      if (!pointerSpecifiers.get(0).equals(PointerSpecifier.UNQUALIFIED)) {
        return false;
      }
      if (!pointerSpecifiers.get(1).equals(PointerSpecifier.UNQUALIFIED)) {
        return false;
      }
      return true;
    }
    return false;
  }
  
  private void addArgsc(Procedure p)
  {
    Identifier argscIdentifier = new Identifier("argsc");
    VariableDeclarator argscDeclarator = new VariableDeclarator(
      argscIdentifier);
    VariableDeclaration argscDeclaration = new VariableDeclaration(
      Specifier.INT, argscDeclarator);
    
    List procedureParameters = p.getParameters();
    if (procedureParameters.size() == 0) {
      p.addDeclaration(argscDeclaration);
    } else {
      p.addDeclarationBefore((Declaration)procedureParameters.get(0), 
        argscDeclaration);
    }
  }
  
  private void addArgsv(Procedure p)
  {
    ArrayList<Specifier> argsvType = new ArrayList();
    argsvType.add(Specifier.CHAR);
    argsvType.add(PointerSpecifier.UNQUALIFIED);
    argsvType.add(PointerSpecifier.UNQUALIFIED);
    Identifier argsvIdentifier = new Identifier("argsv");
    VariableDeclarator argsvDeclarator = new VariableDeclarator(
      argsvIdentifier);
    VariableDeclaration argsvDeclaration = new VariableDeclaration(argsvType, 
      argsvDeclarator);
    
    p.addDeclaration(argsvDeclaration);
  }
  
  protected void addInitFunctionParameters(Procedure mainProc, FunctionCall call)
  {
    Identifier argscIdentifier = 
      (Identifier)((VariableDeclaration)mainProc.getParameters().get(0))
      .getDeclarator(0).getSymbol().clone();
    UnaryExpression argscReference = new UnaryExpression(
      UnaryOperator.ADDRESS_OF, argscIdentifier);
    
    Identifier argsvIdentifier = 
      (Identifier)((VariableDeclaration)mainProc.getParameters().get(1))
      .getDeclarator(0).getSymbol().clone();
    UnaryExpression argsvReference = new UnaryExpression(
      UnaryOperator.ADDRESS_OF, argsvIdentifier);
    
    call.addArgument(argscReference);
    call.addArgument(argsvReference);
  }
}
