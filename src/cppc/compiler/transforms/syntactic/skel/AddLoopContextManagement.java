package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.VariableDeclaration;
import cppc.compiler.cetus.CppcNonportableFunctionMark;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.transforms.shared.DataType;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AddLoopContextManagement
  extends ProcedureWalker
{
  private static String passName = "[AddLoopContextManagement]";
  private ArrayList<Statement> processedLoops;
  
  protected AddLoopContextManagement(Program program)
  {
    super(program);
    this.processedLoops = new ArrayList();
  }
  
  private static final AddLoopContextManagement getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption(
      "CPPC/Transforms/AddLoopContextManagement/ClassName");
    try
    {
      Class theClass = Class.forName(className);
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod("getTransformInstance", param);
      return 
        (AddLoopContextManagement)instancer.invoke(null, new Object[] { program });
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    return null;
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus(passName + " begin", 1);
    
    AddLoopContextManagement transform = getTransformInstance(program);
    transform.start();
    
    Tools.printlnStatus(passName + " end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    try
    {
      while (iter.hasNext())
      {
        CppcNonportableFunctionMark mark = 
          (CppcNonportableFunctionMark)iter.next(
          CppcNonportableFunctionMark.class);
        
        Statement ref = (Statement)ObjectAnalizer.getParentOfClass(mark, 
          Statement.class);
        Statement loop = testInsideLoop(ref);
        while ((loop != null) && (!this.processedLoops.contains(loop)))
        {
          processLoop(loop);
          this.processedLoops.add(loop);
          loop = testInsideLoop(loop);
        }
      }
    }
    catch (NoSuchElementException localNoSuchElementException) {}
  }
  
  private void processLoop(Statement loop)
  {
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(loop, 
      CompoundStatement.class);
    
    Identifier loopIndex = getLoopIndex(loop);
    
    VariableDeclaration vd = null;
    try
    {
      vd = LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
        loop, loopIndex);
    }
    catch (SymbolIsNotVariableException e)
    {
      System.err.println("BUG: Symbol " + loopIndex + " is not a variable in current context.\n");
      System.err.println("\tUnable to analyze loop.\n");
      System.err.println("\tAt " + getClass() + ".processLoop");
      return;
    }
    catch (SymbolNotDefinedException e)
    {
      System.err.println("BUG: Symbol " + loopIndex + " not defined in current context.\n");
      System.err.println("\tUnable to analyze loop.\n");
      System.err.println("\tAt " + getClass() + ".processLoop");
      return;
    }
    Identifier cppcType = null;
    try
    {
      cppcType = (Identifier)
        TypeManager.getType(vd.getSpecifiers()).getBaseType().clone();
    }
    catch (Exception e)
    {
      System.err.println("BUG: Type not supported by the CPPC framework.\n");
      System.err.println("\tUnable to analyze loop.\n");
      System.err.println("\tAt " + getClass() + ".processLoop");
      return;
    }
    FunctionCall addIndexCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().ADD_LOOP_INDEX_FUNCTION()));
    addIndexCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().buildStringLiteral(
      loopIndex.toString()));
    addIndexCall.addArgument(cppcType);
    statementList.addStatementBefore(loop, 
      new ExpressionStatement(addIndexCall));
    
    FunctionCall setIndexCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().SET_LOOP_INDEX_FUNCTION()));
    Declarator indexDeclarator = ObjectAnalizer.getDeclarator(vd, loopIndex);
    if (indexDeclarator == null)
    {
      System.err.println("BUG: Declarator not found for loop index.\n");
      System.err.println("\tUnable to analyze loop.\n");
      System.err.println("\tAt " + getClass() + ".processLoop");
      return;
    }
    setIndexCall.addArgument(
      LanguageAnalyzerFactory.getLanguageAnalyzer().getReference(
      indexDeclarator));
    
    CompoundStatement loopBody = (CompoundStatement)((Loop)loop).getBody();
    loopBody.getChildren().add(0, new ExpressionStatement(setIndexCall));
    
    FunctionCall removeIndexCall = new FunctionCall(new Identifier(
      GlobalNamesFactory.getGlobalNames().REMOVE_LOOP_INDEX_FUNCTION()));
    statementList.addStatementAfter(loop, new ExpressionStatement(
      removeIndexCall));
  }
  
  protected abstract Statement testInsideLoop(Statement paramStatement);
  
  protected abstract Identifier getLoopIndex(Statement paramStatement);
}
