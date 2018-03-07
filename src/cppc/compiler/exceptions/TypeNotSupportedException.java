package cppc.compiler.exceptions;

public class TypeNotSupportedException
  extends Exception
{
  public TypeNotSupportedException(String symbol)
  {
    super(symbol);
  }
}
