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
import cs555.system.metadata.StoreMetadata.DataItem;
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

  private static final String UPLOAD = "upload";

  private static final String GET = "get";

  private static final String DIR = "dir";

  private static final String EXIT = "exit";

  private static final String HELP = "help";

  private final StoreMetadata metadata;

  private final ExecutorService executorService;

  private final Object lock;

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
    this.lock = new Object();
  }

  /**
   * Start listening for incoming connections and establish connection
   * into the peer network.
   *
   * @param args
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    LOG.info( "Store node starting up at: " + new Date() );
    try ( ServerSocket serverSocket = new ServerSocket( 0 ) )
    {
      Store node = new Store( InetAddress.getLocalHost().getHostName(),
          serverSocket.getLocalPort() );

      ( new Thread(
          new TCPServerThread( node, serverSocket, node.executorService ),
          "Server Thread" ) ).start();

      if ( args.length == 0 )
      {
        node.interact();
      } else
      {
        node.execute( args );
        System.exit( 0 );
      }
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to successfully start peer. Exiting. " + e.toString() );
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
      running = execute( input );
    }
    System.exit( 0 );
  }

  /**
   * Commands that can be run from the store.
   * 
   * @param input
   * @return
   */
  private boolean execute(String input[]) {
    switch ( input[ 0 ] )
    {
      case UPLOAD :
        upload( input );
        break;

      case GET :
        get( input );
        break;

      case DIR :
        LOG.info( "Working Directory: " + System.getProperty( "user.dir" ) );
        break;

      case EXIT :
        return false;

      case HELP :
        displayHelp();
        break;

      default :
        LOG.error( "Unable to process. Please enter a valid command! " );
        displayHelp();
        break;
    }
    return true;
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
  private void get(String[] input) {
    if ( input.length != 3 )
    {
      LOG.error( "USAGE: get /fs_path /local_machine_path || "
          + "get /greta.jpeg data/" );
      return;
    }
    metadata.setDataTransferType( StoreMetadata.READ );

    // start with '/'
    String fileSystemPath = input[ 1 ].startsWith( File.separator ) ? input[ 1 ]
        : File.separator + input[ 1 ];
    LOG.info( "HERE: " + fileSystemPath );
    // end with '/'
    String lp =
        input[ 2 ].charAt( input[ 2 ].length() - 1 ) == File.separatorChar
            ? input[ 2 ]
            : input[ 2 ] + File.separator;
    Path localPath = Paths.get( lp + fileSystemPath.substring(
        fileSystemPath.lastIndexOf( File.separator ) + 1,
        fileSystemPath.length() ) );
    discover( localPath, fileSystemPath );
    this.await();
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
    metadata.setDataTransferType( StoreMetadata.WRITE );
    Path inputPath = Paths.get( input[ 1 ] );

    if ( Files.isDirectory( inputPath ) )
    {
      try
      {
        Files.walk( inputPath ).forEach( path ->
        {
          addFile( path, input );
        } );
      } catch ( IOException e )
      {
        LOG.error( "Failure while traversing directory. " + e.toString() );
        e.printStackTrace();
      }
    } else
    {
      addFile( Paths.get( input[ 1 ] ), input );
    }
  }

  /**
   * Add individual file to upload
   * 
   * @param path
   * @param input
   */
  public void addFile(Path path, String[] input) {
    if ( !Files.isDirectory( path ) )
    {
      LOG.debug( "PATH: " + path.toAbsolutePath().toString() );
      String fileSystemPath = input[ 2 ].endsWith( File.separator ) ? input[ 2 ]
          : input[ 2 ] + File.separator;
      fileSystemPath += path.getFileName().toString();
      discover( path, fileSystemPath );
      this.await();
    }
  }

  /**
   * Wait for response back from server before proceeding.
   * 
   */
  public void await() {
    synchronized ( lock )
    {
      try
      {
        lock.wait();
      } catch ( InterruptedException e )
      {
        LOG.error( "Unable to wait before sending next file in directory. "
            + e.toString() );
        e.printStackTrace();
      }
    }
  }

  /**
   * Initialize the transfer between the Store and the network for both
   * the the read and writing transfer types.
   * 
   * @param localPath for where the file is / will be stored on the
   *        local file system
   * @param fileSystemPath for where the file is to be stored / read
   *        from in the network file system
   * 
   */
  private void discover(Path localPath, String fileSystemPath) {
    String identifier =
        IdentifierUtilities.CRC16CCITT( fileSystemPath.getBytes() );
    LOG.info( "Data Has Identifier: " + identifier + ", based off the name "
        + fileSystemPath );
    PeerInformation item =
        metadata.addDataItem( identifier, localPath, fileSystemPath );
    try
    {
      TCPConnection connection = ConnectionUtilities.establishConnection( this,
          Properties.DISCOVERY_HOST, Properties.DISCOVERY_PORT );
      connection.submitTo( executorService );
      connection.getTCPSender().sendData(
          ( new GenericPeerMessage( Protocol.DISCOVER_NODE_REQUEST, item ) )
              .getBytes() );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to send message to Discovery. Exiting. " + e.toString() );
      System.exit( 1 );
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
        transferData( event, connection );
        break;

      case Protocol.STORE_DATA_RESPONSE :
        writeResponse( event, connection );
        break;

      case Protocol.READ_DATA_RESPONSE :
        readResponse( event, connection );
        break;
    }
  }

  /**
   * Process the response message from the peer containing the requested
   * file.
   * 
   * @param event
   * @param connection
   */
  private void readResponse(Event event, TCPConnection connection) {
    connection.close();
    DataTransfer response = ( DataTransfer ) event;
    // peer information ? fs path ? content id ? local path
    String[] message = response.getDescriptor().split( Constants.SEPERATOR );
    byte[] data = response.getData();
    StringBuilder sb =
        ( new StringBuilder() ).append( "The read request from peer ( " )
            .append( message[ 0 ] ).append( ") for " ).append( message[ 1 ] )
            .append( " " ).append( message[ 2 ] ).append( " was " );
    if ( data == null )
    {
      sb.append( "NOT successful!" );
      LOG.error( sb.toString() );
    } else
    {
      sb.append( "successful!" );
      LOG.info( sb.toString() );
      String fs = message[ 3 ];
      try
      {
        Files.createDirectories( Paths.get( message[ 3 ] ).getParent() );
        Files.write( Paths.get( message[ 3 ] ), data );
        LOG.info( "Finished writing " + fs + " to disk." );
      } catch ( IOException e )
      {
        LOG.error( "Unable to save " + fs + " to disk. " + e.toString() );
        e.printStackTrace();
      }
    }
    synchronized ( lock )
    {
      lock.notify();
    }
  }

  /**
   * Process the response message from the peer regarding the status of
   * the writing operation.
   * 
   * @param event
   * @param connection
   */
  private void writeResponse(Event event, TCPConnection connection) {
    connection.close();
    GenericPeerMessage response = ( GenericPeerMessage ) event;
    // fs path ? content id ? local path
    String[] message = response.getMessage().split( Constants.SEPERATOR );
    StringBuilder sb =
        ( new StringBuilder() ).append( "The write request to peer ( " )
            .append( response.getPeer().toString() ).append( ") for " )
            .append( message[ 0 ] ).append( " | " ).append( message[ 1 ] )
            .append( " was " );
    if ( response.getFlag() == Constants.FAILURE )
    {
      sb.append( "NOT successful!" );
      LOG.error( sb.toString() );
    } else
    {
      sb.append( "successful!" );
      LOG.info( sb.toString() );
    }
    synchronized ( lock )
    {
      lock.notify();
    }
  }

  /**
   * Deliver the file to the destination once a peer has responded to
   * this Store containing details of where to deliver the data.
   * 
   * @param event
   * @param connection to the peer that will hold the data
   */
  private void transferData(Event event, TCPConnection connection) {
    DiscoverPeerRequest request = ( DiscoverPeerRequest ) event;

    StringBuilder sb = new StringBuilder( "Network Route Trace:" );
    for ( String s : request.getNetworkTraceIdentifiers() )
    {
      sb.append( " -> " ).append( s );
    }
    LOG.info( sb.toString() );
    try
    {
      DataItem data =
          metadata.getDataItem( request.getDestination().getIdentifier() );
      if ( metadata.getDataTransferType() == StoreMetadata.WRITE )
      {
        byte[] content = Files.readAllBytes( data.getLocalPath() );
        connection.getTCPSender()
            .sendData( ( new DataTransfer( Protocol.STORE_DATA_REQUEST, content,
                data.getFileSystemPath() ) ).getBytes() );
      } else
      {
        // fs path ? content id ? local path
        String message = data.getFileSystemPath() + Constants.SEPERATOR
            + data.getLocalPath().toAbsolutePath().toString();
        connection.getTCPSender().sendData(
            ( new GenericMessage( Protocol.READ_DATA_REQUEST, message ) )
                .getBytes() );
      }
    } catch ( IOException e )
    {
      LOG.error( "Unable to upload file " + e.toString() );
      connection.close();
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
    if ( response.isInitialPeerConnection() )
    {
      LOG.error( "There are no peers in the network. Unable to upload files." );
      return;
    }
    LOG.info(
        "Connecting to the network through source node: " + source.toString() );
    try
    {
      ConnectionUtilities
          .establishConnection( this, source.getHost(), source.getPort() )
          .getTCPSender()
          .sendData( ( new DiscoverPeerRequest( Protocol.DISCOVER_PEER_REQUEST,
              response.getOriginalInformation() ) ).getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message to the source node. " + e.toString() );
      e.printStackTrace();
    }
  }

  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {
    StringBuilder sb = new StringBuilder();

    sb.append( "\n\t" ).append( UPLOAD )
        .append( "\t: deliver a file to the network " )
        .append( "-> upload /local_machine_path /fs_path\n" );

    sb.append( "\n\t" ).append( GET )
        .append( "\t: retrieve a previously uploaded file " )
        .append( "-> get /fs_path /local_machine_path\n" );

    sb.append( "\n\t" ).append( DIR )
        .append( "\t: complete absolute path from where the" )
        .append( " application was initialized \n" );

    sb.append( "\n\t" ).append( EXIT )
        .append( "\t: gracefully shutdown the application\n" );

    System.out.println( sb.toString() );
  }

}
