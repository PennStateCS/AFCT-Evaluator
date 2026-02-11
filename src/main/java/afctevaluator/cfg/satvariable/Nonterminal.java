package afctevaluator.cfg.satvariable;

/**
 * The N variable in the original OCaml implementation. Encodes a nonterminal
 * and the domain it will cover in terminals. 
 * Its documentation provided the following insight: 
 * 
 * (* variable N(A,i,j,m) represents the occurrence of nonterminal A in table entry (i,j) on level m, 
   0 <= i <= j < k, 0 <= m <= 1 *)
 * Notably, there is no `m`. This is probably what is now isTargetGrammar. 
 */
public record Nonterminal(boolean isTargetGrammar, 
                          String symbol, 
                          int start, 
                          int end)
implements SatVariable {}
