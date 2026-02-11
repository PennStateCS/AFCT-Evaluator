package afctevaluator.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import afctevaluator.Feedback;
import afctevaluator.cfg.satvariable.*;
import grammar.CNFConverter;
import grammar.Grammar;
import grammar.LambdaProductionRemover;
import grammar.Production;

/**
 * Determine if two CFGs are equivalent, using SAT reduction. 
 * 
 * The SAT reduction encodes all of the constraints of the two CFGs under test
 * into a Conjunctive Normal Form (CNF) set that emulates both CFGs.
 * It adds an additional constraint that requests a SAT solution where one CFG
 * is able to generate a string while the other fails. If this can be reduced
 * to SAT, then that means that the grammars are not equal. Analyzing the
 * truthiness of the Terminal SAT variables can identify what the witness
 * string is, and looking at the ExactlyOneDerives variables can identify which
 * grammar produced it. 
 * 
 * It should be noted that this is technically misusing the CNF transformation
 * tool -- There is a final step we are ignoring that would rewrite 
 * nonterminals of the form A(t) into other capital letters. We are
 * intentionally skipping this step as it could require more nonterminals than
 * JFLAP supports (26 capital letters).
 * 
 * Also, CNF in this class, unless otherwise stated, 
 * means Conjunctive Normal Form. Chomsky Normalization happens exactly once
 * before SAT processing.
 */
public class GrammarEqualityChecker {
    
    private Grammar submittedGrammar;
    private Grammar targetGrammar;

    private SatMapper sat;

    private record Witness(String mismatch, boolean missing) {}

    public GrammarEqualityChecker(Grammar submittedGrammar, Grammar targetGrammar) {
        this.submittedGrammar = submittedGrammar;
        this.targetGrammar = targetGrammar;

        sat = new SatMapper();
    }

    public Feedback checkEquality() {
        Feedback trivialFailure = checkTrivialInequality();
        if (trivialFailure != null) return trivialFailure;

        convertToChomNF();

        Witness witness = null;
        // lmao
        for (int length = 1; witness == null && length <= 20; witness = findInequalityWitness(length++));

        if (witness == null) {
            return new Feedback("Correct!", true);
        }



        String feedback = "Your Grammar is incorrect. The string \"";
        feedback += witness.mismatch;
        if (witness.missing) {
            feedback += "\" cannot be produced.";
        } else {
            feedback += "\" should not be producable.";
        }

        return new Feedback(feedback, false);
    }

    private void convertToChomNF() {
        submittedGrammar = GrammarTransformer.toChomsky(submittedGrammar);
        targetGrammar = GrammarTransformer.toChomsky(targetGrammar);
    }

    private Feedback checkTrivialInequality() {
        LambdaProductionRemover emptyStringTester = new LambdaProductionRemover();
        boolean targetAllowsEmpty = emptyStringTester
                .getCompleteLambdaSet(targetGrammar)
                .contains(targetGrammar.getStartVariable());
        boolean submissionAllowsEmpty = emptyStringTester
                .getCompleteLambdaSet(submittedGrammar)
                .contains(submittedGrammar.getStartVariable());

        if (targetAllowsEmpty ^ submissionAllowsEmpty) {
            // TODO: Make the output consistent.
            if (targetAllowsEmpty) {
                return new Feedback("Submission does not produce the empty string", false);
            }
            return new Feedback("Submission should not produce the empty string", false);
        }

        String[] targetAlphabet = targetGrammar.getTerminals();
        String[] submittedAlphabet = submittedGrammar.getTerminals();
        Arrays.sort(targetAlphabet);
        Arrays.sort(submittedAlphabet);
        if (!Arrays.equals(targetAlphabet, submittedAlphabet)) {
            return new Feedback("Submission does not produce the correct Alphabet", false);
        }

        return null;
    }

    public Witness findInequalityWitness(int length) {
        List<List<Integer>> constraints = new ArrayList<>();
        constraints.addAll(uniqueSymbolsConstraint(length));
        constraints.addAll(topDownComposition(false, length));
        constraints.addAll(topDownComposition(true, length));
        // TODO: import SAT
        return null;
    }

