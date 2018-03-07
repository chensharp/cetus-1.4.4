package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;

public class ComplexDataType
  extends BasicDataType
{
  public ComplexDataType(Identifier basicType)
  {
    super(basicType);
  }
  
  public int size()
  {
    return 2;
  }
}
