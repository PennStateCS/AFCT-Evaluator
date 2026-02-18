package afctevaluator.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import afctevaluator.Feedback;
import afctevaluator.cfg.satvariable.*;
import grammar.CNFConverter;
import grammar.Grammar;
import grammar.LambdaProductionRemover;
import grammar.Production;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

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

    private static final boolean DEBUG = false;
    
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
        for (int length = 1; witness == null && length <= 15; length++) {
            witness = findInequalityWitness(length);
        }

        if (witness == null) {
            return new Feedback("Correct!", true);
        }



        String feedback = "Your Grammar is incorrect. The string \"";
        feedback += witness.mismatch;
        if (witness.missing) {
            feedback += "\" SHOULD be produced.";
        } else {
            feedback += "\" should NOT be producable.";
        }

        return new Feedback(feedback, false);
    }

    private void convertToChomNF() {
        submittedGrammar = GrammarTransformer.toChomsky(submittedGrammar);
        targetGrammar = GrammarTransformer.toChomsky(targetGrammar);
    }

    private Feedback checkTrivialInequality() {

        String[] targetAlphabet = targetGrammar.getTerminals();
        String[] submittedAlphabet = submittedGrammar.getTerminals();
        Arrays.sort(targetAlphabet);
        Arrays.sort(submittedAlphabet);
        if (!Arrays.equals(targetAlphabet, submittedAlphabet)) {
            return new Feedback("Submission does not produce the correct alphabet", false);
        }

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
                return new Feedback("The empty string SHOULD be produced", false);
            }
            return new Feedback("The empty string should NOT be producable", false);
        }

        return null;
    }

    public Witness findInequalityWitness(int length) {
        if (DEBUG) System.out.println("Checking Equality. LENGTH = " + length);

        List<List<Integer>> constraints = new ArrayList<>();
        constraints.addAll(uniqueSymbolsConstraint(length));
        constraints.addAll(topDownComposition(false, length));
        constraints.addAll(topDownComposition(true, length));
        constraints.addAll(bottomUpComposition(true, length));
        constraints.addAll(bottomUpComposition(false, length));
        constraints.addAll(exactlyOneCfgProducesConstraint(length));

        ISolver solver = SolverFactory.newDefault();
        solver.newVar(sat.maxVar());
        solver.setExpectedNumberOfClauses(constraints.size());

        try {
            // Consider retconning everything below this function to use Sat4J's impl of vecint.
            for (List<Integer> clause : constraints) {
                IVecInt formattedClause = new VecInt(clause.stream().mapToInt(Integer::intValue).toArray());
                //System.out.println(clause);
                solver.addClause(formattedClause);
            }
        } catch (ContradictionException e) {
            // Indicates a trivial contradiction -- impossible?
            if (DEBUG) System.err.println("Trivial Contradiction detected...?");
            return null; 
        }

        IProblem problem = solver;
        try {
            if (problem.isSatisfiable()) {
                return extractWitnessFromSat(problem, length);
            }
        } catch (TimeoutException impossible) {}


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

        for (int position = 1; position <= length; position++) {
            List<Integer> requireSomethingHere = new ArrayList<>();
            for (String terminal : alphabet) {
                requireSomethingHere.add(sat.encodePositive(new Terminal(terminal, position)));
                // If a terminal here is forbidden, then it cannot be in the solution.
                if (DEBUG) {
                    System.out.println(new ForbidsTerminal(terminal, position) + " -> -" + new Terminal(terminal, position));
                }
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
                    if (DEBUG) {
                        System.out.println(new Terminal(otherTerminal, position) + " -> " + new ForbidsTerminal(terminal, position));
                    }

                    constraints.add(List.of(
                        sat.encodePositive(new ForbidsTerminal(terminal, position)),
                        sat.encodeNegative(new Terminal(otherTerminal, position))
                    ));
                }
            }
            constraints.add(requireSomethingHere);
        }

        return constraints;
    }

    /**
     * Construct the top-down rules for one of the two grammars. This converts
     * all of the production rules for the ChomNF grammar into ConjNF logical
     * statements. 
     * @param isTargetGrammar Whether to use the target grammar. 
     * @param length How long the strings we are considering should be.
     * @return A set of CNF constraints that enforce the grammar rules.
     */
    private List<List<Integer>> topDownComposition(boolean isTargetGrammar, int length) {
        List<List<Integer>> constraints = new ArrayList<>();
        Grammar g = isTargetGrammar ? targetGrammar : submittedGrammar;

        HashMap<String, List<List<String>>> productionMap = new HashMap<>();

        // The way Grammar is represented is fine for a column table,
        // but we care about having a comprehensive output list for an input.
        for (Production rule : g.getProductions()) {
           //if (CNFConverter.separateString(rule.getRHS()).length == 0) continue;
            productionMap
                    .computeIfAbsent(rule.getLHS(), ignored -> new ArrayList<>())
                    .add(Arrays.asList(CNFConverter.separateString(rule.getRHS())));
        }

        for (int start = 1; start <= length; start++) {
            for (int end = start; end <= length; end++) {
                for (String input : productionMap.keySet()) {
                    constraints.addAll(
                        singleTopDownStep(input, productionMap.get(input), start, end, isTargetGrammar)
                    );
                }
            }
        }
        return constraints;
    }

    /**
     * Perform the top-down step for a nonterminal at a given position. 
     * @param input The input nonterminal. 
     * @param outputs A list of all possible things that nonterminal
     *                can produce. Either a single terminal or exactly two
     *                nonterminals. 
     * @param start The start of the range this nonterminal is expected to fill
     * @param end The end of the range this nonterminal is expected to fill
     * @param isTargetGrammar If the grammar this symbol comes from is 
     *                        the target grammar.
     * @return All possible CNF rules required to encode this grammar rule.
     */
    private List<List<Integer>> singleTopDownStep(String input, List<List<String>> outputs, int start, int end, boolean isTargetGrammar) {
        
        List<List<Integer>> allProductionRules = new ArrayList<>();
        List<Integer> inputProductionRule = new ArrayList<>();
        inputProductionRule.add(sat.encodeNegative(new Nonterminal(isTargetGrammar, input, start, end)));
        
        
        for (List<String> output : outputs) {
            if (output.size() == 1) {
                if (start == end) {
                    if (DEBUG) {
                        System.out.println(new Nonterminal(isTargetGrammar, input, start, end) + " ->? " + new Terminal(output.get(0), end));
                    }
                    inputProductionRule.add(sat.encodePositive(new Terminal(output.get(0), end)));
                }
                continue;
            }
            if (output.size() != 2) {
                throw new IllegalArgumentException("A production rule starting with " + input + " yields nothing??? ");
            }

            String first = output.get(0);
            String second = output.get(1);
            for (int mid = start; mid < end; mid++) {
                SplitProduction h = new SplitProduction(isTargetGrammar, first, second, start, end, mid);
                if (DEBUG) {
                    System.out.println(new Nonterminal(isTargetGrammar, input, start, end) + " ->? " + h);
                }
                inputProductionRule.add(sat.encodePositive(h));
                
                if (DEBUG) {
                    System.out.println(h + " -> " + new Nonterminal(isTargetGrammar, first, start, mid));
                    System.out.println(h + " -> " + new Nonterminal(isTargetGrammar, second, mid+1, end));
                }
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
            }
            // We are deliberately ignoring the case of A --> BC and B/C --> e,
            //  because ChomNF completely nullifies it.
        }
        allProductionRules.add(inputProductionRule);

        return allProductionRules;
    }

    /**
     * Construct the bottom-up rules for one of the two grammars. This ensures
     * that, if a string is considered valid by SAT, it can follow the rules
     * required to prove it. 
     * @param isTargetGrammar Whether to use the target grammar. 
     * @param length How long the strings we are considering should be. 
     * @return A set of CNF constraints that enforce the String generation rule
     */
    private List<List<Integer>> bottomUpComposition(boolean isTargetGrammar, int length) {
        List<List<Integer>> constraints = new ArrayList<>();
        Grammar g = isTargetGrammar ? targetGrammar : submittedGrammar;

        for (int start = 1; start <= length; start++) {
            for (int end = start; end <= length; end++) {
                // Possible optimization?: if rule is terminal, skip the start-end nonsense. 
                for (Production rule : g.getProductions()) {
                    constraints.addAll(singleBottomUpStep(rule, start, end, isTargetGrammar));
                }
            }
        }

        return constraints;
    }

    /**
     * Perform the bottom-up step for a single rule at a given position.
     * @param rule The rule to deal with.
     * @param start The start of the range of symbols that the production
     *              eventually spans. 
     * @param end The end of the range of symbols the production eventulaly
     *            spans. 
     * @param isTargetGrammar If the grammar this rule comes from is the target grammar. 
     * @return All possible CNF rules required to encode this grammar rule.
     */
    private List<List<Integer>> singleBottomUpStep(Production rule, int start, int end, boolean isTargetGrammar) {
        String lhs = rule.getLHS();
        String[] rhs = CNFConverter.separateString(rule.getRHS());

        if (rhs.length == 1) {
            if (start != end) return Collections.<List<Integer>>emptyList();
            String terminal = rhs[0];
            // Nonterminal(lhs, pos) <-- Terminal(T, pos)
            // N(lhs, pos) v !T(pos)
            if (DEBUG) {
                System.out.println(new Terminal(terminal, end) + " -> " + new Nonterminal(isTargetGrammar, lhs, start, end));
            }
            return List.of(List.of(
                sat.encodeNegative(new Terminal(terminal, end)),
                sat.encodePositive(new Nonterminal(isTargetGrammar, lhs, start, end))
            ));
        }

        assert rhs.length == 2;
        String first = rhs[0];
        String second = rhs[0];
        List<List<Integer>> constraint = new ArrayList<>();

        for (int mid = start; mid < end; mid++) {
            // lhs <-- rhs1 and rhs2
            // lhs v !rhs1 v !rhs2
            if (DEBUG) {
                System.out.println(
                    "(" + new Nonterminal(isTargetGrammar, first, start, mid) + 
                    " AND " + new Nonterminal(isTargetGrammar, second, mid+1, end) + 
                    ") -> " + new Nonterminal(isTargetGrammar, lhs, start, end)
                );
            }
            constraint.add(List.of(
                sat.encodePositive(new Nonterminal(isTargetGrammar, lhs, start, end)),
                sat.encodeNegative(new Nonterminal(isTargetGrammar, first, start, mid)),
                sat.encodeNegative(new Nonterminal(isTargetGrammar, second, mid+1, end))
            ));
        }

        return constraint;
    }

    /**
     * Construct the derivation limitation rules for the grammars. 
     * If exactly one of the grammars can produce a string, then this will
     * allow the program to resolve to SAT. Otherwise, UNSAT indicates that the
     * grammars are equal to this length. 
     * @param length The length of strings we are checking for. 
     * @return A set of CNF constraints that enforce exactly one grammar
     *         producing a string. 
     */
    private List<List<Integer>> exactlyOneCfgProducesConstraint(int length) {
        List<List<Integer>> implication = new ArrayList<>();
        String submittedStart = submittedGrammar.getStartVariable();
        String targetStart = targetGrammar.getStartVariable();

        // Sub should produce --> target produces AND submission doesn't
        implication.add(List.of(
            sat.encodeNegative(new ExactlyOneDerives(true, length)),
            sat.encodePositive(new Nonterminal(true, submittedStart, 1, length))
        ));
        implication.add(List.of(
            sat.encodeNegative(new ExactlyOneDerives(true, length)),
            sat.encodeNegative(new Nonterminal(false, targetStart, 1, length))
        ));

        // Sub should NOT produce --> target produces AND submission doesn't
        implication.add(List.of(
            sat.encodeNegative(new ExactlyOneDerives(false, length)),
            sat.encodeNegative(new Nonterminal(true, submittedStart, 1, length))
        ));
        implication.add(List.of(
            sat.encodeNegative(new ExactlyOneDerives(false, length)),
            sat.encodePositive(new Nonterminal(false, targetStart, 1, length))
        ));

        // For SAT, either a) target should produce something, or b) submission produces something it shouldn't.
        implication.add(List.of(
            sat.encodePositive(new ExactlyOneDerives(true, length)),
            sat.encodePositive(new ExactlyOneDerives(false, length))
        ));

        if (DEBUG) {
            System.out.println(new ExactlyOneDerives(true, length) + " -> " + new Nonterminal(true, submittedStart, 1, length));
            System.out.println(new ExactlyOneDerives(true, length) + " -> !" + new Nonterminal(false, submittedStart, 1, length));
            System.out.println(new ExactlyOneDerives(false, length) + " -> !" + new Nonterminal(true, submittedStart, 1, length));
            System.out.println(new ExactlyOneDerives(false, length) + " -> " + new Nonterminal(false, submittedStart, 1, length));
        }

        return implication;
    }

    private Witness extractWitnessFromSat(IProblem problem, int length) {
        String[] alphabet = targetGrammar.getTerminals();
        String witness = "";
        for (int position = 1; position <= length; position++) {
            for (String terminal : alphabet) {
                if (problem.model(sat.encodePositive(new Terminal(terminal, position)))) {
                    witness += terminal;
                    continue;
                }
            }
        }
        boolean shouldBePresent = problem.model(sat.encodePositive(new ExactlyOneDerives(true, length)));
        return new Witness(witness, shouldBePresent);
    }
}
