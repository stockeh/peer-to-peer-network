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

  final int UNREGISTER_REQUEST = 1;

  final int IDENTIFIER_COLLISION = 2;

  final int DISCOVER_NODE_REQUEST = 3;

  final int DISCOVER_NODE_RESPONSE = 4;

  final int JOIN_NETWORK_REQUEST = 5;

  final int FORWARD_IDENTIFIER = 6;

}
