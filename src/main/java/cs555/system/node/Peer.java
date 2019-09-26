package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.PeerMetadata;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.ConnectionUtilities;
import cs555.system.util.Constants;
import cs555.system.util.IdentifierUtilities;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.DiscoverNodeResponse;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.GenericPeerMessage;
import cs555.system.wireformats.JoinNetwork;
import cs555.system.wireformats.Protocol;

/**
 *
 * @author stock
 *
 */
public class Peer implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String EXIT = "exit";

  private static final String HELP = "help";

  private final ConnectionUtilities connections;

  private final PeerMetadata metadata;

  private ExecutorService executorService;


  /**
   * Default constructor - creates a new peer tying the <b>host:port</b>
   * combination for the node as the identifier for itself.
   * 
   * @param host
   * @param port
   */
  private Peer(String host, int port) {
    this.metadata = new PeerMetadata( host, port );
    this.executorService = Executors.newFixedThreadPool( 2 );
    this.connections = new ConnectionUtilities( executorService );
  }

  /**
   * Start listening for incoming connections and establish connection
   * into the peer network.
   *
   * @param args
   */
  public static void main(String[] args) {
    LOG.info( "peer node starting up at: " + new Date() );
    try ( ServerSocket serverSocket = new ServerSocket( 0 ) )
    {
      Peer node = new Peer( InetAddress.getLocalHost().getHostName(),
          serverSocket.getLocalPort() );

      ( new Thread(
          new TCPServerThread( node, serverSocket, node.executorService ),
          "Server Thread" ) ).start();

      node.discoverConnection( args, null );
      node.interact();
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to successfully start peer. Exiting. " + e.getMessage() );
      System.exit( 1 );
    }
  }

  /**
   * Create an identifier for the peer, and connect with Discovery.
   * 
   * @param args (Optional) from the command line
   * @param connection to Discovery
   * @throws IOException
   */
  private void discoverConnection(String[] args, TCPConnection connection)
      throws IOException {
    if ( connection == null )
    {
      connection = ConnectionUtilities.establishConnection( this,
          Properties.DISCOVERY_HOST, Properties.DISCOVERY_PORT );
      connection.submitTo( executorService );
    }
    if ( args.length > 0 )
    {
      try
      {
        metadata.setIdentifier( String.format( "%04X",
            ( 0xFFFF & Integer.parseInt( args[ 0 ], 16 ) ) ) );
      } catch ( NumberFormatException e )
      {
        metadata.setIdentifier( IdentifierUtilities.timestampToIdentifier() );
      }
    } else
    {
      metadata.setIdentifier( IdentifierUtilities.timestampToIdentifier() );
    }
    GenericPeerMessage request =
        new GenericPeerMessage( Protocol.REGISTER_REQUEST, metadata.self() );
    connection.getTCPSender().sendData( request.getBytes() );
  }

  /**
   * Allow support for commands to be specified while the processes are
   * running.
   * 
   */
  private void interact() {
    System.out.println(
        "\nInput a command to interact with processes. Input 'help' for a "
            + "list of commands.\n" );
    boolean running = true;
    while ( running )
    {
      @SuppressWarnings( "resource" )
      Scanner scan = new Scanner( System.in );
      String[] input = scan.nextLine().toLowerCase().split( "\\s+" );
      switch ( input[ 0 ] )
      {
        case EXIT :
          break;

        case HELP :
          displayHelp();
          break;

        default :
          LOG.error(
              "Unable to process. Please enter a valid command! Input 'help'"
                  + " for options." );
          break;
      }
    }
    LOG.info( metadata.self().getHost() + ":" + metadata.self().getPort()
        + " has unregistered and is terminating." );
    System.exit( 0 );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEvent(Event event, TCPConnection connection) {
    // LOG.debug( event.toString() );
    switch ( event.getType() )
    {
      case Protocol.DISCOVER_NODE_RESPONSE :
        dicoverNodeHandler( event, connection );
        break;

      case Protocol.IDENTIFIER_COLLISION :
        try
        {
          discoverConnection( new String[ 0 ], connection );
        } catch ( IOException e )
        {
          LOG.info( "Unable to discover a new connection. " + e.getMessage() );
          e.printStackTrace();
        }
        break;

      case Protocol.JOIN_NETWORK_REQUEST :
        join( event, connection );
        break;

      case Protocol.FORWARD_IDENTIFIER :
        updateRoutingTable( event );
        LOG.debug( "Add the identifier: " + event.toString() );
        break;

    }
  }

  /**
   * Finds the longest prefix with this and the joining peer and adds
   * that peer to the <tt>p + 1</tt> location in this routing table.
   * 
   * @param event
   */
  private void updateRoutingTable(Event event) {
    PeerInformation peer = ( ( GenericPeerMessage ) event ).getPeer();

    int row = 0;
    for ( ; row < Constants.NUMBER_OF_ROWS; ++row )
    {
      int selfCol =
          Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
      int destCol = Character.digit( peer.getIdentifier().charAt( row ), 16 );

      if ( selfCol - destCol != 0 )
      {
        break;
      }
    }
    metadata.table().addPeerToTable( peer, row );
    metadata.table().display();
  }

  /**
   * Once an entry point to the network has been discovered, the peer
   * should join the network and construct its own DHT and leaf set.
   * 
   * A new peer will join the network at the location nearest to other
   * peer identifiers.
   * 
   * @param event
   * @param connection to original request
   */
  private void join(Event event, TCPConnection connection) {
    JoinNetwork request = ( JoinNetwork ) event;

    if ( metadata.self().equals( request.getDestination() ) )
    {
      initializeDHT( request );
      return;
    } else
    {
      boolean isSourcePeer = request.getRowIndex() == 0;
      try
      {
        constructDHT( request, isSourcePeer, connection );
      } catch ( IOException e )
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Construct the DHT for the peer requesting to join the network.
   * 
   * @param request
   * @param isSourcePeer
   * @param connection
   * @throws IOException
   */
  private void constructDHT(JoinNetwork request, boolean isSourcePeer,
      TCPConnection connection) throws IOException {

    int row = request.getRowIndex();
    request.setTableRow( metadata.table().getTableRow( row ) );

    int selfCol =
        Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
    int destCol = Character
        .digit( request.getDestination().getIdentifier().charAt( row ), 16 );

    request.addNetworkTraceRoute( selfCol );

    if ( selfCol == destCol )
    {
      constructDHT( request, isSourcePeer, connection );
      return;
    } else
    {
      PeerInformation peer = metadata.table().getTableIndex( row, destCol );

      if ( peer != null )
      {
        TCPConnection n = ConnectionUtilities.establishConnection( this,
            peer.getHost(), peer.getPort() );
        n.getTCPSender().sendData( request.getBytes() );
      } else
      {
        constructLeafSet( request, row, destCol );
        if ( isSourcePeer )
        {
          connection.getTCPSender().sendData( request.getBytes() );
        } else
        {
          TCPConnection destination = ConnectionUtilities.establishConnection(
              this, request.getDestination().getHost(),
              request.getDestination().getPort() );
          destination.getTCPSender().sendData( request.getBytes() );
        }
      }
    }
  }

  /**
   * The leaf set is constructed using the row containing the greatest
   * common prefix with the peer joining the network.
   * 
   * A peer will have two leaves, one on either side of it.
   * 
   * <p>
   * <tt>{ L, this, R }</tt>
   * </p>
   * 
   * @param request
   * @param row
   * @param destCol
   */
  private void constructLeafSet(JoinNetwork request, int row, int destCol) {
    for ( int i = 1; i < 16; ++i )
    {
      if ( request.getLeafSetByIndex( 1 ) == null )
      {
        PeerInformation right = metadata.table().getTableIndex( row,
            Math.floorMod( destCol + 1, 16 ) );
        if ( right != null )
        {
          request.setLeafSetIndex( right, 1 );
        }
      }
      if ( request.getLeafSetByIndex( 0 ) == null )
      {
        PeerInformation left = metadata.table().getTableIndex( row,
            Math.floorMod( destCol - 1, 16 ) );
        if ( left != null )
        {
          request.setLeafSetIndex( left, 1 );
        }
      }
    }
  }

  /**
   * Construct this peers leaf set and routing table.
   * 
   * Forward this nodes content to all nodes identified in the leaf set
   * and the routing table for reference.
   * 
   * @param request
   */
  private void initializeDHT(JoinNetwork request) {
    LOG.debug( "Initializing Peer" );
    metadata.table().setTable( request.getTable() );
    metadata.addSelfToTable();
    metadata.table().display();

    String trace = "";
    StringBuilder sb = new StringBuilder( "Network Join Trace: " );
    List<Short> networkTraceIndex = request.getNetworkTraceIndex();
    for ( int i = 0; i < networkTraceIndex.size(); ++i )
    {
      String s =
          request.getTable()[ i ][ networkTraceIndex.get( i ) ].getIdentifier();
      if ( !trace.equals( s ) )
      {
        sb.append( "-> " ).append( s );
        trace = s;
      }
    }
    LOG.info( sb.toString() );
    byte[] data;
    try
    {
      data =
          new GenericPeerMessage( Protocol.FORWARD_IDENTIFIER, metadata.self() )
              .getBytes();
    } catch ( IOException e )
    {
      LOG.error( "Unable to send create output stream for message. "
          + e.getMessage() );
      e.printStackTrace();
      return;
    }
    Stream.of( metadata.table().getTable() ).flatMap( Stream::of )
        .forEach( peer ->
        {
          if ( peer != null )
          {
            try
            {

              TCPConnection connection =
                  connections.cacheConnection( this, peer, false );
              connection.getTCPSender().sendData( data );
            } catch ( NumberFormatException | IOException e )
            {
              LOG.error(
                  "Unable to send message to source node. " + e.getMessage() );
              e.printStackTrace();
            }
          }
        } );
    connections.closeCachedConnections();
    // TODO: establish leaf-set
  }

  /**
   * Connect with Discovery to find a source node for entering the
   * network.
   * 
   * @param event
   * @param connection
   */
  private void dicoverNodeHandler(Event event, TCPConnection connection) {
    DiscoverNodeResponse response = ( DiscoverNodeResponse ) event;
    if ( response.isInitialPeerConnection() )
    {
      LOG.info( "Peer is the first connection in the system." );
      metadata.addSelfToTable();
      metadata.table().display();
    } else
    {
      LOG.info( "Successfully registered peer ( " + metadata.self().toString()
          + " ) with Discovery." );
      PeerInformation source = response.getSourceInformation();
      LOG.info(
          "Connecting to the DHT through source node: " + source.toString() );
      JoinNetwork request = new JoinNetwork( metadata.self() );
      try
      {
        TCPConnection sourceConnection =
            connections.cacheConnection( this, source, true );
        sourceConnection.getTCPSender().sendData( request.getBytes() );
      } catch ( IOException e )
      {
        LOG.error( "Unable to send message to source node. " + e.getMessage() );
        e.printStackTrace();
      }
    }
    connection.close();
  }

  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {}

}
