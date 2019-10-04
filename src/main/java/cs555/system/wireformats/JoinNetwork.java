package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import cs555.system.metadata.PeerInformation;
import cs555.system.util.Constants;
import cs555.system.util.MessageUtilities;

/**
 * 
 * @author stock
 *
 */
public class JoinNetwork implements Event {

  private int type;

  private PeerInformation destination;

  private PeerInformation cw;

  private PeerInformation ccw;

  private PeerInformation[][] table;

  private Set<String> networkTraceIdentifiers;

  private short row;

  /**
   * Default constructor -
   * 
   */
  public JoinNetwork(PeerInformation destination) {
    this.type = Protocol.JOIN_NETWORK_REQUEST;
    this.destination = destination;
    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][ 16 ];
    this.networkTraceIdentifiers = new LinkedHashSet<>();
    this.row = 0;
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public JoinNetwork(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    this.destination = MessageUtilities.readPeerInformation( din );

    if ( din.readBoolean() )
    {
      cw = MessageUtilities.readPeerInformation( din );
      ccw = MessageUtilities.readPeerInformation( din );
    }

    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][ 16 ];

    for ( int row = 0; row < Constants.NUMBER_OF_ROWS; ++row )
    {
      if ( din.readBoolean() )
      {
        table[ row ] = new PeerInformation[ 16 ];

        for ( int i = 0; i < 16; ++i )
        {
          if ( din.readBoolean() )
          {
            table[ row ][ i ] = MessageUtilities.readPeerInformation( din );
          }
        }
      }
    }

    short len = din.readShort();
    this.networkTraceIdentifiers = new LinkedHashSet<>( len );
    int identifierLength;
    byte[] identifier;
    for ( int i = 0; i < len; ++i )
    {
      identifierLength = din.readInt();
      identifier = new byte[ identifierLength ];
      din.readFully( identifier );
      networkTraceIdentifiers.add( new String( identifier ) );
    }

    this.row = din.readShort();

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

  public PeerInformation getDestination() {
    return destination;
  }

  public PeerInformation getCW() {
    return cw;
  }

  public PeerInformation getCCW() {
    return ccw;
  }

  public PeerInformation[][] getTable() {
    return table;
  }

  public short getRow() {
    return row;
  }

  public void setCW(PeerInformation cw) {
    this.cw = cw;
  }

  public void setCCW(PeerInformation ccw) {
    this.ccw = ccw;
  }

  public void setTableRow(PeerInformation[] row) {
    table[ this.row ] = row;
  }

  public Set<String> getNetworkTraceIdentifiers() {
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

    MessageUtilities.writePeerInformation( dout, destination );

    if ( cw == null && ccw == null )
    {
      dout.writeBoolean( false );
    } else
    {
      dout.writeBoolean( true );
      MessageUtilities.writePeerInformation( dout, cw );
      MessageUtilities.writePeerInformation( dout, ccw );
    }
    for ( PeerInformation[] row : table )
    {
      if ( row == null )
      {
        dout.writeBoolean( false );
      } else
      {
        dout.writeBoolean( true );
        for ( PeerInformation peer : row )
        {
          if ( peer == null )
          {
            dout.writeBoolean( false );
          } else
          {
            dout.writeBoolean( true );
            MessageUtilities.writePeerInformation( dout, peer );
          }
        }
      }
    }

    dout.writeShort( networkTraceIdentifiers.size() );

    for ( String s : networkTraceIdentifiers )
    {
      byte[] identifierBytes = s.getBytes();
      dout.writeInt( identifierBytes.length );
      dout.write( identifierBytes );
    }

    dout.writeShort( row );

    dout.flush();
    marshalledBytes = outputStream.toByteArray();

    outputStream.close();
    dout.close();
    return marshalledBytes;
  }

  @Override
  public String toString() {
    return ( new StringBuilder() )
        .append( Protocol.class.getFields()[ type ].getName().toString() )
        .append( " | New Node: " ).append( destination.getIdentifier() )
        .append( " | Hop: " ).append( networkTraceIdentifiers.size() - 1 )
        .append( " | Next Node: " ).toString();
  }
}
