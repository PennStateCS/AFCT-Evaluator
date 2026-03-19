package afctevaluator;

import automata.Automaton;
import grammar.Grammar;
import gui.action.SimulateAction;
import gui.environment.Environment;

public class HeadlessAutomatonSimulator extends SimulateAction {

    public HeadlessAutomatonSimulator(Automaton automaton, Environment environment) {
        super(automaton, environment);
    }

    public HeadlessAutomatonSimulator(Grammar gram, Environment environment) {
        super(gram, environment);
    }
}
