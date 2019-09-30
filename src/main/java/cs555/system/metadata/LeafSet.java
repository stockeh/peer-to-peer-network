package cs555.system.metadata;

import cs555.system.transport.TCPConnection;

/**
 * 
 * @author stock
 *
 */
public class LeafSet {

  private final PeerInformation self;

  private Leaf cw;

  private Leaf ccw;

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
   * 
   * @param peer
   * @param connection
   * @param cw
   */
  public void setLeaf(PeerInformation peer, TCPConnection connection,
      boolean cw) {
    if ( cw )
    {
      if ( this.cw != null )
      {
        this.cw.getTCPConnection().close();
      }
      this.cw = new Leaf( peer, connection );
    } else
    {
      if ( this.ccw != null )
      {
        this.ccw.getTCPConnection().close();
      }
      this.ccw = new Leaf( peer, connection );
    }
  }

  /**
   * 
   * 
   * @param o
   * @param a
   * @param b
   * @return
   */
  public boolean isBetween(int o, int a, int b) {
    return o > a ^ o < b ^ b < a;
  }

  /**
   * 
   * @param other
   * @return
   */
  public Leaf getClosest(PeerInformation other) {
    int o = Integer.parseInt( other.getIdentifier(), 16 );
    int s = Integer.parseInt( self.getIdentifier(), 16 );
    int cw = Integer.parseInt( this.cw.getPeer().getIdentifier(), 16 );
    int ccw = Integer.parseInt( this.ccw.getPeer().getIdentifier(), 16 );

    if ( isBetween( o, cw, s ) )
    {
      int o_cw = o > cw ? Math.floorMod( ( cw - o ), 16 ) : o - cw;
      int o_s = o < s ? Math.floorMod( ( s - o ), 16 ) : o - s;
      return o_cw < o_s ? this.cw : new Leaf( self, null );
    } else if ( isBetween( o, s, ccw ) )
    {
      int o_ccw = o < ccw ? Math.floorMod( ( o - ccw ), 16 ) : o - ccw;
      int o_s = o > s ? Math.floorMod( ( o - s ), 16 ) : s - o;
      return o_ccw < o_s ? this.ccw : new Leaf( self, null );
    }
    return null;
  }

  public Leaf getCW() {
    return cw;
  }

  public Leaf getCCW() {
    return ccw;
  }

  /**
   * Convert the leaf set to a {@code String}
   * 
   */
  public String toString() {
    return ( new StringBuilder( "Updated Leaf Set: { " ) )
        .append( cw.getPeer().getIdentifier() ).append( " <- " )
        .append( self.getIdentifier() ).append( " -> " )
        .append( ccw.getPeer().getIdentifier() ).append( " }" ).toString();
  }

  protected static class Leaf {

    private final PeerInformation p;

    private final TCPConnection c;

    private Leaf(PeerInformation p, TCPConnection c) {
      this.p = p;
      this.c = c;
    }

    protected PeerInformation getPeer() {
      return p;
    }

    protected TCPConnection getTCPConnection() {
      return c;
    }

  }

}
