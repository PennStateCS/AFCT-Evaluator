package afctevaluator.cfg;

import java.util.Arrays;

import grammar.CNFConverter;
import grammar.Production;
import grammar.cfg.ContextFreeGrammar;

/**
 * 
 */
public class ChomskyCFG extends ContextFreeGrammar {
    
    @Override
    public void checkProduction(Production production) {
        if (CNFConverter.separateString(production.getLHS()).length != 1) {
            throw new IllegalArgumentException("The Production LHS \"" + production.getLHS() +"\" has an incorrect number of variables!");
        }
    }

    public void overrideTerminals(String[] terminals) {
        this.myTerminals.clear();
        this.myTerminals.addAll(Arrays.asList(terminals));
    }
}
