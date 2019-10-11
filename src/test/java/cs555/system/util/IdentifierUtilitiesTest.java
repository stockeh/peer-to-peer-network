package cs555.system.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import cs555.system.metadata.PeerInformation;
import cs555.system.metadata.PeerMetadata;
import cs555.system.metadata.RoutingTable;

public class IdentifierUtilitiesTest {

  @Test
  public void testConvertBytesToHex() {
    byte[] buf = new byte[ 2 ];
    buf[ 0 ] = ( byte ) 0;
    buf[ 1 ] = ( byte ) 1;

    String s = IdentifierUtilities.convertBytesToHex( buf );

    assertEquals( "0001", s );
  }

  @Test
  public void testConvertHexToBytes() {
    String s = "0001";
    byte[] buf = IdentifierUtilities.convertHexToBytes( s );

    byte[] expected = new byte[ 2 ];
    expected[ 0 ] = ( byte ) 0;
    expected[ 1 ] = ( byte ) 1;

    assertEquals( expected.length, buf.length );
    assertArrayEquals( expected, buf );
  }

  @Test
  public void testLongestCommonPrefixLength() {
    assertEquals( 4,
        IdentifierUtilities.longestCommonPrefixLength( "AAAA", "AAAA" ) );
    assertEquals( 3,
        IdentifierUtilities.longestCommonPrefixLength( "AAAA", "AAAB" ) );
    assertEquals( 2,
        IdentifierUtilities.longestCommonPrefixLength( "AAAA", "AABB" ) );
    assertEquals( 1,
        IdentifierUtilities.longestCommonPrefixLength( "AAAA", "ABBB" ) );
    assertEquals( 0,
        IdentifierUtilities.longestCommonPrefixLength( "AAAA", "BBBB" ) );
  }

  @Test
  public void testIdentifierCompare() {
    String l = "ABCD", r = "ABCC";
    assertEquals( 1, l.compareTo( r ) );

    r = "ABCE";
    assertEquals( -1, l.compareTo( r ) );

    r = "ABCD";
    assertEquals( 0, l.compareTo( r ) );
  }

