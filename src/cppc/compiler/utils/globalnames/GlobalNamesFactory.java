package cppc.compiler.utils.globalnames;

import cppc.compiler.utils.ConfigurationManager;

public class GlobalNamesFactory
{
  private static GlobalNames instance;
  
  static
  {
    String className = ConfigurationManager.getOption("CPPC/Utils/GlobalNames/ClassName");
    try
    {
      Class instanceClass = Class.forName(className);
      instance = (GlobalNames)instanceClass.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  public static final GlobalNames getGlobalNames()
  {
    return instance;
  }
}
