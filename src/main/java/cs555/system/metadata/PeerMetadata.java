package cs555.system.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import cs555.system.util.Constants;

/**
 * Class to maintain the information needed for a given peer. This
 * includes the Distributed Hash Table (DHT), leaf set, and its own
 * information.
 * 
 * @author stock
 *
 */
public class PeerMetadata {

  private final RoutingTable table;

  private final PeerInformation self;

  private final LeafSet leaf;

  private final Lock lock;

  private final Condition condition;

  private boolean initialized;

  private final List<String> files;

  /**
   * Default Constructor -
   * 
   */
  public PeerMetadata(String host, int port) {
    this.table = new RoutingTable();
    this.self = new PeerInformation( null, host, port );
    this.leaf = new LeafSet( this.self );
    this.lock = new ReentrantLock();
    this.condition = lock.newCondition();
    this.initialized = false;
    this.files = new ArrayList<>();
  }

  /**
   * This peer is initialized after the establishing itself with the
   * DHT. Thereafter, allowing other peers to connect.
   * 
   */
  public void initialized() {
    initialized = true;
    try
    {
      lock.lock();
      condition.signal();
    } finally
    {
      lock.unlock();
    }
  }

  /**
   * 
   * @return this peers information
   */
  public PeerInformation self() {
    return self;
  }

  /**
   * 
   * @return the distributed hash table
   */
  public RoutingTable table() {
    return table;
  }

  /**
   * 
   * @return the leaf set for this peer
   */
  public LeafSet leaf() {
    return leaf;
  }

  /**
   * 
   * @return the {@code ReentrantLock} for the {@code Condition}
   */
  public Lock getLock() {
    return lock;
  }

  /**
   * 
   * @return true if the peer has its DHT and leaf set initialized,
   *         false otherwise
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * 
   * @return
   */
  public Condition getCondition() {
    return condition;
  }

  /**
   * Set the identifier associated with a peer
   * 
   */
  public void setIdentifier(String identifier) {
    this.self.setIdentifier( identifier );
  }

  /**
   * Initializes the location in the routing table for itself.
   * 
   * <p>
   * This is used for when the initial node joins the table, and after
   * receiving the trace DHT rows.
   * </p>
   * 
   */
  public void addSelfToTable() {
    for ( int i = 0; i < Constants.NUMBER_OF_ROWS; i++ )
    {
      table.addPeerToTable( self, i );
    }
  }

  /**
   * 
   * @param filename
   */
  public void addFile(String filename) {
    if ( !files.contains( filename ) )
    {
      files.add( filename );
    }
  }

  /**
   * Sort by length, and then sort alphabetically -> this can be
   * improved.
   * 
   * @return
   */
  public List<String> getSortedFiles() {
    files.sort( Comparator.comparing( String::length )
        .thenComparing( Comparator.naturalOrder() ) );
    return files;
  }
}
