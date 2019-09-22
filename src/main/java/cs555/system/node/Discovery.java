package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import cs555.system.metadata.PeerInformation;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.DiscoverNodeResponse;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.GenericMessage;
import cs555.system.wireformats.Protocol;
import cs555.system.wireformats.RegisterRequest;


/**
 *
 * @author stock
 *
 */
public class Discovery implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String LIST_NODES = "list-nodes";

  private static final String HELP = "help";

  private List<PeerInformation> registeredNodes;

  private String host;

  private int port;

  /**
   * Default constructor - creates a new discovery tying the
   * <b>host:port</b> combination for the node as the identifier for
   * itself.
   * 
   * @param host
   * @param port
   */
  public Discovery(String host, int port) {
    this.registeredNodes = new ArrayList<>();
    this.host = host;
    this.port = port;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHost() {
    return this.host;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPort() {
    return this.port;
  }

  /**
   * Stands-up the discovery as an entry point to the class.
   *
   * @param args
   */
  public static void main(String[] args) {
    LOG.debug( "discovery node starting up at: " + new Date() );

    try ( ServerSocket serverSocket =
        new ServerSocket( Properties.DISCOVERY_PORT ) )
    {
      Discovery discovery =
          new Discovery( InetAddress.getLocalHost().getHostName(),
              serverSocket.getLocalPort() );

      ( new Thread( new TCPServerThread( discovery, serverSocket ),
          "Server Thread" ) ).start();

      discovery.interact();
    } catch ( IOException e )
    {
      LOG.error( "Unable to successfully start discovery. Exiting. "
          + e.getMessage() );
      System.exit( 1 );
    }
  }

  /**
   * Allow support for commands to be specified while the processes are
   * running.
   */
  private void interact() {
    System.out.println(
        "\nInput a command to interact with processes. Input 'help' for a"
            + " list of commands.\n" );
    @SuppressWarnings( "resource" )
    Scanner scan = new Scanner( System.in );
    while ( true )
    {
      String line = scan.nextLine().toLowerCase();
      String[] input = line.split( "\\s+" );
      switch ( input[ 0 ] )
      {
        case LIST_NODES :
          displayConnections();
          break;

        case HELP :
          displayHelp();
          break;

        default :
          LOG.error(
              "Unable to process. Please enter a valid command! Input 'help' "
                  + "for options." );
          break;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEvent(Event event, TCPConnection connection) {
    // LOG.debug( event.toString() );
    switch ( event.getType() )
    {
      case Protocol.REGISTER_REQUEST :
        registerRequestHandler( event, connection );
        break;

      case Protocol.UNREGISTER_REQUEST :
        break;

      case Protocol.DISCOVER_NODE_REQUEST :
        selectPeerNode( connection );
        break;
    }
  }

  /**
   * Attempt to register a new node into the system. The discovery will
   * either (1) identify an ID collision causing the peer to regenerate
   * an identifier, or (b) respond with a peer to connect too and add
   * the node to suitable peers.
   * 
   * @param event
   * @param connection
   */
  private synchronized void registerRequestHandler(Event event,
      TCPConnection connection) {
    RegisterRequest request = ( RegisterRequest ) event;
    PeerInformation peer = new PeerInformation( request.getIdentifier(),
        request.getHost(), request.getPort() );
    if ( registeredNodes.contains( peer ) )
    {
      LOG.debug( "Duplicate Identifier Found." );
      try
      {
        connection.getTCPSender()
            .sendData( ( new GenericMessage( Protocol.IDENTIFIER_COLLISION ) )
                .getBytes() );
      } catch ( IOException e )
      {
        LOG.error(
            "Unable to send response message to peer. " + e.getMessage() );
        e.printStackTrace();
      }
    } else
    {
      selectPeerNode( connection );
      registeredNodes.add( peer );
    }
  }

  /**
   * Responds to the connection with one random node from the set of
   * registered nodes.
   * 
   * @param connection
   */
  private synchronized void selectPeerNode(TCPConnection connection) {
    DiscoverNodeResponse response;
    int numberOfNodes = registeredNodes.size();
    if ( numberOfNodes == 0 )
    {
      response = new DiscoverNodeResponse();
    } else
    {
      int index = ThreadLocalRandom.current().nextInt( numberOfNodes );
      PeerInformation info = registeredNodes.get( index );
      response = new DiscoverNodeResponse( info.getHost(), info.getPort() );
    }
    try
    {
      connection.getTCPSender().sendData( response.getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send response message to peer. " + e.getMessage() );
      e.printStackTrace();
    }
  }

  /**
   * Print out all of the connected chunk servers that have connected.
   * 
   */
  public synchronized void displayConnections() {
    if ( registeredNodes.size() == 0 )
    {
      LOG.error(
          "There are no connections to identify. Initialize a new peer." );
    } else
    {
      System.out.println(
          "\nThere are " + registeredNodes.size() + " total links:\n" );
      registeredNodes.forEach( v -> System.out.println( v.toString() ) );
      System.out.println();
    }
  }

  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {
    System.out.println( "\n\t" + LIST_NODES
        + "\t: list nodes that have registered with Discovery." );
  }
}
