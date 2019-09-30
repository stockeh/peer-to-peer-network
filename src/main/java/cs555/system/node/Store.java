package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.StoreMetadata;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.ConnectionUtilities;
import cs555.system.util.IdentifierUtilities;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.DataTransfer;
import cs555.system.wireformats.DiscoverNodeResponse;
import cs555.system.wireformats.DiscoverPeerRequest;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.GenericMessage;
import cs555.system.wireformats.Protocol;

/**
 *
 * @author stock
 *
 */
public class Store implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String EXIT = "exit";

  private static final String HELP = "help";

  private static final String UPLOAD = "upload";

  private static final String GET = "get";

  private final StoreMetadata metadata;

  private final ExecutorService executorService;

  /**
   * Default constructor - creates a new peer tying the <b>host:port</b>
   * combination for the node as the identifier for itself.
   * 
   * @param host
   * @param port
   */
  private Store(String host, int port) {
    this.metadata = new StoreMetadata( host, port );
    this.executorService = Executors.newCachedThreadPool();
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
      Store node = new Store( InetAddress.getLocalHost().getHostName(),
          serverSocket.getLocalPort() );

      ( new Thread(
          new TCPServerThread( node, serverSocket, node.executorService ),
          "Server Thread" ) ).start();

      node.interact();
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to successfully start peer. Exiting. " + e.getMessage() );
      System.exit( 1 );
    }
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
        case UPLOAD :
          upload( input );
          break;

        case GET :
          lookup( input );
          break;

        case EXIT :
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
    System.exit( 0 );
  }

  /**
   * 
   * <p>
   * <code>
   * get /hdfs_path /local_machine_path
   * </br>
   * get /img.png data/
   * </code>
   * </p>
   * 
   * @param input
   */
  private void lookup(String[] input) {
    if ( input.length != 3 )
    {
      LOG.error( "USAGE: get /hdfs_path /local_machine_path" );
    }
  }

  /**
   * 
   * 
   */
  private void upload(String[] input) {
    if ( input.length != 3 )
    {
      LOG.error( "USAGE: upload /local_machine_path /hdfs_path" );
      return;
    }
    if ( metadata.writable() )
    {
      String identifier =
          IdentifierUtilities.CRC16CCITT( input[ 1 ].getBytes() );
      LOG.info( "Data Has Identifier: " + identifier
          + ", based off the name /local_machine_path" );
      metadata.item().setIdentifier( identifier );
      metadata.setPath( Paths.get( input[ 1 ] ) );
      try
      {
        TCPConnection connection = ConnectionUtilities.establishConnection(
            this, Properties.DISCOVERY_HOST, Properties.DISCOVERY_PORT );
        connection.submitTo( executorService );
        connection.getTCPSender()
            .sendData( ( new GenericMessage( Protocol.DISCOVER_NODE_REQUEST ) )
                .getBytes() );
      } catch ( IOException e )
      {
        LOG.error( "Unable to send message to Discovery. " + e.getMessage() );
        e.printStackTrace();
      }
    } else
    {
      LOG.info( "The Store is currently writing a file. Try again shortly." );
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
        connection.close(); // close connection with Discovery
        dicoverNodeHandler( event );
        break;

      case Protocol.DISCOVER_PEER_REQUEST :
        deliver( event, connection );
        break;
    }
  }

  /**
   * The Discovery will return a peer to connect to in the network for
   * which a peer discovery message will be propagated to find a peer
   * with the closest destination to the {@code metadata.item()}.
   *
   * @param event message from Discovery
   */
  private void dicoverNodeHandler(Event event) {
    DiscoverNodeResponse response = ( DiscoverNodeResponse ) event;
    PeerInformation source = response.getSourceInformation();
    LOG.info(
        "Connecting to the DHT through source node: " + source.toString() );
    try
    {
      ConnectionUtilities
          .establishConnection( this, source.getHost(), source.getPort() )
          .getTCPSender().sendData(
              ( new DiscoverPeerRequest( metadata.item() ) ).getBytes() );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to send message to the source node. " + e.getMessage() );
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param event
   * @param connection
   */
  private void deliver(Event event, TCPConnection connection) {

    DiscoverPeerRequest request = ( DiscoverPeerRequest ) event;

    StringBuilder sb = new StringBuilder( "Network Join Trace:" );
    for ( String s : request.getNetworkTraceIdentifiers() )
    {
      sb.append( " -> " ).append( s );
    }
    LOG.info( sb.toString() );
    try
    {
      byte[] content = Files.readAllBytes( metadata.getPath() );
      connection.getTCPSender().sendData(
          ( new DataTransfer( Protocol.STORE_DATA_REQUEST, content ) )
              .getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to upload file. " + e.getMessage() );
      e.printStackTrace();
    }
  }


  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {}

}
