package cs555.system.node;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import cs555.system.metadata.LeafSet.Leaf;
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
        join( event, connection );
        break;

      case Protocol.FORWARD_PEER_IDENTIFIER :
        updateRoutingTable( event );
        break;

      case Protocol.FORWARD_LEAF_IDENTIFIER :
        updateLeafSet( event, connection );
        break;

      case Protocol.DISCOVER_PEER_REQUEST :
        discoverPeerHandler( event, connection );
        break;

      case Protocol.STORE_DATA_REQUEST :
        write( event, connection );
        break;
    }
  }

  /**
   * Process an incoming file by saving it to disk, and responding to
   * the Store.
   * 
   * @param event
   * @param connection
   */
  private void write(Event event, TCPConnection connection) {
    DataTransfer request = ( DataTransfer ) event;
    String fileSystemPath =
        request.getFileSystemPath() + "-" + metadata.self().getConnection();
    Path path =
        Paths.get( File.separator, "tmp", "stock", "pastry", fileSystemPath );
    boolean success = true;
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
      success = false;
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
   * 
   * @param event
   * @param connection
   */
  private void discoverPeerHandler(Event event, TCPConnection connection) {
    DiscoverPeerRequest request = ( DiscoverPeerRequest ) event;
    if ( request.getNetworkTraceIdentifiers().size() == 0 )
    {
      connection.close();
    }
    request.addNetworkTraceRoute( metadata.self().getIdentifier() );

    Leaf closest = metadata.leaf().getClosestLeaf( request.getDestination() );
    try
    {
      if ( closest != null )
      {
        if ( closest.getPeer().equals( metadata.self() ) )
        {
          connection = ConnectionUtilities.establishConnection( this,
              request.getDestination().getHost(),
              request.getDestination().getPort() );
          connection.submitTo( executorService );
        } else
        {
          connection = closest.getConnection();
        }
      } else
      {
        connection.close();
        PeerInformation peer = lookup( request );
        connection = ConnectionUtilities.establishConnection( this,
            peer.getHost(), peer.getPort() );
      }
      connection.getTCPSender().sendData( request.getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message. " + e.getMessage() );
      e.printStackTrace();
    }
  }

  /**
   * Consult DHT for closest peer to destination
   * 
   * 
   * @param request
   * @return
   */
  private PeerInformation lookup(DiscoverPeerRequest request) {
    int row = request.getRow();

    int selfCol =
        Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
    int destCol = Character
        .digit( request.getDestination().getIdentifier().charAt( row ), 16 );

    if ( selfCol == destCol )
    {
      request.incrementRow();
      return lookup( request );
    } else
    {
      PeerInformation peer = metadata.table().getTableIndex( row, destCol );
      if ( peer != null )
      {
        return peer;
      } else
      {
        int dest =
            Integer.parseInt( request.getDestination().getIdentifier(), 16 );
        int diff = Integer.MAX_VALUE, other, temp_diff;
        PeerInformation closest = null, temp;
        for ( ; row < 4; ++row )
        {
          for ( int i = 1; i < 8; ++i )
          {
            // clockwise
            int col = ( destCol + i ) & 0xF;
            temp = metadata.table().getTableIndex( row, col );
            if ( temp != null )
            {
              other = Integer.parseInt( temp.getIdentifier(), 16 );
              temp_diff = ( other - dest ) & 0xFFFF;
              if ( temp_diff < diff )
              {
                diff = temp_diff;
                closest = temp;
              }
            }
            // counter-clockwise
            col = ( destCol - i ) & 0xF;
            temp = metadata.table().getTableIndex( row, col );
            if ( temp != null )
            {
              other = Integer.parseInt( temp.getIdentifier(), 16 );
              temp_diff = ( dest - other ) & 0xFFFF;
              if ( temp_diff < diff )
              {
                diff = temp_diff;
                closest = temp;
              }
            }
          }
        }
        return closest;
      }
    }
  }

  /**
   * Update the leaf set from a peer who joined the network
   * 
   * @param event
   * @param connection
   */
  private synchronized void updateLeafSet(Event event,
      TCPConnection connection) {
    GenericPeerMessage request = ( GenericPeerMessage ) event;
    metadata.leaf().setLeaf( request.getPeer(), connection, request.getFlag() );
    if ( metadata.leaf().isPopulated() )
    {
      LOG.info( metadata.leaf().toString() );
    }
  }

  /**
   * Finds the longest prefix with this and the joining peer and adds
   * that peer to the <tt>p + 1</tt> location in this routing table.
   * 
   * @param event
   */
  private synchronized void updateRoutingTable(Event event) {
    PeerInformation peer = ( ( GenericPeerMessage ) event ).getPeer();

    int row = 0;
    for ( ; row < Constants.NUMBER_OF_ROWS; ++row )
    {
      int selfCol =
          Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );
      int destCol = Character.digit( peer.getIdentifier().charAt( row ), 16 );

      if ( selfCol - destCol != 0 )
      {
        metadata.table().addPeerToTable( peer, row );
        break;
      }
    }
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
    if ( metadata.self().equals( request.getDestination() ) )
    {
      initializeDHT( request, connection );
      metadata.initialized();
    } else
    {
      final boolean isSourcePeer = request.getRowIndex() == 0;
      try
      {
        metadata.getLock().lock();
        while ( !metadata.isInitialized() )
        {
          metadata.getCondition().await();
        }
        constructDHT( request, isSourcePeer, connection );
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
   * @param isSourcePeer
   * @param connection
   * @throws IOException
   */
  private synchronized void constructDHT(JoinNetwork request,
      final boolean isSourcePeer, TCPConnection connection) throws IOException {

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
    } else
    {
      PeerInformation peer = metadata.table().getTableIndex( row, destCol );

      if ( peer != null )
      {
        TCPConnection intermediate = ConnectionUtilities
            .establishConnection( this, peer.getHost(), peer.getPort() );
        intermediate.getTCPSender().sendData( request.getBytes() );
      } else
      {
        if ( isSourcePeer )
        {
          connection.getTCPSender().sendData( request.getBytes() );
        } else
        {
          TCPConnection destination = ConnectionUtilities.establishConnection(
              this, request.getDestination().getHost(),
              request.getDestination().getPort() );
          destination.submitTo( executorService );
          destination.getTCPSender().sendData( request.getBytes() );
        }
      }
    }
    if ( !isSourcePeer )
    {
      connection.close(); // close connections between intermediate peers
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
  private void constructLeafSet() {
    PeerInformation cw = null, ccw = null;
    int row = Constants.NUMBER_OF_ROWS - 1;
    int selfCol, col;

    rows: while ( row >= 0 )
    {
      selfCol =
          Character.digit( metadata.self().getIdentifier().charAt( row ), 16 );

      for ( int i = 1; i < 16; ++i )
      {
        if ( cw == null )
        {
          col = selfCol + i;
          if ( col < 16 || row == 0 )
          {
            col = col & 0xF;
            cw = metadata.table().getTableIndex( row, col );
          }
        }
        if ( ccw == null )
        {
          col = selfCol - i;
          if ( col >= 0 || row == 0 )
          {
            col = col & 0xF;
            ccw = metadata.table().getTableIndex( row, col );
          }
        }
        if ( cw != null && ccw != null )
        {
          break rows;
        }
      }
      --row;
    }
    try
    {
      // request flag false for cw, true for ccw
      GenericPeerMessage request = new GenericPeerMessage(
          Protocol.FORWARD_LEAF_IDENTIFIER, metadata.self(), false );
      TCPConnection connection = ConnectionUtilities.establishConnection( this,
          cw.getHost(), cw.getPort() );
      connection.submitTo( executorService );
      metadata.leaf().setLeaf( cw, connection, true );
      connection.getTCPSender().sendData( request.getBytes() );

      connection = ConnectionUtilities.establishConnection( this, ccw.getHost(),
          ccw.getPort() );
      connection.submitTo( executorService );
      metadata.leaf().setLeaf( ccw, connection, false );
      request.setFlag( true );
      connection.getTCPSender().sendData( request.getBytes() );

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
   * @param lastPeerConnection
   */
  private synchronized void initializeDHT(JoinNetwork request,
      TCPConnection lastPeerConnection) {
    LOG.debug( "Initializing Peer" );
    metadata.table().setTable( request.getTable() );
    metadata.addSelfToTable();
    metadata.table().display();

    PeerInformation trace = null;
    StringBuilder sb = new StringBuilder( "Network Join Trace:" );
    List<Short> networkTraceIndex = request.getNetworkTraceIndex();
    for ( int i = 0; i < networkTraceIndex.size(); ++i )
    {
      PeerInformation s = request.getTable()[ i ][ networkTraceIndex.get( i ) ];
      if ( !s.equals( trace ) && !s.equals( metadata.self() ) )
      {
        sb.append( " -> " ).append( s.getIdentifier() );
        trace = s;
      }
    }
    // the last peer connection to contact this peer should be the last
    // traced in the join request, and therefore the calling connection.
    connections.addConnection( trace, lastPeerConnection );
    LOG.info( sb.toString() );

    byte[] data;
    try
    {
      data = new GenericPeerMessage( Protocol.FORWARD_PEER_IDENTIFIER,
          metadata.self(), false ).getBytes();
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
          if ( peer != null && !peer.equals( metadata.self() ) )
          {
            LOG.debug( "Send Data to: " + peer.toString() );
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

    constructLeafSet();
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
        TCPConnection sourceConnection =
            connections.cacheConnection( this, source, true );
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
  private void displayHelp() {}

}
