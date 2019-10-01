package cs555.system.metadata;

import java.nio.file.Path;

/**
 * 
 * @author stock
 *
 */
public class StoreMetadata {

  private final PeerInformation item;

  private Path localPath;

  private String fileSystemPath;

  /**
   * 
   * @param host
   * @param port
   */
  public StoreMetadata(String host, int port) {
    this.item = new PeerInformation( null, host, port );
  }

  /**
   * 
   * @return
   */
  public PeerInformation item() {
    return item;
  }

  /**
   * 
   * @return
   */
  public Path getLocalPath() {
    return localPath;
  }

  /**
   * 
   * @param path
   */
  public void setLocalPath(Path path) {
    this.localPath = path;
  }

  /**
   * 
   * @return
   */
  public String getFileSystemPath() {
    return fileSystemPath;
  }

  /**
   * 
   * @param content
   */
  public void setFileSystemPath(String path) {
    if ( path.charAt( 0 ) == '/' || path.charAt( 0 ) == '\\' )
    {
      path = path.substring( 1 );
    }
    this.fileSystemPath = path;
  }

  /**
   * 
   * @return true if the store can write, false otherwise
   */
  public boolean writable() {
    return item.getIdentifier() == null;
  }

  /**
   * Maintain the host and port information, but reset all other
   * metadata fields.
   * 
   */
  public void reset() {
    localPath = null;
    fileSystemPath = null;
    item.setIdentifier( null );
  }

}
