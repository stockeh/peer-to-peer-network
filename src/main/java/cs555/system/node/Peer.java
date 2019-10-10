package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.PeerMetadata;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.ConnectionUtilities;
import cs555.system.util.Constants;
import cs555.system.util.FileUtilities;
import cs555.system.util.IdentifierUtilities;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.DiscoverNodeResponse;
import cs555.system.wireformats.DiscoverPeerRequest;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.GenericMessage;
import cs555.system.wireformats.GenericPeerMessage;
import cs555.system.wireformats.JoinNetwork;
import cs555.system.wireformats.Protocol;

/**
 * An equally shared resource that can perform the same tasks as all
 * other peers.
 * 
 * These tasks include communicating with the network and storing data
 * from a client Store application.
 *
 * @author stock
 *
 */
public class Peer implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String LIST_FILES = "list-files";

  private static final String DISPLAY_DHT = "display-dht";

  private static final String DISPLAY_LEAFSET = "display-leafset";

  private static final String VERIFY_LEAFSET = "verify-leafset";

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
    this.executorService = Executors.newCachedThreadPool();
    this.connections = new ConnectionUtilities( executorService );
  }

  /**
   * Start listening for incoming connections and establish connection
   * into the peer network.
   *
   * @param args
   */
  public static void main(String[] args) {
    try ( ServerSocket serverSocket = new ServerSocket( 0 ) )
    {
      Peer node = new Peer( InetAddress.getLocalHost().getHostName(),
          serverSocket.getLocalPort() );

      LOG.info( "Peer node starting up at: " + new Date() + ", on "
          + node.metadata.self().getConnection() );

      ( new Thread(
          new TCPServerThread( node, serverSocket, node.executorService ),
          "Server Thread" ) ).start();

      node.discoverConnection( args, null );
      node.interact();
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to successfully start peer. Exiting. " + e.toString() );
      System.exit( 1 );
    }
  }

  /**
   * Create an identifier for the peer, and connect with Discovery.
   * 
   * @param args (optional) from the command line
   * @param connection to Discovery, can be null to establish a new
   *        connection.
   * @throws IOException if unable to connect to Discovery, and thus,
   *         the network
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
    GenericPeerMessage request = new GenericPeerMessage(
        Protocol.REGISTER_REQUEST, metadata.self(), false );
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
        case LIST_FILES :
          LOG.info( metadata.filesToString() );
          break;

        case DISPLAY_DHT :
          metadata.table().display();
          break;

        case DISPLAY_LEAFSET :
          LOG.info( metadata.leaf().toString() );
          break;

        case VERIFY_LEAFSET :
          verifyApplicationLeafSet( null, null );
          break;

        case EXIT :
          exit();
          running = false;
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
    LOG.info(
        metadata.self().toString() + " has unregistered and is terminating." );
    System.exit( 0 );
  }

  private synchronized void exit() {
    try
    {
      // 1. Exit from Discovery
      ConnectionUtilities
          .establishConnection( this, Properties.DISCOVERY_HOST,
              Properties.DISCOVERY_PORT )
          .getTCPSender()
          .sendData( ( new GenericPeerMessage( Protocol.UNREGISTER_REQUEST,
              metadata.self() ).getBytes() ) );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send required requests to Discovery for exiting"
          + " the network gracefully. " + e.toString() );
      e.printStackTrace();
    }
    if ( metadata.leaf().isPopulated() )
    {
      // 2. Migrate data to leaf set
      PeerInformation cw = metadata.leaf().getCW();
      metadata.leaf().setSelf( cw );
      metadata.files().entrySet().removeIf( entry -> FileUtilities
          .migrateData( this, metadata, executorService, entry, cw ) );

      PeerInformation ccw = metadata.leaf().getCCW();
      metadata.leaf().setSelf( ccw );
      metadata.files().entrySet().removeIf( entry -> FileUtilities
          .migrateData( this, metadata, executorService, entry, ccw ) );
      try
      {
        // 3. Update leaf sets
        if ( cw.equals( ccw ) )
        {
          ConnectionUtilities
              .establishConnection( this, cw.getHost(), cw.getPort() )
              .getTCPSender().sendData(
                  ( new GenericMessage( Protocol.RESET_PEER ) ).getBytes() );
        } else
        {
          ConnectionUtilities
              .establishConnection( this, cw.getHost(), cw.getPort() )
              .getTCPSender().sendData(
                  ( new GenericPeerMessage( Protocol.FORWARD_LEAF_IDENTIFIER,
                      ccw, Constants.COUNTER_CLOCKWISE ) ).getBytes() );
          ConnectionUtilities
              .establishConnection( this, ccw.getHost(), ccw.getPort() )
              .getTCPSender().sendData(
                  ( new GenericPeerMessage( Protocol.FORWARD_LEAF_IDENTIFIER,
                      cw, Constants.CLOCKWISE ) ).getBytes() );
        }
        // 4. Update routing tables... or have other peers catch exception
        // when finding closest peers
      } catch ( IOException e )
      {
        LOG.error( "Unable to send required requests to other peers to exit"
            + "  the network gracefully. " + e.toString() );
        e.printStackTrace();
      }
    } else
    {
      LOG.error( "There are no peers in the network. All files are lost." );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEvent(Event event, TCPConnection connection) {
    LOG.debug( event.toString() );
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
          LOG.info( "Unable to discover a new connection. " + e.toString() );
          e.printStackTrace();
        }
        break;

      case Protocol.JOIN_NETWORK_REQUEST :
        join( event, connection );
        break;

      case Protocol.FORWARD_PEER_IDENTIFIER :
        updateRoutingTable( event );
        break;

      case Protocol.FORWARD_LEAF_IDENTIFIER :
        updateLeafSet( event );
        break;

      case Protocol.STORE_DATA_RESPONSE :
        migrationResponse( event, connection );
        break;

      case Protocol.DISCOVER_PEER_REQUEST :
        lookup( event, connection );
        break;

      case Protocol.STORE_DATA_REQUEST :
        FileUtilities.write( metadata, event, connection );
        break;

      case Protocol.READ_DATA_REQUEST :
        FileUtilities.read( metadata, event, connection );
        break;

      case Protocol.VERIFY_APPLICAITON_LEAVES :
        connection.close();
        verifyApplicationLeafSet( ( DiscoverPeerRequest ) event, connection );
        break;

      case Protocol.RESET_PEER :
        reset( connection );
        break;
    }
  }

  /**
   * Reset the peer since it the last and only in the network
   * 
   * @param connection
   */
  private synchronized void reset(TCPConnection connection) {
    connection.close();
    metadata.table().reset();
    metadata.addSelfToTable();
    metadata.leaf().reset();
    LOG.info( "Initial Routing Table: " );
    metadata.table().display();
    LOG.info( metadata.leaf().toString() );
  }

  /**
   * Process the response message from the peer regarding the status of
   * the migrations operation.
   * 
   * @param event
   * @param connection
   */
  private void migrationResponse(Event event, TCPConnection connection) {
    connection.close();
    GenericPeerMessage response = ( GenericPeerMessage ) event;

    StringBuilder sb =
        ( new StringBuilder() ).append( "The file migration to peer ( " )
            .append( response.getPeer().toString() ).append( ") was " );
    if ( response.getFlag() == Constants.FAILURE )
    {
      sb.append( "NOT " );
    }
    sb.append( "successful!" );
    LOG.info( sb.toString() );
  }

  /**
   * Send requests in the clockwise and counter-clockwise direction and
   * print the trace to verify correctness of the leaf set.
   * 
   * @param connection
   * @param request
   * 
   */
  private void verifyApplicationLeafSet(DiscoverPeerRequest request,
      TCPConnection connection) {
    if ( metadata.leaf().isPopulated() )
    {
      try
      {
        if ( request == null )
        {
          DiscoverPeerRequest data = new DiscoverPeerRequest(
              Protocol.VERIFY_APPLICAITON_LEAVES, metadata.self() );
          data.addNetworkTraceRoute( metadata.self().getIdentifier() );
          ConnectionUtilities
              .establishConnection( this, metadata.leaf().getCW().getHost(),
                  metadata.leaf().getCW().getPort() )
              .getTCPSender().sendData( ( data.getBytes() ) );
        } else if ( !request.getDestination().equals( metadata.self() ) )
        {
          request.addNetworkTraceRoute( metadata.self().getIdentifier() );
          ConnectionUtilities
              .establishConnection( this, metadata.leaf().getCW().getHost(),
                  metadata.leaf().getCW().getPort() )
              .getTCPSender().sendData( request.getBytes() );
        } else
        {
          request.addNetworkTraceRoute( metadata.self().getIdentifier() );
          StringBuilder sb =
              new StringBuilder( "Clockwise Network Route Trace ( " )
                  .append( request.getNetworkTraceIdentifiers().size() )
                  .append( " ): " );
          for ( String s : request.getNetworkTraceIdentifiers() )
          {
            sb.append( " -> " ).append( s );
          }
          LOG.info( sb.toString() );
        }
      } catch ( IOException e )
      {
        LOG.error( "Unable to send message to peer. " + e.toString() );
        e.printStackTrace();
      }
    } else
    {
      LOG.info(
          "Leaf set has not been established for peer yet. Please add another"
              + " peer to the network." );
    }
  }

  /**
   * Lookup the peer that is numerically closest to the identifier in
   * the request.
   * 
   * <p>
   * This operation is done by checking if the request identifier falls
   * within the leaf set, otherwise the lookup is done within the DHT.
   * </p>
   * 
   * @param event
   * @param connection
   */
  private void lookup(Event event, TCPConnection connection) {
    connection.close();
    DiscoverPeerRequest request = ( DiscoverPeerRequest ) event;
    request.addNetworkTraceRoute( metadata.self().getIdentifier() );
    String next = "";
    PeerInformation closest = metadata.leaf()
        .getClosestLeaf( request.getDestination().getIdentifier() );
    try
    {
      // 1. check within bounds of leafset
      if ( closest != null )
      {
        if ( closest.equals( metadata.self() ) )
        {
          connection = ConnectionUtilities.establishConnection( this,
              request.getDestination().getHost(),
              request.getDestination().getPort() );
          connection.submitTo( executorService );
          next = request.getDestination().getIdentifier();
        } else
        {
          connection = ConnectionUtilities.establishConnection( this,
              closest.getHost(), closest.getPort() );
          next = closest.getIdentifier();
        }
      } else
      {
        // 2. check in DHT and leaves
        closest =
            IdentifierUtilities.closest( metadata, request.getDestination() );
        connection = ConnectionUtilities.establishConnection( this,
            closest.getHost(), closest.getPort() );
        next = closest.getIdentifier();
      }
      connection.getTCPSender().sendData( request.getBytes() );
      LOG.info( request.toString() + next );
    } catch ( IOException e )
    {
      if ( metadata.removePeerFromTable( closest ) )
      {
        LOG.info( ( new StringBuilder( "The peer ( " )
            .append( closest.toString() )
            .append( " ) was removed from the routing table." ).toString() ) );
        metadata.table().display();
      }
      lookup( event, connection );
      return;
    }
  }

  /**
   * Update the leaf set from a peer who recently joined the network.
   * 
   * @param event
   */
  private synchronized void updateLeafSet(Event event) {
    GenericPeerMessage request = ( GenericPeerMessage ) event;
    metadata.leaf().setLeaf( request.getPeer(), request.getFlag() );
    if ( metadata.leaf().isPopulated() )
    {
      LOG.info( metadata.leaf().toString() );
      synchronized ( metadata.files() )
      {
        metadata.files().entrySet()
            .removeIf( entry -> FileUtilities.migrateData( this, metadata,
                executorService, entry, request.getPeer() ) );
      }
    }
    updateRoutingTable( event );
  }

  /**
   * Updates this DHT with the identifier of the newly joined peer in
   * every row, except for those that follow a shared prefix.
   * 
   * @param event
   */
  private synchronized void updateRoutingTable(Event event) {
    PeerInformation peer = ( ( GenericPeerMessage ) event ).getPeer();
    if ( !metadata.addPeerToTable( peer ) )
    {
      LOG.info( "Updating Routing Table with " + peer.getIdentifier() );
      metadata.table().display();
    }
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
  private synchronized void join(Event event, TCPConnection connection) {
    JoinNetwork request = ( JoinNetwork ) event;
    connection.close();
    if ( metadata.self().equals( request.getDestination() ) )
    {
      initializeDHT( request );
      metadata.initialized();
    } else
    {
      try
      {
        metadata.getLock().lock();
        while ( !metadata.isInitialized() )
        {
          metadata.getCondition().await();
        }
        constructDHT( request );
      } catch ( InterruptedException e )
      {
        e.printStackTrace();
      } finally
      {
        metadata.getLock().unlock();
      }
    }
  }

  /**
   * Construct the DHT for the peer requesting to join the network.
   * 
   * @param request
   */
  private synchronized void constructDHT(JoinNetwork request) {

    int modifiedLcp = IdentifierUtilities.longestCommonPrefixLength(
        metadata.self().getIdentifier(),
        request.getDestination().getIdentifier() ) + 1;
    int row = request.getRow();

    if ( modifiedLcp > row )
    {
      for ( int i = 0; i < modifiedLcp - row; ++i )
      {
        request.incrementRow();
        request.setTableRow( metadata.table().getTableRow( request.getRow() ) );
      }
    }
    request.addNetworkTraceRoute( metadata.self().getIdentifier() );

    String next = "";
    PeerInformation closest = metadata.leaf()
        .getClosestLeaf( request.getDestination().getIdentifier() );
    try
    {
      TCPConnection connection;

      // 1. check within bounds of leafset
      if ( closest != null )
      {
        if ( closest.equals( metadata.self() ) )
        {
          LOG.debug( "Found closest node and responding to destination." ); // B.
          if ( !metadata.leaf().isPopulated() )
          {
            request.setCW( metadata.self() );
            request.setCCW( metadata.self() );
          } else
          {
            if ( metadata.leaf()
                .isBetweenClockwise( request.getDestination() ) )
            {
              request.setCW( metadata.leaf().getCW() );
              request.setCCW( metadata.self() );
            } else
            {
              request.setCW( metadata.self() );
              request.setCCW( metadata.leaf().getCCW() );
            }
          }
          connection = ConnectionUtilities.establishConnection( this,
              request.getDestination().getHost(),
              request.getDestination().getPort() );
          next = request.getDestination().getIdentifier();
        } else
        {
          connection = ConnectionUtilities.establishConnection( this,
              closest.getHost(), closest.getPort() );
          next = closest.getIdentifier();
        }
      } else
      {
        // 2. check in DHT and leaves
        closest =
            IdentifierUtilities.closest( metadata, request.getDestination() );
        connection = ConnectionUtilities.establishConnection( this,
            closest.getHost(), closest.getPort() );
        next = closest.getIdentifier();
      }
      connection.getTCPSender().sendData( request.getBytes() );
      LOG.info( request.toString() + next );
    } catch ( IOException e )
    {
      if ( metadata.removePeerFromTable( closest ) )
      {
        LOG.info( ( new StringBuilder( "The peer ( " )
            .append( closest.toString() )
            .append( " ) was removed from the routing table." ).toString() ) );
        metadata.table().display();
      }
      constructDHT( request );
      return;
    }
    LOG.info( request.toString() + next );
  }

  /**
   * The leaf set is constructed using the row containing the greatest
   * common prefix with the peer joining the network.
   * 
   * A peer will have two leaves, one on either side of it, following
   * the form:
   * 
   * <p>
   * <tt>{ counter-clockwise <- this -> clockwise }</tt>
   * </p>
   * 
   * @param joinRequest
   * 
   */
  private void constructLeafSet(JoinNetwork joinRequest) {
    PeerInformation cw = joinRequest.getCW(), ccw = joinRequest.getCCW();
    try
    {
      GenericPeerMessage request = new GenericPeerMessage(
          Protocol.FORWARD_LEAF_IDENTIFIER, metadata.self() );

      TCPConnection connection = connections.cacheConnection( this, cw, false );
      metadata.leaf().setLeaf( cw, Constants.CLOCKWISE );
      metadata.addPeerToTable( cw );
      request.setFlag( Constants.COUNTER_CLOCKWISE );
      LOG.debug( "Sending Data to: " + cw.toString() );
      connection.getTCPSender().sendData( request.getBytes() );

      connection = connections.cacheConnection( this, ccw, false );
      metadata.leaf().setLeaf( ccw, Constants.COUNTER_CLOCKWISE );
      metadata.addPeerToTable( ccw );
      request.setFlag( Constants.CLOCKWISE );
      LOG.debug( "Sending Data to: " + ccw.toString() );
      connection.getTCPSender().sendData( request.getBytes() );

    } catch ( IOException e )
    {
      LOG.error( "Unable to send leaf set request to peers. " + e.toString() );
      e.printStackTrace();
    }
    LOG.info( metadata.leaf().toString() );
  }

  /**
   * Construct this peers leaf set and routing table.
   * 
   * Forward this nodes content to all nodes identified in the leaf set
   * and the routing table for reference.
   * 
   * @param request
   */
  private synchronized void initializeDHT(JoinNetwork request) {
    LOG.debug( "Initializing Peer" );
    metadata.table().setTable( request.getTable() );

    StringBuilder sb = new StringBuilder( "Network Route Trace:" );
    for ( String s : request.getNetworkTraceIdentifiers() )
    {
      sb.append( " -> " ).append( s );
    }
    LOG.info( sb.toString() );

    byte[] data;
    try
    {
      data = new GenericPeerMessage( Protocol.FORWARD_PEER_IDENTIFIER,
          metadata.self() ).getBytes();
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to send create output stream for message. " + e.toString() );
      e.printStackTrace();
      return;
    }
    Set<PeerInformation> processed = new HashSet<>();
    processed.add( metadata.self() );
    processed.add( request.getCW() );
    processed.add( request.getCCW() );
    Stream.of( metadata.table().getTable() ).flatMap( Stream::of )
        .forEach( peer ->
        {
          if ( peer != null && !processed.contains( peer ) )
          {
            LOG.debug( "Sending Data to: " + peer.toString() );
            try
            {
              TCPConnection connection =
                  connections.cacheConnection( this, peer, false );
              connection.getTCPSender().sendData( data );
            } catch ( NumberFormatException | IOException e )
            {
              metadata.removePeerFromTable( peer );
            }
            processed.add( peer );
          }
        } );
    constructLeafSet( request );
    connections.closeCachedConnections();

    metadata.addSelfToTable();
    LOG.info( "Initial Routing Table: " );
    metadata.table().display();
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
      LOG.info( "Peer ( " + metadata.self().toString()
          + " ) is the first connection in the system." );
      metadata.addSelfToTable();
      LOG.info( "Initial Routing Table: " );
      metadata.table().display();
      metadata.initialized();
    } else
    {
      LOG.info( "Successfully registered peer ( " + metadata.self().toString()
          + " ) with Discovery." );
      PeerInformation source = response.getSourceInformation();
      LOG.info(
          "Connecting to the DHT through source node: " + source.toString() );
      try
      {
        TCPConnection sourceConnection = ConnectionUtilities
            .establishConnection( this, source.getHost(), source.getPort() );
        sourceConnection.getTCPSender()
            .sendData( ( new JoinNetwork( metadata.self() ) ).getBytes() );
      } catch ( IOException e )
      {
        LOG.error( "Unable to send message to source node. " + e.toString() );
        e.printStackTrace();
      }
    }
    connection.close(); // close connection with discovery node
  }

  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {
    StringBuilder sb = new StringBuilder();

    sb.append( "\n\t" ).append( LIST_FILES )
        .append( "\t: list files that have been written to disk.\n" );

    sb.append( "\n\t" ).append( DISPLAY_DHT )
        .append( "\t: display the routing DHT.\n" );

    sb.append( "\n\t" ).append( DISPLAY_LEAFSET )
        .append( "\t: display the leaf nodes as " )
        .append( "{ clockwise, this, counter-clockwise }.\n" );

    sb.append( "\n\t" ).append( VERIFY_LEAFSET )
        .append( "\t: route a request in the clockwise and " )
        .append( "clounter-clockwise directions for correctness.\n" );

    sb.append( "\n\t" ).append( EXIT )
        .append( "\t\t: gracefully leave the network and distribute stored " )
        .append( "files.\n" );

    System.out.println( sb.toString() );
  }

}
