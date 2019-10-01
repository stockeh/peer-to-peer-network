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
    PeerInformation other, self;

    self = new PeerInformation( "5555", null, 0 );
    LeafSet set = new LeafSet( self );

    PeerInformation cw = new PeerInformation( "1111", null, 0 );
    set.setLeaf( cw, null, true );

    PeerInformation ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "7777", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "4444", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "1111", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "FFFF", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "3333", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "2222", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );

    /** -------------------------------------- */
    
     self = new PeerInformation( "5555", null, 0 );
     set = new LeafSet( self );
    
     cw = new PeerInformation( "8888", null, 0 );
     set.setLeaf( cw, null, true );
    
     ccw = new PeerInformation( "BBBB", null, 0 );
     set.setLeaf( ccw, null, false );
    
     other = new PeerInformation( "6666", null, 0 );
     assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
     other = new PeerInformation( "BBBB", null, 0 );
     assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
     other = new PeerInformation( "7777", null, 0 );
     assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
     other = new PeerInformation( "1111", null, 0 );
     assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
     other = new PeerInformation( "CCCC", null, 0 );
     assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
     other = new PeerInformation( "9999", null, 0 );
     assertTrue( set.getClosestLeaf( other ) == null );
     other = new PeerInformation( "AAAA", null, 0 );
     assertTrue( set.getClosestLeaf( other ) == null );

    /** -------------------------------------- */

    self = new PeerInformation( "0001", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "5555", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "0000", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "FFFF", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAAB", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "4444", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "1111", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAA9", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "6666", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    
    /** -------------------------------------- */

    self = new PeerInformation( "FFFF", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "5555", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "0000", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "FFFE", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAAB", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "4444", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "1111", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAA9", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "6666", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    
    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "FFFF", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "FFFE", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "FF00", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "5500", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "3334", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "3300", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "0000", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    
    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "0000", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "3333", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "FFFE", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "FF00", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "5500", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "3334", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "3300", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "0001", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    
    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "FFFF", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "0000", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "5500", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAA0", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "AAAB", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "FFFE", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    
    /** -------------------------------------- */

    self = new PeerInformation( "5555", null, 0 );
    set = new LeafSet( self );

    cw = new PeerInformation( "AAAA", null, 0 );
    set.setLeaf( cw, null, true );

    ccw = new PeerInformation( "0000", null, 0 );
    set.setLeaf( ccw, null, false );

    other = new PeerInformation( "0001", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( ccw ) );
    other = new PeerInformation( "5500", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "5555", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( self ) );
    other = new PeerInformation( "AAA0", null, 0 );
    assertTrue( set.getClosestLeaf( other ).getPeer().equals( cw ) );
    other = new PeerInformation( "AAAB", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
    other = new PeerInformation( "FFFF", null, 0 );
    assertTrue( set.getClosestLeaf( other ) == null );
  }
}
