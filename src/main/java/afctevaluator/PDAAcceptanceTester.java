package afctevaluator;

import automata.Automaton;
import automata.Configuration;
import automata.pda.PDAStepWithClosureSimulator;
import automata.pda.PushdownAutomaton;
import grammar.Grammar;
import gui.action.SimulateAction;
import gui.environment.Environment;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class PDAAcceptanceTester extends PDAStepWithClosureSimulator {
    public PDAAcceptanceTester(Automaton automaton) {
        super(automaton);
    }

    public int checkAcceptance(String witness) {
        // How many configurations have accepted?
        int numberAccepted = 0;

        Instant start = Instant.now();
        Duration timeElapsed;

        // Get the initial configurations.
        Configuration[] configs = this.getInitialConfigurations(witness);

        while (configs.length > 0) {
            // Make sure we should continue.
            timeElapsed = Duration.between(start, Instant.now());
            if (timeElapsed.toMillis() > 10_000) { // check if 10 seconds have passed
                return -1;
            }
            // Get the next batch of configurations.
            ArrayList<Configuration> next = new ArrayList<>();
            for (int i = 0; i < configs.length; i++) {
                if (configs[i].isAccept()) {
                    numberAccepted++;
                    return 1;
                } else {
                    next.addAll(this.stepConfiguration(configs[i]));
                }
            }
            configs = next.toArray(new Configuration[0]);
        }

        return numberAccepted;
    }


}
