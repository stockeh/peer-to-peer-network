package cs555.system.wireformats;

/**
 * Interface defining the wireformats between discovery, peer, and
 * chunk servers.
 *
 * @author stock
 *
 */
public interface Protocol {

  final int REGISTER_REQUEST = 0;

  final int REGISTER_RESPONSE = 1;

  final int UNREGISTER_REQUEST = 2;

  final int IDENTIFIER_COLLISION = 3;
  
  final int DISCOVER_NODE_REQUEST = 4;

  final int DISCOVER_NODE_RESPONSE = 5;
  
}
