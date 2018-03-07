package cppc.compiler.transforms.semantic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Tools;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

public abstract class AddOpenFilesControl
  extends ProcedureWalker
{
  private static String passName = "[AddOpenFilesControl]";
  protected static final String CPPC_IO_OPEN_ROLE = "CPPC/IO/Open";
  protected static final String CPPC_IO_CLOSE_ROLE = "CPPC/IO/Close";
  private static final HashSet<Identifier> ioOpenFunctions = initHashSet(
    "CPPC/IO/Open");
  private static final HashSet<Identifier> ioCloseFunctions = initHashSet(
    "CPPC/IO/Close");
  
  private static final HashSet<Identifier> initHashSet(String role)
  {
    return CppcRegisterManager.getProceduresWithRole(role);
  }
  
  protected AddOpenFilesControl(Program program)
  {
    super(program);
  }
  
  private static final AddOpenFilesControl getTransformInstance(Program program)
  {
    String className = ConfigurationManager.getOption(
      "CPPC/Transforms/AddOpenFilesControl/ClassName");
    try
    {
      Class theClass = Class.forName(className);
      Class[] param = { Program.class };
      Method instancer = theClass.getMethod("getTransformInstance", param);
      return (AddOpenFilesControl)instancer.invoke(
        null, new Object[] { program });
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
    
    AddOpenFilesControl transform = getTransformInstance(program);
    transform.start();
    
    Tools.printlnStatus(passName + " begin", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    ArrayList<FunctionCall> openingFunctions = new ArrayList();
    ArrayList<FunctionCall> closingFunctions = new ArrayList();
    try
    {
      while (iter.hasNext())
      {
        FunctionCall call = (FunctionCall)iter.next(FunctionCall.class);
        if (ioOpenFunctions.contains(call.getName())) {
          openingFunctions.add(call);
        }
        if (ioCloseFunctions.contains(call.getName())) {
          closingFunctions.add(call);
        }
      }
    }
    catch (NoSuchElementException localNoSuchElementException) {}
    for (FunctionCall call : openingFunctions)
    {
      Statement cppcCall = getCppcOpenCall(call);
      if (cppcCall != null)
      {
        CompoundStatement statementList = 
          (CompoundStatement)ObjectAnalizer.getParentOfClass(call, 
          CompoundStatement.class);
        Statement s = (Statement)ObjectAnalizer.getParentOfClass(call, 
          Statement.class);
        statementList.addStatementAfter(s, cppcCall);
        ObjectAnalizer.encloseWithExecutes(cppcCall);
      }
    }
    for (FunctionCall call : closingFunctions)
    {
      Statement cppcCall = getCppcCloseCall(call);
      if (cppcCall != null)
      {
        CompoundStatement statementList = 
          (CompoundStatement)ObjectAnalizer.getParentOfClass(call, 
          CompoundStatement.class);
        Statement s = (Statement)ObjectAnalizer.getParentOfClass(call, 
          Statement.class);
        statementList.addStatementAfter(s, cppcCall);
      }
    }
  }
  
  protected abstract Statement getCppcOpenCall(FunctionCall paramFunctionCall);
  
  protected abstract Statement getCppcCloseCall(FunctionCall paramFunctionCall);
}
