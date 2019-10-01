package cs555.system.util;

/**
 * Constants that will not change in the program.
 * 
 * @author stock
 *
 */
public interface Constants {

  final boolean SUCCESS = true;

  final boolean FAILURE = false;

  final boolean CLOCKWISE = true;

  final boolean COUNTER_CLOCKWISE = false;

  final int IDENTIFIER_BIT_LENGTH = 16;

  final int NUMBER_OF_ROWS = IDENTIFIER_BIT_LENGTH / 4;

  final int LEAF_SET_SIZE = 2;
}
