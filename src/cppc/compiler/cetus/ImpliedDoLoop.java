package cppc.compiler.cetus;


import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ImpliedDoLoop
  extends Expression
{
  private static Method classPrintMethod;
  private int exprCount;
  
  static
  {
    Class[] params = { ImpliedDoLoop.class, OutputStream.class };
    try
    {
      classPrintMethod = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      e.printStackTrace();
    }
  }
  
  public ImpliedDoLoop(List<Expression> objects, Identifier doVar, Expression start, Expression stop)
  {
    super(4);
    
    this.object_print_method = classPrintMethod;
    this.exprCount = objects.size();
    for (Expression expr : objects) {
      addChild(expr);
    }
    addChild(doVar);
    addChild(start);
    addChild(stop);
  }
  
  public ImpliedDoLoop(List<Expression> objects, Identifier doVar, Expression start, Expression stop, Expression step)
  {
    super(5);
    
    this.object_print_method = classPrintMethod;
    this.exprCount = objects.size();
    for (Expression expr : objects) {
      addChild(expr);
    }
    addChild(doVar);
    addChild(start);
    addChild(stop);
    addChild(step);
  }
  
  protected void addChild(Traversable t)
  {
    if (t.getParent() != null) {
      throw new NotAnOrphanException();
    }
    this.children.add(t);
    t.setParent(this);
  }
  
  public List<Expression> getExpressions()
  {
    ArrayList<Expression> exprList = new ArrayList(this.exprCount);
    for (int i = 0; i < this.exprCount; i++) {
      exprList.add((Expression)this.children.get(i));
    }
    return exprList;
  }
  
  public Identifier getDoVar()
  {
    return (Identifier)this.children.get(this.exprCount);
  }
  
  public Expression getStart()
  {
    return (Expression)this.children.get(this.exprCount + 1);
  }
  
  public Expression getStop()
  {
    return (Expression)this.children.get(this.exprCount + 2);
  }
  
  public Expression getStep()
  {
    try
    {
      return (IntegerLiteral)this.children.get(this.exprCount + 3);
    }
    catch (IndexOutOfBoundsException e) {}
    return null;
  }
  
  public static void defaultPrint(ImpliedDoLoop obj, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    PrintWriter pw = new PrintWriter(stream);
    
    p.print("( ");
    
    Iterator<Expression> iter = obj.getExpressions().iterator();
    while (iter.hasNext())
    {
      ((Expression)iter.next()).print(pw);
      if (iter.hasNext()) {
        p.print(", ");
      }
    }
    p.print(", ");
    obj.getDoVar().print(pw);
    p.print(" = ");
    obj.getStart().print(pw);
    p.print(", ");
    obj.getStop().print(pw);
    if (obj.getStep() != null)
    {
      p.print(", ");
      obj.getStep().print(pw);
    }
    p.print(" )");
  }
  
  public static void setClassPrintMethod(Method m)
  {
    classPrintMethod = m;
  }
}
