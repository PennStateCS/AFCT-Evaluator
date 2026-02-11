package afctevaluator.cfg.satvariable;

/**
 * One of the two T vars used in the original OCaml implementation.
 * This one is used exclusively for the unqiueness constraint.
 * Multiple terminals cannot be in the same position. 
 * Its documentation provided the following insight:
 * 
 * variable T(a,i) is used to denote that the i-th letter of the word to be
 * found is `a'.
 * 0 <= i < k, a : alphabet 
 */
public record ForbidsTerminal(String symbol, int depth) implements SatVariable {}
