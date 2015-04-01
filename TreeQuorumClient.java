import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * Created by priyanka on 3/25/15.
 */
public class TreeQuorumClient {
  static int nodeId;
  static int port;
  static int csCount;
  static int msgsCurrCS = 0;
  static int messagesSent = 0;
  static int messagesRecvd = 0;
  static int oneTimeUnit = 100;
  static int our_sequence_number;
  static final int numServers = 7;
  static volatile boolean quorumFormed;
  static int critical_section_limit = 20;
  static int highest_sequence_number = 0;
  static volatile int completionReports = 0;
  static int maxNodesAtServer = 7;
  static int serverRootId = 1;
  static boolean requesting_critical_section = false;
  static ArrayList<Boolean> locksReceived = new ArrayList<Boolean>(maxNodesAtServer);
  static ArrayList<Integer> failsReceived = new ArrayList<Integer>();
  static ArrayList<Integer> deferredInquiries = new ArrayList<Integer>();
  static ArrayList<Integer> serverAddresses = new ArrayList<Integer>(numServers);

  public static void populateServerAddresses() {
    serverAddresses.add(1, 3231);
    serverAddresses.add(2, 3232);
    serverAddresses.add(3, 3233);
    serverAddresses.add(4, 3234);
    serverAddresses.add(5, 3235);
    serverAddresses.add(6, 3236);
    serverAddresses.add(7, 3237);
  }

  /*  This is called when the critical section is activated.
  * */
  public static void criticalSectionActivated() {
    Date now = new Date();
    System.out.println("Entering CS.....");
    System.out.println(nodeId + " " + System.currentTimeMillis());
    try {
      Thread.sleep(3 * oneTimeUnit);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void clientCall(Integer ipAddress, String callType, int serverID, int seqNum, int csCnt) {
    try
    {
      //Socket client = new Socket(ipAddress, port);
      Socket client = new Socket("localhost", serverAddresses.get(serverID));
      ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
      outToServer.writeObject(new Message(callType, nodeId, serverID, seqNum, csCnt));

      messagesSent += 1;
      msgsCurrCS += 1;

      client.close();

    } catch (ConnectException c) {
      c.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  /*  This function requests all the nodes in the system, for entry into its critical section.
      Only those nodes are sent a request who currently have the token with them
  * */
  public static void requestCS() {
    try {
      for (int i = 0; i < serverAddresses.size(); i++) {
        clientCall(serverAddresses.get(i), "REQUEST", i, our_sequence_number, csCount);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
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

    // Call this function to send request to all (numNodes - 1) nodes in the system.
    requestCS();

    // Wait until you have received a reply from everyone.
    while (!(quorumFormed)) {}

    long currTime2 = System.nanoTime();

    System.out.println("Time elapsed while making requests for CS: " + ((currTime2 - currTime)/1000000) + " milisecond(s)");

    // When in CS, execute these steps.
    criticalSectionActivated();

    // You no longer need this privilege
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

    // Phase: 1
    csCount = 1;
    while (csCount <= critical_section_limit) {
      System.out.println("~~~~~~~~~~~~~~~~NODE: " + nodeId  + " CRITICAL SECTION NUMBER: " + csCount + "~~~~~~~~~~~~~~~~");
      // Keep count of messages exchanged for executing the CS.
      msgsCurrCS = 0;
      stepsForCS();
      System.out.println("Messages Exchanged: " + msgsCurrCS);
      csCount++;
      locksReceived.clear();
      failsReceived.clear();
    }

    // Phase: 2
    csCount = 21;
    while (csCount - 20 <= critical_section_limit) {
      System.out.println("~~~~~~~~~~~~~~~~NODE: " + nodeId  + " CRITICAL SECTION NUMBER: " + csCount + "~~~~~~~~~~~~~~~~");
      // Keep count of messages exchanged for executing the CS.
      msgsCurrCS = 0;
      stepsForCS();
      System.out.println("Messages Exchanged: " + msgsCurrCS);
      csCount++;
      locksReceived.clear();
      failsReceived.clear();
      deferredInquiries.clear();
      if (nodeId % 2 == 0) {
        // wait for a random unit of time between the range [45-50]
        Random random = new Random();
        try {
          Thread.sleep(generateRandomNumber(45, 50, random));
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    }

  }
}
