package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author stock
 *
 */
public class DiscoverNodeResponse implements Event {

  private int type;

  private boolean initialPeerConnection;

  private String host;

  private int port;

  public DiscoverNodeResponse() {
    this.type = Protocol.DISCOVER_NODE_RESPONSE;
    this.initialPeerConnection = true;
  }

  public DiscoverNodeResponse(String host, int port) {
    this.type = Protocol.DISCOVER_NODE_RESPONSE;
    this.initialPeerConnection = false;
    this.host = host;
    this.port = port;
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
      int len = din.readInt();
      byte[] bytes = new byte[ len ];
      din.readFully( bytes );

      this.host = new String( bytes );

      this.port = din.readInt();
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
   * @return if is the initial peer connection, false otherwise
   */
  public boolean isInitialPeerConnection() {
    return this.initialPeerConnection;
  }

  /**
   * 
   * @return the host address from the connection
   */
  public String getHost() {
    return this.host;
  }

  /**
   * 
   * @return the port number from the connection
   */
  public int getPort() {
    return this.port;
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
      byte[] hostBytes = host.getBytes();
      dout.writeInt( hostBytes.length );
      dout.write( hostBytes );

      dout.writeInt( port );
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
