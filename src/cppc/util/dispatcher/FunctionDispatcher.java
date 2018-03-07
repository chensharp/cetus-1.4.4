package cppc.util.dispatcher;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.util.Hashtable;

public abstract class FunctionDispatcher<T>
{
  private Hashtable<String, Hashtable<Class, Method>> matches;
  
  public FunctionDispatcher()
  {
    this.matches = new Hashtable();
  }
  
  public Method dispatch(T t, String methodName)
  {
    Hashtable<Class, Method> methodMatches = (Hashtable)this.matches.get(methodName);
    Method m;
    if (methodMatches == null)
    {
      try
      {
        Method m = getHierarchyDeclaredMethod(getClass(), methodName, 
          t.getClass());
        methodMatches = new Hashtable();
        methodMatches.put(t.getClass(), m);
        this.matches.put(methodName, methodMatches);
      }
      catch (NoSuchMethodException e)
      {
        return null;
      }
    }
    else
    {
      m = (Method)methodMatches.get(t.getClass());
      if (m == null) {
        try
        {
          m = getHierarchyDeclaredMethod(getClass(), 
            methodName, t.getClass());
          methodMatches.put(t.getClass(), m);
        }
        catch (NoSuchMethodException e)
        {
          return null;
        }
      }
    }
    return m;
  }
  
  private static Method getHierarchyDeclaredMethod(Class subClass, String methodName, Class parameterType)
    throws NoSuchMethodException, SecurityException
  {
    try
    {
      return subClass.getDeclaredMethod(methodName, new Class[] { parameterType });
    }
    catch (NoSuchMethodException e)
    {
      try
      {
        subClass = (Class)subClass.getGenericSuperclass();
        if (subClass == null) {
          throw e;
        }
        return getHierarchyDeclaredMethod(subClass, methodName, 
          parameterType);
      }
      catch (ClassCastException ex)
      {
        throw e;
      }
      catch (TypeNotPresentException ex)
      {
        ex.printStackTrace();
      }
      catch (MalformedParameterizedTypeException ex)
      {
        ex.printStackTrace();
      }
    }
    return null;
  }
}
