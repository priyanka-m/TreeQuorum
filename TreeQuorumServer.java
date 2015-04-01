import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class TimestampComp implements Comparator<Message> {

  @Override
  public int compare(Message m1, Message m2) {
    if(m1.getTimestamp() < m2.getTimestamp()){
      return 1;
    } else if (m1.getTimestamp() == m2.getTimestamp()) {
      if (m1.getSender() < m2.getSender()) {
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
public class TreeQuorumServer {
  int serverID;
  private ServerSocket serverSocket;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private Message lockedClientMessage = null;
  private int numClients = 5;
  private int port;
  ArrayList<Integer> clientAddresses = new ArrayList<Integer>(numClients);
  ArrayList<Message> waitingQueue = new ArrayList<Message>();

  public TreeQuorumServer(int port) throws IOException
  {
    serverSocket = new ServerSocket(port);
  }

  public void closeServer() {
    try {
      System.out.println("~~~~~~~~~~~~~~~~MESSAGES EXCHANGED~~~~~~~~~~~~~~~~");
      System.out.println("Messages sent by this node: " );
      System.out.println("Messages received by this node: ");
      System.out.println("~~~~~~~~~~~~~~~~SHUTTING MYSELF DOWN~~~~~~~~~~~~~~~~");
      serverSocket.close();
    } catch (IOException e) {
      System.out.println("Socket Closed");
    }
  }

  public void populateClientAddresses() {
    clientAddresses.add(1,4341);
    clientAddresses.add(2, 4342);
    clientAddresses.add(3, 4343);
    clientAddresses.add(4, 4344);
    clientAddresses.add(5, 4345);
  }
  public void sendMessageToClient(Message m) {
    try {
      //String ipAddress = clientAddresses.get(m.destination);
      Integer port = clientAddresses.get(m.destination);
      Socket toClient = new Socket("localhost", port);
      ObjectOutputStream outToServer = new ObjectOutputStream(toClient.getOutputStream());
      outToServer.writeObject(m);
      toClient.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void addtoWaitingQueue(Message m) {
    waitingQueue.add(m);
    Collections.sort(waitingQueue,new TimestampComp());
  }

  public void run()
  {
    while(true)
    {
      try
      {
        System.out.println("Server started");
        // Accept client connections.
        Socket server = serverSocket.accept();
        in = new ObjectInputStream(server.getInputStream());
        // Read the requests by the clients
        try {
          Message m = (Message) in.readObject();
          if (m.messageType.equals("REQUEST")) {
            addtoWaitingQueue(m);
            Message lockingMessage = waitingQueue.get(0);
            if (lockedClientMessage == null) {
              sendMessageToClient(new Message("LOCKED", serverID, lockingMessage.sender,lockingMessage.sequenceNumber, lockingMessage.csCount));
            } else if (lockedClientMessage != null) {
              if (lockedClientMessage.sequenceNumber > lockingMessage.sequenceNumber) {
                sendMessageToClient(new Message("INQUIRE", serverID, lockedClientMessage.sender, lockedClientMessage.sequenceNumber, lockedClientMessage.csCount));
              }
            }
            if (lockedClientMessage != m) {
              sendMessageToClient(new Message("FAILED", serverID, m.sender, m.sequenceNumber, m.csCount));
            }
          } else if (m.messageType.equals("RELINQUISH")) {
            addtoWaitingQueue(lockedClientMessage);
            sendMessageToClient(new Message("FAILED", serverID, lockedClientMessage.sender, lockedClientMessage.sequenceNumber, lockedClientMessage.csCount));
            Message lockingMessage = waitingQueue.get(0);
            waitingQueue.remove(lockingMessage);
            lockedClientMessage = lockingMessage;
            sendMessageToClient(new Message("LOCKED", serverID, lockingMessage.sender,lockingMessage.sequenceNumber, lockingMessage.csCount));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        server.close();
      }catch(SocketTimeoutException s)
      {
        System.out.println("Socket timed out!");
        break;

      } catch(IOException e) {
        System.out.println("~~~~~~~Algorithm Ends~~~~~~");
        break;
      }
    }
  }
  public static void main(String[] args) {
    try {
      TreeQuorumServer TQS = new TreeQuorumServer(Integer.parseInt(args[1]));
      TQS.port = Integer.parseInt(args[1]);
      TQS.serverID = Integer.parseInt(args[0]);
      TQS.populateClientAddresses();
      TQS.run();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
