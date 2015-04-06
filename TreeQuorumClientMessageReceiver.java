import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author Priyanka Menghani
 * @version 1.0
 */
public class TreeQuorumClientMessageReceiver extends Thread{
  private ServerSocket serverSocket;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  public TreeQuorumClientMessageReceiver(int port) throws IOException
  {
    serverSocket = new ServerSocket(port);
  }

  public void closeServer() {
    try {
      System.out.println("~~~~~~~~~~~~~~~~MESSAGES EXCHANGED~~~~~~~~~~~~~~~~");
      System.out.println("Messages sent by this node: " + TreeQuorumClient.messagesSent);
      System.out.println("Messages received by this node: " + TreeQuorumClient.messagesRecvd);
      System.out.println("~~~~~~~~~~~~~~~~SHUTTING MYSELF DOWN~~~~~~~~~~~~~~~~");
      serverSocket.close();
    } catch (IOException e) {
      System.out.println("Socket Closed");
    }
  }

  public int getRightChild(int n) {
    return 2*n + 1;
  }

  public int getLeftChild(int n) {
    return 2*n;
  }

  /**
   * Recursive function to check if the current set of replies form a quorum or not.
   * @param root root of the tree, s1
   * @return True or false, depending on whether the set of replies form a quorum of not
   */
  public boolean isQuorumFormed(int root) {
    if (root > Math.ceil(TreeQuorumClient.maxNodesAtServer/2)) {
      if (TreeQuorumClient.locksReceived.get(root) == false) {
        return false;
      } else {
        return true;
      }
    } else {
      if (TreeQuorumClient.locksReceived.get(root) == true) {
        if (isQuorumFormed(getLeftChild(root)) || isQuorumFormed(getRightChild(root))) {
          return true;
        } else {
          return false;
        }
      } else {
        if (isQuorumFormed(getRightChild(root)) && isQuorumFormed(getLeftChild(root))) {
          return true;
        } else {
          return false;
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
        // Read the requests by the clients
        try {
          Message m = (Message) in.readObject();
          /**
           * Keep track of the highest sequence number received from a reply. Use a higher timestamp for
           * future requests.
           */
          TreeQuorumClient.highest_sequence_number = m.sequenceNumber;

          if (m.messageType.equals("LOCKED") && m.csCount == TreeQuorumClient.csCount) { // If a lock has been granted
            if (TreeQuorumClient.criticalSectionActivated == false) {
              TreeQuorumClient.msgsCurrCS += 1;
              TreeQuorumClient.messagesRecvd += 1;
            }
            TreeQuorumClient.locksReceived.add(m.sender, true);

            //check if current set of replies forms a quorum
            if (isQuorumFormed(1)) {
              TreeQuorumClient.quorumFormed = true;
            }

          } else if (m.messageType.equals("LOCKED") && m.csCount < TreeQuorumClient.csCount) { // If a lock is granted later
            TreeQuorumClient.clientCall(TreeQuorumClient.serverAddresses.get(m.sender), "RELEASE", m.sender,
                TreeQuorumClient.our_sequence_number, m.csCount);
          }
        } catch (Exception e) {
          server.close();
          break;
        }
        server.close();
      }catch(SocketTimeoutException s)
      {
        System.out.println("Socket timed out!");
        break;

      } catch(IOException e) {
        break;
      }
    }
  }
}
