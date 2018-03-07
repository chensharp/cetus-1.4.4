package cppc.compiler.utils;

import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Traversable;

public abstract interface VariableSizeAnalizer
{
  public abstract Expression getSize(Identifier paramIdentifier, Traversable paramTraversable);
}
