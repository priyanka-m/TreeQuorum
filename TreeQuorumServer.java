import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class is used to compare the messages in the waiting queue, based on their priority
 */
class TimestampComp implements Comparator<Message> {

  @Override
  public int compare(Message m1, Message m2) {
    if(m1.getTimestamp() > m2.getTimestamp()){
      return 1;
    } else if (m1.getTimestamp() == m2.getTimestamp()) {
      if (m1.getSender() > m2.getSender()) {
        return 1;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }
}
/**
 * This class represents a server, that receives messages from clients
 * and performs actions based on the messages.
 */
public class TreeQuorumServer extends Thread{
  int serverID;
  private ServerSocket serverSocket;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private Message lockedClientMessage = null;
  private int numClients = 5;
  private int port;
  private int highest_request_seen = 0;
  private int completedNodes = 0;
  ArrayList<String> clientAddresses = new ArrayList<String>(numClients + 1);
  List<Message> waitingQueue = Collections.synchronizedList(new ArrayList<Message>());

  public TreeQuorumServer(int port) throws IOException
  {
    serverSocket = new ServerSocket(port);
  }

  public void closeServer() {
    try {
      serverSocket.close();
    } catch (IOException e) {
      System.out.println("Socket Closed");
    }
  }

  public void populateClientAddresses() {
    clientAddresses.add(0, "");
    clientAddresses.add(1,"dc11.utdallas.edu");
    clientAddresses.add(2,"dc12.utdallas.edu");
    clientAddresses.add(3,"dc13.utdallas.edu");
    clientAddresses.add(4,"dc14.utdallas.edu");
    clientAddresses.add(5,"dc15.utdallas.edu");
  }

  /**
   * Communicate a message to the client requesting a CS
   * @param m Message to be sent to the client
   */
  public void sendMessageToClient(Message m) {
      try {
        Socket toClient = new Socket(clientAddresses.get(m.destination), port);
        ObjectOutputStream outToServer = new ObjectOutputStream(toClient.getOutputStream());
        outToServer.writeObject(m);
        toClient.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  /**
   * Add new requests to the waiting queue if they are not highest priority. Sort the newly added request
   * with respect to its priority
   * @param m Message object to be added to the waiting queue
   */
  public void addtoWaitingQueue(Message m) {
    synchronized (waitingQueue) {
      waitingQueue.add(m);
      if (waitingQueue.size() >= 1)
        Collections.sort(waitingQueue,new TimestampComp());
    }

  }

  /**
   * Find the request sent by 'clientID' and delete it, since its request has been satisfied
   * @param clientID node ID of the client sending the request
   */
  void findRequestInQueueAndDelete(int clientID) {
    synchronized (waitingQueue) {
      for (Message request : waitingQueue) {
        if (request.getSender() == clientID) {
          waitingQueue.remove(request);
          break;
        }
      }
    }
  }

  public void run()
  {
    while(true)
    {
      try
      {
        // Accept client connections.
        Socket server = serverSocket.accept();
        in = new ObjectInputStream(server.getInputStream());

        Message m = (Message) in.readObject();
        if (m != null) {
          try {
            if (m.messageType.equals("REQUEST")) { // If a request for CS is received
              highest_request_seen = highest_request_seen > m.sequenceNumber ? highest_request_seen : m.sequenceNumber;
              if (lockedClientMessage == null) { // If the server is unlocked, grant the lock
                lockedClientMessage = m;
                sendMessageToClient(new Message("LOCKED", serverID, m.sender, highest_request_seen, m.csCount));
              } else { // If the server is locked by another request, add to the waiting queue, sorted
                addtoWaitingQueue(m);
              }
            }
            else if (m.messageType.equals("RELEASE")) { // If a release is received from a client
              if (waitingQueue.size() == 0) { // If no other request is waiting
                lockedClientMessage = null;
              } else {
                if (m.sender != lockedClientMessage.sender) { // If the client which has released, is not the locking client
                  findRequestInQueueAndDelete(m.sender);
                } else { // Find the next highest priority request and lock it
                  synchronized (waitingQueue) {
                    lockedClientMessage = waitingQueue.get(0);
                    waitingQueue.remove(0);
                  }
                  sendMessageToClient(new Message("LOCKED", serverID, lockedClientMessage.sender, highest_request_seen,
                      lockedClientMessage.csCount));
                }
              }
            } else if (m.messageType.equals("COMPLETION")) { // If completion notification is received
              completedNodes += 1;
              if (completedNodes == 5) {
                closeServer();
              }
            }
          } catch (Exception e) {
            server.close();
            break;
          }
        }
        server.close();
      }catch(Exception s)
      {
        System.out.println("Socket timed out!");
        break;
      }
    }
  }
  public static void main(String[] args) {
    try {
      // Start the server
      TreeQuorumServer tqs = new TreeQuorumServer(Integer.parseInt(args[1]));
      tqs.port = Integer.parseInt(args[1]);
      tqs.serverID = Integer.parseInt(args[0]);
      tqs.populateClientAddresses();
      tqs.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
