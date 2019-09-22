package cs555.system.metadata;

/**
 * 
 * @author stock
 *
 */
public class PeerInformation {

  final String identifier;

  final String host;

  final int port;

  /**
   * 
   * @param identifier
   * @param host
   * @param port
   */
  public PeerInformation(String identifier, String host, int port) {
    this.identifier = identifier;
    this.host = host;
    this.port = port;
  }

  /**
   * 
   * @return the host address from the peer
   */
  public String getHost() {
    return this.host;
  }

  /**
   * 
   * @return the port number from the peer
   */
  public int getPort() {
    return this.port;
  }

  @Override
  public boolean equals(Object o) {
    if ( o == this )
    {
      return true;
    }
    if ( !( o instanceof PeerInformation ) )
    {
      return false;
    }
    return ( ( PeerInformation ) o ).identifier.equals( identifier );
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public String toString() {
    return ( new StringBuilder() ).append( "\t>\t" ).append( identifier )
        .append( " | " ).append( host ).append( ":" ).append( port ).toString();
  }

}
