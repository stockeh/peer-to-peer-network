package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import cs555.system.util.Constants;

/**
 * 
 * @author stock
 *
 */
public class DataTransfer implements Event {

  private int type;

  private byte[] data;

  private String descriptor;

  /**
   * Default constructor -
   * 
   */
  public DataTransfer(int type, byte[] data, String fileSystemPath) {
    this.type = type;
    this.data = data;
    this.descriptor = fileSystemPath;
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public DataTransfer(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    if ( din.readBoolean() == Constants.SUCCESS )
    {
      int len = din.readInt();
      this.data = new byte[ len ];
      din.readFully( this.data );
    }

    int len = din.readInt();
    byte[] pathname = new byte[ len ];
    din.readFully( pathname );
    descriptor = new String( pathname );

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
   * @return the data as a {@code byte[]}
   */
  public byte[] getData() {
    return data;
  }

  /**
   * 
   * @return get the file system path or the peer information as a
   *         response
   */
  public String getDescriptor() {
    return descriptor;
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

    if ( data == null )
    {
      dout.writeBoolean( Constants.FAILURE );
    } else
    {
      dout.writeBoolean( Constants.SUCCESS );
      dout.writeInt( data.length );
      dout.write( data );
    }

    byte[] filepath = descriptor.getBytes();
    dout.writeInt( filepath.length );
    dout.write( filepath );

    dout.flush();
    marshalledBytes = outputStream.toByteArray();

    outputStream.close();
    dout.close();
    return marshalledBytes;
  }

  @Override
  public String toString() {
    return Protocol.class.getFields()[ type ].getName().toString();
  }

}
