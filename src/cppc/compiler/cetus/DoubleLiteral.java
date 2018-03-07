package cppc.compiler.cetus;

import cetus.hir.FloatLiteral;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class DoubleLiteral
  extends FloatLiteral
{
  private static Method class_print_method;
  
  static
  {
    Class[] params = new Class[2];
    try
    {
      params[0] = FloatLiteral.class;
      params[1] = OutputStream.class;
      class_print_method = params[0].getMethod("defaultPrint", params);
    }
    catch (NoSuchMethodException e)
    {
      throw new InternalError();
    }
  }
  
  public DoubleLiteral(double value)
  {
    super(value);
    this.object_print_method = class_print_method;
  }
  
  public static void setClassPrintMethod(Method m)
  {
    class_print_method = m;
  }
}
