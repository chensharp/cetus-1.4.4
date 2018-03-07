package cppc.compiler.utils;

import java.util.HashSet;
import java.util.Set;

public final class SetOperations<T>
{
  public final Set<T> setIntersection(Set<T> lhs, Set<T> rhs)
  {
    HashSet<T> returnSet = null;
    Set<T> otherSet = null;
    if (lhs.size() > rhs.size())
    {
      returnSet = new HashSet(rhs);
      otherSet = lhs;
    }
    else
    {
      returnSet = new HashSet(lhs);
      otherSet = rhs;
    }
    returnSet.retainAll(otherSet);
    return returnSet;
  }
  
  public final Set<T> setMinus(Set<T> lhs, Set<T> rhs)
  {
    HashSet<T> returnSet = new HashSet(lhs);
    returnSet.removeAll(rhs);
    return returnSet;
  }
}
