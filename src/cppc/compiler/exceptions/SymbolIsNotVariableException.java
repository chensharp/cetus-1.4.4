package cppc.compiler.exceptions;

public class SymbolIsNotVariableException
  extends Exception
{
  public SymbolIsNotVariableException(String symbol)
  {
    super(symbol);
  }
}
