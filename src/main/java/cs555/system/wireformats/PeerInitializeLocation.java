package cs555.system.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import cs555.system.metadata.PeerInformation;
import cs555.system.util.Constants;
import cs555.system.util.MessageUtilities;

/**
 * 
 * @author stock
 *
 */
public class PeerInitializeLocation implements Event {

  private int type;

  private PeerInformation destination;

  private PeerInformation[][] table;

  private PeerInformation[] leafSet;

  private int rowIndex;

  /**
   * Default constructor -
   * 
   */
  public PeerInitializeLocation(PeerInformation destination, int rowIndex) {
    this.type = Protocol.PEER_INITIALIZE_LOCATION;
    this.destination = destination;
    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][];
    this.leafSet = new PeerInformation[ Constants.LEAF_SET_SIZE ];
    this.rowIndex = rowIndex;
  }

  /**
   * Constructor - Unmarshall the <code>byte[]</code> to the respective
   * class elements.
   * 
   * @param marshalledBytes is the byte array of the class.
   * @throws IOException
   */
  public PeerInitializeLocation(byte[] marshalledBytes) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream( marshalledBytes );
    DataInputStream din =
        new DataInputStream( new BufferedInputStream( inputStream ) );

    this.type = din.readInt();

    this.destination = MessageUtilities.readPeerInformation( din );

    this.table = new PeerInformation[ Constants.NUMBER_OF_ROWS ][];

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

    this.leafSet = new PeerInformation[ Constants.LEAF_SET_SIZE ];

    for ( int i = 0; i < Constants.LEAF_SET_SIZE; ++i )
    {
      if ( din.readBoolean() )
      {
        leafSet[ i ] = MessageUtilities.readPeerInformation( din );
      }
    }

    this.rowIndex = din.readInt();

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

  public PeerInformation[] getLeafSet() {
    return leafSet;
  }

  public PeerInformation getLeafSetByIndex(int index) {
    return leafSet[ index ];
  }

  public int getRowIndex() {
    return rowIndex;
  }

  public void incrementRowIndex() {
    ++rowIndex;
  }

  public void setTableRow(PeerInformation[] row) {
    table[ rowIndex ] = row;
  }

  public void setLeafSetIndex(PeerInformation leaf, int index) {
    leafSet[ index ] = leaf;
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
    for ( PeerInformation peer : leafSet )
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

    dout.writeInt( rowIndex );

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
