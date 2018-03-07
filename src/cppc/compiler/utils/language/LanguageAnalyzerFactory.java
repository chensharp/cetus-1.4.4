package cppc.compiler.utils.language;

import cppc.compiler.utils.ConfigurationManager;

public final class LanguageAnalyzerFactory
{
  private static LanguageAnalyzer instance;
  
  static
  {
    String className = ConfigurationManager.getOption(
      "CPPC/Utils/LanguageAnalyzer/ClassName");
    try
    {
      Class instanceClass = Class.forName(className);
      instance = (LanguageAnalyzer)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  public static final LanguageAnalyzer getLanguageAnalyzer()
  {
    return instance;
  }
}
