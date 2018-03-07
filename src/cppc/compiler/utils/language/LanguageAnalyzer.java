package cppc.compiler.utils.language;

import cetus.hir.Annotation;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.Statement;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cppc.compiler.exceptions.SymbolIsNotVariableException;
import cppc.compiler.exceptions.SymbolNotDefinedException;
import java.util.List;

public abstract interface LanguageAnalyzer
{
  public abstract VariableDeclaration addVariableDeclaration(VariableDeclaration paramVariableDeclaration, Statement paramStatement);
  
  public abstract boolean annotationIsPragma(Annotation paramAnnotation);
  
  public abstract boolean beginsInclude(Annotation paramAnnotation);
  
  public abstract List<Traversable> buildInclude(String paramString);
  
  public abstract Expression buildStringLiteral(String paramString);
  
  public abstract void checkIncludes(Procedure paramProcedure);
  
  public abstract VariableDeclaration cloneDeclaration(VariableDeclaration paramVariableDeclaration, Identifier paramIdentifier);
  
  public abstract boolean endsInclude(Annotation paramAnnotation, String paramString);
  
  public abstract Statement getContainerLoopBody(Traversable paramTraversable);
  
  public abstract String getIncludeFile(Annotation paramAnnotation);
  
  public abstract String getPragmaText(Annotation paramAnnotation);
  
  public abstract Expression getReference(Declarator paramDeclarator);
  
  public abstract VariableDeclaration getVariableDeclaration(Traversable paramTraversable, Identifier paramIdentifier)
    throws SymbolNotDefinedException, SymbolIsNotVariableException;
  
  public abstract boolean insideLoop(Traversable paramTraversable);
}
