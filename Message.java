/**
 * @author Priyanka
 * @version 1.0
 */
public final class Message implements java.io.Serializable {
  public String messageType;
  public int sender;
  public int destination;
  public int sequenceNumber;
  public int csCount;

  public Message(String messageType, int sender, int destination,
                 int sequenceNumber, int csCount) {
    this.messageType = messageType;
    this.sender = sender;
    this.destination = destination;
    this.sequenceNumber = sequenceNumber;
    this.csCount = csCount;
  }

  public int getTimestamp() {
    return this.sequenceNumber;
  }

  public int getSender() {
    return this.sender;
  }

} // Message