    /**
     * Construct the CNF clauses that encode all possible string outputs. 
     * Trivially, A terminal being in one position forbids any other terminal
     * from being in the same position. 
     * @param length The length of the strings we are dealing with.
     * @return A set of CNF constraints that enforce terminal placement rules.
     */
    private List<List<Integer>> uniqueSymbolsConstraint(int length) {
        List<List<Integer>> constraints = new ArrayList<>();

        // ASSUMPTION: by this point we've already checked that both grammars
        //  provide the same alphabet. That's trivially checkable. 
        String[] alphabet = targetGrammar.getTerminals();

        for (int position = 0; position < length; position++) {
            for (String terminal : alphabet) {
                // If a terminal here is forbidden, then it cannot be in the solution.
                // Forbids(t, pos) --> -Terminal(t, pos)
                constraints.add(List.of(
                    sat.encodeNegative(new Terminal(terminal, position)),
                    sat.encodeNegative(new ForbidsTerminal(terminal, position))
                ));

                for (String otherTerminal : alphabet) {
                    if (otherTerminal.equals(terminal)) continue;

                    // If some other terminal is in this position, it forbids this one.
                    // Terminal(t1, pos) --> Forbids(t2, pos)
                    // Essentially, it's "B here --> Not A and Not B and Not C and ..."
                    // but it has to be written this way. 
                    constraints.add(List.of(
                        sat.encodePositive(new ForbidsTerminal(terminal, position)),
                        sat.encodeNegative(new Terminal(otherTerminal, position))
                    ));
                }
            }
        }

        return constraints;
    }

    /**
     * 
     * @param isTargetGrammar
     * @param length
     * @return
     */
    private List<List<Integer>> topDownComposition(boolean isTargetGrammar, int length) {
        List<List<Integer>> constraints = new ArrayList<>();
        Grammar g = isTargetGrammar ? targetGrammar : submittedGrammar;

        HashMap<String, List<List<String>>> productionMap = new HashMap<>();

        // The way Grammar is represented is fine for a column table,
        // but we care about having a comprehensive output list for an input.
        for (Production rule : g.getProductions()) {
            productionMap
                    .computeIfAbsent(rule.getLHS(), ignored -> new ArrayList<>())
                    .add(Arrays.asList(CNFConverter.separateString(rule.getRHS())));
        }

        for (int start = 0; start < length; start++) {
            for (int end = start; end < length; end++) {
                for (String input : productionMap.keySet()) {
                    constraints.addAll(
                        singleTopDownStep(input, productionMap.get(input), start, end, isTargetGrammar)
                    );
                }
            }
        }
        return constraints;
    }

    private List<List<Integer>> singleTopDownStep(String input, List<List<String>> outputs, int start, int end, boolean isTargetGrammar) {
        
        List<List<Integer>> allProductionRules = new ArrayList<>();
        List<Integer> inputProductionRule = new ArrayList<>();
        inputProductionRule.add(sat.encodeNegative(new Nonterminal(isTargetGrammar, input, start, end)));
        
        
        for (List<String> output : outputs) {
            // ASSUMPTION (TODO: Resolve before PR): 
            //  all JFLAP ChomNF rules either produce 2 nonterminals or 1 terminal.
            if (output.size() == 1) {
                if (start == end) {
                    inputProductionRule.add(sat.encodePositive(new Terminal(output.get(0), end)));
                }
                continue;
            }
            if (output.size() != 2) throw new RuntimeException("The list " + output + " was expected to be CNF, but isn't!");
            String first = output.get(0);
            String second = output.get(1);
            for (int mid = start; mid < end; mid++) {
                SplitProduction h = new SplitProduction(isTargetGrammar, first, second, start, end, mid);
                inputProductionRule.add(sat.encodePositive(h));
                
                // Split(fst, snd, i, j, k) --> NonTerminal(fst, i, j) AND NonTerminal(snd, j, k).
                allProductionRules.add(List.of(
                    sat.encodeNegative(h),
                    sat.encodePositive(new Nonterminal(isTargetGrammar, first, start, mid))
                ));
                // CNF requires 2 Conjunctions for this. Split(f, second, i, j, k) --> Nonterminal(second, j+1, k)
                allProductionRules.add(List.of(
                    sat.encodeNegative(h),
                    sat.encodePositive(new Nonterminal(isTargetGrammar, second, mid+1, end))
                ));
                // I'm not sure if this is a CYK implementation. 
                //  If I later learn that this can be reduced to CYK somehow for guaranteed O(n^4), 
                //  I'm gonna be mad.
            }
            // We are deliberately ignoring the case of A --> BC and B/C --> e,
            //  because ChomNF completely nullifies it.
        }
        allProductionRules.add(inputProductionRule);

        return allProductionRules;
    }
}
