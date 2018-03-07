package cppc.compiler.transforms.semantic.skel;

import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.NullStatement;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Tools;
import cppc.compiler.analysis.CommunicationAnalyzer;
import cppc.compiler.analysis.ExpressionAnalyzer;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.transforms.shared.ProcedureWalker;
import cppc.compiler.transforms.shared.comms.Communication;
import cppc.compiler.utils.ConfigurationManager;
import cppc.compiler.utils.ObjectAnalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class CommunicationMatching
  extends ProcedureWalker
{
  private static final String passName = "[CommunicationMatching]";
  private static Map<Identifier, Set<Identifier>> communicationVariables = new HashMap();
  private static boolean modified = true;
  
  protected CommunicationMatching(Program program)
  {
    super(program);
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[CommunicationMatching] begin", 1);
    if (ConfigurationManager.hasOption("DisableCommAnalysis")) {
      return;
    }
    CommunicationMatching transform = new CommunicationMatching(program);
    transform.start();
    
    Set<Identifier> remove = new HashSet();
    for (Identifier procName : communicationVariables.keySet()) {
      if (((Set)communicationVariables.get(procName)).isEmpty()) {
        remove.add(procName);
      }
    }
    for (Identifier procName : remove) {
      communicationVariables.remove(procName);
    }
    Procedure main = ObjectAnalizer.findMainProcedure(program);
    CommunicationAnalyzer.setCommunicationVariables(communicationVariables);
    CommunicationAnalyzer.analyze(main);
    
    Tools.printlnStatus("[CommunicationMatching] end", 1);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    if (communicationVariables.containsKey((Identifier)procedure.getName())) {
      return;
    }
    Set<Identifier> commVariables = new HashSet();
    
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    ProcedureParameter param;
    while (iter.hasNext())
    {
      FunctionCall call = null;
      try
      {
        call = (FunctionCall)iter.next(FunctionCall.class);
      }
      catch (NoSuchElementException e)
      {
        break;
      }
      String tagString;
      if ((Communication.sendFunctions.contains(call.getName())) || 
        (Communication.recvFunctions.contains(call.getName())) || 
        (Communication.waitFunctions.contains(call.getName())))
      {
        Communication comm = Communication.fromCall(call);
        String sourceString = comm.getProperty("Source");
        String targetString = comm.getProperty("Destination");
        tagString = comm.getProperty("Tag");
        String requestString = comm.getProperty("Request");
        
        int sourcePos = 0;int targetPos = 0;int tagPos = 0;int requestPos = 0;
        CppcStatement helper = null;
        if (sourceString != null)
        {
          sourcePos = new Integer(sourceString).intValue();
          helper = new CppcStatement(new NullStatement());
          commVariables.addAll(ExpressionAnalyzer.analyzeExpression(helper, 
            call.getArgument(sourcePos - 1)));
          commVariables.addAll(helper.getConsumed());
        }
        if (targetString != null)
        {
          targetPos = new Integer(targetString).intValue();
          helper = new CppcStatement(new NullStatement());
          commVariables.addAll(ExpressionAnalyzer.analyzeExpression(helper, 
            call.getArgument(targetPos - 1)));
          commVariables.addAll(helper.getConsumed());
        }
        if (tagString != null)
        {
          tagPos = new Integer(tagString).intValue();
          helper = new CppcStatement(new NullStatement());
          commVariables.addAll(ExpressionAnalyzer.analyzeExpression(helper, 
            call.getArgument(tagPos - 1)));
          commVariables.addAll(helper.getConsumed());
        }
        if (requestString != null)
        {
          requestPos = new Integer(requestString).intValue();
          helper = new CppcStatement(new NullStatement());
          
          ExpressionAnalyzer.analyzeExpression(helper, call.getArgument(
            requestPos - 1));
          commVariables.addAll(helper.getConsumed());
        }
      }
      else
      {
        Set<Identifier> internalVariables = (Set)communicationVariables.get(
          (Identifier)call.getName());
        if ((internalVariables == null) && 
          (call.getProcedure() != null))
        {
          walkOverProcedure(call.getProcedure());
          internalVariables = (Set)communicationVariables.get(
            (Identifier)call.getName());
        }
        if ((internalVariables != null) && 
          (!internalVariables.isEmpty()))
        {
          dependencies = new HashSet();
          for (Identifier id : internalVariables)
          {
            param = ObjectAnalizer.idToProcParam(
              call.getProcedure().getParameters(), id, true);
            if (param != null)
            {
              Expression expr = call.getArgument(param.getPosition());
              CppcStatement nullStmt = 
                new CppcStatement(new NullStatement());
              commVariables.addAll(ExpressionAnalyzer.analyzeExpression(
                nullStmt, expr));
              commVariables.addAll(nullStmt.getConsumed());
            }
            else if (ObjectAnalizer.isGlobal(id, call.getProcedure().getBody()))
            {
              commVariables.add(id);
            }
          }
        }
      }
    }
    ProcedureCharacterization procedureChar = 
      CppcRegisterManager.getCharacterization((Identifier)
      procedure.getName());
    for (Set<Identifier> dependencies = procedureChar.getCalls().iterator(); dependencies.hasNext(); param.hasNext())
    {
      Identifier calleeName = (Identifier)dependencies.next();
      Procedure callee = CppcRegisterManager.getCharacterization(calleeName)
        .getProcedure();
      for (Identifier id : commVariables) {
        if (ObjectAnalizer.isGlobal(id, callee.getBody()))
        {
          ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
            (Identifier)callee.getName());
          
          ((Set)communicationVariables.get(callee.getName())).add(id);
        }
      }
      addVariableDependencies(callee, (Set)communicationVariables.get(
        callee.getName()));
      
      param = ((Set)communicationVariables.get(callee.getName())).iterator(); continue;Identifier id = (Identifier)param.next();
      if (ObjectAnalizer.isGlobal(id, procedure.getBody())) {
        commVariables.add(id);
      }
    }
    addVariableDependencies(procedure, commVariables);
    communicationVariables.put((Identifier)procedure.getName(), commVariables);
  }
  
  private static Set<Identifier> addVariableDependencies(Procedure procedure, Set<Identifier> vars)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)procedure.getName());
    
    int formerSize = 0;
    while (formerSize != vars.size())
    {
      formerSize = vars.size();
      Set<Identifier> add = new HashSet();
      for (Identifier id : vars)
      {
        Set<Identifier> dependencies = c.getVariableDependencies(id);
        if (dependencies != null) {
          add.addAll(dependencies);
        }
      }
      vars.addAll(add);
    }
    return vars;
  }
}
