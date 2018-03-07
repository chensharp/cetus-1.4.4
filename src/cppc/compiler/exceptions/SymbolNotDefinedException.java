package cppc.compiler.exceptions;

public class SymbolNotDefinedException
  extends Exception
{
  public SymbolNotDefinedException(String symbol)
  {
    super(symbol);
  }
}
