package cs555.system.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class IdentifierUtilitiesTest {

  @Test
  public void testConvertBytesToHex() {
    byte[] buf = new byte[ 2 ];
    buf[ 0 ] = ( byte ) 0;
    buf[ 1 ] = ( byte ) 1;

    String s = IdentifierUtilities.convertBytesToHex( buf );

    assertEquals( "0001", s );
  }

  @Test
  public void testConvertHexToBytes() {
    String s = "0001";
    byte[] buf = IdentifierUtilities.convertHexToBytes( s );

    byte[] expected = new byte[ 2 ];
    expected[ 0 ] = ( byte ) 0;
    expected[ 1 ] = ( byte ) 1;

    assertEquals( expected.length, buf.length );
    assertArrayEquals( expected, buf );
  }
}
