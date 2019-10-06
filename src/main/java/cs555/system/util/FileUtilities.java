package cs555.system.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.PeerMetadata;
import cs555.system.node.Node;
import cs555.system.transport.TCPConnection;
import cs555.system.wireformats.DataTransfer;
import cs555.system.wireformats.Event;
import cs555.system.wireformats.GenericMessage;
import cs555.system.wireformats.GenericPeerMessage;
import cs555.system.wireformats.Protocol;

public class FileUtilities {

  private static final Logger LOG = Logger.getInstance();

  /**
   * Process an incoming file by saving it to disk, and responding to
   * the Store with the status of the write operation.
   * 
   * <p>
   * Data is stored by the specified {@code fileSystemPath} and a unique
   * host connection string.
   * </p>
   * 
   * @param metadata
   * @param event
   * @param connection from the Store that will be used for response
   */
  public static void write(PeerMetadata metadata, Event event,
      TCPConnection connection) {
    DataTransfer request = ( DataTransfer ) event;
    String[] descriptor = request.getDescriptor().split( "\t" );
    metadata.addFile( descriptor[ 0 ], descriptor[ 1 ] );

    Path path = constructPath( metadata, descriptor[ 0 ] );
    boolean success = Constants.SUCCESS;
    try
    {
      Files.createDirectories( path.getParent() );
      Files.write( path, request.getData() );
      LOG.info( "Finished writing " + path.toString() + " to disk." );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to save " + path.toString() + " to disk. " + e.toString() );
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
      LOG.error( "Unable to send message to store. " + e.toString() );
      e.printStackTrace();
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
   * @param metadata
   * @param event
   * @param connection from the Store that will be used for response
   */
  public static void read(PeerMetadata metadata, Event event,
      TCPConnection connection) {
    GenericMessage request = ( GenericMessage ) event;

    Path path =
        constructPath( metadata, request.getMessage().split( "\t" )[ 0 ] );

    byte[] data = null;
    try
    {
      data = Files.readAllBytes( path );
    } catch ( IOException e )
    {
      LOG.error(
          "Unable to read " + path.toString() + " from disk. " + e.toString() );
      e.printStackTrace();
    }
    try
    {
      connection.getTCPSender()
          .sendData( ( new DataTransfer( Protocol.READ_DATA_RESPONSE, data,
              metadata.self().toString() ) ).getBytes() );
    } catch ( IOException e )
    {
      LOG.error( "Unable to send message to store. " + e.toString() );
      e.printStackTrace();
    }
  }

  /**
   * Migrate data from this node to the newly added neighbor, which is
   * the same as the connection who just joined.
   * 
   * @param node
   * @param metadata
   * @param executorService
   * @param entry key/value pair as fileSystemPath/fileIdentifier
   * @param leaf
   * @return true if a file has to be migrated, false otherwise
   */
  public static boolean migrateData(Node node, PeerMetadata metadata,
      ExecutorService executorService, Entry<String, String> entry,
      PeerInformation leaf) {
    String k = entry.getKey(), v = entry.getValue();
    PeerInformation closest = metadata.leaf().getClosestLeaf( v );
    if ( closest == null || closest.equals( leaf ) )
    {
      String fileSystemPath = k + "-" + metadata.self().getConnection();
      Path path =
          Paths.get( File.separator, "tmp", "stock", "pastry", fileSystemPath );
      byte[] data = null;
      try
      {
        data = Files.readAllBytes( path );
        TCPConnection connection = ConnectionUtilities
            .establishConnection( node, leaf.getHost(), leaf.getPort() );
        connection.submitTo( executorService );
        connection.getTCPSender()
            .sendData( ( new DataTransfer( Protocol.STORE_DATA_REQUEST, data,
                k + "\t" + v ) ).getBytes() );
        LOG.info( ( new StringBuilder() ).append( "The file " )
            .append( File.separator ).append( k )
            .append( " is being migrated to " ).append( leaf.toString() )
            .toString() );
        Files.delete( path );
      } catch ( IOException e )
      {
        LOG.error(
            "Unable to migrate " + fileSystemPath + ", " + e.toString() );
        e.printStackTrace();
        return false;
      }
      return true;
    } else
    {
      return false;
    }
  }

  /**
   * 
   * Construct the file system path that will be used to read / write a
   * file on the peer file system.
   * 
   * @param metadata
   * @param filename
   * @return
   */
  private static Path constructPath(PeerMetadata metadata, String filename) {
    return Paths.get( File.separator, "tmp", "stock", "pastry",
        filename + "-" + metadata.self().getConnection() );
  }

}
