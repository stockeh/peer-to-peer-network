package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import cs555.system.metadata.PeerInformation;
import cs555.system.util.MessageUtilities;

/**
 * 
 * @author stock
 *
 */
public class DiscoverPeerRequest implements Event {

  private int type;

  private int row;

  private PeerInformation destination;

  private List<String> networkTraceIdentifiers;

  /**
   * Default constructor -
   * 
   */
  public DiscoverPeerRequest(PeerInformation destination) {
    this.type = Protocol.DISCOVER_PEER_REQUEST;
    this.row = 0;
    this.destination = destination;
    this.networkTraceIdentifiers = new ArrayList<>();
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public DiscoverPeerRequest(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    this.row = din.readInt();

    this.destination = MessageUtilities.readPeerInformation( din );

    short len = din.readShort();
    this.networkTraceIdentifiers = new ArrayList<>( len );
    int identifierLength;
    byte[] identifier;
    for ( int i = 0; i < len; ++i )
    {
      identifierLength = din.readInt();
      identifier = new byte[ identifierLength ];
      din.readFully( identifier );
      networkTraceIdentifiers.add( new String( identifier ) );
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

  public int getRow() {
    return row;
  }

  public PeerInformation getDestination() {
    return destination;
  }

  public List<String> getNetworkTraceIdentifiers() {
    return networkTraceIdentifiers;
  }

  public void addNetworkTraceRoute(String s) {
    networkTraceIdentifiers.add( s );
  }

  public void incrementRow() {
    ++row;
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

    dout.writeInt( row );

    MessageUtilities.writePeerInformation( dout, destination );

    dout.writeShort( networkTraceIdentifiers.size() );

    for ( String s : networkTraceIdentifiers )
    {
      byte[] identifierBytes = s.getBytes();
      dout.writeInt( identifierBytes.length );
      dout.write( identifierBytes );
    }

    dout.flush();
    marshalledBytes = outputStream.toByteArray();

    outputStream.close();
    dout.close();
    return marshalledBytes;
  }

  @Override
  public String toString() {
    int size = networkTraceIdentifiers.size();
    return ( new StringBuilder() )
        .append( Protocol.class.getFields()[ type ].getName().toString() )
        .append( " | Routing Identifier: " )
        .append( destination.getIdentifier() ).append( " | Hop: " )
        .append( size + 1 ).toString();
  }
}
