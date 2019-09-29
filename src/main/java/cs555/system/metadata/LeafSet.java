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
   * @param peer
   * @param connection
   * @param cw
   */
  public void setLeaf(PeerInformation peer, TCPConnection connection,
      boolean cw) {
    if ( cw )
    {
      this.cw = new Leaf( peer, connection );
    } else
    {
      this.ccw = new Leaf( peer, connection );
    }
  }

  public boolean isWithinLeafSet(PeerInformation other) {
    int a = Integer.parseInt( self.getIdentifier(), 16 );
    int o = Integer.parseInt( other.getIdentifier(), 16 );
    int cw = Integer.parseInt( this.cw.getPeer().getIdentifier(), 16 );
    int ccw = Integer.parseInt( this.ccw.getPeer().getIdentifier(), 16 );

    boolean between = o > cw ^ o < ccw ^ ccw < cw;

    int o_a = o ^ a;

    int o_cw = o ^ cw;

    int o_ccw = o ^ ccw;
    
    return between;
  }

  public void updateCW(PeerInformation other) {

  }

  public Leaf getCW() {
    return cw;
  }

  public Leaf getCCW() {
    return ccw;
  }

  private static class Leaf {

    private final PeerInformation p;

    private final TCPConnection c;

    private Leaf(PeerInformation p, TCPConnection c) {
      this.p = p;
      this.c = c;
    }

    private PeerInformation getPeer() {
      return p;
    }

    private TCPConnection getTCPConnection() {
      return c;
    }

  }

}
