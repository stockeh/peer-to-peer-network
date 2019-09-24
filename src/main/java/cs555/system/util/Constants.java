package cs555.system.util;

/**
 * 
 * @author stock
 *
 */
public interface Constants {

  final byte FAILURE = ( byte ) 0;

  final byte SUCCESS = ( byte ) 1;

  final int IDENTIFIER_BIT_LENGTH = 16;

  final int NUMBER_OF_ROWS = IDENTIFIER_BIT_LENGTH / 4;

  final int LEAF_SET_SIZE = 2;
}
