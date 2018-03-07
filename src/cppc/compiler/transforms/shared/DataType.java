package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;

public abstract class DataType
{
  private Identifier baseType;
  
  public DataType(Identifier baseType)
  {
    this.baseType = baseType;
  }
  
  public Identifier getBaseType()
  {
    return this.baseType;
  }
  
  public abstract int size();
}