  @Test
  public void testClosest() {

    PeerMetadata metadata = new PeerMetadata( null, 0 );
    PeerInformation a = new PeerInformation( "5678", null, 0 );
    PeerInformation b = new PeerInformation( "3456", null, 0 );
    PeerInformation c = new PeerInformation( "1200", null, 0 );
    PeerInformation d = new PeerInformation( "1237", null, 0 );
    PeerInformation e = new PeerInformation( "1231", null, 0 );

    RoutingTable t = metadata.table();
    t.addPeerToTable( a, 0 );
    t.addPeerToTable( b, 0 );
    t.addPeerToTable( c, 2 );
    t.addPeerToTable( d, 3 );
    t.addPeerToTable( e, 3 );

    metadata.setIdentifier( "1234" );
    metadata.addSelfToTable();
    // t.display();

    assertEquals( c, IdentifierUtilities.closest( metadata,
        new PeerInformation( "BBBB", null, 0 ) ) );

    assertEquals( d, IdentifierUtilities.closest( metadata,
        new PeerInformation( "2000", null, 0 ) ) );

    assertEquals( a, IdentifierUtilities.closest( metadata,
        new PeerInformation( "6000", null, 0 ) ) );

    /* -------------------------------------------- */

    metadata = new PeerMetadata( null, 0 );
    a = new PeerInformation( "1254", null, 0 );
    b = new PeerInformation( "1123", null, 0 );

    t = metadata.table();
    t.addPeerToTable( a, 1 );
    t.addPeerToTable( b, 2 );

    metadata.setIdentifier( "1111" );
    metadata.addSelfToTable();
    // t.display();

    assertEquals( a, IdentifierUtilities.closest( metadata,
        new PeerInformation( "1E47", null, 0 ) ) );

    /* -------------------------------------------- */

    metadata = new PeerMetadata( null, 0 );
    a = new PeerInformation( "2222", null, 0 );
    b = new PeerInformation( "3333", null, 0 );
    c = new PeerInformation( "797A", null, 0 );
    d = new PeerInformation( "8888", null, 0 );
    e = new PeerInformation( "9999", null, 0 );
    PeerInformation f = new PeerInformation( "AAAA", null, 0 );
    PeerInformation g = new PeerInformation( "DDDD", null, 0 );
    PeerInformation h = new PeerInformation( "FFFF", null, 0 );
    PeerInformation i = new PeerInformation( "4259", null, 0 );
    PeerInformation j = new PeerInformation( "4BE7", null, 0 );
    PeerInformation k = new PeerInformation( "4CFA", null, 0 );

    t = metadata.table();
    t.addPeerToTable( a, 0 );
    t.addPeerToTable( b, 0 );
    t.addPeerToTable( c, 0 );
    t.addPeerToTable( d, 0 );
    t.addPeerToTable( e, 0 );
    t.addPeerToTable( f, 0 );
    t.addPeerToTable( g, 0 );
    t.addPeerToTable( h, 0 );
    t.addPeerToTable( i, 1 );
    t.addPeerToTable( j, 1 );
    t.addPeerToTable( k, 1 );

    metadata.setIdentifier( "4142" );
    metadata.addSelfToTable();
    // t.display();

    assertEquals( k, IdentifierUtilities.closest( metadata,
        new PeerInformation( "5F0C", null, 0 ) ) );

    /* -------------------------------------------- */

    metadata = new PeerMetadata( null, 0 );
    a = new PeerInformation( "1254", null, 0 );
    b = new PeerInformation( "112A", null, 0 );
    c = new PeerInformation( "1123", null, 0 );

    t = metadata.table();
    t.addPeerToTable( a, 1 );
    t.addPeerToTable( b, 2 );

    metadata.leaf().setLeaf( c, Constants.CLOCKWISE );
    metadata.leaf().setLeaf( a, Constants.COUNTER_CLOCKWISE );

    metadata.setIdentifier( "1111" );
    metadata.addSelfToTable();
    // t.display();
    System.out.println( metadata.leaf().toString() );

    assertEquals( a, IdentifierUtilities.closest( metadata,
        new PeerInformation( "1254", null, 0 ) ) );

    assertEquals( a, IdentifierUtilities.closest( metadata,
        new PeerInformation( "1253", null, 0 ) ) );

    assertEquals( c, IdentifierUtilities.closest( metadata,
        new PeerInformation( "1124", null, 0 ) ) );

    assertEquals( b, IdentifierUtilities.closest( metadata,
        new PeerInformation( "112D", null, 0 ) ) );
    
    /* -------------------------------------------- */

    metadata = new PeerMetadata( null, 0 );
    a = new PeerInformation( "3CEC", null, 0 );
    b = new PeerInformation( "43A9", null, 0 );
    c = new PeerInformation( "555B", null, 0 );
    d = new PeerInformation( "6ACC", null, 0 );
    e = new PeerInformation( "76AD", null, 0 );
    f = new PeerInformation( "AEDC", null, 0 );
    g = new PeerInformation( "CEE7", null, 0 );
    h = new PeerInformation( "D294", null, 0 );
    i = new PeerInformation( "D8FE", null, 0 );
    j = new PeerInformation( "DA59", null, 0 );
    k = new PeerInformation( "DBBC", null, 0 );
    PeerInformation l = new PeerInformation( "DC01", null, 0 );

    t = metadata.table();
    t.addPeerToTable( a, 0 );
    t.addPeerToTable( b, 0 );
    t.addPeerToTable( c, 0 );
    t.addPeerToTable( d, 0 );
    t.addPeerToTable( e, 0 );
    t.addPeerToTable( f, 0 );
    t.addPeerToTable( g, 0 );
    t.addPeerToTable( h, 1 );
    t.addPeerToTable( i, 1 );
    t.addPeerToTable( j, 1 );
    t.addPeerToTable( k, 1 );
    t.addPeerToTable( l, 1 );

    metadata.leaf().setLeaf( h, Constants.CLOCKWISE );
    metadata.leaf().setLeaf( g, Constants.COUNTER_CLOCKWISE );

    metadata.setIdentifier( "D161" );
    metadata.addSelfToTable();
//     t.display();
    System.out.println( metadata.leaf().toString() );

    assertEquals( i, IdentifierUtilities.closest( metadata,
        new PeerInformation( "D825", null, 0 ) ) );
    
//    -----------------------------------------------------------------------------------------------------------------
//    | ---- | ---- | ---- | 3CEC | 43A9 | 555B | 6ACC | 76AD | ---- | ---- | AEDC | ---- | CEE7 | D161 | ---- | ---- | 
//    | ---- | D161 | D294 | ---- | ---- | ---- | ---- | ---- | D8FE | ---- | DA59 | DBBC | DC01 | ---- | ---- | ---- | 
//    | ---- | ---- | ---- | ---- | ---- | ---- | D161 | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | 
//    | ---- | D161 | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | 
//    -----------------------------------------------------------------------------------------------------------------
//    cs555.system.node.Peer(interact:168) [INFO] - Updated Leaf Set: { CEE7 ccw <- D161 -> cw D294 }
//    cs555.system.node.Store(onEvent:322) [DEBUG] - DISCOVER_PEER_REQUEST | Content Identifier: D825 | Hop: 3 | Next Node: 
//    cs555.system.node.Store(transferData:433) [INFO] - Network Route Trace: -> CE00 -> D161 -> D294 -> D8FE

  }

}
