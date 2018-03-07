package cppc.compiler.cetus;

import cetus.hir.Expression;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Statement;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FormatStatement
  extends Statement
{
  private static Method classPrintMethod;
  
  static
  {
    Class[] params = { FormatStatement.class, OutputStream.class };
    try
    {
      classPrintMethod = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      e.printStackTrace();
    }
  }
  
  public FormatStatement()
  {
    this.object_print_method = classPrintMethod;
  }
  
  public FormatStatement(List<Expression> parameters)
  {
    this.object_print_method = classPrintMethod;
    
    Iterator<Expression> iter = parameters.iterator();
    while (iter.hasNext())
    {
      Expression expr = (Expression)iter.next();
      this.children.add(expr);
      expr.setParent(this);
    }
  }
  
  public static void defaultPrint(FormatStatement obj, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    //PrintWriter pw = new PrintWriter(stream);
    
    p.print("       FORMAT( ");
    
    Iterator iter = obj.children.iterator();
    while (iter.hasNext())
    {
      Expression expr = (Expression)iter.next();
      expr.print(stream);
      if (iter.hasNext()) {
        p.print(", ");
      }
    }
    p.println(" )");
  }
  
  public List getParameters()
  {
    return this.children;
  }
  
  public void addParameter(Expression parameter)
  {
    if (parameter.getParent() != null) {
      throw new NotAnOrphanException();
    }
    this.children.add(parameter);
    parameter.setParent(this);
  }
  
  public void addParameterBefore(Expression reference, Expression parameter)
  {
    if (parameter.getParent() != null) {
      throw new NotAnOrphanException();
    }
    for (int i = 0; i < this.children.size(); i++) {
      if (this.children.get(i).equals(reference))
      {
        this.children.add(i, parameter);
        parameter.setParent(this);
        return;
      }
    }
    throw new NoSuchElementException();
  }
  
  public void addParameterAfter(Expression reference, Expression parameter)
  {
    if (parameter.getParent() != null) {
      throw new NotAnOrphanException();
    }
    for (int i = 0; i < this.children.size(); i++) {
      if (this.children.get(i).equals(reference))
      {
        this.children.add(i + 1, parameter);
        parameter.setParent(this);
        return;
      }
    }
    throw new NoSuchElementException();
  }
  
  public static void setClassPrintMethod(Method m)
  {
    classPrintMethod = m;
  }
}
