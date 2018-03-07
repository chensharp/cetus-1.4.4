package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.CompoundStatement;
import cetus.hir.DeclarationStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.SymbolTable;
import cetus.hir.Tools;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;

import cppc.compiler.analysis.StatementAnalyzer;
import cppc.compiler.cetus.CppcCheckpointPragma;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcNonportableMark;
import cppc.compiler.cetus.CppcPragmaStatement;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.cetus.CppcUnregisterPragma;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.SetOperations;
import cppc.compiler.utils.VariableSizeAnalizer;
import cppc.compiler.utils.VariableSizeAnalizerFactory;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class CppcDependenciesAnalizer
{
  private static final String passName = "[CppcDependenciesAnalizer]";
  private static final Set<Identifier> globalRegisters = new HashSet();
  private static final Set<Procedure> analyzed = new HashSet();
  private Set<Identifier> generated;
  private Set<Identifier> registered;
  private Set<Identifier> commitedRegisters;
  
  protected CppcDependenciesAnalizer()
  {
    this.generated = new HashSet();
    this.registered = new HashSet();
    this.commitedRegisters = new HashSet();
  }
  
  public static void run(Program program)
  {
    Tools.printlnStatus("[CppcDependenciesAnalizer] begin", 1);
    
    CppcDependenciesAnalizer transform = new CppcDependenciesAnalizer();
    transform.start(program);
    
    Tools.printlnStatus("[CppcDependenciesAnalizer] end", 1);
  }
  
  protected void start(Program program)
  {
    Procedure main = ObjectAnalizer.findMainProcedure(program);
    walkOverProcedure(main);
  }
  
  protected void walkOverProcedure(Procedure procedure)
  {
    if (analyzed.contains(procedure)) {
      return;
    }
    this.generated.clear();
    this.registered.clear();
    this.commitedRegisters.clear();
    analyzed.add(procedure);
    
    Set<Identifier> generatedInInitializers = generatedInInitializers(procedure);
    this.generated.addAll(generatedInInitializers);
    
    DepthFirstIterator procIter = new DepthFirstIterator(procedure);
    procIter.pruneOn(Expression.class);
    while (procIter.hasNext())
    {
      IfStatement ifStatement = null;
      Statement thenStatement = null;
      Statement elseStatement = null;
      try
      {
        ifStatement = (IfStatement)procIter.next(IfStatement.class);
        thenStatement = ifStatement.getThenStatement();
        elseStatement = ifStatement.getElseStatement();
      }
      catch (NoSuchElementException e) {}
      continue;
      try
      {
        DepthFirstIterator thenIter = new DepthFirstIterator(thenStatement);
        thenIter.pruneOn(Expression.class);
        
        CppcPragmaStatement pragma = (CppcPragmaStatement)thenIter.next(CppcPragmaStatement.class);
        CppcNonportableMark mark = new CppcNonportableMark(ifStatement.getControlExpression());
        Statement ref = ifStatement;
        while (!(ref.getParent() instanceof CompoundStatement)) {
          ref = (Statement)ref.getParent();
        }
        CompoundStatement list = (CompoundStatement)ref.getParent();
        list.addStatementBefore(ref, mark);
      }
      catch (NoSuchElementException e)
      {
        try
        {
          if (elseStatement != null)
          {
            DepthFirstIterator elseIter = new DepthFirstIterator(elseStatement);
            elseIter.pruneOn(Expression.class);
            
            CppcPragmaStatement pragma = (CppcPragmaStatement)elseIter.next(CppcPragmaStatement.class);
            CppcNonportableMark mark = new CppcNonportableMark(ifStatement.getControlExpression());
            
            Statement ref = ifStatement;
            while (!(ref.getParent() instanceof CompoundStatement)) {
              ref = (Statement)ref.getParent();
            }
            CompoundStatement list = (CompoundStatement)ref.getParent();
            list.addStatementBefore(ref, mark);
          }
        }
        catch (NoSuchElementException ex) {}
      }
    }
    procIter.reset();
    procIter.pruneOn(Expression.class);
    while (procIter.hasNext()) {
      try
      {
        Statement s = (Statement)procIter.next(Statement.class);
        if ((s instanceof CppcPragmaStatement))
        {
          if ((s instanceof CppcCheckpointPragma))
          {
            this.generated.clear();
            this.generated.addAll(this.registered);
            this.generated.addAll(generatedInInitializers);
            processCheckpointBlock((CppcCheckpointPragma)s);
          }
          else if ((s instanceof CppcExecutePragma))
          {
            processExecuteBlock((CppcExecutePragma)s);
          }
        }
        else if (((s instanceof ExpressionStatement)) && ((((ExpressionStatement)s).getExpression() instanceof FunctionCall)))
        {
          FunctionCall call = (FunctionCall)((ExpressionStatement)s).getExpression();
          if (CppcRegisterManager.isRegistered((Identifier)call.getName()))
          {
            ProcedureCharacterization c = CppcRegisterManager.getCharacterization((Identifier)call.getName());
            if (c.getCheckpointed()) {
              processCheckpointBlock(call);
            }
            if (call.getProcedure() != null)
            {
              Set<Identifier> generatedCopy = new HashSet(this.generated);
              Set<Identifier> registeredCopy = new HashSet(this.registered);
              Set<Identifier> commitedRegistersCopy = new HashSet(this.commitedRegisters);
              walkOverProcedure(call.getProcedure());
              this.generated = generatedCopy;
              this.registered = registeredCopy;
              this.commitedRegisters = commitedRegistersCopy;
            }
          }
        }
      }
      catch (NoSuchElementException e) {}
    }
  }
  
  private Set<Identifier> generatedInInitializers(Procedure procedure)
  {
    Set<Identifier> declarationGenerated = new HashSet();
    
    DepthFirstIterator declIter = new DepthFirstIterator(procedure.getBody());
    CppcStatement cppcStatement = null;
    while (declIter.hasNext())
    {
      try
      {
        cppcStatement = (CppcStatement)declIter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e)
      {
        this.generated.addAll(declarationGenerated);
      }
      if ((cppcStatement.getStatement() instanceof DeclarationStatement)) {
        declarationGenerated.addAll(cppcStatement.getGenerated());
      }
    }
    return declarationGenerated;
  }
  
  private void processExecuteBlock(CppcExecutePragma pragma)
  {
    CppcStatement begin = (CppcStatement)ObjectAnalizer.getParentOfClass(pragma.getBegin(), CppcStatement.class);
    CppcStatement end = (CppcStatement)ObjectAnalizer.getParentOfClass(pragma.getEnd(), CppcStatement.class);
    
    Set<Identifier> blockGenerated = new HashSet();
    Set<Identifier> blockConsumed = new HashSet();
    Set<Identifier> blockInitialized = new HashSet();
    
    blockGenerated.addAll(this.generated);
    ObjectAnalizer.characterizeBlock(begin, end, blockGenerated, blockConsumed, blockInitialized);
    this.generated.addAll(blockInitialized);
    
    SetOperations<Identifier> setOps = new SetOperations();
    Set<Identifier> required = setOps.setMinus(blockConsumed, this.generated);
    if (!required.isEmpty())
    {
      List<CppcRegister> registerContent = buildRegisterList(begin, required);
      addRegisterPragmaBefore(begin, registerContent);
    }
    this.generated.addAll(blockGenerated);
    this.generated.addAll(required);
    this.registered.addAll(required);
  }
  
  private void processCheckpointBlock(Traversable t)
  {
    Procedure procedure = (Procedure)ObjectAnalizer.getParentOfClass(t.getParent(), Procedure.class);
    CompoundStatement statementList = procedure.getBody();
    DepthFirstIterator procIter = new DepthFirstIterator(statementList);
    CppcStatement lastCppcStatement = null;
    while (procIter.hasNext()) {
      try
      {
        lastCppcStatement = (CppcStatement)procIter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e) {}
    }
    Statement chkptStatement = (Statement)ObjectAnalizer.getParentOfClass(t, CppcStatement.class);
    
    Set<Identifier> blockGenerated = new HashSet();
    Set<Identifier> blockConsumed = new HashSet();
    Set<Identifier> blockInitialized = new HashSet();
    if ((t instanceof FunctionCall))
    {
      FunctionCall call = (FunctionCall)t;
      for (int i = 0; i < call.getNumArguments(); i++)
      {
        Expression expr = call.getArgument(i);
        if ((expr instanceof Identifier)) {
          blockConsumed.add((Identifier)expr);
        }
      }
    }
    ObjectAnalizer.characterizeBlock(chkptStatement, lastCppcStatement, blockGenerated, blockConsumed, blockInitialized);
    this.generated.addAll(blockInitialized);
    
    SetOperations<Identifier> setOps = new SetOperations();
    Set<Identifier> required = setOps.setMinus(blockConsumed, this.generated);
    
    Statement ref = chkptStatement;
    if (!required.isEmpty())
    {
      List<CppcRegister> registerContent = buildRegisterList(chkptStatement, required);
      ref = addRegisterPragmaBefore(chkptStatement, registerContent);
    }
    Set<Identifier> unrequired = setOps.setMinus(this.commitedRegisters, blockConsumed);
    
    this.registered.addAll(required);
    if (!unrequired.isEmpty())
    {
      addUnregisterPragmaBefore(ref, unrequired);
      this.registered = setOps.setMinus(this.registered, unrequired);
      this.generated = setOps.setMinus(this.generated, unrequired);
    }
    this.generated.addAll(blockGenerated);
    this.generated.addAll(required);
    this.commitedRegisters.addAll(this.registered);
  }
  
  private List<CppcRegister> buildRegisterList(Statement reference, Set<Identifier> set)
  {
    Iterator<Identifier> iter = set.iterator();
    Procedure procedure = reference.getProcedure();
    CompoundStatement statementList = procedure.getBody();
    SetOperations<Identifier> setOps = new SetOperations();
    ArrayList<CppcRegister> returnList = new ArrayList();
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    while (iter.hasNext())
    {
      Identifier id = (Identifier)iter.next();
      
      VariableDeclaration vd = null;
      try
      {
        vd = analyzer.getVariableDeclaration(procedure, id);
      }
      catch (Exception e)
      {
        try
        {
          vd = analyzer.getVariableDeclaration(reference, id);
        }
        catch (Exception ex) {}
      }
      if (vd == null)
      {
        iter.remove();
      }
      else if (!procedure.getParameters().contains(vd))
      {
        SymbolTable table = (SymbolTable)ObjectAnalizer.getParentOfClass(reference, SymbolTable.class);
        if (ObjectAnalizer.isGlobal(id, table))
        {
          if (!globalRegisters.contains(id)) {
            globalRegisters.add(id);
          }
        }
        else
        {
          Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize(id, reference);
          if (size != null) {
            returnList.addAll(getSizeDependencies(size, reference, null));
          }
          returnList.add(new CppcRegister(id, size));
        }
      }
    }
    return returnList;
  }
  
  private List<CppcRegister> getSizeDependencies(Expression size, Statement reference, List<CppcRegister> former)
  {
    if (former == null) {
      former = new ArrayList();
    }
    if ((size instanceof FunctionCall)) {
      return former;
    }
    ExpressionStatement statement = new ExpressionStatement(size);
    CppcStatement cppcStatement = new CppcStatement(statement);
    StatementAnalyzer.analyzeStatement(cppcStatement);
    Set<Identifier> deps = cppcStatement.getConsumed();
    Iterator<Identifier> iter = deps.iterator();
    while (iter.hasNext())
    {
      Identifier id = (Identifier)iter.next();
      if (!this.generated.contains(id))
      {
        Expression innerSize = VariableSizeAnalizerFactory.getAnalizer().getSize(id, reference);
        if (innerSize != null) {
          getSizeDependencies(innerSize, reference, former);
        }
        former.add(new CppcRegister(id, innerSize));
        this.generated.add(id);
      }
    }
    return former;
  }
  
  protected CppcRegisterPragma addRegisterPragmaBefore(Statement reference, List<CppcRegister> registers)
  {
    if (LanguageAnalyzerFactory.getLanguageAnalyzer().insideLoop(reference))
    {
      Statement loop = (Statement)ObjectAnalizer.getParentOfClass(reference, Loop.class);
      
      Statement newRef = (Statement)loop.getParent();
      
      return addRegisterPragmaBefore(newRef, registers);
    }
    CompoundStatement statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass(reference, CompoundStatement.class);
    
    int index = statementList.getChildren().indexOf(reference);
    try
    {
      CppcStatement t = (CppcStatement)statementList.getChildren().get(index - 1);
      if ((t.getStatement() instanceof CppcExecutePragma)) {
        return addRegisterPragmaBefore(t, registers);
      }
    }
    catch (ClassCastException e) {}catch (ArrayIndexOutOfBoundsException e) {}
    CppcRegisterPragma pragma = new CppcRegisterPragma(registers);
    statementList.addStatementBefore(reference, pragma);
    pragma.setLineNumber(reference.where());
    
    return pragma;
  }
  
  private CppcUnregisterPragma addUnregisterPragmaBefore(Statement reference, Set<Identifier> unregisters)
  {
    CppcUnregisterPragma pragma = new CppcUnregisterPragma();
    Iterator<Identifier> iter = unregisters.iterator();
    while (iter.hasNext()) {
      pragma.addUnregister((Identifier)iter.next());
    }
    CompoundStatement statementList = (CompoundStatement)ObjectAnalizer.getParentOfClass(reference, CompoundStatement.class);
    
    statementList.addStatementBefore(reference, pragma);
    
    return pragma;
  }
}
