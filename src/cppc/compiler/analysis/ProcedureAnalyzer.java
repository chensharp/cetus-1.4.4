package cppc.compiler.analysis;

import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.VariableDeclaration;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public final class ProcedureAnalyzer
{
  public static final void analyzeProcedure(Procedure procedure)
  {
    if (!CppcRegisterManager.isRegistered((Identifier)procedure.getName()))
    {
      CompoundStatement statementList = procedure.getBody();
      DepthFirstIterator iter = new DepthFirstIterator(statementList);
      iter.next();
      iter.pruneOn(Statement.class);
      
      ProcedureCharacterization c = new ProcedureCharacterization((Identifier)
        procedure.getName());
      CppcRegisterManager.addProcedure((Identifier)procedure.getName(), c);
      
      long totalWeight = 0L;
      int statementCount = 0;
      while (iter.hasNext()) {
        try
        {
          CppcStatement cppcStatement = (CppcStatement)
            iter.next(CppcStatement.class);
          StatementAnalyzer.analyzeStatement(cppcStatement);
          totalWeight += cppcStatement.getWeight();
          statementCount += cppcStatement.statementCount;
        }
        catch (NoSuchElementException localNoSuchElementException) {}
      }
      c.statementCount = statementCount;
      c.setWeight(totalWeight);
      addProcedureRegister(procedure);
    }
  }
  
  private static void addProcedureRegister(Procedure procedure)
  {
    ProcedureCharacterization characterization = null;
    if (CppcRegisterManager.isRegistered((Identifier)procedure.getName())) {
      characterization = CppcRegisterManager.getCharacterization(
        (Identifier)procedure.getName());
    } else {
      characterization = new ProcedureCharacterization(
        (Identifier)procedure.getName());
    }
    characterization.setProcedure(procedure);
    
    HashSet<Identifier> consumed = new HashSet();
    HashSet<Identifier> generated = new HashSet();
    HashSet<VariableDeclaration> globalConsumed = 
      new HashSet();
    HashSet<VariableDeclaration> globalGenerated = 
      new HashSet();
    Map<Identifier, Set<Identifier>> variableDependencies = 
      new HashMap();
    SetOperations<Identifier> setOps = new SetOperations();
    SetOperations<VariableDeclaration> vdSetOps = 
      new SetOperations();
    Stack<IfStatement> conditionals = new Stack();
    Set<Identifier> conditionalDependencies = new HashSet();
    Set<Identifier> calls = new HashSet();
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    while (iter.hasNext())
    {
      CppcStatement cppcStatement = null;
      Set<Identifier> consumedParameters;
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e)
      {
        Set<Identifier> generatedParameters = filterProcedureParameters(
          procedure, generated);
        consumedParameters = filterProcedureParameters(
          procedure, consumed);
        
        Set<ProcedureParameter> generatedPositions = 
          ObjectAnalizer.setIdToSetProcParam(procedure, generatedParameters);
        Set<ProcedureParameter> consumedPositions = 
          ObjectAnalizer.setIdToSetProcParam(procedure, consumedParameters);
        
        globalConsumed.addAll(vdSetOps.setMinus(filterGlobalVariables(
          procedure, consumed), globalGenerated));
        globalGenerated.addAll(filterGlobalVariables(procedure, generated));
        
        characterization.setGenerated(generatedPositions);
        characterization.setConsumed(consumedPositions);
        characterization.setGlobalGenerated(globalGenerated);
        characterization.setGlobalConsumed(globalConsumed);
        characterization.setVariableDependencies(variableDependencies);
        if (!CppcRegisterManager.isRegistered((Identifier)procedure.getName())) {
          CppcRegisterManager.addProcedure((Identifier)procedure.getName(), 
            characterization);
        }
        return;
      }
      boolean peekParent = false;
      while ((!conditionals.isEmpty()) && (!peekParent))
      {
        IfStatement ifStmt = (IfStatement)ObjectAnalizer.getParentOfClass(
          cppcStatement, IfStatement.class);
        if (ifStmt != conditionals.peek())
        {
          conditionals.pop();
          
          conditionalDependencies = 
            calculateConditionalDependencies(
            conditionals);
        }
        else
        {
          peekParent = true;
        }
      }
      if ((cppcStatement.getStatement() instanceof IfStatement))
      {
        conditionals.push((IfStatement)cppcStatement.getStatement());
        conditionalDependencies = 
          calculateConditionalDependencies(
          conditionals);
      }
      consumed.addAll(setOps.setMinus(cppcStatement.getConsumed(), 
        generated));
      globalConsumed.addAll(vdSetOps.setMinus(
        cppcStatement.getGlobalConsumed(), globalGenerated));
      
      generated.addAll(cppcStatement.getGenerated());
      globalGenerated.addAll(cppcStatement.getGlobalGenerated());
      if (!cppcStatement.getConsumed().isEmpty()) {
        for (Identifier genId : cppcStatement.getPartialGenerated())
        {
          Set<Identifier> dependencies = (Set)variableDependencies.get(genId);
          if (dependencies == null)
          {
            dependencies = new HashSet();
            variableDependencies.put(genId, dependencies);
          }
          dependencies.addAll(cppcStatement.getConsumed());
          
          dependencies.addAll(conditionalDependencies);
        }
      }
    }
    Set<Identifier> generatedParameters = filterProcedureParameters(procedure, 
      generated);
    Set<Identifier> consumedParameters = filterProcedureParameters(procedure, 
      consumed);
    
    Set<ProcedureParameter> generatedPositions = 
      ObjectAnalizer.setIdToSetProcParam(procedure, generatedParameters);
    Set<ProcedureParameter> consumedPositions = 
      ObjectAnalizer.setIdToSetProcParam(procedure, consumedParameters);
    
    globalConsumed.addAll(vdSetOps.setMinus(filterGlobalVariables(
      procedure, consumed), globalGenerated));
    globalGenerated.addAll(filterGlobalVariables(procedure, generated));
    
    characterization.setGenerated(generatedPositions);
    characterization.setConsumed(consumedPositions);
    characterization.setGlobalGenerated(globalGenerated);
    characterization.setGlobalConsumed(globalConsumed);
    characterization.setVariableDependencies(variableDependencies);
    if (!CppcRegisterManager.isRegistered((Identifier)procedure.getName())) {
      CppcRegisterManager.addProcedure((Identifier)procedure.getName(), 
        characterization);
    }
  }
  
  private static Set<Identifier> calculateConditionalDependencies(Stack<IfStatement> conditionals)
  {
    Set<Identifier> conditionalDependencies = new HashSet();
    for (IfStatement ifStmt : conditionals) {
      conditionalDependencies.addAll(ExpressionAnalyzer.analyzeExpression(
        (CppcStatement)ifStmt.getParent(), ifStmt.getControlExpression()));
    }
    return conditionalDependencies;
  }
  /*
  private static Set<Identifier> filterProcedureParameters(Procedure procedure, Set<Identifier> set)
  {
    Iterator iter = procedure.getParameters().iterator();
    HashSet<Identifier> parameters = new HashSet();
    VariableDeclaration declaration;
    int i;
    for (; iter.hasNext(); i < declaration.getNumDeclarators())
    {
      Object obj = iter.next();
      if (!(obj instanceof VariableDeclaration))
      {
        System.err.println("WARNING: cppc.compiler.analysis.ProcedureAnalyzer.filterProcedureParameters() not implemented for " + 
        
          obj.getClass());
        System.exit(0);
      }
      declaration = (VariableDeclaration)obj;
      
      i = 0; continue;
      Declarator declarator = declaration.getDeclarator(i);
      parameters.add((Identifier)declarator.getSymbol().clone());i++;
    }
    SetOperations<Identifier> setOps = new SetOperations();
    return setOps.setIntersection(parameters, set);
  }*/

  private static Set<Identifier> filterProcedureParameters(final Procedure procedure, final Set<Identifier> set) {
    final Iterator iter = procedure.getParameters().iterator();
    final HashSet<Identifier> parameters = new HashSet<Identifier>();
    while (iter.hasNext()) {
      final Object obj = iter.next();
      if (!(obj instanceof VariableDeclaration)) {
        System.err.println("WARNING: cppc.compiler.analysis.ProcedureAnalyzer.filterProcedureParameters() not implemented for " + obj.getClass());
        System.exit(0);
      }
      final VariableDeclaration declaration = (VariableDeclaration)obj;
      for (int i = 0; i < declaration.getNumDeclarators(); ++i) {
        final Declarator declarator = declaration.getDeclarator(i);
        parameters.add((Identifier)declarator.getSymbol().clone());
      }
    }
    final SetOperations<Identifier> setOps = new SetOperations<Identifier>();
    return setOps.setIntersection(parameters, set);
  }




  
  private static Set<VariableDeclaration> filterGlobalVariables(Procedure procedure, Set<Identifier> set)
  {
    HashSet<VariableDeclaration> globals = new HashSet(
      set.size());
    SymbolTable table = procedure.getBody();
    for (Identifier id : set) {
      if (ObjectAnalizer.isGlobal(id, table)) {
        try
        {
          globals.add(
            LanguageAnalyzerFactory.getLanguageAnalyzer().getVariableDeclaration(
            (Statement)procedure.getBody().getChildren().get(0), id));
        }
        catch (Exception localException) {}
      }
    }
    return globals;
  }
}
