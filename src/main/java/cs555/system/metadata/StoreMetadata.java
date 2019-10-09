package cs555.system.metadata;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import cs555.system.util.Constants;

/**
 * Class containing metadata for the store. This is updated every time
 * an upload or read operation is performed.
 * 
 * @author stock
 *
 */
public class StoreMetadata {

  public final static boolean WRITE = true;

  public final static boolean READ = false;

  private final String host;

  private final int port;

  private final Map<String, DataItem> items;

  private boolean dataTransferType;

  /**
   * Default constructor - sets the host and port for the Store, and
   * reuses the {@code PeerInformation} object to locate a peer for some
   * identifier.
   * 
   * @param host
   * @param port
   */
  public StoreMetadata(String host, int port) {
    this.host = host;
    this.port = port;
    this.items = new HashMap<>();
    this.dataTransferType = false;
  }

  /**
   * 
   * @param identifier
   * @param localPath
   * @param fileSystemPath
   * @return
   */
  public PeerInformation addDataItem(String identifier, Path localPath,
      String fileSystemPath) {
    PeerInformation item = new PeerInformation( identifier, host, port );
    items.put( identifier,
        new DataItem( item, identifier, localPath, fileSystemPath ) );
    return item;
  }

  /**
   * Remove and return data item from items.
   * 
   * @param identifier
   * @return
   */
  public DataItem getDataItem(String identifier) {
    return items.remove( identifier );
  }

  /**
   * 
   * @return
   */
  public boolean getDataTransferType() {
    return dataTransferType;
  }

  /**
   * 
   * @param type
   */
  public void setDataTransferType(boolean type) {
    dataTransferType = type;
  }

  /**
   * 
   * @return the number of items in the Store
   */
  public int numberOfItems() {
    return items.size();
  }

  /**
   * 
   * @author stock
   *
   */
  public class DataItem {

    private final PeerInformation item;

    private final Path localPath;

    private final String fileSystemPath;

    /**
     * Default constructor
     * 
     * @param identifier
     */
    private DataItem(PeerInformation item, String identifier, Path localPath,
        String fileSystemPath) {
      this.item = item;
      this.localPath = localPath;
      this.fileSystemPath =
          ( new StringBuilder( fileSystemPath.startsWith( File.separator )
              ? fileSystemPath.substring( 1 )
              : fileSystemPath ) ).append( Constants.SEPERATOR )
                  .append( identifier ).toString();
    }

    /**
     * 
     * @return the information for the Store host:port as well as the
     *         content identifier.
     */
    public PeerInformation item() {
      return item;
    }

    /**
     * 
     * @return the path for where a file is read from on the Store's local
     *         disk
     */
    public Path getLocalPath() {
      return localPath;
    }

    /**
     * 
     * @return the path, as a {@code String} of where a file should be
     *         written to on the peer (once discovered)
     */
    public String getFileSystemPath() {
      return fileSystemPath;
    }

  }

}
