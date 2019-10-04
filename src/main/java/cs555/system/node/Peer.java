package cs555.system.node;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
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
import cs555.system.wireformats.DataTransfer;
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

  private static final String DISPLAY_LEAF = "display-leaf";

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
    LOG.info( "Peer node starting up at: " + new Date() );
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
          LOG.info( metadata.leaf().toString() );
          break;

        case DISPLAY_LEAF :
          metadata.table().display();
          break;

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
          LOG.info( "Unable to discover a new connection. " + e.getMessage() );
          e.printStackTrace();
        }
        break;

      case Protocol.JOIN_NETWORK_REQUEST :
        // LOG.info( event.toString() );
        join( event, connection );
        break;

      case Protocol.FORWARD_PEER_IDENTIFIER :
        updateRoutingTable( event );
        break;

      case Protocol.FORWARD_LEAF_IDENTIFIER :
        updateLeafSet( event, connection );
        break;

      case Protocol.DISCOVER_PEER_REQUEST :
        // LOG.info( event.toString() );
        lookup( event, connection );
        break;

      case Protocol.STORE_DATA_REQUEST :
        write( event, connection );
        break;

      case Protocol.READ_DATA_REQUEST :
        read( event, connection );
    }
  }

  /**
   * Read a file on the request peer if it exists.
   * 
   * <p>
   * Data is stored by the specified {@code fileSystemPath} and a unique
   * host connection string.
   * </p>
   * 
   * @param event
   * @param connection from the Store that will be used for response
   */
  private void read(Event event, TCPConnection connection) {
    GenericMessage request = ( GenericMessage ) event;

    String fileSystemPath =
        request.getMessage() + "-" + metadata.self().getConnection();
    Path path =
        Paths.get( File.separator, "tmp", "stock", "pastry", fileSystemPath );
    byte[] data = null;
    try
    {
      data = Files.readAllBytes( path );
    } catch ( IOException e )
    {
      LOG.error( "Unable to read " + fileSystemPath + " from disk. "
          + e.getMessage() );
      e.printStackTrace();
    }
    try
    {
      connection.getTCPSender()
          .sendData( ( new DataTransfer( Protocol.READ_DATA_RESPONSE, data,
              metadata.self().toString() ) ).getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message to store. " + e.getMessage() );
      e.printStackTrace();
    }
  }

  /**
   * Process an incoming file by saving it to disk, and responding to
   * the Store with the status of the write operation.
   * 
   * <p>
   * Data is stored by the specified {@code fileSystemPath} and a unique
   * host connection string.
   * </p>
   * 
   * @param event
   * @param connection from the Store that will be used for response
   */
  private void write(Event event, TCPConnection connection) {
    DataTransfer request = ( DataTransfer ) event;
    metadata.addFile( request.getDescriptor() );
    String fileSystemPath =
        request.getDescriptor() + "-" + metadata.self().getConnection();
    Path path =
        Paths.get( File.separator, "tmp", "stock", "pastry", fileSystemPath );
    boolean success = Constants.SUCCESS;
    try
    {
      Files.createDirectories( path.getParent() );
      Files.write( path, request.getData() );
      LOG.info( "Finished writing " + fileSystemPath + " to disk." );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to save " + fileSystemPath + " to disk. " + e.getMessage() );
      e.printStackTrace();
      success = Constants.FAILURE;
    }
    try
    {
      connection.getTCPSender()
          .sendData( ( new GenericPeerMessage( Protocol.STORE_DATA_RESPONSE,
              metadata.self(), success ) ).getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message to store. " + e.getMessage() );
      e.printStackTrace();
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

    PeerInformation closest =
        metadata.leaf().getClosestLeaf( request.getDestination() );
    try
    {
      if ( closest != null )
      {
        if ( closest.equals( metadata.self() ) )
        {
          connection = ConnectionUtilities.establishConnection( this,
              request.getDestination().getHost(),
              request.getDestination().getPort() );
          connection.submitTo( executorService );
        } else
        {
          connection = ConnectionUtilities.establishConnection( this,
              closest.getHost(), closest.getPort() );
        }
      } else
      {
        closest = lookupDHT( request );
        connection = ConnectionUtilities.establishConnection( this,
            closest.getHost(), closest.getPort() );
      }
      connection.getTCPSender().sendData( request.getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message. " + e.getMessage() );
      e.printStackTrace();
    }
  }

  /**
   * Consult the DHT to find the closest peer to request identifier.
   * 
   * <p>
   * <ol>
   * <li>Check if the index of {@code self} and {@code request}
   * identifiers are the same, and keep going further down the rows
   * until they are not the same.</li>
   * <li>The peer at the {@code destination} column / row is not
   * {@code null}; indicating the {@code self} DHT contains an entry
   * matching the prefix of the {@code request} identifier.</li>
   * <li>The {@code self} DHT does not contain any matching prefixes for
   * a given row; forcing the a search around the rings for all
   * remaining lower rows.</li>
   * </ol>
   * </p>
   * 
   * @param request
   * @return the {@code PeerIdentifier} that is deemed closest
   */
  private PeerInformation lookupDHT(DiscoverPeerRequest request) {
    int row = request.getRow();

    int selfCol =
        Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
    int destCol = Character
        .digit( request.getDestination().getIdentifier().charAt( row ), 16 );
    // 1.
    if ( selfCol == destCol )
    {
      request.incrementRow();
      return lookupDHT( request );
    } else
    {
      PeerInformation peer = metadata.table().getTableIndex( row, destCol );
      // 2.
      if ( peer != null )
      {
        return peer;
      } else
      { // 3.
        return metadata.table().closest( metadata.self(),
            request.getDestination(), row );
      }
    }
  }

  /**
   * Update the leaf set from a peer who recently joined the network.
   * 
   * @param event
   * @param connection
   */
  private synchronized void updateLeafSet(Event event,
      TCPConnection connection) {
    connection.close();
    GenericPeerMessage request = ( GenericPeerMessage ) event;
    metadata.leaf().setLeaf( request.getPeer(), request.getFlag() );
    if ( metadata.leaf().isPopulated() )
    {
      LOG.info( metadata.leaf().toString() );
    }
  }

  /**
   * Updates this DHT with the identifier of the newly joined peer in
   * every row, except for those that follow a shared prefix.
   * 
   * @param event
   */
  private synchronized void updateRoutingTable(Event event) {
    LOG.info( "Updating Routing Table: " );
    metadata.addPeerToTable( ( ( GenericPeerMessage ) event ).getPeer() );
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
      } catch ( IOException | InterruptedException e )
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
   * @throws IOException
   */
  private synchronized void constructDHT(JoinNetwork request)
      throws IOException {

    int row = request.getRow();

    if ( row == request.getNetworkTraceIdentifiers().size() )
    {
      request.setTableRow( metadata.table().getTableRow( row ) );
    }
    request.addNetworkTraceRoute( metadata.self().getIdentifier() );

    int selfCol =
        Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
    int destCol = Character
        .digit( request.getDestination().getIdentifier().charAt( row ), 16 );

    if ( selfCol == destCol )
    {
      request.incrementRow();
      constructDHT( request );
    } else
    {
      PeerInformation peer = metadata.table().getTableIndex( row, destCol );
      if ( peer != null )
      {
        request.incrementRow();
        TCPConnection intermediate = ConnectionUtilities
            .establishConnection( this, peer.getHost(), peer.getPort() );
        intermediate.getTCPSender().sendData( request.getBytes() );
      } else
      {
        peer = metadata.table().closest( metadata.self(),
            request.getDestination(), row );
        if ( peer.equals( metadata.self() ) )
        {
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
          TCPConnection destination = ConnectionUtilities.establishConnection(
              this, request.getDestination().getHost(),
              request.getDestination().getPort() );
          destination.getTCPSender().sendData( request.getBytes() );
        } else
        {
          TCPConnection intermediate = ConnectionUtilities
              .establishConnection( this, peer.getHost(), peer.getPort() );
          intermediate.getTCPSender().sendData( request.getBytes() );
        }
      }
    }
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
   * @param data
   * 
   */
  private void constructLeafSet(JoinNetwork joinRequest, byte[] data) {
    PeerInformation cw = joinRequest.getCW(), ccw = joinRequest.getCCW();
    try
    {
      GenericPeerMessage request =
          new GenericPeerMessage( Protocol.FORWARD_LEAF_IDENTIFIER,
              metadata.self(), Constants.COUNTER_CLOCKWISE );

      TCPConnection connection = connections.cacheConnection( this, cw, false );

      metadata.leaf().setLeaf( cw, Constants.CLOCKWISE );
      metadata.addPeerToTable( cw );
      request.setFlag( Constants.COUNTER_CLOCKWISE );
      LOG.debug( "Sending Data to: " + cw.toString() );
      connection.getTCPSender().sendData( request.getBytes() );
      connection.getTCPSender().sendData( data );

      connection = connections.cacheConnection( this, ccw, false );

      metadata.leaf().setLeaf( ccw, Constants.COUNTER_CLOCKWISE );
      metadata.addPeerToTable( ccw );
      request.setFlag( Constants.CLOCKWISE );
      LOG.debug( "Sending Data to: " + ccw.toString() );
      connection.getTCPSender().sendData( request.getBytes() );
      connection.getTCPSender().sendData( data );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to send leaf set request to peers. " + e.getMessage() );
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
            LOG.debug( "Sending Data to: " + peer.toString() );
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
    constructLeafSet( request, data );
    connections.closeCachedConnections();
    
    metadata.addSelfToTable();
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
        LOG.error( "Unable to send message to source node. " + e.getMessage() );
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

    sb.append( "\n\t" ).append( DISPLAY_LEAF )
        .append( "\t: display the leaf nodes as " )
        .append( "{ clockwise, this, counter-clockwise }.\n" );

    sb.append( "\n\t" ).append( EXIT )
        .append( "\t\t: gracefully leave the network and distribute stored " )
        .append( "files.\n" );

    System.out.println( sb.toString() );
  }

}
