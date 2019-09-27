package cs555.system.metadata;

import cs555.system.util.Constants;

/**
 * 
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
   */
  public PeerInformation[][] getTable() {
    return table;
  }

  /**
   * 
   * @param row
   */
  public void setTable(PeerInformation[][] table) {
    this.table = table;
  }

  /**
   * 
   * @param index
   * @return
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
   * Print the DHT to the console in a readable format
   * 
   */
  public void display() {
    String lineSeparator = new String( new char[ 110 ] ).replace( "\0", "-" );
    System.out.println( "\n" + lineSeparator );
    StringBuilder sb = new StringBuilder();
    for ( int i = 0; i < Constants.NUMBER_OF_ROWS; i++ )
    {
      for ( PeerInformation peer : table[ i ] )
      {
        if ( peer == null )
        {
          sb.append( "null" );
        } else
        {
          // sb.append( String.format( "%4s",
          // peer.getIdentifier().substring( 0, i + 1 ) ) );
          sb.append( String.format( "%4s", peer.getIdentifier() ) );
        }
        sb.append( " | " );
      }
      sb.append( "\n" );
    }
    System.out.print( sb.toString() );
    System.out.println( lineSeparator );
  }

}
