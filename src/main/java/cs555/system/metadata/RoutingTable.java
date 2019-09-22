package cs555.system.metadata;

import cs555.system.util.Constants;

/**
 * 
 * 
 * @author stock
 *
 */
public class RoutingTable {

  protected final static short NUM_ROWS = Constants.IDENTIFIER_BIT_LENGTH / 4;

  private PeerInformation[][] table;

  /**
   * Default Constructor -
   * 
   */
  protected RoutingTable() {
    this.table = new PeerInformation[ NUM_ROWS ][ 16 ];
  }

  /**
   * 
   * @param row
   */
  public void addTableRow(PeerInformation[] row) {

  }

  /**
   * 
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
    for ( int i = 0; i < NUM_ROWS; i++ )
    {
      for ( PeerInformation peer : table[ i ] )
      {
        if ( peer == null )
        {
          sb.append( "null" );
        } else
        {
          sb.append( String.format( "%4s",
              peer.getIdentifier().substring( 0, i + 1 ) ) );
        }
        sb.append( " | " );
      }
      sb.append( "\n" );
    }
    System.out.println( sb.toString() );
    System.out.println( lineSeparator );
  }

}
