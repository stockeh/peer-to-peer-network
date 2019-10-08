package cs555.system.metadata;

import cs555.system.util.Constants;

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
   * Get the closest peer to the destination
   * 
   * @param self
   * @param destination
   * @return
   */
  public PeerInformation closest(PeerInformation self,
      PeerInformation destination) {

    int row = 0;
    int destCol =
        Character.digit( destination.getIdentifier().charAt( row ), 16 );

    int dest = Integer.parseInt( destination.getIdentifier(), 16 );
    int diff = Integer.MAX_VALUE, other, temp_diff;
    PeerInformation closest = null, temp;

    boolean direction = false;
    if ( ( temp = this.getTableIndex( row, destCol ) ) != null )
    {
      other = Integer.parseInt( temp.getIdentifier(), 16 );
      diff = Math.abs( other - dest );
      closest = temp;
      if ( dest > other )
      {
        direction = Constants.COUNTER_CLOCKWISE;
      } else
      {
        direction = Constants.CLOCKWISE;
      }
    }

    boolean first = true;
    for ( int i = 1; i < 16; ++i )
    {
      // clockwise
      int col = ( destCol + i ) & 0xF;
      temp = this.getTableIndex( row, col );
      if ( temp != null )
      {
        other = Integer.parseInt( temp.getIdentifier(), 16 );
        temp_diff = ( other - dest ) & 0xFFFF;
        if ( temp_diff < diff )
        {
          diff = temp_diff;
          closest = temp;
        }
        if ( temp.equals( self ) && first )
        {
          first = false;
          direction = Constants.CLOCKWISE;
        }
      }
      // counter-clockwise
      col = ( destCol - i ) & 0xF;
      temp = this.getTableIndex( row, col );
      if ( temp != null )
      {
        other = Integer.parseInt( temp.getIdentifier(), 16 );
        temp_diff = ( dest - other ) & 0xFFFF;
        if ( temp_diff < diff )
        {
          diff = temp_diff;
          closest = temp;
        }
        if ( temp.equals( self ) && first )
        {
          first = false;
          direction = Constants.COUNTER_CLOCKWISE;
        }
      }
    }
    for ( int r = row + 1; r < Constants.NUMBER_OF_ROWS; ++r )
    {
      for ( int col = 0; col < Constants.IDENTIFIER_BIT_LENGTH; ++col )
      {
        temp = this.getTableIndex( r, col );
        if ( temp != null )
        {
          other = Integer.parseInt( temp.getIdentifier(), 16 );
          temp_diff =
              direction == Constants.CLOCKWISE ? ( other - dest ) & 0xFFFF
                  : ( dest - other ) & 0xFFFF;
          if ( temp_diff < diff )
          {
            diff = temp_diff;
            closest = temp;
          }
        }
      }
    }
    return closest;
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
