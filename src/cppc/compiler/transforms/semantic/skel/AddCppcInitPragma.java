package cppc.compiler.transforms.semantic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cetus.hir.PrintTools;
import cetus.hir.Traversable;

import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.transforms.shared.CppcRegisterManager;

import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

//抽象类，有一些函数未实现，在后续类中实现，通过chendriver里参数控制。
public abstract class AddCppcInitPragma
{
  private Program program;
  private static final String passName = "[AddCppcInitPragma]";
  private static final String CPPC_COMM_INITIALIZER_ROLE = "CPPC/Comm/Initializer";
  private static final HashSet<Identifier> commInitializerFunctions = initHashSet("CPPC/Comm/Initializer");
  
  //初始化hashset ，
  private static final HashSet<Identifier> initHashSet(String role)
  {
    return CppcRegisterManager.getProceduresWithRole(role);
  }
  
  private static final String initString(String[] i)
  {
    String s = i[0];
    for (int j = 1; j < i.length; j++) {
      s = s + " " + i[j];
    }
    return s;
  }
  
  protected AddCppcInitPragma(Program program)
  {
    this.program = program;
  }
  
  private static final AddCppcInitPragma getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption(
      "CPPC/Transforms/AddCppcInitPragma/ClassName");//决定了使用c还是Fortran类型的具体类
    try
    {
      Class theClass = Class.forName(className);//取出class类型
      Class[] param = { Program.class };     //构造参数
      Method instancer = theClass.getMethod("getTransformInstance", param);//取出类型的函数
      return (AddCppcInitPragma)instancer.invoke(null, new Object[] { program });//调用该函数并返回一个该抽象类类型 对象。
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
    return null;
  }
  
  //启动函数，所有pass全类似的规格
  public static void run(Program program)
  {

    double timer = Tools.getTime();
    PrintTools.println(passName + " begin", 0);

    AddCppcInitPragma transform = getTransformInstance(program);
    transform.start();
    
    PrintTools.println(passName + " end in " + String.format("%.2f seconds", Tools.getTime(timer)), 0);

  }
  
  //操作函数，
  private void start()
  {
    Statement ref = insertCppcInitConfiguration();//
    
    DepthFirstIterator iter = new DepthFirstIterator(this.program);
    ArrayList<FunctionCall> calls = new ArrayList(1);
    
    try
    {
      while (iter.hasNext())
      {
        FunctionCall call = (FunctionCall)iter.next(FunctionCall.class);
        if (commInitializerFunctions.contains(call.getName())) {
          calls.add(call);
        }
      }
    }
    catch (NoSuchElementException localNoSuchElementException) 
    {

    }

    if (calls.size() > 1)//数组大于一个，报错
    {
      System.err.println("Error: Program has more than one call to a CPPC/Comm/Initializer role.");
      
      System.err.println("\tIn cppc.compiler.cetus.transforms.semantic.skel.AddCppcInitPragma.start()");
      
      System.exit(0);
    }

    if (calls.size() == 0) {// 数组为空，
      insertCppcInitState(ref, false);
    
    } else {
      
      insertCppcInitState((Statement)ObjectAnalizer.getParentOfClass( (Traversable)calls.get(0), Statement.class), true);
    }
  }
  
  protected abstract void checkMainProcedure(Procedure paramProcedure);
  
  protected abstract void addInitFunctionParameters(Procedure paramProcedure, FunctionCall paramFunctionCall);
  
  //
  private Statement insertCppcInitConfiguration()
  {
    Procedure mainProc = ObjectAnalizer.findMainProcedure(this.program);
    CompoundStatement mainCode = mainProc.getBody();
    
    checkMainProcedure(mainProc);
    
    FunctionCall call = new FunctionCall(new Identifier( GlobalNamesFactory.getGlobalNames().INIT_CONFIGURATION_FUNCTION()));
    addInitFunctionParameters(mainProc, call);
    ExpressionStatement callStmt = new ExpressionStatement(call);
    Statement ref = ObjectAnalizer.findLastDeclaration(mainProc);
    if (ref != null)
    {
      mainCode.addStatementAfter(ref, callStmt);
    }
    else
    {
      ref = (Statement)mainCode.getChildren().get(0);
      mainCode.addStatementBefore(ref, callStmt);
    }
    CppcConditionalJump jump = new CppcConditionalJump();
    mainCode.addStatementAfter(callStmt, jump);
    
    return jump;
  }
  
  private void insertCppcInitState(Statement ref, boolean foundComm)
  {
    CompoundStatement statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass(ref, CompoundStatement.class);
    
    FunctionCall call = new FunctionCall(new Identifier( GlobalNamesFactory.getGlobalNames().INIT_STATE_FUNCTION()));
    ExpressionStatement callStmt = new ExpressionStatement(call);
    statementList.addStatementAfter(ref, callStmt);
    
    CppcLabel label = new CppcLabel(new Identifier( GlobalNamesFactory.getGlobalNames().CHECKPOINT_LABEL()));
    if (foundComm) {
      statementList.addStatementBefore(ref, label);
    } else {
      statementList.addStatementBefore(callStmt, label);
    }
    CppcConditionalJump jump = new CppcConditionalJump();
    statementList.addStatementAfter(callStmt, jump);
  }
}
