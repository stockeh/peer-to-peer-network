package cs555.system.metadata;

import cs555.system.util.Constants;
import cs555.system.util.Properties;

/**
 * Container for the Distributed Hash Table (DHT) and all the helper
 * methods for updating, adding and removing peers from said table.
 * 
 * @author stock
 *
 */
public class RoutingTable {

  private PeerInformation[][] table;

  /**
   * Default Constructor -
   * 
   */
  protected RoutingTable() {
    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][ 16 ];
  }

  /**
   * 
   * @return the table
   */
  public PeerInformation[][] getTable() {
    return table;
  }

  /**
   * 
   * @param table
   */
  public void setTable(PeerInformation[][] table) {
    this.table = table;
  }

  /**
   * 
   * @param index
   * @return the row of the table as specified by the index
   */
  public PeerInformation[] getTableRow(int index) {
    return table[ index ];
  }

  /**
   * 
   * @param row
   * @param col
   * @return
   */
  public PeerInformation getTableIndex(int row, int col) {
    return table[ row ][ col ];
  }

  /**
   * 
   * @param peer
   * @param row
   */
  public void addPeerToTable(PeerInformation peer, int row) {
    int col = Character.digit( peer.getIdentifier().charAt( row ), 16 );
    table[ row ][ col ] = peer;
  }

  /**
   * Remove a peer from a routing table by setting the entry to
   * {@code null}.
   * 
   * @param row
   * @param col
   */
  public void removePeerFromTable(int row, int col) {
    table[ row ][ col ] = null;
  }

  /**
   * Remove all entries in the DHT.
   * 
   */
  public void reset() {
    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][ 16 ];
  }

  /**
   * Print the DHT to the console in a readable format.
   * 
   */
  public void display() {
    String lineSeparator = new String( new char[ 113 ] ).replace( "\0", "-" );
    System.out.println( lineSeparator );
    StringBuilder sb = new StringBuilder();
    for ( int i = 0; i < Constants.NUMBER_OF_ROWS; i++ )
    {
      sb.append( "| " );
      for ( PeerInformation peer : table[ i ] )
      {
        if ( peer == null )
        {
          sb.append( "null" );
        } else
        {
          if ( Properties.SYSTEM_LOG_LEVEL.contentEquals( "INFO" ) )
          {
            sb.append( String.format( "%4s",
                peer.getIdentifier().substring( 0, i + 1 ) ) );
          } else
          {
            sb.append( String.format( "%4s", peer.getIdentifier() ) );
          }
        }
        sb.append( " | " );
      }
      sb.append( "\n" );
    }
    System.out.print( sb.toString() );
    System.out.println( lineSeparator );
  }

}
