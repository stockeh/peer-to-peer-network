package cs555.system.metadata;

import java.nio.file.Path;

/**
 * 
 * @author stock
 *
 */
public class StoreMetadata {

  private final PeerInformation item;

  private Path path;

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
  public Path getPath() {
    return path;
  }

  /**
   * 
   * @param content
   */
  public void setPath(Path path) {
    this.path = path;
  }

  /**
   * 
   * @return true if the store can write, false otherwise
   */
  public boolean writable() {
    return item.getIdentifier() == null;
  }

}
