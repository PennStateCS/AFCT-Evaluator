package afctevaluator;

import automata.State;
import automata.fsa.FSATransition;
import equivalence.EquivalenceNlgNWitness;
import equivalence.Grader;
import file.XMLCodec;

import java.awt.*;
import java.io.*;
import java.util.Map;

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
import gui.environment.RegularEnvironment;
import gui.environment.Universe;
import gui.regular.ConvertToAutomatonPane;
import gui.regular.REToFSAController;
import pumping.ContextFreePumpingLemma;
import pumping.PumpingLemma;
import pumping.RegularPumpingLemma;
import regular.Discretizer;
import regular.RegularExpression;

public class CheckSubmission {
    private final XMLCodec codec = new XMLCodec();

    public static class Feedback {
        public String feedback;
        public boolean correct;

        public Feedback(String feedback, boolean correct) {
            this.feedback = feedback;
            this.correct = correct;
        }
    }

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

    public Feedback isCorrect(String answerFilePath, String submissionFilePath, int maxStates, boolean deterministic) {
        Serializable answer = readAndDecode(answerFilePath);
        Serializable submitted = readAndDecode(submissionFilePath);
        return this.isCorrect(answer, submitted, maxStates, deterministic);
    }

    public Feedback isCorrect(File answerFile, File submissionFile, int maxStates, boolean deterministic) {
        Serializable answer = decode(answerFile);
        Serializable submitted = decode(submissionFile);
        return this.isCorrect(answer, submitted, maxStates, deterministic);
    }

    private static Feedback submissionTypeError(String expected, Serializable submitted) {
        String text = String.format("ERROR: expected submission to be a %s, but got a %s", expected, submitted.getClass());
        return new Feedback(text, false);
    }

    private Feedback handleFSA(FiniteStateAutomaton answerFSA, FiniteStateAutomaton submittedFSA) {
        EquivalenceNlgNWitness grader = new EquivalenceNlgNWitness(answerFSA, submittedFSA, true);

        boolean correct = grader.areAutomataEquivalent();
        String feedback = "Correct!";

        if (grader.getHasInputError()) {
            feedback = grader.getInputErrorMessage();
        } else if (!correct) {
            String witness = grader.getWitness();
            feedback = "Your answer is incorrect. ";

            if (witness.contentEquals("")) {
                feedback = feedback + "The empty string is an example.";
            } else {
                feedback = feedback + "The string \"" + witness + "\" is an example.";
            }
        }

        return new Feedback(feedback, correct);
    }

    private static FiniteStateAutomaton REToNFA(RegularExpression re) {
        FiniteStateAutomaton nfa = new FiniteStateAutomaton();
        State initialState = nfa.createState(new Point(60, 40));
        State finalState = nfa.createState(new Point(450, 250));
        String transString = Discretizer.delambda(re.asString().replace('!', Universe.curProfile.getEmptyString().charAt(0)));
        FSATransition initialTransition = new FSATransition(initialState, finalState, transString);
        nfa.setInitialState(initialState);
        nfa.addFinalState(finalState);
        nfa.addTransition(initialTransition);
        RegularEnvironment reEnv = new RegularEnvironment(re);
        ConvertToAutomatonPane convPane = new ConvertToAutomatonPane(reEnv);
        REToFSAController controller = new REToFSAController(convPane, nfa);
        controller.completeAll();
        return nfa;
    }

    private Feedback handleRE(RegularExpression answer, RegularExpression submitted) {
        FiniteStateAutomaton answerFSA = REToNFA(answer);
        FiniteStateAutomaton submittedFSA = REToNFA(submitted);
        return handleFSA(answerFSA, submittedFSA);
    }

    public Feedback isCorrect(Serializable answer, Serializable submitted, int maxStates, boolean deterministic) {
        if (answer == null && submitted == null) {
            return new Feedback("ERROR: both the submission and the answer are null!", false);
        } else if (answer == null) {
            return new Feedback("ERROR: answer is null!", false);
        } else if (submitted == null) {
            return new Feedback("ERROR: submission is null!", false);
        }

        switch (answer) {
            case FiniteStateAutomaton answerFSA -> {
                System.out.println("FiniteStateAutomaton");
                if (!(submitted instanceof FiniteStateAutomaton submittedFSA)) {
                    return submissionTypeError(FiniteStateAutomaton.class.getName(), submitted);
                }
                if (deterministic && !Grader.isSipserDFA(submittedFSA)) {
                    return new Feedback("Your submission is not deterministic.", false);
                } else if ((maxStates > 0) && (submittedFSA.getStates().length > maxStates)) {
                    return new Feedback("Your submission has too many states.", false);
                }
                return handleFSA(answerFSA, submittedFSA);
            }
            case RegularExpression answerRE -> {
                System.out.println("RegularExpression");
                if (!(submitted instanceof RegularExpression submittedRE)) {
                    //return new Feedback("Your submission must be a Regular Expression.", false);
                    return submissionTypeError(RegularExpression.class.getName(), submitted);
                }
                return handleRE(answerRE, submittedRE);
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
