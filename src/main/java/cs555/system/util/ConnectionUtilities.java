package cs555.system.util;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import cs555.system.metadata.PeerInformation;
import cs555.system.node.Node;
import cs555.system.transport.TCPConnection;

/**
 * Shared connection utilities between the discovery, peer, and store.
 * 
 * @author stock
 *
 */
public class ConnectionUtilities {

  private static final Logger LOG = Logger.getInstance();

  private final Map<String, TCPConnection> temporaryConnections;

  private ExecutorService executorService;

  /**
   * Default constructor -
   * 
   * @param executorService
   * 
   */
  public ConnectionUtilities(ExecutorService executorService) {
    this.temporaryConnections = new HashMap<>();
    this.executorService = executorService;
  }

  /**
   * Either establish a new or retrieve a cached connection made
   * previously.
   * 
   * @param node corresponding to the connection
   * @param peer to connect to
   * @param startConnection true to start the TCP Receiver Thread, false
   *        otherwise
   * 
   * @return the cached TCP connection
   * @throws IOException
   * @throws NumberFormatException
   */
  public TCPConnection cacheConnection(Node node, PeerInformation peer,
      boolean startConnection) throws NumberFormatException, IOException {

    TCPConnection connection;
    if ( temporaryConnections.containsKey( peer.getIdentifier() ) )
    {
      connection = temporaryConnections.get( peer.getIdentifier() );
    } else
    {
      connection = ConnectionUtilities.establishConnection( node,
          peer.getHost(), peer.getPort() );
      temporaryConnections.put( peer.getIdentifier(), connection );
      if ( startConnection )
      {
        connection.submitTo( executorService );
      }
    }
    return connection;
  }

  /**
   * Close and remove all temporary connections.
   * 
   */
  public void closeCachedConnections() {
    temporaryConnections.forEach( (k, v) ->
    {
      try
      {
        v.close();
      } catch ( IOException | InterruptedException e )
      {
        LOG.error( "Unable to close the connection for " + k );
      }
    } );
    temporaryConnections.clear();
  }

  /**
   * Establish generic connection with a given node.
   * 
   * @param node used to discovery receiving thread
   * @param host name associated with outgoing node
   * @param port number associated with outgoing node
   * @return connection to server
   * @throws IOException
   */
  public static TCPConnection establishConnection(Node node, String host,
      Integer port) throws IOException {
    Socket socketToTheServer = new Socket( host, port );
    return new TCPConnection( node, socketToTheServer );
  }

}
