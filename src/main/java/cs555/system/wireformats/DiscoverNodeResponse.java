package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import cs555.system.metadata.PeerInformation;
import cs555.system.util.MessageUtilities;

/**
 * 
 * @author stock
 *
 */
public class DiscoverNodeResponse implements Event {

  private int type;

  private boolean initialPeerConnection;

  private PeerInformation source;

  public DiscoverNodeResponse() {
    this.type = Protocol.DISCOVER_NODE_RESPONSE;
    this.initialPeerConnection = true;
  }

  public DiscoverNodeResponse(PeerInformation source) {
    this.type = Protocol.DISCOVER_NODE_RESPONSE;
    this.initialPeerConnection = false;
    this.source = source;
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public DiscoverNodeResponse(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    this.initialPeerConnection = din.readBoolean();

    if ( !initialPeerConnection )
    {
      this.source = MessageUtilities.readPeerInformation( din );
    }
    inputStream.close();
    din.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getType() {
    return type;
  }

  /**
   * 
   * @return the peer that will be an entry point to the network
   */
  public PeerInformation getSourceInformation() {
    return this.source;
  }

  /**
   * 
   * @return if is the initial peer connection, false otherwise
   */
  public boolean isInitialPeerConnection() {
    return this.initialPeerConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getBytes() throws IOException {
    byte[] marshalledBytes = null;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DataOutputStream dout =
        new DataOutputStream( new BufferedOutputStream( outputStream ) );

    dout.writeInt( type );

    dout.writeBoolean( initialPeerConnection );

    if ( !initialPeerConnection )
    {
      MessageUtilities.writePeerInformation( dout, source );
    }
    dout.flush();
    marshalledBytes = outputStream.toByteArray();

    outputStream.close();
    dout.close();
    return marshalledBytes;
  }

  @Override
  public String toString() {
    return "\n" + type;
  }

}
