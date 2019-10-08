package cs555.system.wireformats;

/**
 * Interface defining the wireformats between discovery, peer, and
 * store nodes.
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

  final int FORWARD_PEER_IDENTIFIER = 6;

  final int FORWARD_LEAF_IDENTIFIER = 7;

  final int DISCOVER_PEER_REQUEST = 8;

  final int DISCOVER_PEER_RESPONSE = 9;

  final int STORE_DATA_REQUEST = 10;

  final int STORE_DATA_RESPONSE = 11;

  final int READ_DATA_REQUEST = 12;

  final int READ_DATA_RESPONSE = 13;
  
  final int VERIFY_APPLICAITON_LEAVES = 14;
  
  final int RESET_PEER = 15;
}
