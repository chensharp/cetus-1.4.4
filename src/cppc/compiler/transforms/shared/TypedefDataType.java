package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;

public class TypedefDataType
  extends DataType
{
  private int basicSize;
  
  public TypedefDataType(Identifier basicType, int size)
  {
    super(basicType);
    
    this.basicSize = size;
  }
  
  public int size()
  {
    return this.basicSize;
  }
}
