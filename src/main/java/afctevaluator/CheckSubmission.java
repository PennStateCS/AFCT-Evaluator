package afctevaluator;

import file.XMLCodec;

import java.io.*;

import automata.Automaton;
import automata.fsa.FiniteStateAutomaton;
import automata.mealy.MealyMachine;
import automata.mealy.MooreMachine;
import automata.pda.PushdownAutomaton;
import automata.turing.TuringMachine;
import automata.turing.TuringMachineBuildingBlocks;
import grammar.ConvertedUnrestrictedGrammar;
import grammar.Grammar;
import grammar.UnboundGrammar;
import grammar.UnrestrictedGrammar;
import grammar.cfg.ContextFreeGrammar;
import grammar.reg.RegularGrammar;
import grammar.reg.RightLinearGrammar;
import gui.action.OpenAction;
import pumping.ContextFreePumpingLemma;
import pumping.PumpingLemma;
import pumping.RegularPumpingLemma;
import regular.RegularExpression;

public class CheckSubmission {
    private final XMLCodec codec = new XMLCodec();

    public CheckSubmission() {
        OpenAction.setOpenOrRead(true);
    }

    public Serializable decode(File file) {
        if (!file.getName().endsWith(".jff")) {
            return null;
        }
        return codec.decode(file, null);
    }

    public Serializable readAndDecode(String filePath) {
        File file = new File(filePath);
        return this.decode(file);
    }

    public void isCorrect(String answerFilePath, String submissionFilePath, boolean deterministic) {
        Serializable answer = readAndDecode(answerFilePath);
        Serializable submitted = readAndDecode(submissionFilePath);
        this.isCorrect(answer, submitted, deterministic);
    }

    public void isCorrect(File answerFile, File submissionFile, boolean deterministic) {
        Serializable answer = decode(answerFile);
        Serializable submitted = decode(submissionFile);
        this.isCorrect(answer, submitted, deterministic);
    }

    public void isCorrect(Serializable answer, Serializable submitted, boolean deterministic) {
        if (answer == null && submitted == null) {
            System.err.println("ERROR: both the answer and the submission are null!");
            return;
        } else if (answer == null) {
            System.err.println("ERROR: answer is null!");
            return;
        } else if (submitted == null) {
            System.err.println("ERROR: submission is null!");
            return;
        }

        switch (answer) {
            case FiniteStateAutomaton fsa -> {
                System.out.println("FiniteStateAutomaton");
            }
            case RegularExpression re -> {
                System.out.println("RegularExpression");
            }
            case PushdownAutomaton pda -> {
                System.out.println("PushdownAutomaton");
            }
            case ContextFreeGrammar cfg -> {
                System.out.println("ContextFreeGrammar");
            }
            default -> {
                System.out.println("BAD");
            }
        }
    }
}
