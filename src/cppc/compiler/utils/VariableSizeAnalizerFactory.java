package cppc.compiler.utils;

public final class VariableSizeAnalizerFactory
{
  private static VariableSizeAnalizer instance;
  
  static
  {
    String className = ConfigurationManager.getOption("CPPC/Utils/VariableSizeAnalyzer/ClassName");
    try
    {
      Class instanceClass = Class.forName(className);
      instance = (VariableSizeAnalizer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  public static VariableSizeAnalizer getAnalizer()
  {
    return instance;
  }
}
