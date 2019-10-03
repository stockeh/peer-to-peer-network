package cs555.system.metadata;

import java.io.File;
import java.nio.file.Path;

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

  private final PeerInformation item;

  private Path localPath;

  private String fileSystemPath;

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
    this.item = new PeerInformation( null, host, port );
    this.dataTransferType = false;
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

  /**
   * 
   * @return
   */
  public boolean getDataTransferType() {
    return dataTransferType;
  }

  /**
   * 
   * @param path
   */
  public void setLocalPath(Path path) {
    this.localPath = path;
  }

  /**
   * Removes any leading file separator, e.g., '/' or '\\' before
   * sending to the peer.
   * 
   * @param content
   */
  public void setFileSystemPath(String path) {
    this.fileSystemPath =
        path.startsWith( File.separator ) ? path.substring( 1 ) : path;
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
   * @return true if the store can write, false otherwise
   */
  public boolean transferable() {
    return item.getIdentifier() == null;
  }

  /**
   * Maintain the host:port information for the Store, but reset all
   * other metadata fields.
   * 
   */
  public void reset() {
    localPath = null;
    fileSystemPath = null;
    item.setIdentifier( null );
  }

}
