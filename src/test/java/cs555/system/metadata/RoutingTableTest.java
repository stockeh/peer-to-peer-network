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
  }  
  /**
   * 
-----------------------------------------------------------------------------------------------------------------
| 0010 | 1111 | 28AA | 3CCD | 4000 | 5591 | 6251 | 7BCA | 8E13 | 9999 | AAAA | BBBB | CCCF | DFFD | EBB9 | null | 
| null | 1111 | 1254 | null | null | null | null | null | null | null | null | null | null | null | null | null | 
| null | 1111 | 1123 | null | null | null | null | null | null | null | null | null | null | null | null | null | 
| null | 1111 | null | null | null | null | null | null | null | null | null | null | null | null | null | null | 
-----------------------------------------------------------------------------------------------------------------
cs555.system.transport.TCPReceiverThread(run:74) [DEBUG] - Closing connection... java.io.EOFException
cs555.system.node.Peer(onEvent:254) [DEBUG] - JOIN_NETWORK_REQUEST | New Node: 1E47 | Hop: 0 | Next Node: 
cs555.system.node.Peer(constructDHT:569) [DEBUG] - Current row for peer ( 1E47 | neptune:36089 ) is: 1
cs555.system.node.Peer(constructDHT:620) [DEBUG] - Found closest node and responding to destination.
cs555.system.node.Peer(constructDHT:674) [INFO] - JOIN_NETWORK_REQUEST | New Node: 1E47 | Hop: 1 | Next Node: 1E47
   */
}
