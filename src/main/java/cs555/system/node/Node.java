package cs555.system.node;

import cs555.system.transport.TCPConnection;
import cs555.system.wireformats.Event;

/**
 * Interface for the chunk server, peer and discovery, so underlying
 * communication is indistinguishable, i.e., Nodes send messages to
 * Nodes.
 * 
 * @author stock
 *
 */
public interface Node {

  /**
   * Gives the ability for events to be triggered by incoming messages
   * on a given node.
   * 
   * @param event
   * @param connection
   */
  public void onEvent(Event event, TCPConnection connection);

}
