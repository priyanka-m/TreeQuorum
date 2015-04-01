import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by priyanka on 3/26/15.
 */
public class TreeQuorumClientMessageReceiver {
  int serverID;
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
      System.out.println("Messages sent by this node: " );
      System.out.println("Messages received by this node: ");
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

  public boolean isQuorumFormed(int root) {
    if (root > TreeQuorumClient.maxNodesAtServer/2) {
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
          if (m.messageType.equals("LOCKED") && m.csCount == TreeQuorumClient.csCount) {
            TreeQuorumClient.locksReceived.add(m.sender, true);
            //check if current set of replies forms a quorum
            if (isQuorumFormed(1)) {
              TreeQuorumClient.quorumFormed = true;
            }
          } else if (m.messageType.equals("LOCKED") && m.csCount < TreeQuorumClient.csCount) {
            TreeQuorumClient.clientCall(TreeQuorumClient.serverAddresses.get(m.sender),"RELINQUISH", m.sender, TreeQuorumClient.our_sequence_number, m.csCount);
          } else if (m.messageType.equals("FAILED") && m.csCount == TreeQuorumClient.csCount) {
            TreeQuorumClient.failsReceived.add(m.sender);
            for (int fails : TreeQuorumClient.deferredInquiries) {
              TreeQuorumClient.clientCall(TreeQuorumClient.serverAddresses.get(fails),"RELINQUISH", fails, TreeQuorumClient.our_sequence_number, TreeQuorumClient.csCount);
            }
          } else if (m.messageType.equals("INQUIRE") && m.csCount == TreeQuorumClient.csCount) {
            if (TreeQuorumClient.failsReceived.size() > 0) {
              TreeQuorumClient.clientCall(TreeQuorumClient.serverAddresses.get(m.sender),"RELINQUISH", m.sender, TreeQuorumClient.our_sequence_number, m.csCount);
              TreeQuorumClient.locksReceived.remove(m.sender);
            } else {
              TreeQuorumClient.deferredInquiries.add(m.sender);
            }
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
}
