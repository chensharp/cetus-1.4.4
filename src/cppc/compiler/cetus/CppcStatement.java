package cppc.compiler.cetus;

import cetus.hir.Identifier;
import cetus.hir.NotAChildException;
import cetus.hir.Statement;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cppc.compiler.transforms.shared.comms.Communication;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CppcStatement
  extends Statement
{
  private Set<Identifier> generated;
  private Set<Identifier> consumed;
  private Set<Identifier> partialGenerated;
  private Set<VariableDeclaration> globalGenerated;
  private Set<VariableDeclaration> globalConsumed;
  private Set<Communication> matchingCommunications;
  private boolean safePoint;
  private long weight;
  public int statementCount;
  
  private CppcStatement() {}
  
  public CppcStatement(Statement statement)
  {
    this.children.add(statement);
    this.generated = new HashSet();
    this.consumed = new HashSet();
    this.partialGenerated = new HashSet();
    this.globalGenerated = new HashSet();
    this.globalConsumed = new HashSet();
    this.matchingCommunications = new HashSet();
    this.safePoint = true;
    this.weight = 0L;
  }
  
  public Set<Identifier> getGenerated()
  {
    return this.generated;
  }
  
  public Set<VariableDeclaration> getGlobalGenerated()
  {
    return this.globalGenerated;
  }
  
  public void setGenerated(Set<Identifier> generated)
  {
    this.generated = generated;
  }
  
  public void setGlobalGenerated(Set<VariableDeclaration> globalGenerated)
  {
    this.globalGenerated = globalGenerated;
  }
  
  public Set<Identifier> getConsumed()
  {
    return this.consumed;
  }
  
  public Set<VariableDeclaration> getGlobalConsumed()
  {
    return this.globalConsumed;
  }
  
  public Set<Identifier> getPartialGenerated()
  {
    return this.partialGenerated;
  }
  
  public Set<Communication> getMatchingCommunications()
  {
    return this.matchingCommunications;
  }
  
  public boolean getSafePoint()
  {
    return this.safePoint;
  }
  
  public void setSafePoint(boolean safePoint)
  {
    this.safePoint = safePoint;
  }
  
  public long getWeight()
  {
    return this.weight;
  }
  
  public void setWeight(long weight)
  {
    this.weight = weight;
  }
  
  public void setConsumed(Set<Identifier> consumed)
  {
    this.consumed = consumed;
  }
  
  public void setGlobalConsumed(Set<VariableDeclaration> globalConsumed)
  {
    this.globalConsumed = globalConsumed;
  }
  
  public void setPartialGenerated(Set<Identifier> partialGenerated)
  {
    this.partialGenerated = partialGenerated;
  }
  
  public Statement getStatement()
  {
    return (Statement)this.children.get(0);
  }
  
  public void removeChild(Traversable child)
    throws NotAChildException
  {
    if (!getStatement().equals(child)) {
      throw new NotAChildException();
    }
    this.children.remove(child);
  }
  
  public void print(OutputStream stream)
  {
    getStatement().print(stream);
  }
  
  public void setLineNumber(int line)
  {
    getStatement().setLineNumber(line);
  }
  
  public String toString()
  {
    return getStatement().toString();
  }
  
  public int where()
  {
    return getStatement().where();
  }
  
  public Object clone()
  {
    CppcStatement clone = new CppcStatement(
      (Statement)getStatement().clone());
    clone.getStatement().setParent(clone);
    clone.generated.addAll(this.generated);
    clone.consumed.addAll(this.consumed);
    clone.globalGenerated.addAll(this.globalGenerated);
    clone.globalConsumed.addAll(this.globalConsumed);
    
    clone.matchingCommunications.addAll(this.matchingCommunications);
    clone.safePoint = this.safePoint;
    clone.weight = this.weight;
    
    return clone;
  }
}
