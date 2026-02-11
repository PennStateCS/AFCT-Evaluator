package afctevaluator.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import automata.vdg.VariableDependencyGraph;
import grammar.CNFConverter;
import grammar.Grammar;
import grammar.LambdaProductionRemover;
import grammar.Production;
import grammar.UnitProductionRemover;
import grammar.UselessProductionRemover;
import grammar.cfg.ContextFreeGrammar;

/**
 * Helper class to perform basic utilities that JFLAP/AFCT should have a single
 * step for, but doesn't. 
 * 
 * Currently, this is mainly a simple converter for an arbitrary grammar to
 * Chomsky Normal Form. 
 */
public class GrammarTransformer {
    
    public static Grammar toChomsky(Grammar g) {
        g = removeEmptyProductions(g);
        g = reduceTransitiveProductions(g);
        g = removeUselessRules(g);
        g = convertToChomsky(g);
        return g;
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
     * Eliminate rules that are "useless" -- that is, there are other rules
     * that yield the same results. Running other functions in this class may 
     * cause this, and the code here cleans them up. 
     * @param g The grammar to perform this operation on. It is unaffected.
     * @return A grammar where every production rule must be used to create
     *         any string in its alphabet. 
     */
    private static Grammar removeUselessRules(Grammar g) {
        return UselessProductionRemover.getUselessProductionlessGrammar(g);
    }

    /**
     * Convert a Grammar to Chomsky Normal Form. This is based on the three
     * functions above this one. 
     * @param g The grammar to perform this operation on. It is unaffected.
     * @return A grammar in Chomsky Normal Form, aka CNF aka 2NF. 
     */
    private static Grammar convertToChomsky(Grammar g) {
        // TODO: ensure that an IllegalArgumentException isn't thrown
        // CNFConverter:L270

        CNFConverter converter = new CNFConverter(g);
        Grammar output = new ContextFreeGrammar();
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

        return output;
    }
}
