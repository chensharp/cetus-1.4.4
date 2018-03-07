package cppc.compiler.transforms.shared.comms;

import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.Identifier;
import cppc.compiler.transforms.shared.CppcRegisterManager;
import cppc.compiler.transforms.shared.ProcedureCharacterization;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Communication
  implements Cloneable
{
  private Map<String, String> properties;
  private Map<String, Expression> expressionProperties;
  private FunctionCall call;
  private List<Expression> conditions;
  public static final String RANKER = "CPPC/Comm/Ranker";
  public static final String RECV = "CPPC/Comm/Recv";
  public static final String SEND = "CPPC/Comm/Send";
  public static final String SIZER = "CPPC/Comm/Sizer";
  public static final String WAIT = "CPPC/Comm/Wait";
  public static final String ROLE = "Role";
  public static final String P2P = "P2P";
  public static final String COLLECTIVE = "Collective";
  public static final String BLOCKING = "Blocking";
  public static final String BUFFER = "Buffer";
  public static final String COMMUNICATOR = "Communicator";
  public static final String COUNT = "Count";
  public static final String DATATYPE = "Datatype";
  public static final String DESTINATION = "Destination";
  public static final String RANK = "Rank";
  public static final String REQUEST = "Request";
  public static final String SIZE = "Size";
  public static final String SOURCE = "Source";
  public static final String TAG = "Tag";
  public static final String TYPE = "Type";
  public static final HashSet<Identifier> rankerFunctions = initHashSet("CPPC/Comm/Ranker");
  public static final HashSet<Identifier> recvFunctions = initHashSet("CPPC/Comm/Recv");
  public static final HashSet<Identifier> sendFunctions = initHashSet("CPPC/Comm/Send");
  public static final HashSet<Identifier> sizerFunctions = initHashSet("CPPC/Comm/Sizer");
  public static final HashSet<Identifier> waitFunctions = initHashSet("CPPC/Comm/Wait");
  
  private static final HashSet<Identifier> initHashSet(String role)
  {
    return CppcRegisterManager.getProceduresWithRole(role);
  }
  
  public Communication()
  {
    this.properties = new HashMap();
    this.expressionProperties = new HashMap();
    this.call = null;
    this.conditions = new ArrayList();
  }
  
  public String getProperty(String key)
  {
    return (String)this.properties.get(key);
  }
  
  public Expression getExpressionProperty(String key)
  {
    return (Expression)this.expressionProperties.get(key);
  }
  
  public void setProperty(String key, String value)
  {
    this.properties.put(key, value);
  }
  
  public void setExpressionProperty(String key, Expression value)
  {
    this.expressionProperties.put(key, value);
  }
  
  public Map<String, String> getProperties()
  {
    return this.properties;
  }
  
  public Map<String, Expression> getExpressionProperties()
  {
    return this.expressionProperties;
  }
  
  public FunctionCall getCall()
  {
    return this.call;
  }
  
  public List<Expression> getConditions()
  {
    return this.conditions;
  }
  
  public void addCondition(Expression condition)
  {
    this.conditions.add(condition);
  }
  
  public Expression getCallValue(String key)
  {
    Integer valuePos = new Integer((String)this.properties.get(key));
    return this.call.getArgument(valuePos.intValue() - 1);
  }
  
  public static final Communication fromCall(FunctionCall call)
  {
    ProcedureCharacterization c = CppcRegisterManager.getCharacterization(
      (Identifier)call.getName());
    
    Communication communication = new Communication();
    Hashtable<String, String> s = c.getSemantic("CPPC/Comm/Send");
    if (s == null)
    {
      s = c.getSemantic("CPPC/Comm/Recv");
      if (s == null)
      {
        s = c.getSemantic("CPPC/Comm/Wait");
        communication.setProperty("Role", "CPPC/Comm/Wait");
      }
      else
      {
        communication.setProperty("Role", "CPPC/Comm/Recv");
      }
    }
    else
    {
      communication.setProperty("Role", "CPPC/Comm/Send");
    }
    for (Enumeration<String> e = s.keys(); e.hasMoreElements();)
    {
      String k = (String)e.nextElement();
      
      communication.setProperty(k, (String)s.get(k));
    }
    communication.call = call;
    
    return communication;
  }
  
  public String toString()
  {
    String txt = "Communication: " + this.call.getName() + "\n";
    for (String k : this.properties.keySet())
    {
      String t = (String)this.properties.get(k);
      try
      {
        int pos = new Integer(t).intValue();
        Expression v = this.call.getArgument(pos - 1);
        txt = txt + "\t" + k + ": " + v.toString() + "\n";
      }
      catch (Exception e)
      {
        txt = txt + "\t" + k + ": " + t + "\n";
      }
    }
    return txt;
  }
  
  public Object clone()
  {
    Communication clone = new Communication();
    clone.properties.putAll(this.properties);
    clone.expressionProperties.putAll(this.expressionProperties);
    
    clone.call = ((FunctionCall)this.call.clone());
    
    clone.call.setParent(this.call.getParent());
    
    return clone;
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Communication)) {
      return false;
    }
    Communication safeObj = (Communication)obj;
    return getCall().equals(safeObj.getCall());
  }
}
