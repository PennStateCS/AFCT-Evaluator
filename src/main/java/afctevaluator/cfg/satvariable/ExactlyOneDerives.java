package afctevaluator.cfg.satvariable;

/**
 * The "B" in the original OCaml implementation. 
 * Encodes an asymmetric check that will allow SAT iff there is a string that
 * exactly one of the two grammars can produce.
 * Its documentation provided the following insight:
 * 
 * variable B(j) is used as an auxiliary variable to encode the constraints for starting nonterminals in
   the intersection and inclusion problem
 */
public record ExactlyOneDerives(boolean submissionShouldProduce, int targetLength) implements SatVariable {}
