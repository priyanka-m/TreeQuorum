import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

/** This class represents the client, that wants to enter the CS
 * @author Priyanka Menghani
 * @version 1.0
 */
public class TreeQuorumClient {
  static int nodeId;
  static int port;
  static int csCount;
  static int msgsCurrCS = 0;
  static int messagesSent = 0;
  static int messagesRecvd = 0;
  static int oneTimeUnit;
  static int our_sequence_number;
  static final int numServers = 7;
  static volatile boolean quorumFormed;
  static int critical_section_limit = 20;
  static volatile int highest_sequence_number = 0;
  static int maxNodesAtServer = 7;
  static volatile boolean criticalSectionActivated = false;
  static boolean requesting_critical_section = false;
  static volatile ArrayList<Boolean> locksReceived = new ArrayList<Boolean>(Collections.nCopies(maxNodesAtServer+1,false));
  static ArrayList<String> serverAddresses = new ArrayList<String>(numServers + 1);

  /**
   * This method populates the list of server addresses.
   */
  public static void populateServerAddresses() {
    serverAddresses.add(0,"");
    serverAddresses.add(1, "dc31.utdallas.edu");
    serverAddresses.add(2, "dc32.utdallas.edu");
    serverAddresses.add(3, "dc33.utdallas.edu");
    serverAddresses.add(4, "dc34.utdallas.edu");
    serverAddresses.add(5, "dc35.utdallas.edu");
    serverAddresses.add(6, "dc36.utdallas.edu");
    serverAddresses.add(7, "dc37.utdallas.edu");
  }

  /** Critical section activated.
   * In a CS, a node sleeps for 3 units of time before exiting.
  */
  public static void criticalSectionActivated() {
    criticalSectionActivated = true;
    try {
      Thread.sleep(3 * oneTimeUnit);
    } catch (Exception e) {
      e.printStackTrace();
    }
    criticalSectionActivated = false;
  }

  /**
   * Send completion message to the root server(s1)
   */
  public static void sendCompletionReport() {
    clientCall(serverAddresses.get(0), "COMPLETION", 0, our_sequence_number, csCount);
  }

  /**
   * Connect to the server at 'ipAddress' and send a request of type 'callType'
   * @param ipAddress IP address of the server
   * @param callType Type of message being sent, i.e REQUEST or RELEASE
   * @param serverID ID of the server
   * @param seqNum The current sequence number(timestamp)
   * @param csCnt The current Critical Section Count(1-40)
   */
  public static void clientCall(String ipAddress, String callType, int serverID, int seqNum, int csCnt) {
    try {
      Socket client = new Socket(ipAddress, port);
      try
      {
        ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
        outToServer.writeObject(new Message(callType, nodeId, serverID, seqNum, csCnt));
        client.close();

      } catch (Exception c) {
        client.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Release the lock on servers
   */
  public static void relinquishCS() {
    try {
      for (int i = 1; i < serverAddresses.size(); i++) {
        messagesSent += 1;
        msgsCurrCS += 1;
        clientCall(serverAddresses.get(i), "RELEASE", i, 0, 0);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Send requests to all the servers for entry into CS
   */
  public static void requestCS() {
    try {
      for (int i = 1; i < serverAddresses.size(); i++) {
        messagesSent += 1;
        msgsCurrCS += 1;
        clientCall(serverAddresses.get(i), "REQUEST", i, our_sequence_number, csCount);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * The list of steps to be executed, before entry and after exit from the CS
   */
  public static void stepsForCS() {
    int START = 5, END = 10;
    Random random = new Random();
    try {
      Thread.sleep(generateRandomNumber(START, END, random) * oneTimeUnit);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Begin the actual computation
    requesting_critical_section = true;
    quorumFormed = false;
    our_sequence_number = highest_sequence_number + 1;

    // We need to keep a record of the elapsed time.
    long currTime = System.nanoTime();

    // Call this function to send request to all servers in the system.
    requestCS();

    // Wait until you have received a reply from a particular quorum.
    while (!quorumFormed) {}

    long currTime2 = System.nanoTime();

    System.out.print(" latency: " + ((currTime2 - currTime) / 1000000) + " milisecond(s)");

    // When in CS, execute these steps.
    criticalSectionActivated();

    // Release the lock on the servers
    relinquishCS();

    requesting_critical_section = false;

  }

  // Generates a random number between the range [aStart, aEnd]
  private static int generateRandomNumber(int aStart, int aEnd, Random aRandom){
    if (aStart > aEnd) {
      throw new IllegalArgumentException("Start cannot exceed End.");
    }
    //get the range, casting to long to avoid overflow problems
    long range = (long)aEnd - (long)aStart + 1;
    // compute a fraction of the range, 0 <= frac < range
    long fraction = (long)(range * aRandom.nextDouble());
    return  (int)(fraction + aStart);
  }

  public static void main(String[] args) {
    nodeId = Integer.parseInt(args[0]);
    port = Integer.parseInt(args[1]);
    oneTimeUnit = Integer.parseInt(args[2]);

    // Populate the server addresses in a list
    populateServerAddresses();

    try {
      // Start the message receiver thread
      TreeQuorumClientMessageReceiver tqcmr = new TreeQuorumClientMessageReceiver(port);
      tqcmr.start();

      System.out.println("Wait time = [5, 10] units and time in CS = 3 units");
      // Phase: 1
      csCount = 1;
      while (csCount <= critical_section_limit) {
        System.out.print("request: " + csCount);
        msgsCurrCS = 0;
        stepsForCS();
        System.out.println(" Messages Exchanged: " + msgsCurrCS);
        csCount++;
        // Clear all the locks
        locksReceived.clear();
        locksReceived.addAll(Collections.nCopies(8,false));
      }

      // Phase: 2
      csCount = 21;
      while (csCount - 20 <= critical_section_limit) {
        System.out.print("request: " + csCount);
        // Keep count of messages exchanged for executing the CS.
        msgsCurrCS = 0;
        stepsForCS();
        System.out.println(" Messages Exchanged: " + msgsCurrCS);
        csCount++;
        locksReceived.clear();
        locksReceived.addAll(Collections.nCopies(8,false));
        if (nodeId % 2 == 0) {
          // wait for a random unit of time between the range [45-50]
          Random random = new Random();
          try {
            Thread.sleep(generateRandomNumber(45, 50, random)*oneTimeUnit);
          } catch (Exception e) {
            e.printStackTrace();
          }

        }
      }

      Date dt = new Date();
      System.out.println("finished at " + dt.toString());

      // Send a report to S1 for CS execution completion
      sendCompletionReport();

      // Close the message receiver thread
      tqcmr.closeServer();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
