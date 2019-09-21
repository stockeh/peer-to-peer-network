package cs555.system.util;

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
      int valA = Integer.parseInt( a, 16 );
      i++;
      buf[ j ] = ( byte ) valA;
      j++;
    }
    return buf;
  }

}
