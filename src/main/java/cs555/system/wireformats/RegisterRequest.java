package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
public class RegisterRequest implements Event {

  private int type;

  private String identifier;

  private String host;

  private int port;

  /**
   * Default constructor - create a new register or unregister message.
   * 
   * @param type Specified for use of register or unregister message.
   * @param identifier
   * @param host
   * @param port
   */
  public RegisterRequest(int type, String identifier, String host, int port) {
    this.type = type;
    this.identifier = identifier;
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
  public RegisterRequest(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    int len = din.readInt();
    byte[] bytes = new byte[ len ];
    din.readFully( bytes );

    this.identifier = new String( bytes );

    len = din.readInt();
    bytes = new byte[ len ];
    din.readFully( bytes );

    this.host = new String( bytes );

    this.port = din.readInt();

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
   * Converts the IP Address and Port to a readable format.
   * 
   * @return Returns a string in the format <code>host:port</code>
   */
  public String getConnection() {
    return this.host + ":" + this.port;
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
   * Retrieve the identifier for the discovery message.
   * 
   * @return a <tt>String</tt> of the unique identifier
   */
  public String getIdentifier() {
    return this.identifier;
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

    byte[] identifierBytes = identifier.getBytes();
    dout.writeInt( identifierBytes.length );
    dout.write( identifierBytes );

    byte[] hostBytes = host.getBytes();
    dout.writeInt( hostBytes.length );
    dout.write( hostBytes );

    dout.writeInt( port );

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
    return type + " " + this.getConnection();
  }
}
