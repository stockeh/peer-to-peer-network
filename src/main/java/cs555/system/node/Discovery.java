package cs555.system.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Scanner;
import cs555.system.transport.TCPConnection;
import cs555.system.transport.TCPServerThread;
import cs555.system.util.Logger;
import cs555.system.util.Properties;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.Protocol;


/**
 *
 * @author stock
 *
 */
public class Discovery implements Node {

  private static final Logger LOG = Logger.getInstance();

  private static final String LIST_CHUNK_NODES = "list";

  private static final String HELP = "help";

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
        new ServerSocket( Integer.valueOf( Properties.DISCOVERY_PORT ) ) )
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
        case LIST_CHUNK_NODES :
          break;

        case HELP :
          System.out.println( "\n\t" + LIST_CHUNK_NODES
              + "\t: show the nodes connected with the discovery.\n" );
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
    LOG.debug( event.toString() );
    switch ( event.getType() )
    {
      case Protocol.REGISTER_REQUEST :
        break;

      case Protocol.UNREGISTER_REQUEST :
        break;
    }
  }
}
