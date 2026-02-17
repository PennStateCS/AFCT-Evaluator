package afctevaluator;

import automata.vdg.VariableDependencyGraph;
import grammar.*;

import java.util.ArrayList;
import java.util.Set;

public class EasyCNFConverter {
    public static Grammar convertToCNF(Grammar grammar) {
        // Remove lambda productions
        LambdaProductionRemover lambdaRemover = new LambdaProductionRemover();
        Set<String> lambdaDerivers = lambdaRemover.getCompleteLambdaSet(grammar);
        lambdaRemover.getCompleteLambdaSet(grammar);
        if (!lambdaDerivers.isEmpty()) {
            grammar = lambdaRemover.getLambdaProductionlessGrammar(grammar, lambdaDerivers);
        }

        // Remove unit productions
        UnitProductionRemover unitRemover = new UnitProductionRemover();
        if (unitRemover.getUnitProductions(grammar).length > 0) {
            VariableDependencyGraph variableDependencyGraph = unitRemover.getVariableDependencyGraph(grammar);
            grammar = unitRemover.getUnitProductionlessGrammar(grammar, variableDependencyGraph);
        }

        // Remove useless productions
        Grammar g2 = UselessProductionRemover.getUselessProductionlessGrammar(grammar);
        if (g2.getTerminals().length==0) {
            // This grammar does not accept any Strings
            return null;
        }
        grammar = g2;

        // Finalize Chomsky Normal Form
        CNFConverter converter;
        try {
            converter = new CNFConverter(grammar);
        } catch (IllegalArgumentException e) {
            // "Illegal Grammar"
            return null;
        }
        Production[] productions = grammar.getProductions();
        boolean chomsky = true;
        for (Production production : productions) {
            chomsky &= converter.isChomsky(production);
        }
        if (!chomsky) {
            ArrayList<Production> tempCNF;
            ArrayList <Production> resultList=new ArrayList <Production>();
            for (Production production : productions) {
                tempCNF = new ArrayList<>();
                converter = new CNFConverter(grammar);
                convertToCNFHelper(tempCNF, converter, production);
                resultList.addAll(tempCNF);
            }
            Production[] pp = new Production[resultList.size()];
            for (int i=0; i < pp.length; i++) {
                pp[i] = resultList.get(i);
            }
            pp=CNFConverter.convert(pp);
            String var = grammar.getStartVariable();
            grammar = new UnrestrictedGrammar();
            grammar.addProductions(pp);
            grammar.setStartVariable(var);
        }

        return grammar;
    }

    private static void convertToCNFHelper(ArrayList <Production> tempCNF, CNFConverter converter, Production production) {
        if (converter.isChomsky(production)) {
            tempCNF.add(production);
        } else {
            Production[] temp = converter.replacements(production);
            for (Production value : temp) {
                convertToCNFHelper(tempCNF, converter, value);
            }
        }
    }
}
