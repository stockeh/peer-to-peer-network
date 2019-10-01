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
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.StoreMetadata;
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
import cs555.system.wireformats.Protocol;

/**
 * Client Store application responsible for delivering or reading data
 * into the peer-to-peer network.
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

        case "fake" :
          FAKEUPLOAD( input );
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

  // TODO: DELETE ME. I AM A FAKE HELPER METHOD.
  private void FAKEUPLOAD(String[] input) {
    if ( metadata.writable() )
    {
      String identifier = input[ 1 ];
      LOG.info(
          "Data Has Identifier: " + identifier + ", based off being fake." );
      metadata.item().setIdentifier( identifier );
      metadata.setLocalPath( Paths.get( "data/greta.jpeg" ) );
      metadata.setFileSystemPath( "greta.jpeg" );
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
   * Read a file that was previously uploaded to the peer-to-peer
   * network to a given path, e.g., read the file in the file system
   * '/greta.jpeg' to the directory 'data'.
   * 
   * <p>
   * USAGE:
   * {@code get /fs_path /local_machine_path | get /greta.jpeg data/ }
   * </p>
   * 
   * @param input
   */
  private void lookup(String[] input) {
    if ( input.length != 3 )
    {
      LOG.error( "USAGE: get /fs_path /local_machine_path || "
          + "get /greta.jpeg data/" );
    }
  }

  /**
   * Upload a file into the peer-to-peer network by specifying the local
   * file that will be delivered, and then the path of where the file
   * should be stored at the peer.
   * 
   * <p>
   * The identifier is based off the /fs_path + the name for the file,
   * e.g., if uploading the file 'data/greta.jpeg' to the path '/' the
   * identifier and file name on the peer will be '/greta.jpeg'.
   * </p>
   * 
   * <p>
   * USAGE:
   * {@code upload /local_machine_path /fs_path | upload data/greta.jpeg / }
   * </p>
   * 
   * @param input
   */
  private void upload(String[] input) {
    if ( input.length != 3 )
    {
      LOG.error( "USAGE: upload /local_machine_path /fs_path || "
          + "upload data/greta.jpeg /" );
      return;
    }
    if ( metadata.writable() )
    {
      Path localPath = Paths.get( input[ 1 ] );
      String fileSystemPath = input[ 2 ].endsWith( File.separator ) ? input[ 2 ]
          : input[ 2 ] + File.separator;
      fileSystemPath += localPath.getFileName().toString();
      String identifier =
          IdentifierUtilities.CRC16CCITT( fileSystemPath.getBytes() );
      LOG.info( "Data Has Identifier: " + identifier
          + ", based off the name /fs_path and file name." );
      metadata.item().setIdentifier( identifier );
      metadata.setLocalPath( localPath );
      metadata.setFileSystemPath( fileSystemPath );
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

      case Protocol.STORE_DATA_RESPONSE :
        deliveryResponse( event, connection );
        break;
    }
  }

  /**
   * Process the response message from the peer regarding the status of
   * the writing operation.
   * 
   * @param event
   * @param connection
   */
  private void deliveryResponse(Event event, TCPConnection connection) {
    connection.close();
    GenericPeerMessage response = ( GenericPeerMessage ) event;

    StringBuilder sb =
        ( new StringBuilder() ).append( "The write request to peer ( " )
            .append( response.getPeer().toString() ).append( ") for " )
            .append( metadata.getFileSystemPath() ).append( " | " )
            .append( metadata.item().getIdentifier() ).append( " was " );
    if ( response.getFlag() == Constants.FAILURE )
    {
      sb.append( "NOT " );
    }
    sb.append( "successful!" );
    LOG.info( sb.toString() );
    metadata.reset();
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
   * Deliver the file to the destination once a peer has responded to
   * this Store containing details of where to deliver the data.
   * 
   * @param event
   * @param connection to the peer that will hold the data
   */
  private void deliver(Event event, TCPConnection connection) {
    DiscoverPeerRequest request = ( DiscoverPeerRequest ) event;

    StringBuilder sb = new StringBuilder( "Network Route Trace:" );
    for ( String s : request.getNetworkTraceIdentifiers() )
    {
      sb.append( " -> " ).append( s );
    }
    LOG.info( sb.toString() );
    try
    {
      byte[] content = Files.readAllBytes( metadata.getLocalPath() );
      connection.getTCPSender()
          .sendData( ( new DataTransfer( Protocol.STORE_DATA_REQUEST, content,
              metadata.getFileSystemPath() ) ).getBytes() );
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
