package afctevaluator;

import automata.vdg.VariableDependencyGraph;
import grammar.*;
import grammar.parse.BruteParser;
import grammar.parse.BruteParserEvent;
import grammar.parse.BruteParserListener;
import grammar.parse.CYKParser;
import gui.grammar.transform.LambdaController;
import gui.grammar.transform.LambdaPane;
import gui.grammar.transform.UnitController;
import gui.grammar.transform.UnitPane;

import javax.swing.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class CFGParser {
    private String status = null;
    private Integer result = null;

    public CFGParser() {}

    public synchronized String getStatus() {
        return this.status;
    }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    public synchronized Integer getResult() {
        return this.result;
    }

    public synchronized void setResult(Integer result) {
        this.result = result;
    }


    private Integer doBruteForceParse(Grammar grammar, String input) {
        BruteParser parser = BruteParser.get(grammar, input);

        parser.addBruteParserListener(new BruteParserListener() {
            public void bruteParserStateChange(BruteParserEvent e) {
                synchronized (e.getParser()) {
                    switch (e.getType()) {
                        case BruteParserEvent.START:
                            break;
                        case BruteParserEvent.REJECT:
                            setStatus("String rejected.");
                            break;
                        case BruteParserEvent.PAUSE:
                            setStatus("Parser paused.");
                            break;
                        case BruteParserEvent.ACCEPT:
                            setStatus("String accepted!");
                            break;
                    }
                    if (parser.isFinished()) {
                        if (e.isAccept()) {
                            // Accepted
                            setResult(Math.max(parser.getTotalNodeCount(), 1));
                        } else if (e.isReject()) {
                            // Rejected!
                            setResult(0);
                        } else {
                            setResult(-1);
                        }
                    }
                }
            }
        });
        parser.start();

        Thread parseThread = parser.getParseThread();
        try {
            parseThread.join(Duration.ofSeconds(10));
        } catch (InterruptedException ignored) { }

        if (parser.getAnswer() != null) {
            return this.getResult();
        } else {
            return -1;
        }
    }



    private Integer doCYKParse(Grammar grammar, String input) {
        // Convert to CNF
        Grammar cnf = EasyCNFConverter.convertToCNF(grammar);
        // If conversion failed: cannot Proceed with CYK
        if (cnf == null) {
            return null;
        }

        CYKParser parser = new CYKParser(cnf);
        boolean accepted = parser.solve(input);

        if (accepted) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     *
     * @param grammar
     * @param input
     * @return int >= 1 if the input is accepted, 0 if the input is rejected, -1 if the test ended early,
     *         null if some catastrophic error occurred
     */
    public Integer parse(Grammar grammar, String input) {
        // TODO: pick which parser to use intelligently
        //  - i.e. pick the one that is likely to be the fastest

        // If the input is the empty string, check differently
        if (Objects.equals(input, "")) {
            LambdaProductionRemover remover = new LambdaProductionRemover();
            Set<String> lambdaDerivers = remover.getCompleteLambdaSet(grammar);
            remover.getCompleteLambdaSet(grammar);
            if (lambdaDerivers.contains(grammar.getStartVariable())) {
                return 1;
            } else {
                return 0;
            }
        }

        // Try CYK Parse
        Integer result = doCYKParse(grammar, input);
        if (result == null) {
            // Fall back on BruteForceParser
            return doBruteForceParse(grammar, input);
        }

        return result;
    }
}
