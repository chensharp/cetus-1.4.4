package cppc.compiler.utils;


import cetus.hir.*;


import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


//import cppc.compiler.analysis.StatementAnalyzer;
import cppc.compiler.cetus.CppcExecutePragma;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.cetus.CppcRegister;
import cppc.compiler.cetus.CppcRegisterPragma;
import cppc.compiler.cetus.CppcStatement;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
//import cppc.compiler.fortran.CommonBlock;
import cppc.compiler.transforms.shared.ProcedureParameter;
import cppc.compiler.utils.language.LanguageAnalyzer;
import cppc.compiler.utils.language.LanguageAnalyzerFactory;

//有待检查解决，，，


/*
对象分析工具类，全是静态方法，公用。

*/
public final class ObjectAnalizer
{
  private static final String initString(String[] i)//用空格隔开，转换为一个string
  {
    String s = i[0];
    for (int j = 1; j < i.length; j++) {
      s = s + " " + i[j];
    }
    return s;
  }
  
  /*
  查找合适的函数
  */
  public static final Procedure getProcedure(Program program, Identifier name)
  {
    DepthFirstIterator iter = new DepthFirstIterator(program);
    while (iter.hasNext()) {
      try
      {
        Procedure proc = (Procedure)iter.next(Procedure.class);
        if (proc.getName().equals(name)) {
          return proc;
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    return null;
  }
  
  //查找主函数
  public static final Procedure findMainProcedure(Program program)
  {
    DepthFirstIterator iter = new DepthFirstIterator(program);
    Procedure mainProc = null;
    try
    {
      while (iter.hasNext())
      {
        Procedure proc = (Procedure)iter.next(Procedure.class);
        if (isMainProcedure(proc)) {
          if (mainProc == null)
          {
            mainProc = proc;
          }
          else
          {
            System.err.println("error: Duplicate entry procedures.");
            System.err.println("Your application contains more than one \"main\" procedure. Cannot perform analyses.");
            
            System.err.println("Exiting...");
            System.exit(0);
          }
        }
      }
    }
    catch (NoSuchElementException localNoSuchElementException) {}
    if (mainProc != null) {
      return mainProc;
    }
    System.err.println("BUG: No main procedure found.");
    System.err.println("\tIn cppc.compiler.utils.ObjectAnalizer");
    System.exit(0);
    return null;
  }
  
  //是否是chen的函数
  public static final boolean isChenProcedure(Procedure procedure)
  {
    DepthFirstIterator iter = new DepthFirstIterator(procedure);
    try
    {
      CppcLabel localCppcLabel = (CppcLabel)iter.next(CppcLabel.class);
    }
    catch (NoSuchElementException e)
    {
      return false;
    }
    return true;
  }
  
  public static final boolean isMainProcedure(Procedure p)
  {
    String procName = p.getName().toString();
    return (procName.equals("main")) || 
      (procName.equals("__MAIN")) || 
      (procName.equals("MAIN_"));
  }
  
  //
  public static final ProcedureParameter idToProcParam(List parameters, Identifier parameter, boolean recursive)
  {
    if (parameter.equals(new Identifier("..."))) {
      return ProcedureParameter.VARARGS;
    }

    Iterator iter = parameters.iterator();
    for (int i = 0; iter.hasNext(); i++)
    {
      Declaration declaration = (Declaration)iter.next();
      Identifier id = (Identifier)declaration.getDeclaredSymbols().get(0);//出现错误。。！！！！
      if (id.equals(parameter)) {
        return new ProcedureParameter(i);
      }
    }
    if (recursive)
    {
      ProcedureParameter returnVal = idToProcParam(parameters, new Identifier( "__" + parameter.toString()), false);
      if (returnVal != null) {
        return returnVal;
      }
      returnVal = idToProcParam(parameters, new Identifier( parameter.toString() + "_"), false);
      return returnVal;
    }
    return null;
  }
  
  public static final Set<ProcedureParameter> setIdToSetProcParam(Declaration procedureDeclaration, Set<Identifier> localParams)
  {
    List declaredParams = null;
    if ((procedureDeclaration instanceof Procedure)) {
      declaredParams = ((Procedure)procedureDeclaration).getParameters();
    }
    if ((procedureDeclaration instanceof VariableDeclaration)) {
      declaredParams = ((VariableDeclaration)procedureDeclaration).getDeclarator(0).getParameters();
    }
    if (declaredParams == null)
    {
      System.err.println("ERROR: BUG found in " + ObjectAnalizer.class);
      System.err.println("\tMethod: setIdToSetProcParam( cetus.hir.Declaration, java.util.Set<Identifier> )");
      
      System.err.println("\tCause : Passed Declaration not instanceof  VariableDeclaration or Procedure");
      
      System.exit(0);
    }
    Iterator<Identifier> iter = localParams.iterator();
    Set<ProcedureParameter> returnSet = new HashSet();
    while (iter.hasNext())
    {
      ProcedureParameter procParam = idToProcParam(declaredParams, (Identifier)iter.next(), true);
      if (procParam != null) {
        returnSet.add(procParam);
      }
    }
    return returnSet;
  }
  
  public static final SymbolTable getSymbolTable(Traversable t)
  {
    if ((t instanceof SymbolTable)) {
      return (SymbolTable)t;
    }
    if (t == null) {
      return null;
    }
    return getSymbolTable(t.getParent());
  }
  
  public static final boolean matchStringWithArray(String text, String[] parts)
  {
    for (int i = 0; i < parts.length; i++)
    {
      if (!text.startsWith(parts[i])) {
        return false;
      }
      text = text.replaceFirst(parts[i], "");
      text = text.trim();
    }
    return text.length() == 0;
  }
  
  //得到类型的父类型，必须是可遍历类型的
  public static Traversable getParentOfClass(Traversable t, Class c)
  {
    while ((t != null) && (!c.isInstance(t))) {
      t = t.getParent();
    }
    return t;
  }
  
  //全局变量转换
  public static Set<Identifier> globalDeclarationsToSet(Set<VariableDeclaration> globals)
  {
    final HashSet<Identifier> set = new HashSet<Identifier>(globals.size());
    for (final VariableDeclaration vd : globals) {
      for (int i = 0; i < vd.getNumDeclarators(); ++i) {
          set.add((Identifier)vd.getDeclarator(i).getSymbol());
      }
    }
        

    return set;

  }
  
  //
  private static boolean characterizeBlock(CppcStatement stmt, Statement end, Set<Identifier> generated, Set<Identifier> consumed, Set<Identifier> initialized)
  {
    boolean stop = false;
    
    DepthFirstIterator iter = new DepthFirstIterator(stmt);
    CppcStatement cppcStatement = null;
    while (iter.hasNext())
    {
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
      }
      catch (Exception localException) {}
      if (cppcStatement == end)
      {
        stop = true;
        break;
      }
    }
    simpleBlockCharacterization(stmt, cppcStatement, generated, consumed,  initialized);
    return stop;
  }
  
  //
  private static boolean characterizeBlock(IfStatement ifStmt, Statement end, Set<Identifier> generated, Set<Identifier> consumed, Set<Identifier> initialized)
  {
    Set<Identifier> localGenerated = new HashSet(generated);
    CppcStatement thenStmt = (CppcStatement)ifStmt.getThenStatement();
    boolean stop = characterizeBlock(thenStmt, end, localGenerated, consumed, initialized);
    if (stop) {
      return stop;
    }
    CppcStatement elseStmt = (CppcStatement)ifStmt.getElseStatement();
    if (elseStmt != null)
    {
      localGenerated = new HashSet(generated);
      stop = characterizeBlock(elseStmt, end, localGenerated, consumed,  initialized);
    }
    return stop;
  }
  
  private static void simpleBlockCharacterization(Statement begin, Statement end, Set<Identifier> generated, Set<Identifier> consumed, Set<Identifier> initialized)
  {
    Procedure proc = (Procedure)getParentOfClass(begin, Procedure.class);
    CompoundStatement statementList = proc.getBody();
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    
    CppcStatement cppcStatement = null;
    while (cppcStatement != begin) {
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e)
      {
        System.out.println("BUG: Cannot find first Statement in cppc.compiler.utils.ObjectAnalizer.simpleBlockCharacterization");
        System.exit(0);
      }
    }
    SetOperations<Identifier> setOps = new SetOperations();
    consumed.addAll(setOps.setMinus(cppcStatement.getConsumed(), generated));
    consumed.addAll(setOps.setMinus(globalDeclarationsToSet( cppcStatement.getGlobalConsumed()), generated));
    generated.addAll(cppcStatement.getGenerated());
    generated.addAll(globalDeclarationsToSet( cppcStatement.getGlobalGenerated()));
    
    SymbolTable table = (SymbolTable)getParentOfClass( cppcStatement, SymbolTable.class);
    
/*
    Expression sizeExpr;
    Identifier id;
    if (!chenStatement.getGlobalConsumed().isEmpty()) {
      for (Iterator localIterator1 = chenStatement.getGlobalConsumed().iterator(); localIterator1.hasNext(); ???.hasNext())
      {
        VariableDeclaration vd = (VariableDeclaration)localIterator1.next();
        List<Identifier> symbolsToAdd;
        List<Identifier> symbolsToAdd;
        if ((vd.getParent() instanceof CommonBlock)) {
          symbolsToAdd = 
            ((CommonBlock)vd.getParent()).getDeclaredSymbols();
        } else {
          symbolsToAdd = vd.getDeclaredSymbols();
        }
        for (Identifier id : symbolsToAdd)
        {
          SymbolTable exTable = 
            (SymbolTable)getParentOfClass(vd, 
            SymbolTable.class);
          sizeExpr = 
            VariableSizeAnalizerFactory.getAnalizer().getSize(id, 
            (Traversable)exTable);
          if (sizeExpr != null)
          {
            DepthFirstIterator exprIter = new DepthFirstIterator(sizeExpr);
            while (exprIter.hasNext()) {
              try
              {
                Identifier sizeId = (Identifier)exprIter.next(
                  Identifier.class);
                VariableDeclaration sizeVd = analyzer.getVariableDeclaration(
                  (Traversable)exTable, sizeId);
                Identifier newSizeId = 
                  addClonedVariableDeclaration(sizeVd, sizeId, 
                  cppcStatement);
                if (newSizeId != sizeId) {
                  sizeId.swapWith(newSizeId);
                }
                if (getDeclarator(sizeVd, 
                  sizeId).getInitializer() != null) {
                  initialized.add(newSizeId);
                }
              }
              catch (NoSuchElementException localNoSuchElementException1) {}catch (SymbolNotDefinedException localSymbolNotDefinedException) {}catch (SymbolIsNotVariableException localSymbolIsNotVariableException) {}
            }
          }
        }
        ??? = vd.getDeclaredSymbols().iterator(); continue;id = (Identifier)???.next();
        Identifier newId = addClonedVariableDeclaration(vd, 
          id, cppcStatement);
        if ((id != newId) && (consumed.contains(id)))
        {
          consumed.remove(id);
          consumed.add(newId);
        }
      }
    }

*/

    if (!cppcStatement.getGlobalConsumed().isEmpty()) {
      for (final VariableDeclaration vd : cppcStatement.getGlobalConsumed()) {
        List<Identifier> symbolsToAdd;

        //if (vd.getParent() instanceof CommonBlock) {
        //  symbolsToAdd = (List<Identifier>)((CommonBlock)vd.getParent()).getDeclaredSymbols();
        //}
        //else {
          symbolsToAdd = (List<Identifier>)vd.getDeclaredSymbols();
        //}

        for (final Identifier id : symbolsToAdd) {
          final SymbolTable exTable = (SymbolTable)getParentOfClass((Traversable)vd, SymbolTable.class);
          final Expression sizeExpr = VariableSizeAnalizerFactory.getAnalizer().getSize(id, (Traversable)exTable);
          if (sizeExpr != null) {
            final DepthFirstIterator exprIter = new DepthFirstIterator((Traversable)sizeExpr);
            while (exprIter.hasNext()) {
              try {
                final Identifier sizeId = (Identifier)exprIter.next((Class)Identifier.class);
                final VariableDeclaration sizeVd = analyzer.getVariableDeclaration((Traversable)exTable, sizeId);
                final Identifier newSizeId = addClonedVariableDeclaration(sizeVd, sizeId, (Traversable)cppcStatement);
                if (newSizeId != sizeId) {
                  sizeId.swapWith((Expression)newSizeId);
                }
                if (getDeclarator(sizeVd, sizeId).getInitializer() == null) {
                  continue;
                }
                initialized.add(newSizeId);
              }
              catch (NoSuchElementException ex) {}
              catch (SymbolNotDefinedException ex2) {}
              catch (SymbolIsNotVariableException ex3) {}
            }
          }
        }
        for (final Identifier id : vd.getDeclaredSymbols()) {
          final Identifier newId = addClonedVariableDeclaration(vd, id, (Traversable)cppcStatement);
          if (id != newId && consumed.contains(id)) {
            consumed.remove(id);
            consumed.add(newId);
          }
        }
      }
    }


    iter.pruneOn(IfStatement.class);
    boolean stop = false;
    if (begin == end) {
      stop = true;
    }
    if ((cppcStatement.getStatement() instanceof IfStatement)) {
      stop = (stop) || 
        (characterizeBlock((IfStatement)cppcStatement.getStatement(), end, generated, consumed, initialized));
    }
    if (stop) {
      return;
    }
    while (iter.hasNext())
    {
      try
      {
        cppcStatement = (CppcStatement)iter.next(CppcStatement.class);
      }
      catch (NoSuchElementException e)
      {
        System.out.println("BUG: Cannot find last Statement in cppc.compiler.utils.ObjectAnalizer.characterizeBlock");
        
        System.out.println("I was seeking statement = " + end);
        System.exit(0);
      }

      consumed.addAll(setOps.setMinus(cppcStatement.getConsumed(), generated));
      consumed.addAll(setOps.setMinus(globalDeclarationsToSet( cppcStatement.getGlobalConsumed()), generated));
      generated.addAll(cppcStatement.getGenerated());
      generated.addAll(globalDeclarationsToSet( cppcStatement.getGlobalGenerated()));
      
      table = (SymbolTable)getParentOfClass( cppcStatement, SymbolTable.class);
      VariableDeclaration clone = null;

      if (!cppcStatement.getGlobalConsumed().isEmpty()) {

        //for (id = cppcStatement.getGlobalConsumed().iterator(); id.hasNext(); sizeExpr.hasNext())
        for( VariableDeclaration vd2 :cppcStatement.getGlobalConsumed() )
        {
          //VariableDeclaration vd = (VariableDeclaration)id.next();
          //Object symbolsToAdd;
          //Object symbolsToAdd;
          
          //if ((vd.getParent() instanceof CommonBlock)) {
          //  symbolsToAdd = 
          //    ((CommonBlock)vd.getParent()).getDeclaredSymbols();
          //} else {
          //  symbolsToAdd = vd.getDeclaredSymbols();
          //}

          List<Identifier> symbolsToAdd;
          //if (vd2.getParent() instanceof CommonBlock) {
          //  symbolsToAdd = (List<Identifier>)((CommonBlock)vd2.getParent()).getDeclaredSymbols();
          //}
          //else {
            symbolsToAdd = (List<Identifier>)vd2.getDeclaredSymbols();
          //}

          for (Identifier id : symbolsToAdd)
          {
            SymbolTable exTable =  (SymbolTable)getParentOfClass(vd,  SymbolTable.class);
            Expression sizeExpr =  VariableSizeAnalizerFactory.getAnalizer().getSize(id,  (Traversable)exTable);
            if (sizeExpr != null)
            {
              DepthFirstIterator exprIter = new DepthFirstIterator( sizeExpr);
              while (exprIter.hasNext()) {
                try
                {
                  Identifier sizeId = (Identifier)exprIter.next( Identifier.class);
                  VariableDeclaration sizeVd = analyzer.getVariableDeclaration((Traversable)exTable, sizeId);
                  Identifier newSizeId = addClonedVariableDeclaration(sizeVd, sizeId, cppcStatement);
                  if (newSizeId != sizeId) {
                    sizeId.swapWith(newSizeId);
                  }
                  if (getDeclarator(sizeVd, sizeId).getInitializer() != null) {
                    initialized.add(newSizeId);
                  }
                }
                catch (NoSuchElementException localNoSuchElementException2) {}
                catch (SymbolNotDefinedException localSymbolNotDefinedException1) {}
                catch (SymbolIsNotVariableException localSymbolIsNotVariableException1) {}
              }
            }
          }

          //sizeExpr = vd.getDeclaredSymbols().iterator(); continue;
          //Identifier id = (Identifier)sizeExpr.next();
          //Identifier newId = addClonedVariableDeclaration(vd, id, cppcStatement);
          
          
          for( Identifier id2 : vd.getDeclaredSymbols() ){
            Identifier newId2 = addClonedVariableDeclaration(vd2, id2, cppcStatement);
            if ((id2 != newId2) && (consumed.contains(id2)))
            {
              consumed.remove(id2);
              consumed.add(newId2);
            }
          }

        }
      }

      if (cppcStatement == end) {
        stop = true;
      }
      if (cppcStatement.getStatement() instanceof IfStatement)
      {
        stop = ( stop ||  characterizeBlock((IfStatement)cppcStatement.getStatement(), end, generated, consumed, initialized) );
      }
      if (stop) {
        return;
      }
    }


  }
  
  private static Identifier addClonedVariableDeclaration(VariableDeclaration vd, Identifier id, Traversable ref)
  {
    SymbolTable table = (SymbolTable)getParentOfClass(ref, SymbolTable.class);
    VariableDeclaration localDecl = (VariableDeclaration)table.findSymbol(id);
    VariableDeclaration clone = null;
    LanguageAnalyzer analyzer = LanguageAnalyzerFactory.getLanguageAnalyzer();
    
    if (localDecl == null)
    {
      clone = analyzer.cloneDeclaration(vd, id);
      VariableDeclaration ret = analyzer.addVariableDeclaration(clone, (Statement)ref);
      if (ret != clone) {
        for (int i = 0; i < vd.getNumDeclarators(); i++) {
          if (id.equals(vd.getDeclarator(i).getSymbol())) {
            return (Identifier)ret.getDeclarator(i).getSymbol();
          }
        }
      }

    }
    return id;
  }
  
  public static void characterizeBlock(Statement begin, Statement end, Set<Identifier> generated, Set<Identifier> consumed, Set<Identifier> initialized)
  {
    if (!LanguageAnalyzerFactory.getLanguageAnalyzer().insideLoop(begin))
    {
      simpleBlockCharacterization(begin, end, generated, consumed, initialized);
      return;
    }
    Statement loopBody = LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody(begin);
    CppcStatement loopStatement = (CppcStatement)getParentOfClass(loopBody.getParent(), CppcStatement.class);
    
    SetOperations<Identifier> setOps = new SetOperations();
    
    Statement endLoopBody = LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody(end);
    while ((endLoopBody != null) && (endLoopBody != loopBody))
    {
      endLoopBody = (Statement)endLoopBody.getParent();
      endLoopBody = LanguageAnalyzerFactory.getLanguageAnalyzer().getContainerLoopBody(endLoopBody.getParent());
    }

    if (endLoopBody != null)
    {
      simpleBlockCharacterization(begin, end, generated, consumed, initialized);
      
      consumed.addAll(setOps.setMinus(loopStatement.getConsumed(), generated));
      generated.addAll(loopStatement.getGenerated());
      return;
    }

    CppcStatement loopEnd = null;
    DepthFirstIterator loopIter = new DepthFirstIterator(loopBody);
    try
    {
      while (loopIter.hasNext()) {
        loopEnd = (CppcStatement)loopIter.next(CppcStatement.class);
      }
    }
    catch (NoSuchElementException localNoSuchElementException1) 
    {

    }
    simpleBlockCharacterization(begin, loopEnd, generated, consumed, initialized);
    
    consumed.addAll(setOps.setMinus(loopStatement.getConsumed(), generated));
    generated.addAll(loopStatement.getGenerated());
    
    loopIter.reset();
    CppcStatement loopBegin = null;
    try
    {
      loopBegin = (CppcStatement)loopIter.next(CppcStatement.class);
    }
    catch (NoSuchElementException e)
    {
      System.err.println("BUG: Loop doesn't contain any CppcStatement. In cppc.compiler.utils.ObjectAnalizer.characterizeBlock()");
      
      System.exit(0);
    }
    simpleBlockCharacterization(loopBegin, begin, generated, consumed, initialized);
    
    simpleBlockCharacterization(loopEnd, end, generated, consumed, initialized);
  }
  
  public static void addRegisterPragmaBefore(Statement reference, Set<Identifier> registers)
  {
    CppcRegisterPragma pragma = new CppcRegisterPragma();
    Iterator<Identifier> idIter = registers.iterator();
    CompoundStatement statementList = (CompoundStatement)getParentOfClass(reference, CompoundStatement.class);
    Set<Identifier> sizeDependencies = new HashSet();
    while (idIter.hasNext())
    {
      Identifier id = (Identifier)idIter.next();
      Expression size = VariableSizeAnalizerFactory.getAnalizer().getSize(id, statementList);
      pragma.addRegister(new CppcRegister(id, size));
      if (size != null)
      {
        ExpressionStatement statement = new ExpressionStatement(size);
        CppcStatement cppcStatement = new CppcStatement(statement);
        StatementAnalyzer.analyzeStatement(cppcStatement);
        Iterator<Identifier> iter = cppcStatement.getConsumed().iterator();
        while (iter.hasNext()) {
          pragma.addRegister(new CppcRegister((Identifier)iter.next(), null));
        }
      }
    }
    statementList.addStatementBefore(reference, pragma);
  }
  
  //判断全局变量
  public static boolean isGlobal(Identifier id, SymbolTable table)
  {
    Declaration vd = table.findSymbol(id);
    //if ((vd instanceof CommonBlock)) {
    //  return true;
    //}
    SymbolTable parentTable = (SymbolTable)getParentOfClass(vd, SymbolTable.class);
    if ((parentTable instanceof TranslationUnit)) {
      return true;
    }
    return false;
  }
  
  public static final Statement findFirstExecutable(CompoundStatement statementList)
  {
    DepthFirstIterator iter = new DepthFirstIterator(statementList);
    iter.next();
    iter.pruneOn(Statement.class);
    while (iter.hasNext()) {
      try
      {
        Statement stmt = (Statement)iter.next(Statement.class);
        if (!(stmt instanceof DeclarationStatement)) {
          return stmt;
        }
      }
      catch (NoSuchElementException localNoSuchElementException) 
      {

      }
    }
    return null;
  }
  
  public static final Statement findFirstExecutable(Procedure procedure)
  {
    return findFirstExecutable(procedure.getBody());
  }
  
  //
  public static final Statement findLastDeclaration(Procedure procedure)
  {
    CompoundStatement statementList = procedure.getBody();
    
    int i = 0;
    List children = statementList.getChildren();

    DeclarationStatement decl;
    while (
      (i < children.size()) && 
      ( (((children.get(i) instanceof CppcStatement)) && 
      
      ((((CppcStatement)children.get(i)).getStatement() instanceof DeclarationStatement))) || 
      
      ((children.get(i) instanceof DeclarationStatement)))
      )
    {
      
      //DeclarationStatement decl;
      if ((children.get(i) instanceof ChenStatement)) {
        decl = (DeclarationStatement) ((CppcStatement)children.get(i)).getStatement();
      } else {
        decl = (DeclarationStatement)children.get(i);
      }
      i++;
    }
    if (i == 0) {
      return null;
    }
    return (Statement)children.get(--i);
  }
  
  public static final Statement findLastStatement(Procedure procedure)
  {
    int size = procedure.getBody().getChildren().size();
    return (Statement)procedure.getBody().getChildren().get(size - 1);
  }
  
  public static final void encloseWithExecutes(Statement s)
  {
    CompoundStatement statementList = (CompoundStatement)getParentOfClass(s, CompoundStatement.class);
    
    CppcExecutePragma pragma = new CppcExecutePragma(s, s);
    statementList.addStatementBefore(s, pragma);
  }
  
  public static VariableDeclarator getDeclarator(VariableDeclaration vd, Identifier id)
  {
    for (int i = 0; i < vd.getNumDeclarators(); i++) {
      if (vd.getDeclarator(i).getSymbol().equals(id)) {
        return (VariableDeclarator)vd.getDeclarator(i);
      }
    }
    return null;
  }
  
  public static Identifier getBaseIdentifier(Expression expr)
  {
    if ((expr instanceof Identifier)) {
      return (Identifier)expr;
    }
    if ((expr instanceof ArrayAccess)) {
      return getBaseIdentifier(((ArrayAccess)expr).getArrayName());
    }
    if ((expr instanceof UnaryExpression)) {
      return getBaseIdentifier(((UnaryExpression)expr).getExpression());
    }
    return null;
  }


}
