package cppc.compiler.cetus;

import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IOCall
  extends FunctionCall
{
  private static Method classPrintMethod;
  private int numParams;
  
  static
  {
    Class[] params = { IOCall.class, OutputStream.class };
    try
    {
      classPrintMethod = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      e.printStackTrace();
    }
  }
  
  public IOCall(Expression function)
  {
    super(function);
    
    this.object_print_method = classPrintMethod;
  }
  
  public IOCall(Expression function, List<Expression> varargs)
  {
    super(function);
    
    this.object_print_method = classPrintMethod;
    for (Expression expr : varargs) {
      addChild(expr);
    }
    this.numParams = 0;
  }
  
  public IOCall(Expression function, List<Expression> parameters, List<Expression> varargs)
  {
    super(function);
    
    this.object_print_method = classPrintMethod;
    for (Expression expr : parameters) {
      addChild(expr);
    }
    for (Expression expr : varargs) {
      addChild(expr);
    }
    this.numParams = parameters.size();
  }
  
  private void addChild(Traversable t)
  {
    if (t.getParent() != null) {
      throw new NotAnOrphanException();
    }
    this.children.add(t);
    t.setParent(this);
  }
  
  public void addArgument(Expression expr)
  {
    this.children.add(this.numParams++, expr);
  }
  
  public int getNumArguments()
  {
    return this.numParams;
  }
  
  public List<Expression> getParameters()
  {
    ArrayList<Expression> params = new ArrayList(this.numParams);
    for (int i = 0; i < this.numParams; i++) {
      params.add(getArgument(i));
    }
    return params;
  }
  
  public List<Expression> getVarargs()
  {
    ArrayList<Expression> varargs = new ArrayList(
      this.children.size() - this.numParams - 1);
    for (int i = this.numParams; i < this.children.size() - 1; i++) {
      varargs.add(getArgument(i));
    }
    return varargs;
  }
  
  
  public static void defaultPrint(IOCall obj, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    PrintWriter pw = new PrintWriter(stream);

    obj.getName().print(pw);
    p.print(" ");
    if (obj.numParams != 0)
    {
      p.print("( ");
      Iterator<Expression> iter = obj.getParameters().iterator();
      while (iter.hasNext())
      {
        ((Expression)iter.next()).print(pw);
        if (iter.hasNext()) {
          p.print(", ");
        }
      }
      p.print(" )");
    }
    Iterator<Expression> iter = obj.getVarargs().iterator();
    while (iter.hasNext())
    {
      ((Expression)iter.next()).print(pw);
      if (iter.hasNext()) {
        p.print(", ");
      }
    }
  }
  
  public static void setClassPrintMethod(Method m)
  {
    classPrintMethod = m;
  }
}
