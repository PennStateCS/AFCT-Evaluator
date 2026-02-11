package afctevaluator.cfg.satvariable;

/**
 * Originally "H" in the original OCaml implementation. 
 * Demonstrates a production where a variable A, spanning
 * start <= positions < end, produces 2 symbols that split the difference
 * in some way specified by midpoint. 
 * 
 * Its documentation provided the following insight: 
 * 
 * variable H(B,C,i,j,h) is used as an auxiliary variable to obtain CNF
 */
public record SplitProduction(boolean isTargetGrammar, 
                              String firstProduction, 
                              String secondProduction, 
                              int start, 
                              int end, 
                              int midpoint)
implements SatVariable {}
