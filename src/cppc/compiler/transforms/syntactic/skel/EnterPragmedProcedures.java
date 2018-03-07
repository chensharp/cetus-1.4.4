package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.util.NoSuchElementException;

public class EnterPragmedProcedures
  extends ProcedureWalker
{
  private static String passName = "[EnterPragmedProcedures]";
  
  private EnterPragmedProcedures(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    EnterPragmedProcedures transform = new EnterPragmedProcedures(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    while (iter.hasNext()) {
      try
      {
        FunctionCall call = (FunctionCall)iter.next(FunctionCall.class);
        Identifier fName = (Identifier)call.getName();
        if (CppcRegisterManager.isRegistered(fName))
        {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            fName);
          if (c.getPragmed())
          {
            Statement callStatement = 
              (Statement)ObjectAnalizer.getParentOfClass(call, 
              Statement.class);
            
            Statement push = addContextPushBefore(callStatement, 
              (Identifier)call.getName());
            Statement pop = addContextPopAfter(callStatement);
            
            CppcExecutePragma pragma = new CppcExecutePragma(push, pop);
            CompoundStatement statementList = (CompoundStatement)
              ObjectAnalizer.getParentOfClass(push, CompoundStatement.class);
            statementList.addStatementBefore(push, pragma);
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
  
  private Statement addContextPushBefore(Statement ref, Identifier functionName)
  {
    FunctionCall call = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().CONTEXT_PUSH_FUNCTION()));
    
    call.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      functionName.toString()));
    
    call.addArgument(new IntegerLiteral(ref.where()));
    
    Statement callStatement = new ExpressionStatement(call);
    
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(ref, 
      CompoundStatement.class);
    statementList.addStatementBefore(ref, callStatement);
    
    return callStatement;
  }
  
  private Statement addContextPopAfter(Statement ref)
  {
    FunctionCall call = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().CONTEXT_POP_FUNCTION()));
    
    Statement callStatement = new ExpressionStatement(call);
    
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(ref, 
      CompoundStatement.class);
    statementList.addStatementAfter(ref, callStatement);
    
    return callStatement;
  }
}
