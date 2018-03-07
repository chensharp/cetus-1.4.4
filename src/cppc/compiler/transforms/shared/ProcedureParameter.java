package cppc.compiler.transforms.shared;


//
//函数参数类型 就是一个int position
public class ProcedureParameter
{
  //可变参数，
  public static final ProcedureParameter VARARGS = new ProcedureParameter(-1);
  //位置 ？ 意思未知  
  private int position;
  
  public ProcedureParameter(int position)
  {
    this.position = position;
  }
  
  public int getPosition()
  {
    return this.position;
  }
  
  //判等，比较position
  public boolean equals(Object obj)
  {
    if (!(obj instanceof ProcedureParameter)) {
      return false;
    }
    return this.position == ((ProcedureParameter)obj).position;
  }
  
  //
  public int hashCode()
  {
    return new Integer(this.position).hashCode();
  }
  
  public String toString()
  {
    return "ProcedureParameter in position: " + this.position;
  }
}
