package afctevaluator.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import automata.vdg.VariableDependencyGraph;
import grammar.CNFConverter;
import grammar.Grammar;
import grammar.LambdaProductionRemover;
import grammar.Production;
import grammar.UnitProductionRemover;
import grammar.cfg.ContextFreeGrammar;

/**
 * Helper class to perform basic utilities that JFLAP/AFCT should have a single
 * step for, but doesn't. 
 * 
 * Currently, this is mainly a simple converter for an arbitrary grammar to
 * Chomsky Normal Form. 
 */
public class GrammarTransformer {
    
    /**
     * Convert a grammar to Chomsky Normal Form. 
     * 
     * Note that this will remove the empty string as a valid production.
     * 
     * Note that the resulting grammar will have nonterminals whose length may
     * be longer than a single character, such as `B(1)`. Use 
     * <code>CNFConverter.seperateString</code> to divide the string in such
     *  cases. 
     * @param g The grammar to convert.
     * @return A grammar in Chomsky Normal Form, with the considerations listed
     *         above.
     */
    public static Grammar toChomsky(Grammar g) {

        String start = g.getStartVariable();
        String[] ends = g.getTerminals();
        Grammar nonEmptyG = removeEmptyProductions(g);
        Grammar nonTransitiveG = reduceTransitiveProductions(nonEmptyG);
        Grammar usefulG = removeUselessRules(nonTransitiveG, start, ends);
        Grammar chomskyG = convertToChomsky(usefulG, start, ends);
        return chomskyG;
    }

    /**
     * Eliminate all rules that cause a variable to reduce to nothing.
     * "nothing" being the empty string, aka "epsilon", aka "lambda". 
     * 
     * Note that this eliminates the empty string as a valid production
     * for this grammar. 
     * @param g The grammar to perform this operation on. It is unaffected.
     * @return A grammar where all epsilon productions are removed.
     */
    private static Grammar removeEmptyProductions(Grammar g) {
        LambdaProductionRemover remover = new LambdaProductionRemover();
        Set<String> lambdaDerivers = remover.getCompleteLambdaSet(g);
        return remover.getLambdaProductionlessGrammar(g, lambdaDerivers);
    }

    /**
     * Eliminate all rules of the form A -> B. 
     * If A -> B, and B -> CD, then A -> CD as well. 
     * This applies transitively to all productions. 
     * @param input The grammar to perform this operation on. It is unaffected.
     * @return A grammar where all productions are either to empty string or to
     *         more than 1 nonterminal symbol.
     */
    private static Grammar reduceTransitiveProductions(Grammar input) {
        UnitProductionRemover remover = new UnitProductionRemover();
        VariableDependencyGraph graph = remover.getVariableDependencyGraph(input);
        return remover.getUnitProductionlessGrammar(input, graph);
    }

    /**
     * Eliminate rules that are unable to terminate. This is possible once 
     * lambda states are removed. 
     * @param g The grammar to perform this operation on. It is unaffected.
     * @return A grammar where every production rule must be used to create
     *         any string in its alphabet. 
     */
    private static Grammar removeUselessRules(Grammar g, String start, String[] ends) {
        Grammar output = new ContextFreeGrammar();
        output.setStartVariable(start);

        Set<String> usableSymbols = new HashSet<>();
        List<Production> addableProductions = new ArrayList<>(Arrays.asList(g.getProductions()));
        usableSymbols.addAll(Arrays.asList(ends));

        boolean somethingAdded;

        do {
            somethingAdded = false;
            for (int i = addableProductions.size()-1; i >= 0; i--) {
                Production p = addableProductions.get(i);
                boolean usable = true;
                for (char c : p.getRHS().toCharArray()) {
                    String symbol = c + "";
                    if (!usableSymbols.contains(symbol)) {
                        usable = false;
                        break;
                    }
                }
                if (!usable) continue;

                addableProductions.remove(i);
                somethingAdded = true;
                output.addProduction(p);
                usableSymbols.add(p.getLHS());
            }

        } while (somethingAdded);

        if (!usableSymbols.contains(start)) return new ContextFreeGrammar();
        return output;
    }

    /**
     * Convert a Grammar to Chomsky Normal Form. This is based on the three
     * functions above this one. 
     * @param g The grammar to perform this operation on. It is unaffected.
     * @return A grammar in Chomsky Normal Form, aka CNF aka 2NF. 
     */
    private static Grammar convertToChomsky(Grammar g, String start, String[] ends) {
        // TODO: ensure that an IllegalArgumentException isn't thrown
        // CNFConverter:L270
        g.setStartVariable(start);
        CNFConverter converter = new CNFConverter(g);
        ChomskyCFG output = new ChomskyCFG();
        output.addProductions(g.getProductions());

        // Based on the Chomsky pane because CNFConverter is terrible
        ArrayList<Production> notChomsky = new ArrayList<>();

        do {
            for (Production toReplace : notChomsky) {
                // TODO: small chance of IllegalArgumentException. Unsure if valid issue here.
                Production[] replacements = converter.replacements(toReplace);
                output.removeProduction(toReplace);
                output.addProductions(replacements);
            }
            notChomsky.clear();

            for (Production p : output.getProductions()) {
                if (!converter.isChomsky(p)) {
                    notChomsky.add(p);
                }
            }
        } while (!notChomsky.isEmpty());

        output.setStartVariable(start);
        // This is why we created ChomskyCFG
        output.overrideTerminals(ends);

        return output;
    }
}
