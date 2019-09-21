package cs555.system.util;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import cs555.system.node.Node;
import cs555.system.transport.TCPConnection;
import cs555.system.wireformats.Protocol;
import cs555.system.wireformats.RegisterRequest;

/**
 * Shared connection utilities between the discovery and peer / chunk
 * servers.
 * 
 * @author stock
 *
 */
public class ConnectionUtilities {

  private static final Logger LOG = Logger.getInstance();

  private final Map<String, TCPConnection> temporaryConnections;

  private final StringBuilder connectionStringBuilder;

  /**
   * Default constructor -
   * 
   */
  public ConnectionUtilities() {
    this.temporaryConnections = new HashMap<>();
    this.connectionStringBuilder = new StringBuilder();
  }

  /**
   * Either establish a new or retrieve a cached connection made
   * previously.
   * 
   * @param startConnection true to start the TCP Receiver Thread, false
   *        otherwise
   * @param connectionDetails to connect to
   * @return the cached TCP connection
   * @throws IOException
   * @throws NumberFormatException
   */
  public TCPConnection cacheConnection(Node node, String[] address,
      boolean startConnection) throws NumberFormatException, IOException {
    String connectionDetails = connectionStringBuilder.append( address[ 0 ] )
        .append( ":" ).append( address[ 1 ] ).toString();
    connectionStringBuilder.setLength( 0 );

    TCPConnection connection;
    if ( temporaryConnections.containsKey( connectionDetails ) )
    {
      connection = temporaryConnections.get( connectionDetails );
    } else
    {
      connection = ConnectionUtilities.establishConnection( node, address[ 0 ],
          Integer.parseInt( address[ 1 ] ) );
      temporaryConnections.put( connectionDetails, connection );
      if ( startConnection )
      {
        connection.start();
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
