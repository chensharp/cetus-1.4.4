package cppc.compiler.transforms.syntactic.skel.modules;

import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;
import cppc.compiler.cetus.CppcUnregisterPragma;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

public abstract class CppcUnregisterTranslator
  extends TranslationModule<CppcUnregisterPragma>
{
  public Class getTargetClass()
  {
    return CppcUnregisterPragma.class;
  }
  
  public void translate(CppcUnregisterPragma pragma)
  {
    Statement after = pragma;
    for (Identifier unregister : pragma.getUnregisters()) {
      try
      {
        after = addCppcUnregisterCall(after, unregister.toString());
      }
      catch (SymbolNotDefinedException e)
      {
        String message = "Warning: symbol '" + unregister + 
          "'not unregistered: it is not defined in this scope";
        printErrorInTranslation(System.out, pragma, message);
      }
      catch (SymbolIsNotVariableException e)
      {
        String message = "Warning: symbol '" + unregister + 
          "' not unregistered: it is not a variable symbol";
        printErrorInTranslation(System.out, pragma, message);
      }
    }
    pragma.detach();
  }
  
  private Statement addCppcUnregisterCall(Statement after, String unregister)
    throws SymbolNotDefinedException, SymbolIsNotVariableException
  {
    SymbolTable symbolTable = (SymbolTable)after.getParent();
    
    Declaration decl = symbolTable.findSymbol(new Identifier(unregister));
    if (decl == null) {
      throw new SymbolNotDefinedException(unregister);
    }
    if (!(decl instanceof VariableDeclaration)) {
      throw new SymbolIsNotVariableException(unregister);
    }
    VariableDeclaration varDecl = (VariableDeclaration)decl;
    
    FunctionCall cppcUnregisterCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().UNREGISTER_FUNCTION()));
    
    cppcUnregisterCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      unregister));
    ExpressionStatement cppcUnregisterCallStatement = new ExpressionStatement(
      cppcUnregisterCall);
    
    CompoundStatement statementList = (CompoundStatement)after.getParent();
    statementList.addStatementAfter(after, cppcUnregisterCallStatement);
    
    return cppcUnregisterCallStatement;
  }
}
