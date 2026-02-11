package afctevaluator.cfg.satvariable;

/**
 * One of the two T vars used in the original OCaml implementation.
 * Its documentation provided the following insight:
 * 
 * variable T(a,i) is used to denote that the i-th letter of the word to be
 * found is `a'.
 * 0 <= i < k, a : alphabet 
 */
public record Terminal(String symbol, int position) implements SatVariable {}
