package cppc.compiler.cetus;

import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.NotAnOrphanException;
import cetus.hir.Traversable;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class MultiExpression
  extends Expression
{
  private static Method class_print_method;
  private Identifier var;
  Map<Expression, Expression> values;
  
  static
  {
    Class[] params = { MultiExpression.class, OutputStream.class };
    try
    {
      class_print_method = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      throw new InternalError();
    }
  }
  
  public MultiExpression(Identifier var)
  {
    this.object_print_method = class_print_method;
    this.var = var;
    this.values = new TreeMap();
  }
  
  public MultiExpression(Identifier var, Map<Expression, Expression> values)
  {
    this.object_print_method = class_print_method;
    this.var = var;
    values = new TreeMap();
    for (Expression key : values.keySet())
    {
      this.values.put(key, (Expression)values.get(key));
      addChildren((Traversable)values.get(key));
    }
  }
  
  private void addChildren(Traversable t)
  {
    if (t.getParent() != null) {
      throw new IllegalArgumentException();
    }
    this.children.add(t);
    t.setParent(this);
  }
  
  public void setChild(int index, Traversable t)
  {
    if (t.getParent() != null) {
      throw new NotAnOrphanException();
    }
    if ((t instanceof Expression))
    {
      Expression value = (Expression)this.children.get(index);
      for (Expression key : this.values.keySet()) {
        if (this.values.get(key) == value)
        {
          this.values.put(key, (Expression)t);
          break;
        }
      }
      this.children.set(index, t);
      t.setParent(this);
    }
    else
    {
      throw new IllegalArgumentException();
    }
  }
  
  public void addExpression(Expression key, Expression value)
  {
    if (this.values.containsKey(key))
    {
      Expression old = (Expression)this.values.get(key);
      this.children.remove(old);
    }
    this.values.put(key, value);
    addChildren(value);
  }
  
  public Identifier getVar()
  {
    return this.var;
  }
  
  public Expression getValue(Expression key)
  {
    if (this.values.containsKey(key)) {
      return (Expression)this.values.get(key);
    }
    return null;
  }
  
  public Set<Expression> getValueSet()
  {
    return this.values.keySet();
  }
  
  public Expression getKeyOf(Expression value)
  {
    if (this.values.containsValue(value)) {
      for (Expression key : this.values.keySet()) {
        if (this.values.get(key) == value) {
          return key;
        }
      }
    }
    return null;
  }
  

  public static void defaultPrint(MultiExpression mexpr, OutputStream stream)
  {
    PrintStream p = new PrintStream(stream);
    PrintWriter pw = new PrintWriter(stream);

    p.print("[[");
    for (int i = 0; i < mexpr.children.size() - 1; i++)
    {
      ((Expression)mexpr.children.get(i)).print(pw);
      p.print(", ");
    }
    if (mexpr.children.size() > 0) {
      ((Expression)mexpr.children.get(mexpr.children.size() - 1)).print(pw);
    }
    p.print("]]");
  }
  
  public static void setClassPrintMethod(Method m)
  {
    class_print_method = m;
  }
  
  public Object clone()
  {
    MultiExpression clone = new MultiExpression((Identifier)this.var.clone());
    for (Expression key : this.values.keySet()) {
      clone.addExpression(key, (Expression)((Expression)this.values.get(key)).clone());
    }
    return clone;
  }
}


