package cs555.system.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LeafSetTest {

  /**
   * if ( cw > ccw ) { if ( cw > o && o > ccw ) { return true; } } else
   * { if ( !( ccw > o && o > cw ) ) { return true; } } return false;
   */
  @Test
  public void testIsWithinLeafSet() {
    PeerInformation self = new PeerInformation( "5", null, 0 );
    LeafSet set = new LeafSet( self );

    set.setLeaf( new PeerInformation( "1", null, 0 ), null, true );
    set.setLeaf( new PeerInformation( "3", null, 0 ), null, false );

    PeerInformation other = new PeerInformation( "7", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "4", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "1", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "3", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "2", null, 0 );
    assertFalse( set.isWithinLeafSet( other ) );

    set.setLeaf( new PeerInformation( "7", null, 0 ), null, true );
    set.setLeaf( new PeerInformation( "9", null, 0 ), null, false );

    other = new PeerInformation( "6", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "9", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "7", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "1", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "10", null, 0 );
    assertTrue( set.isWithinLeafSet( other ) );
    other = new PeerInformation( "8", null, 0 );
    assertFalse( set.isWithinLeafSet( other ) );
  }

}
