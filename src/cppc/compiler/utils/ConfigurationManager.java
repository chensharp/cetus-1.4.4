package cppc.compiler.utils;

import java.io.PrintStream;
import java.util.HashMap;


//配置管理
//
public final class ConfigurationManager
{
  private static HashMap<String, String[]> options = new HashMap();
  
  private static final String CPPC_ARG_PREFIX = "-CPPC,";
  public static final String DISABLE_CKPT_ANALYSIS = "DisableCkptAnalysis";
  public static final String DISABLE_COMM_ANALYSIS = "DisableCommAnalysis";
  public static final String MANUAL_PRAGMAS_OPTION = "ManualPragmas";
  public static final String PROCESS_NUMBER_OPTION = "ProcessNumber";
  
  //
  public static final String[] parseCommandLine(String[] args){
    if (args.length == 0) {
      return args;
    }

    for (int i = 0; i < args.length; i++)
    {
      String option = args[i].trim();
      if (option.startsWith("-CPPC,"))
      {
        option = option.replaceFirst("-CPPC,", "");
        option.trim();
        
        String[] subOptions = option.split(",");
        for (int j = 0; j < subOptions.length; j++)
        {
          String subOption = subOptions[j].trim();
          
          String[] expr = subOption.split("=");
          if (expr.length != 2)
          {
            System.err.println("WARNING: Ignoring command-line argument '" + 
              subOption + "' : Syntax error");
          }
          else
          {
            String key = expr[0].trim();
            String value = expr[1].trim();
            if (options.containsKey(key))
            {
              String[] values = (String[])options.get(key);
              String[] newValues = new String[values.length + 1];
              System.arraycopy(values, 0, newValues, 0, values.length);
              newValues[values.length] = value;
              options.put(key, newValues);
            }
            else
            {
              String[] values = { value };
              options.put(key, values);
            }
          }
        }
        String[] newArgs = new String[args.length - 1];
        for (int j = 0; j < i; j++) {
          newArgs[j] = args[j];
        }
        for (int j = i; j < args.length - 1; j++) {
          newArgs[j] = args[(j + 1)];
        }
        return newArgs;
      }
    }
    return args;
  }
  
  public static final boolean hasOption(String key)
  {
    return options.containsKey(key);
  }
  
  public static final String getOption(String key)
  {
    if (options.containsKey(key)) {
      return ((String[])options.get(key))[0];
    }
    return null;
  }
  
  public static final String[] getOptionArray(String key)
  {
    if (options.containsKey(key)) {
      return (String[])options.get(key);
    }
    return new String[0];
  }
  
  public static final void setOption(String key, String value)
  {
    String[] values = { value.trim() };
    options.put(key.trim(), values);
  }
}
