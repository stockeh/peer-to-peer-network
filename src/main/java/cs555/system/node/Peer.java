package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.PeerMetadata;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.ConnectionUtilities;
import cs555.system.util.IdentifierUtilities;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.DiscoverNodeResponse;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.PeerInitializeLocation;
import cs555.system.wireformats.Protocol;
import cs555.system.wireformats.RegisterRequest;

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
   * Initialize the peer with the discovery.
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
   * 
   * @param args
   * @param connection
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
    RegisterRequest request =
        new RegisterRequest( Protocol.REGISTER_REQUEST, metadata.self() );
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
        disvoverNodeHandler( event, connection );
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

      case Protocol.PEER_INITIALIZE_LOCATION :
        peerInitializeHandler( event, connection );
        break;
    }
  }

  /**
   * 
   * @param event
   * @param connection
   */
  private void peerInitializeHandler(Event event, TCPConnection connection) {
    PeerInitializeLocation request = ( PeerInitializeLocation ) event;

    if ( metadata.self().equals( request.getDestination() ) )
    {
      connection.close();
      initializeDHT( request );
      return;
    } else
    {
      boolean isFirstPeer = request.getRowIndex() == 0;
      try
      {
        traversePrefix( request, isFirstPeer, connection );
      } catch ( IOException e )
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * 
   * @param request
   * @param isFirstPeer
   * @param connection
   * @throws IOException
   */
  private void traversePrefix(PeerInitializeLocation request,
      boolean isFirstPeer, TCPConnection connection) throws IOException {

    int row = request.getRowIndex();
    request.setTableRow( metadata.table().getTableRow( row ) );

    int selfCol =
        Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
    int destCol = Character
        .digit( request.getDestination().getIdentifier().charAt( row ), 16 );

    request.addNetworkTraceRoute( selfCol );

    if ( selfCol == destCol )
    {
      traversePrefix( request, isFirstPeer, connection );
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
      { // assumes leaf set size is 2
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
        if ( isFirstPeer )
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
   * 
   * @param request
   */
  private void initializeDHT(PeerInitializeLocation request) {
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
  }


  /**
   * Connect with Discovery to find a source node for entering the
   * network.
   * 
   * @param event
   * @param connection
   */
  private void disvoverNodeHandler(Event event, TCPConnection connection) {
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
      PeerInitializeLocation request =
          new PeerInitializeLocation( metadata.self() );
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
