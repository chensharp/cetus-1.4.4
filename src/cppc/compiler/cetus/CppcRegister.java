package cppc.compiler.cetus;


import cetus.hir.Expression;
import cetus.hir.Identifier;
import java.io.ByteArrayOutputStream;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class CppcRegister
{
  private Identifier name;
  private Expression size;
  
  private CppcRegister() {}
  
  public CppcRegister(Identifier name)
  {
    this.name = name;
    this.size = null;
  }
  
  public CppcRegister(Identifier name, Expression size)
  {
    this.name = name;
    this.size = size;
  }
  
  public Identifier getName()
  {
    return this.name;
  }
  
  public Expression getSize()
  {
    return this.size;
  }
  
  public void setSize(Expression size)
  {
    this.size = size;
  }
  
  public void print(OutputStream stream)
  {

    PrintStream p = new PrintStream(stream);
    
    PrintWriter pw = new PrintWriter(stream);
    this.name.print(pw);

    if (this.size != null)
    {
      p.print("[ ");
      this.size.print(pw);
      p.print(" ]");
    }

  }
  
  public String toString()
  {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    print(stream);
    return stream.toString();
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof CppcRegister)) {
      return false;
    }
    CppcRegister rhs = (CppcRegister)obj;
    
    return (this.name.equals(rhs.name)) && 
      (this.size.equals(rhs.size));
  }
}


