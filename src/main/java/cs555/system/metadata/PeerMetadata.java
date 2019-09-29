package cs555.system.metadata;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import cs555.system.util.Constants;

/**
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
   * @return
   */
  public Lock getLock() {
    return lock;
  }

  /**
   * 
   * @return
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
   * Initializes the first location in the routing table ( itself ).
   * 
   * <p>
   * This is used for when the initial node joins the table.
   * </p>
   * 
   */
  public void addSelfToTable() {
    for ( int i = 0; i < Constants.NUMBER_OF_ROWS; i++ )
    {
      table.addPeerToTable( self, i );
    }
  }

}
