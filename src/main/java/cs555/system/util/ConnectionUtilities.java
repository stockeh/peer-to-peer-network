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
  public TCPConnection cacheConnection(Node node, String[] initialConnection,
      boolean startConnection) throws NumberFormatException, IOException {
    String connectionDetails =
        connectionStringBuilder.append( initialConnection[ 0 ] ).append( ":" )
            .append( initialConnection[ 1 ] ).toString();
    connectionStringBuilder.setLength( 0 );

    TCPConnection connection;
    if ( temporaryConnections.containsKey( connectionDetails ) )
    {
      connection = temporaryConnections.get( connectionDetails );
    } else
    {
      connection = ConnectionUtilities.establishConnection( node,
          initialConnection[ 0 ], Integer.parseInt( initialConnection[ 1 ] ) );
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

  /**
   * Registers a node with the discovery.
   *
   * @param node requesting to connect
   * @param identifier distinguishes the type of node
   * @param discoveryHost identifier for the discovery node
   * @param discoveryPort number for the discovery node
   * 
   * @return a TCPConnection to the discovery
   * @throws IOException
   */
  public static TCPConnection registerNode(Node node, int identifier,
      String discoveryHost, Integer discoveryPort) throws IOException {
    try
    {
      TCPConnection connection =
          establishConnection( node, discoveryHost, discoveryPort );

      RegisterRequest registerRequest =
          new RegisterRequest( Protocol.REGISTER_REQUEST, identifier,
              node.getHost(), node.getPort() );

      LOG.info( "peer Identifier: " + node.getHost() + ":" + node.getPort() );
      connection.getTCPSender().sendData( registerRequest.getBytes() );
      connection.start();

      return connection;
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to connect to the discovery. Check that it is running, and"
              + " the connection details are correct. " + e.getMessage() );
      throw e;
    }
  }

  /**
   * Unregister a node with the discovery.
   * 
   * @param node
   * @param identifier
   * @param discoveryConnection
   */
  public static void unregisterNode(Node node, int identifier,
      TCPConnection discoveryConnection) {
    RegisterRequest registerRequest =
        new RegisterRequest( Protocol.UNREGISTER_REQUEST, identifier,
            node.getHost(), node.getPort() );

    try
    {
      discoveryConnection.getTCPSender().sendData( registerRequest.getBytes() );
      discoveryConnection.close();
    } catch ( IOException | InterruptedException e )
    {
      LOG.error( e.getMessage() );
      e.printStackTrace();
    }
  }

}
