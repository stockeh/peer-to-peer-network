package cs555.system.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import cs555.system.metadata.PeerInformation;

/**
 * 
 * 
 * @author stock
 *
 */
public class MessageUtilities {

  /**
   * 
   * @param dout
   * @param peer
   * @throws IOException
   */
  public static void writePeerInformation(DataOutputStream dout,
      PeerInformation peer) throws IOException {
    byte[] identifierBytes = peer.getIdentifier().getBytes();
    dout.writeInt( identifierBytes.length );
    dout.write( identifierBytes );

    byte[] hostBytes = peer.getHost().getBytes();
    dout.writeInt( hostBytes.length );
    dout.write( hostBytes );

    dout.writeInt( peer.getPort() );
  }

  /**
   * 
   * @param din
   * @return
   * @throws IOException
   */
  public static PeerInformation readPeerInformation(DataInputStream din)
      throws IOException {
    int len = din.readInt();
    byte[] bytes = new byte[ len ];
    din.readFully( bytes );

    String identifier = new String( bytes );

    len = din.readInt();
    bytes = new byte[ len ];
    din.readFully( bytes );

    String host = new String( bytes );

    int port = din.readInt();

    return new PeerInformation( identifier, host, port );
  }

}
