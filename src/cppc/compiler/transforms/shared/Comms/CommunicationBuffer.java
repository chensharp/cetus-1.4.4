package cppc.compiler.transforms.shared.comms;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CommunicationBuffer
  implements Cloneable
{
  private Queue<Communication> unmatchedSends;
  private Queue<Communication> unmatchedRecvs;
  private Queue<Communication> unwaitedComms;
  private Queue<Communication> unmatchedWaits;
  private boolean pendingCommunications;
  
  public CommunicationBuffer()
  {
    this.unmatchedSends = new LinkedList();
    this.unmatchedRecvs = new LinkedList();
    this.unwaitedComms = new LinkedList();
    this.unmatchedWaits = new LinkedList();
    
    this.pendingCommunications = false;
  }
  
  public CommunicationBuffer(CommunicationBuffer parent)
  {
    this.unmatchedSends = new LinkedList();
    this.unmatchedRecvs = new LinkedList();
    this.unwaitedComms = new LinkedList();
    this.unmatchedWaits = new LinkedList();
    
    this.pendingCommunications = parent.getPendingCommunications();
  }
  
  public Queue<Communication> getUnmatchedSends()
  {
    return this.unmatchedSends;
  }
  
  public void setUnmatchedSends(Queue<Communication> unmatchedSends)
  {
    this.unmatchedSends = unmatchedSends;
  }
  
  public Queue<Communication> getUnmatchedRecvs()
  {
    return this.unmatchedRecvs;
  }
  
  public void setUnmatchedRecvs(Queue<Communication> unmatchedRecvs)
  {
    this.unmatchedRecvs = unmatchedRecvs;
  }
  
  public Queue<Communication> getUnwaitedComms()
  {
    return this.unwaitedComms;
  }
  
  public void setUnwaitedComms(Queue<Communication> unwaitedComms)
  {
    this.unwaitedComms = unwaitedComms;
  }
  
  public Queue<Communication> getUnmatchedWaits()
  {
    return this.unmatchedWaits;
  }
  
  public void setUnmatchedWaits(Queue<Communication> unmatchedWaits)
  {
    this.unmatchedWaits = unmatchedWaits;
  }
  
  public boolean getPendingCommunications()
  {
    return (this.pendingCommunications) || (!isEmpty());
  }
  
  public void setPendingCommunications(boolean pendingCommunications)
  {
    this.pendingCommunications = pendingCommunications;
  }
  
  public List<Communication> getAll()
  {
    ArrayList<Communication> allComms = new ArrayList(
      this.unmatchedSends.size() + this.unmatchedRecvs.size() + 
      this.unwaitedComms.size() + this.unmatchedWaits.size());
    
    allComms.addAll(this.unmatchedSends);
    allComms.addAll(this.unmatchedRecvs);
    allComms.addAll(this.unwaitedComms);
    allComms.addAll(this.unmatchedWaits);
    
    return allComms;
  }
  
  public boolean remove(Communication comm)
  {
    if (removeFromBuffer(comm, this.unmatchedSends)) {
      return true;
    }
    if (removeFromBuffer(comm, this.unmatchedRecvs)) {
      return true;
    }
    if (removeFromBuffer(comm, this.unwaitedComms)) {
      return true;
    }
    if (removeFromBuffer(comm, this.unmatchedWaits)) {
      return true;
    }
    return false;
  }
  
  private boolean removeFromBuffer(Communication comm, Queue<Communication> buffer)
  {
    for (Communication c : buffer) {
      if (c == comm)
      {
        buffer.remove(c);
        return true;
      }
    }
    return false;
  }
  
  public boolean isEmpty()
  {
    return (this.unmatchedSends.isEmpty()) && (this.unmatchedRecvs.isEmpty()) && 
      (this.unwaitedComms.isEmpty()) && (this.unmatchedWaits.isEmpty());
  }
  
  public Object clone()
  {
    CommunicationBuffer clone = new CommunicationBuffer();
    
    clone.unmatchedSends.addAll(this.unmatchedSends);
    clone.unmatchedRecvs.addAll(this.unmatchedRecvs);
    clone.unwaitedComms.addAll(this.unwaitedComms);
    clone.unmatchedWaits.addAll(this.unmatchedWaits);
    
    clone.pendingCommunications = this.pendingCommunications;
    
    return clone;
  }
  
  public String toString()
  {
    return 
    
      "Unmatched sends: " + this.unmatchedSends.size() + "\n" + "Unmatched recvs: " + this.unmatchedRecvs.size() + "\n" + "Unwaited  comms: " + this.unwaitedComms.size() + "\n" + "Unmatched waits: " + this.unmatchedWaits.size();
  }
}
