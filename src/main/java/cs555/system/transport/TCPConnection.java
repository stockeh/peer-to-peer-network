package cs555.system.transport;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import cs555.system.node.Node;
import cs555.system.util.Logger;

/**
 * This class is used to establish a connection by starting a new
 * TCPReceiverThread and associating the TCPSender with the socket.
 * 
 * @author stock
 *
 */
public class TCPConnection {

  private static final Logger LOG = Logger.getInstance();

  private Socket socket;

  private TCPSender sender;

  private TCPReceiverThread receiver;

  /**
   * Default constructor - create a new TCPConnection given a Node,
   * i.e., chunk server or discovery, and the socket for the connection.
   * 
   * @param node
   * @param socket
   * @throws IOException
   */
  public TCPConnection(Node node, Socket socket) throws IOException {
    this.socket = socket;
    this.sender = new TCPSender( this.socket );
    this.receiver = new TCPReceiverThread( node, this.socket, this );
  }

  /**
   * Get the Socket for the connection to verify INet information
   * 
   * @return the socket for the connection.
   */
  public Socket getSocket() {
    return this.socket;
  }

  /**
   * Get the TCPSender so the peer or server can send a message over the
   * socket
   * 
   * @return the TCPSender instance that was instantiated during the
   *         {@link #run()} method of the new thread.
   */
  public TCPSender getTCPSender() {
    return this.sender;
  }

  /**
   * Allow the TCPConnection to start receiving messages.
   * 
   * @param executorService to submit the receiver to
   */
  public void submitTo(ExecutorService executorService) {
    executorService.submit( this.receiver );
    // new Thread( this.receiver ).start();
  }

  /**
   * Close the socket sender and receiver.
   * 
   */
  public void close() {
    try
    {
      this.sender.dout.close();
      this.receiver.din.close();
      this.socket.close();
    } catch ( IOException e )
    {
      LOG.error( "Unable to close the connection with node. " + e.toString() );
      e.printStackTrace();
    }
  }
}
