package cs555.system.metadata;

import java.io.File;
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
   * Add a peer to all applicable locations in the routing table.
   * 
   * @param peer
   */
  public void addPeerToTable(PeerInformation peer) {
    for ( int row = 0; row < Constants.NUMBER_OF_ROWS; ++row )
    {
      int selfCol = Character.digit( self.getIdentifier().charAt( row ), 16 );
      int destCol = Character.digit( peer.getIdentifier().charAt( row ), 16 );

      if ( selfCol - destCol != 0 )
      {
        table.addPeerToTable( peer, row );
        break;
      }
    }
  }

  /**
   * 
   * @param filename
   */
  public synchronized void addFile(String filename) {
    if ( !files.contains( filename ) )
    {
      files.add( filename );
    }
  }

  /**
   * 
   * @return the {@code String} representation of the files stored on
   *         this peer, or an error message if none are.
   */
  public synchronized String filesToString() {
    StringBuilder sb = new StringBuilder();
    if ( files.isEmpty() )
    {
      return sb.append( "There are no files stored on peer " )
          .append( self.toString() ).toString();
    }
    files.sort( Comparator.comparing( String::length )
        .thenComparing( Comparator.naturalOrder() ) );
    sb.append( "Files stored on " ).append( self.toString() )
        .append( " include: \n\n" );
    files
        .forEach( v -> sb.append( File.separator ).append( v ).append( "\n" ) );
    return sb.toString();
  }
}
