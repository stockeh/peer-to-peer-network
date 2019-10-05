package cs555.system.metadata;

/**
 * Contains information relating to the leaf set for some peer.
 * 
 * <p>
 * The leaf set contains {@code l} peers in the clockwise direction
 * and {@code l} peers in the counter-clockwise direction.
 * </p>
 * 
 * @author stock
 *
 */
public class LeafSet {

  private final PeerInformation self;

  private PeerInformation cw;

  private PeerInformation ccw;

  /**
   * Default constructor -
   * 
   * @param self
   */
  public LeafSet(PeerInformation self) {
    this.self = self;
  }

  /**
   * 
   * @return true if {@code cw} and {@code ccw} are non {@code null},
   *         false otherwise.
   */
  public boolean isPopulated() {
    return cw != null && ccw != null;
  }

  /**
   * Set the peer with either the clockwise or counter clockwise peer.
   * 
   * @param peer
   * @param cw true to update the clockwise peer, false otherwise
   */
  public void setLeaf(PeerInformation peer, boolean cw) {
    if ( cw )
    {
      this.cw = peer;
    } else
    {
      this.ccw = peer;
    }
  }

  public PeerInformation getCW() {
    return cw;
  }

  public PeerInformation getCCW() {
    return ccw;
  }

  /**
   * 
   * @param other
   * @return
   */
  public boolean isBetweenClockwise(PeerInformation other) {
    int o = Integer.parseInt( other.getIdentifier(), 16 );
    int s = Integer.parseInt( self.getIdentifier(), 16 );
    int cw = Integer.parseInt( this.cw.getIdentifier(), 16 );

    if ( isBetween( o, cw, s ) )
    {
      return true;
    } else
    {
      return false;
    }
  }

  /**
   * Check if an integer falls within two end points on a circular ring
   * of clockwise increasing values.
   * 
   * @param o 'other' item
   * @param a most clockwise item
   * @param b less clockwise item
   * @return true if {@code o} is between {@code a} and {@code b}
   */
  public boolean isBetween(int o, int a, int b) {
    return o > a ^ o < b ^ b < a;
  }

  /**
   * Check if the {@code other} peer falls within this leaf set, and
   * returns the {@code Leaf} that is closest by identifier to
   * {@code other}.
   * <p>
   * <b>IMPORTANT:</b> assumes each of the peer identifiers are 16-bits.
   * </p>
   *
   * @param other peer identifier to check if within bounds
   * @return the {@code Leaf} that is closest by identifier to
   *         {@code other}, <b>or</b> {@code null} if {@code other}
   *         falls outside the leaf set boundaries.
   */
  public PeerInformation getClosestLeaf(String otherIdentifier) {

    if ( isPopulated() )
    {
      int o = Integer.parseInt( otherIdentifier, 16 );
      int s = Integer.parseInt( self.getIdentifier(), 16 );
      int cw = Integer.parseInt( this.cw.getIdentifier(), 16 );
      int ccw = Integer.parseInt( this.ccw.getIdentifier(), 16 );

      if ( isBetween( o, cw, s ) )
      {
        int o_cw = ( cw - o ) & 0xFFFF;
        int o_s = ( o - s ) & 0xFFFF;
        return o_cw < o_s ? this.cw : self;
      } else if ( isBetween( o, s, ccw ) )
      {
        int o_ccw = ( o - ccw ) & 0xFFFF;
        int o_s = ( s - o ) & 0xFFFF;
        return o_ccw < o_s ? this.ccw : self;
      }
    } else
    {
      return self;
    }
    return null;
  }

  /**
   * Convert the leaf set to a {@code String}
   * 
   */
  public String toString() {
    return ( new StringBuilder( "Updated Leaf Set: { " ) )
        .append( ccw.getIdentifier() ).append( " ccw <- " )
        .append( self.getIdentifier() ).append( " -> cw " )
        .append( cw.getIdentifier() ).append( " }" ).toString();
  }

}
