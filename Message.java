/**
 * Created by priyanka on 3/25/15.
 */
public final class Message implements java.io.Serializable {
  // Message Type options
  public static final int REQUEST = 0;
  public static final int INQUIRE = 1;
  public static final int RELINQUISH = 2;
  public static final int RELEASE = 3;
  public static final int LOCKED = 4;
  public static final int FAILED = 5;

  public static final String[] messageNames = {"request", "inquire", "relinquish", "release", "locked", "failed"};


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

