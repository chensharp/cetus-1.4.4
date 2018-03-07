package cppc.compiler.utils.language;

import cetus.hir.Annotation;
import cetus.hir.BreadthFirstIterator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declaration;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declarator;
import cetus.hir.DoLoop;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.Loop;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.StringLiteral;
import cetus.hir.SymbolTable;
import cetus.hir.TranslationUnit;
import cetus.hir.Traversable;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.WhileLoop;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import cppc.compiler.utils.ObjectAnalizer;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class CLanguageAnalyzer
  implements LanguageAnalyzer
{
  public VariableDeclaration addVariableDeclaration(VariableDeclaration d, Statement ref)
  {
    CompoundStatement statementList = 
      (CompoundStatement)ObjectAnalizer.getParentOfClass(ref, 
      CompoundStatement.class);
    
    statementList.addDeclaration(d);
    
    return d;
  }
  
  public boolean annotationIsPragma(Annotation annote)
  {
    return annote.getText().startsWith("#pragma");
  }
  
  public boolean beginsInclude(Annotation annote)
  {
    String txt = annote.getText();
    return txt.startsWith("#pragma startinclude");
  }
  
  public List<Traversable> buildInclude(String file)
  {
    ArrayList<Traversable> ret = new ArrayList(2);
    ret.add(new Annotation("#pragma startinclude #include <" + file + ">"));
    ret.add(new Annotation("#pragma endinclude"));
    for (Traversable t : ret) {
      ((Annotation)t).setPrintMethod(Annotation.print_raw_method);
    }
    return ret;
  }
  
  public Expression buildStringLiteral(String text)
  {
    return new StringLiteral(text);
  }
  
  public void checkIncludes(Procedure proc)
  {
    TranslationUnit tunit = (TranslationUnit)proc.getParent();
    BreadthFirstIterator iter = new BreadthFirstIterator(tunit);
    iter.pruneOn(Declaration.class);
    iter.pruneOn(Statement.class);
    while (iter.hasNext()) {
      try
      {
        Declaration decl = (Declaration)iter.next(Declaration.class);
        if ((decl instanceof Annotation))
        {
          Annotation annote = (Annotation)decl;
          String file = getIncludeFile(annote);
          if ((file != null) && 
            (file.endsWith(GlobalNamesFactory.getGlobalNames().INCLUDE_FILE()))) {
            return;
          }
        }
      }
      catch (NoSuchElementException localNoSuchElementException) {}
    }
    iter.reset();
    iter.next();
    while (iter.hasNext()) {
      try
      {
        Object obj = iter.next();
        if (((obj instanceof Annotation)) && 
          (beginsInclude((Annotation)obj)))
        {
          int incDepth = 1;
          String file = getIncludeFile((Annotation)obj);
          while (incDepth > 0)
          {
            obj = iter.next(Declaration.class);
            if ((obj instanceof Annotation))
            {
              if (beginsInclude((Annotation)obj)) {
                incDepth++;
              }
              if (endsInclude((Annotation)obj, file)) {
                incDepth--;
              }
            }
          }
        }
        else
        {
          List<Traversable> cppcInclude = 
            buildInclude(GlobalNamesFactory.getGlobalNames().INCLUDE_FILE());
          if ((obj instanceof Annotation))
          {
            for (Traversable t : cppcInclude)
            {
              tunit.addDeclarationAfter((Declaration)obj, (Declaration)t);
              obj = t;
            }
          }
          else
          {
            Declaration decl = null;
            if ((obj instanceof Procedure)) {
              decl = (Procedure)obj;
            } else {
              decl = ((DeclarationStatement)obj).getDeclaration();
            }
            for (Traversable t : cppcInclude) {
              tunit.addDeclarationBefore(decl, (Declaration)t);
            }
          }
          return;
        }
      }
      catch (NoSuchElementException localNoSuchElementException1) {}
    }
  }
  
  public VariableDeclaration cloneDeclaration(VariableDeclaration vd, Identifier id)
  {
    return (VariableDeclaration)vd.clone();
  }
  
  public boolean endsInclude(Annotation annote, String file)
  {
    String txt = annote.getText();
    return txt.startsWith("#pragma endinclude");
  }
  
  public String getIncludeFile(Annotation annote)
  {
    String txt = annote.getText();
    if (!txt.startsWith("#pragma startinclude")) {
      return null;
    }
    txt = txt.replaceFirst("#pragma startinclude #include \\\"", "").trim();
    txt = txt.replaceFirst("#pragma startinclude #include <", "").trim();
    txt = txt.replaceAll("\\\"", "").trim();
    return txt.replaceAll(">", "").trim();
  }
  
  public String getPragmaText(Annotation annote)
  {
    return annote.getText().trim();
  }
  
  public Expression getReference(Declarator declarator)
  {
    if ((declarator.getSpecifiers().size() == 0) && 
      (declarator.getArraySpecifiers().size() == 0)) {
      return new UnaryExpression(UnaryOperator.ADDRESS_OF, 
        (Identifier)declarator.getSymbol().clone());
    }
    return (Identifier)declarator.getSymbol().clone();
  }
  
  public VariableDeclaration getVariableDeclaration(Traversable ref, Identifier var)
    throws SymbolNotDefinedException, SymbolIsNotVariableException
  {
    SymbolTable symbolTable = (SymbolTable)ObjectAnalizer.getParentOfClass(ref, 
      SymbolTable.class);
    
    Declaration decl = symbolTable.findSymbol(var);
    if (decl == null) {
      throw new SymbolNotDefinedException(var.toString());
    }
    if (!(decl instanceof VariableDeclaration)) {
      throw new SymbolIsNotVariableException(var.toString());
    }
    return (VariableDeclaration)decl;
  }
  
  public boolean insideLoop(Traversable t)
  {
    ForLoop floop = (ForLoop)ObjectAnalizer.getParentOfClass(t, 
      ForLoop.class);
    if (floop != null) {
      return true;
    }
    WhileLoop wloop = (WhileLoop)ObjectAnalizer.getParentOfClass(t, 
      WhileLoop.class);
    if (wloop != null) {
      return true;
    }
    DoLoop dloop = (DoLoop)ObjectAnalizer.getParentOfClass(t, 
      DoLoop.class);
    if (dloop != null) {
      return true;
    }
    return false;
  }
  
  public Statement getContainerLoopBody(Traversable t)
  {
    Loop loop = (Loop)ObjectAnalizer.getParentOfClass(t, Loop.class);
    return (Statement)loop;
  }
}
