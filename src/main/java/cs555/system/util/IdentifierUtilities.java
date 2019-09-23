package cs555.system.util;

import java.nio.ByteBuffer;

/**
 * 
 * @author stock
 *
 */
public class IdentifierUtilities {

  /**
   * Converts a set of bytes into a Hexadecimal <tt>String</tt>
   * representation.
   * 
   * @param buf to convert to hex
   * @return
   */
  public static String convertBytesToHex(byte[] buf) {
    StringBuffer strBuf = new StringBuffer();
    for ( int i = 0; i < buf.length; i++ )
    {
      int byteValue = ( int ) buf[ i ] & 0xff;
      if ( byteValue <= 15 )
      {
        strBuf.append( "0" );
      }
      strBuf.append( Integer.toString( byteValue, 16 ) );
    }
    return strBuf.toString();
  }

  /**
   * Convert a specified hexadecimal <tt>String</tt> into a set of
   * bytes.
   * 
   * @param hexString to convert to bytes
   * @return
   */
  public static byte[] convertHexToBytes(String hexString) {
    int size = hexString.length();
    byte[] buf = new byte[ size / 2 ];
    int j = 0;
    for ( int i = 0; i < size; i++ )
    {
      String a = hexString.substring( i, i + 2 );
      i++;
      buf[ j ] = ( byte ) Integer.parseInt( a, 16 );
      j++;
    }
    return buf;
  }

  /**
   * Uses the current timestamp to generate a 16-bit CRC checksum.
   * 
   * @return a <tt>String</tt> of 16-bit hexadecimal
   */
  public static String timestampToIdentifier() {
    long timestamp = System.currentTimeMillis();
    byte[] bytes = ByteBuffer.allocate( 8 ).putLong( timestamp ).array();
    return CRC16CCITT( bytes );
  }

  /**
   * Computes the 16-bit Cylcic Redundancy Check (CRC-CCIIT 0xFFFF) from
   * a sequence of bytes.
   * <p>
   * Uses the <tt>CRC-CCITT: x16 + x12 + x5 + 1</tt> polynomial
   * </p>
   * 
   * @return a <tt>String</tt> of 16-bit hexadecimal
   */
  private static String CRC16CCITT(byte[] bytes) {
    final int polynomial = 0x1021;
    int crc = 0xFFFF;

    for ( byte b : bytes )
    {
      for ( int i = 0; i < 8; i++ )
      {
        boolean bit = ( ( b >> ( 7 - i ) & 1 ) == 1 );
        boolean c15 = ( ( crc >> 15 & 1 ) == 1 );
        crc <<= 1;
        if ( c15 ^ bit )
        {
          crc ^= polynomial;
        }
      }
    }
    return String.format( "%04X", ( 0xFFFF & crc ) );
  }

  /**
   * Compare a source identifier with that of an item.
   * 
   * @param source
   * @param item
   * @param index
   * @return 0 if they are same, otherwise the value of the item index
   */
  public static int compareIdentifiers(String source, String item, int index) {
    int a = Character.digit( source.charAt( index ), 16 );
    int b = Character.digit( item.charAt( index ), 16 );
    return ( a - b == 0 ) ? 0 : b;
  }
}
