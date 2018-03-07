package cppc.compiler.cetus;

import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Traversable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class UnfoldedExpression
  extends Expression
{
  private static Method class_print_method;
  private Identifier var;
  private Expression varValue;
  
  static
  {
    Class[] params = { UnfoldedExpression.class, OutputStream.class };
    try
    {
      class_print_method = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      throw new InternalError();
    }
  }
  
  public UnfoldedExpression(Identifier var, Expression varValue, Expression inner)
  {
    this.object_print_method = class_print_method;
    
    this.var = var;
    this.varValue = varValue;
    
    addChildren(inner);
  }
  
  public Identifier getVar()
  {
    return this.var;
  }
  
  public Expression getVarValue()
  {
    return this.varValue;
  }
  
  private void addChildren(Traversable t)
  {
    if (t.getParent() != null) {
      throw new IllegalArgumentException();
    }
    this.children.add(t);
    t.setParent(this);
  }
  
  public Expression getExpression()
  {
    return (Expression)this.children.get(0);
  }
  
  
  public static void defaultPrint(UnfoldedExpression mexpr, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    PrintWriter pw = new PrintWriter(stream);
    p.print("[[");
    p.print("(");
    mexpr.var.print(pw);
    p.print("=");
    mexpr.varValue.print(pw);
    p.print(")");
    mexpr.getExpression().print(pw);
    p.print("]]");
  }
  
  
  public static void setClassPrintMethod(Method m)
  {
    class_print_method = m;
  }
  
  public Object clone()
  {
    UnfoldedExpression clone = new UnfoldedExpression(this.var, this.varValue, 
      (Expression)getExpression().clone());
    return clone;
  }
}




