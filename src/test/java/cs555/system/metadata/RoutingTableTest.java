package cs555.system.metadata;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RoutingTableTest {

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
    t.display();

    assertEquals( c, t.closest( metadata.self(),
        new PeerInformation( "BBBB", null, 0 ), 0 ) );

    assertEquals( d, t.closest( metadata.self(),
        new PeerInformation( "2000", null, 0 ), 0 ) );
    
    assertEquals( a, t.closest( metadata.self(),
        new PeerInformation( "6000", null, 0 ), 0 ) );
    
    /* --------------------------------------------*/
    
    metadata = new PeerMetadata( null, 0 );
    a = new PeerInformation( "1254", null, 0 );
    b = new PeerInformation( "1123", null, 0 );

    t = metadata.table();
    t.addPeerToTable( a, 1 );
    t.addPeerToTable( b, 2 );

    metadata.setIdentifier( "1111" );
    metadata.addSelfToTable();
    t.display();

    assertEquals( a, t.closest( metadata.self(),
        new PeerInformation( "1E47", null, 0 ), 1 ) );
    
    /* --------------------------------------------*/
    
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
    t.display();

    assertEquals( k, t.closest( metadata.self(),
        new PeerInformation( "5F0C", null, 0 ), 0 ) );
    
    /**
             
    cs555.system.node.Peer(updateRoutingTable:523) [INFO] - Updating Routing Table with 797A
    -----------------------------------------------------------------------------------------------------------------
    | null | null |    2 |    3 |    4 | null | null |    7 |    8 |    9 |    A | null | null |    D | null |    F | 
    | null |   41 |   42 | null | null | null | null | null | null | null | null |   4B |   4C | null | null | null | 
    | null | null | null | null |  414 | null | null | null | null | null | null | null | null | null | null | null | 
    | null | null | 4142 | null | null | null | null | null | null | null | null | null | null | null | null | null | 
    -----------------------------------------------------------------------------------------------------------------
    cs555.system.transport.TCPReceiverThread(run:74) [DEBUG] - Closing connection... java.io.EOFException
    cs555.system.node.Peer(onEvent:255) [DEBUG] - DISCOVER_PEER_REQUEST | Content Identifier: 5F0C | Hop: 0 | Next Node: 
    cs555.system.node.Peer(lookup:430) [INFO] - DISCOVER_PEER_REQUEST | Content Identifier: 5F0C | Hop: 1 | Next Node: 797A

    >   03A3 | mars:38507
    >   26B4 | blowfish:35659
    >   3065 | eel:41849
    >   4142 | brill:35339
    >   4259 | annapolis:38971
    >   4BE7 | lansing:43237
    >   4CFA | saturn:46053
    >   709F | olympia:46127
    >   7715 | harrisburg:44863
    >   797A | dover:33755
    >   7D3F | montpelier:39415
    >   812B | earth:42269
    >   8B41 | lincoln:39115
    >   8B6C | little-rock:36443
    >   9311 | phoenix:45761
    >   9EEE | hartford:40771
    >   A259 | lamborghini:46587
    >   A2D1 | juneau:37533
    >   AB55 | salem:35137
    >   ACC0 | boise:42289
    >   C3DE | nashville:46719
    >   D1A9 | denver:36485
    >   FF41 | neptune:42225

     */
    
  }  
}
