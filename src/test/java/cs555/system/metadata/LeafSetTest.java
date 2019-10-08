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
  public void testGetClosest() {
    PeerInformation self;

    self = new PeerInformation( "5555", null, 0 );
    LeafSet set = new LeafSet( self );

    PeerInformation cw = new PeerInformation( "1111", null, 0 );
    set.setLeaf( cw, true );

    PeerInformation ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "7777" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "4444" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "1111" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "FFFF" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "3333" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "2222" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "8888", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "BBBB", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "6666" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "BBBB" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "7777" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "1111" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "CCCC" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "9999" ) == null );
    assertTrue( set.getClosestLeaf( "AAAA" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "0001", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "5555", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "0000" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "FFFF" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAAB" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "4444" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "1111" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAA9" ) == null );
    assertTrue( set.getClosestLeaf( "6666" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "FFFF", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "5555", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "0000" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "FFFE" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAAB" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "4444" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "1111" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAA9" ) == null );
    assertTrue( set.getClosestLeaf( "6666" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "FFFF", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "FFFE" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "FF00" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "5500" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "3334" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "3300" ) == null );
    assertTrue( set.getClosestLeaf( "0000" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "0000", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "FFFE" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "FF00" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "5500" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "3334" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "3300" ) == null );
    assertTrue( set.getClosestLeaf( "0001" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "FFFF", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "0000" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "5500" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAA0" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "AAAB" ) == null );
    assertTrue( set.getClosestLeaf( "FFFE" ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "0000", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "0001" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "5500" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "5555" ).equals( self ) );
    assertTrue( set.getClosestLeaf( "AAA0" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "AAAB" ) == null );
    assertTrue( set.getClosestLeaf( "FFFF" ) == null );

    /** -------------------------------------- */

    // Before: { EC52 <- F414 -> 0685 }

    // After : { EC52 <- 0685 -> 0685 }
    self = new PeerInformation( "0685", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "0685", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "EC52", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "F4F6" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "0500" ).equals( cw ) );
    assertTrue( set.getClosestLeaf( "1111" ) == null );

    /** -------------------------------------- */
    // Before: { EC52 <- F414 -> 0685 }

    // After : { EC52 <- EC52 -> 0685 }
    self = new PeerInformation( "EC52", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "0685", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "EC52", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "F414" ).equals( ccw ) );
    assertTrue( set.getClosestLeaf( "0500" ).equals( cw ) );
    
    /** -------------------------------------- */

    self = new PeerInformation( "1B3C", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "8153", null, 0 );
    set.setLeaf( cw, true );

    ccw = new PeerInformation( "C99C", null, 0 );
    set.setLeaf( ccw, false );

    assertTrue( set.getClosestLeaf( "B6DB" ) == null );
  }
}
