package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Scanner;
import cs555.system.transport.TCPConnection;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.Event;

/**
 *
 * @author stock
 *
 */
public class Peer implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String EXIT = "exit";

  private static final String HELP = "help";

  private static final String LIST = "list";

  private TCPConnection discoveryConnection;

  private final String host;

  private final int port;

  /**
   * Default constructor - creates a new peer tying the <b>host:port</b>
   * combination for the node as the identifier for itself.
   * 
   * @param host
   * @param port
   */
  private Peer(String host, int port) {
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
   * 
   * @return the connection to the discovery
   */
  public TCPConnection getdiscoveryConnection() {
    return this.discoveryConnection;
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

      node.discoveryConnection = node.registerPeer( Properties.DISCOVERY_HOST,
          Integer.valueOf( Properties.DISCOVERY_PORT ) );

      node.interact();
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to successfully start peer. Exiting. " + e.getMessage() );
      System.exit( 1 );
    }
  }

  private TCPConnection registerPeer(String discoveryHost, Integer valueOf) {
    return null;
  }

  /**
   * Allow support for commands to be specified while the processes are
   * running.
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
        case LIST :
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
    LOG.info( host + ":" + port + " has unregistered and is terminating." );
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

    }
  }

  /**
   * Display a help message for how to interact with the application.
   * 
   */
  private void displayHelp() {
    System.out.println( "\n\t" + EXIT + "\n\n\t" + LIST
        + "\t: list readable files stored on the chunk servers." + "\n\n\t"
        + LIST + "\' input.\n" );
  }

}
