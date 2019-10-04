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
  }
}
