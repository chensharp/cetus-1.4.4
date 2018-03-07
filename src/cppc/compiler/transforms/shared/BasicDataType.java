package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;

public class BasicDataType
  extends DataType
{
  public BasicDataType(Identifier basicType)
  {
    super(basicType);
  }
  
  public int size()
  {
    return 1;
  }
}
