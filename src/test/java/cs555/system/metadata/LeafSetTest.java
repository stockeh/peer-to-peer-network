package cs555.system.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LeafSetTest {


  @Test
  public void testIsBetween() {
    LeafSet set = new LeafSet( null );
    int a = 1;
    int b = 5;
    assertTrue( set.isBetween( 15, a, b ) );
    assertTrue( set.isBetween( 7, a, b ) );
    assertTrue( set.isBetween( 5, a, b ) );
    assertTrue( set.isBetween( 1, a, b ) );
    assertFalse( set.isBetween( 2, a, b ) );
    assertFalse( set.isBetween( 3, a, b ) );
    assertFalse( set.isBetween( 4, a, b ) );

    a = 5;
    b = 3;
    assertTrue( set.isBetween( 3, a, b ) );
    assertTrue( set.isBetween( 4, a, b ) );
    assertTrue( set.isBetween( 5, a, b ) );
    assertFalse( set.isBetween( 1, a, b ) );
    assertFalse( set.isBetween( 2, a, b ) );
    assertFalse( set.isBetween( 6, a, b ) );
    assertFalse( set.isBetween( 10, a, b ) );
  }

  @Test
  public void testIsWithin() {
    PeerInformation other, self;

    self = new PeerInformation( "5", null, 0 );
    LeafSet set = new LeafSet( self );

    PeerInformation cw = new PeerInformation( "1", null, 0 );
    set.setLeaf( cw, null, true );

    PeerInformation ccw = new PeerInformation( "3", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "7", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( self ) );
    other = new PeerInformation( "4", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( self ) );
    other = new PeerInformation( "1", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "F", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "3", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "2", null, 0 );
    assertTrue( set.getClosest( other ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "7", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "A", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "6", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "A", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "7", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "1", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( self ) );
    other = new PeerInformation( "B", null, 0 );
    assertTrue( set.getClosest( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "8", null, 0 );
    assertTrue( set.getClosest( other ) == null );
    other = new PeerInformation( "9", null, 0 );
    assertTrue( set.getClosest( other ) == null );
  }
}
