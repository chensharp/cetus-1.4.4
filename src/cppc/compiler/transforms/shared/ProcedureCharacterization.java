package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;
import cetus.hir.Procedure;
import cetus.hir.VariableDeclaration;

import cppc.compiler.transforms.shared.comms.CommunicationBuffer;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


//函数的描述。。。简称 pc
//
//
public class ProcedureCharacterization
{
  private Identifier name;//名字

  private Procedure procedure;//指向归属的函数

  private Set<ProcedureParameter> generated;//

  private Set<ProcedureParameter> consumed;//
  
  private Set<VariableDeclaration> globalGenerated;
  
  private Set<VariableDeclaration> globalConsumed;

  private Hashtable<String, Hashtable<String, String>> semantics;//语义
  
  private boolean isPragmed;//

  private boolean isCheckpointed;//是否被checkpoint

  private boolean isNull;

  private CommunicationBuffer commBuffer;
  
  private long weight;//权值
  
  public int statementCount;//语句计数
  
  private Set<Identifier> calledFrom;
  
  private Set<Identifier> calls;//调用
  
  private Map<Identifier, Set<Identifier>> variableDependencies;//变量依赖
  
  //构造函数，参数均置为空
  public ProcedureCharacterization(Identifier name)
  {
    this.name = name;
    this.procedure = null;
    this.generated = new HashSet(0);
    this.consumed = new HashSet(0);
    this.globalGenerated = new HashSet(0);
    this.globalConsumed = new HashSet(0);
    this.semantics = new Hashtable(0);
    this.isPragmed = false;
    this.isCheckpointed = false;
    this.isNull = false;
    this.commBuffer = null;
    this.weight = 1L;
    this.calledFrom = new HashSet();
    this.calls = new HashSet();
    this.variableDependencies = null;
  }
  
  public Identifier getName()
  {
    return this.name;
  }
  
  public Procedure getProcedure()
  {
    return this.procedure;
  }
  
  public void setProcedure(Procedure procedure)
  {
    this.procedure = procedure;
  }
  
  public Set<ProcedureParameter> getGenerated()
  {
    return this.generated;
  }
  
  public void setGenerated(Set<ProcedureParameter> generated)
  {
    this.generated = generated;
  }
  
  public Set<ProcedureParameter> getConsumed()
  {
    return this.consumed;
  }
  
  public void setConsumed(Set<ProcedureParameter> consumed)
  {
    this.consumed = consumed;
  }
  
  public void setGlobalGenerated(Set<VariableDeclaration> globalGenerated)
  {
    this.globalGenerated = globalGenerated;
  }
  
  public Set<VariableDeclaration> getGlobalGenerated()
  {
    return this.globalGenerated;
  }
  
  public void setGlobalConsumed(Set<VariableDeclaration> globalConsumed)
  {
    this.globalConsumed = globalConsumed;
  }
  
  public Set<VariableDeclaration> getGlobalConsumed()
  {
    return this.globalConsumed;
  }
  
  public void setSemantics(Hashtable<String, Hashtable<String, String>> semantics)
  {
    this.semantics = semantics;
  }
  
  public boolean hasSemantic(String key)
  {
    return this.semantics.get(key) != null;
  }
  
  public void addSemantic(String role, Hashtable<String, String> parameters)
  {
    this.semantics.put(role, parameters);
  }
  
  public Hashtable<String, String> getSemantic(String key)
  {
    return (Hashtable)this.semantics.get(key);
  }
  
  public boolean getPragmed()
  {
    return this.isPragmed;
  }
  
  public void setPragmed(boolean isPragmed)
  {
    this.isPragmed = isPragmed;
  }
  
  public boolean getCheckpointed()
  {
    return this.isCheckpointed;
  }
  
  public void setCheckpointed(boolean isCheckpointed)
  {
    this.isCheckpointed = isCheckpointed;
  }
  
  public boolean isNull()
  {
    return this.isNull;
  }
  
  public void setNull(boolean b)
  {
    this.isNull = b;
  }
  
  public CommunicationBuffer getCommunicationBuffer()
  {
    return this.commBuffer;
  }
  
  public void setCommunicationBuffer(CommunicationBuffer commBuffer)
  {
    this.commBuffer = commBuffer;
  }
  
  public long getWeight()
  {
    return this.weight;
  }
  
  public void setWeight(long weight)
  {
    this.weight = weight;
  }
  
  public Set<Identifier> getCalledFrom()
  {
    return this.calledFrom;
  }
  
  public Set<Identifier> getCalls()
  {
    return this.calls;
  }
  
  public void addCalledFrom(Identifier proc)
  {
    this.calledFrom.add(proc);
  }
  
  public void addCall(Identifier proc)
  {
    this.calls.add(proc);
  }
  
  public Map<Identifier, Set<Identifier>> getVariableDependencies()
  {
    return this.variableDependencies;
  }
  
  public Set<Identifier> getVariableDependencies(Identifier id)
  {
    return (Set)this.variableDependencies.get(id);
  }
  
  public void setVariableDependencies(Map<Identifier, Set<Identifier>> vd)
  {
    this.variableDependencies = vd;
  }
}
