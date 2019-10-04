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
 * Register message type to initialize itself with another node.
 * 
 * This is a reusable class for registering, and unregistering chunk
 * servers with the discovery. As well as connecting messaging nodes
 * to other chunk servers to construct the overlay.
 * 
 * @author stock
 *
 */
public class GenericPeerMessage implements Event {

  private int type;

  private PeerInformation peer;

  private boolean flag;

  /**
   * Default constructor - create a new register or unregister message.
   * 
   * @param type Specified for use of register or unregister message.
   * @param peer
   * @param flag
   */
  public GenericPeerMessage(int type, PeerInformation peer, boolean flag) {
    this.type = type;
    this.peer = peer;
    this.flag = flag;
  }
  
  /**
   * Default constructor - create a new register or unregister message.
   * 
   * @param type Specified for use of register or unregister message.
   * @param peer
   */
  public GenericPeerMessage(int type, PeerInformation peer) {
    this.type = type;
    this.peer = peer;
    this.flag = false;
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public GenericPeerMessage(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    this.peer = MessageUtilities.readPeerInformation( din );

    this.flag = din.readBoolean();

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
   * @return the peer from the connection
   */
  public PeerInformation getPeer() {
    return peer;
  }

  /**
   * 
   * @return the boolean flag for the message
   */
  public boolean getFlag() {
    return flag;
  }

  /**
   * 
   * @return the host address from the connection
   */
  public String getHost() {
    return peer.getHost();
  }

  /**
   * 
   * @return the port number from the connection
   */
  public int getPort() {
    return peer.getPort();
  }

  /**
   * Retrieve the identifier for the discovery message.
   * 
   * @return a <tt>String</tt> of the unique identifier
   */
  public String getIdentifier() {
    return peer.getIdentifier();
  }

  /**
   * Set the flag for the message
   * 
   * @param flag
   */
  public void setFlag(boolean flag) {
    this.flag = flag;
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

    MessageUtilities.writePeerInformation( dout, peer );

    dout.writeBoolean( flag );

    dout.flush();
    marshalledBytes = outputStream.toByteArray();

    outputStream.close();
    dout.close();
    return marshalledBytes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Protocol.class.getFields()[ type ].getName().toString() + " | "
        + peer.toString();
  }
}
