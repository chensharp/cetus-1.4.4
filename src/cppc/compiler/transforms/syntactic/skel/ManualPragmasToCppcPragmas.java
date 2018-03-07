package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.*;
import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;

import cppc.compiler.cetus.CppcCheckpointLoopPragma;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;



 

public class ManualPragmasToCppcPragmas extends ProcedureWalker
{
  private static String passName = "[ManualPragmasToCppcPragmas]";
  
  private ManualPragmasToCppcPragmas(Program program)
  {
    super(program);
  }


  public static void run(Program program)
  {

    double timer = Tools.getTime();
    PrintTools.println(passName + " begin", 0);

    if (!ConfigurationManager.hasOption("ManualPragmas")) {//这里应该是命令行参数含有 手动参数的时候调用。
      return;
    }

    ManualPragmasToCppcPragmas transform = new ManualPragmasToCppcPragmas(program);
    transform.start();
    
    PrintTools.println(passName + " end in " + String.format("%.2f seconds", Tools.getTime(timer)), 0);

  }
  
  //遍历函数
  protected void walkOverProcedure(Procedure procedure)
  {
    GlobalNames names = GlobalNamesFactory.getGlobalNames();

    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    
    iter.pruneOn(Expression.class);
    
    while (iter.hasNext()) {
      try
      {
        Annotation annote = (Annotation)iter.next(Annotation.class);
        if (analyzer.annotationIsPragma(annote))
        {
          String pragmaText = analyzer.getPragmaText(annote);
          if (ObjectAnalizer.matchStringWithArray(pragmaText, names.BEGIN_EXECUTE_PRAGMA())) {
            processBeginExecute(annote);
          }
          if (ObjectAnalizer.matchStringWithArray(pragmaText, names.CHECKPOINT_PRAGMA())) {
            processCheckpoint(annote);
          }
          if (ObjectAnalizer.matchStringWithArray(pragmaText, names.CHECKPOINT_LOOP_PRAGMA())) {
            processCheckpointLoop(annote);
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
  }
  
  private void processBeginExecute(Annotation annote)
  {
    GlobalNames names = GlobalNamesFactory.getGlobalNames();
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    
    CompoundStatement body = 
      ((Statement)annote.getParent()).getProcedure().getBody();
    
    DepthFirstIterator iter = new DepthFirstIterator(body);
    iter.pruneOn(Expression.class);
    
    Annotation match = null;
    while ((iter.hasNext()) && (match != annote)) {
      try
      {
        match = (Annotation)iter.next(Annotation.class);
      }
      catch (NoSuchElementException e)
      {
        System.err.println("BUG: cannot find current annote. In ManualPragmasToCppcPragmas.processBeginExecute()");
        
        System.exit(0);
      }
    }
    Statement executeBegin = null;
    try
    {
      executeBegin = (Statement)iter.next(Statement.class);
    }
    catch (NoSuchElementException e)
    {
      System.err.println("BUG: cannot find begin statement. In ManualPragmasToCppcPragmas.processBeginExecute()");
      
      System.exit(0);
    }
    Statement executeEnd = executeBegin;
    while (iter.hasNext()) {
      try
      {
        Statement next = (Statement)iter.next(Statement.class);
        if ((next instanceof DeclarationStatement))
        {
          Declaration declaration = 
            ((DeclarationStatement)next).getDeclaration();
          if ((declaration instanceof Annotation))
          {
            Annotation nextAnnote = (Annotation)declaration;
            if (analyzer.annotationIsPragma(nextAnnote))
            {
              String pragmaText = analyzer.getPragmaText(nextAnnote);
              if (ObjectAnalizer.matchStringWithArray(pragmaText, names.END_EXECUTE_PRAGMA()))
              {
                CppcExecutePragma pragma = new CppcExecutePragma(executeBegin, 
                  executeEnd);
                
                Statement annoteParent = (Statement)annote.getParent();
                annoteParent.swapWith(pragma);
                
                next.detach();
                
                return;
              }
            }
          }
        }
        executeEnd = next;
      }
      catch (NoSuchElementException e)
      {
        Statement annoteParent = (Statement)annote.getParent();
        System.err.println("warning: match not found for CPPC EXECUTE ON RESTART manually placed on line " + 
          annoteParent.where() + 
          ". Ignoring directive.");
      }
    }
  }
  
  private void processCheckpoint(Annotation annote)
  {
    CppcCheckpointPragma pragma = new CppcCheckpointPragma();
    
    Statement annoteParent = (Statement)annote.getParent();
    annoteParent.swapWith(pragma);
  }
  
  private void processCheckpointLoop(Annotation annote)
  {
    CppcCheckpointLoopPragma pragma = new CppcCheckpointLoopPragma();
    
    Statement annoteParent = (Statement)annote.getParent();
    CompoundStatement statementList = (CompoundStatement)
      annoteParent.getParent();
    
    int index = statementList.getChildren().indexOf(annoteParent);
    Statement loop = (Statement)statementList.getChildren().get(index + 1);
    if (!(loop instanceof Loop))
    {
      TranslationUnit tunit = (TranslationUnit)loop.getProcedure().getParent();
      System.err.println(tunit.getInputFilename() + ": " + 
        annoteParent.where() + ": " + 
        "checkpoint loop directive not before loop.");
      System.exit(0);
    }
    Loop safeLoop = (Loop)loop;
    Statement loopBody = safeLoop.getBody();
    if (!(loopBody instanceof CompoundStatement))
    {
      CompoundStatement newBody = new CompoundStatement();
      newBody.addStatement(loopBody);
      loopBody.swapWith(newBody);
      loopBody = newBody;
    }
    ((CompoundStatement)loopBody).getChildren().add(0, pragma);
    pragma.setParent(loopBody);
    pragma.setLineNumber(loopBody.where());
    
    annoteParent.detach();
  }
}
