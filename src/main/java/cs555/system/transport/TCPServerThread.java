package cs555.system.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import cs555.system.node.Node;
import cs555.system.util.Logger;

/**
 * A new TCP Server Thread is setup on the discovery and each new
 * chunk server to accept new connections.
 * 
 * Upon a new connection being made a TCP Connection is established on
 * to send and receive messages as a response. The thread is blocked
 * on the accept statement until these new connections are
 * established.
 * 
 * @author stock
 *
 */
public class TCPServerThread implements Runnable {

  private static final Logger LOG = Logger.getInstance();

  private Node node;

  private ServerSocket serverSocket;

  private ExecutorService executorService;

  /**
   * Default constructor - setup the server socket for the thread to run
   * on.
   * 
   * @param node
   * @param serverSocket
   * @param executorService
   */
  public TCPServerThread(Node node, ServerSocket serverSocket,
      ExecutorService executorService) {
    this.node = node;
    this.serverSocket = serverSocket;
    this.executorService = executorService;
  }

  /**
   * Listen for incoming connections and start a new thread for each
   * socket once connected.
   * 
   * {@inheritDoc}
   */
  @Override
  public void run() {
    while ( serverSocket != null )
    {
      try
      {
        Socket incomingConnectionSocket = serverSocket.accept();
        ( new TCPConnection( node, incomingConnectionSocket ) )
            .submitTo( executorService );
      } catch ( IOException e )
      {
        LOG.debug( "Closing Server Socket Connection... " + e.toString() );
        break;
      }
    }
  }
}
