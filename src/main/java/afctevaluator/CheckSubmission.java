package afctevaluator;

import automata.State;
import automata.fsa.FSATransition;
import equivalence.EquivalenceNlgNWitness;
import equivalence.Grader;
import file.XMLCodec;

import java.awt.*;
import java.io.*;

import automata.fsa.FiniteStateAutomaton;
import automata.pda.PushdownAutomaton;
import grammar.*;
import grammar.cfg.ContextFreeGrammar;
import gui.action.OpenAction;
import gui.environment.RegularEnvironment;
import gui.environment.Universe;
import gui.regular.ConvertToAutomatonPane;
import gui.regular.REToFSAController;
import regular.Discretizer;
import regular.RegularExpression;

import static afctevaluator.CFGAnalyzerInterface.convertGrammar;
import static afctevaluator.CFGAnalyzerInterface.gradeCFG;
import static conversions.PDAToCFG.setupPDA;
import static conversions.PDAToCFG.transformPDA;

public class CheckSubmission {
    private final XMLCodec codec = new XMLCodec();
    //int limit = Integer.parseInt(this.env.getProperty("cfganalyzer.limit"));
    //String analyzer = this.env.getProperty("cfganalyzer.binary");
    // TODO - set these
    int limit;
    String analyzer;

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

    private static Feedback tooManyStates(int expected, int actual) {
        String text = String.format("Your submission has too many states. (%d > %d)", expected, actual);
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

    private Feedback handleCFG(ContextFreeGrammar answer, ContextFreeGrammar submitted) {
        String answerStr = convertGrammar(answer);
        String submittedStr = convertGrammar(submitted);

        return gradeCFG(answerStr, submittedStr, this.analyzer, this.limit);
    }

    private static ContextFreeGrammar PDAToCFG(PushdownAutomaton pda) {
        setupPDA(pda);
        return transformPDA(pda);
    }

    private Feedback handlePDA(PushdownAutomaton answer, PushdownAutomaton submitted) {
        ContextFreeGrammar answerCFG = PDAToCFG(answer);
        ContextFreeGrammar submittedCFG = PDAToCFG(submitted);
        return handleCFG(answerCFG, submittedCFG);
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
                if (!(submitted instanceof FiniteStateAutomaton submittedFSA)) {
                    return submissionTypeError(FiniteStateAutomaton.class.getName(), submitted);
                }
                if (deterministic && !Grader.isSipserDFA(submittedFSA)) {
                    return new Feedback("Your submission is not deterministic.", false);
                } else if ((maxStates > 0) && (submittedFSA.getStates().length > maxStates)) {
                    return tooManyStates(maxStates, submittedFSA.getStates().length);
                }
                return handleFSA(answerFSA, submittedFSA);
            }
            case RegularExpression answerRE -> {
                if (!(submitted instanceof RegularExpression submittedRE)) {
                    return submissionTypeError(RegularExpression.class.getName(), submitted);
                }
                return handleRE(answerRE, submittedRE);
            }
            case ContextFreeGrammar answerCFG -> {
                if (!(submitted instanceof ContextFreeGrammar submittedCFG)) {
                    return submissionTypeError(ContextFreeGrammar.class.getName(), submitted);
                }
                if (!GrammarChecker.isContextFreeGrammar(submittedCFG)) {
                    return new Feedback("Your grammar is not context-free.", false);
                }
                String[] unresolved = GrammarChecker.getUnresolvedVariables(submittedCFG);
                if (unresolved.length > 0) {
                    String feedback;
                    if (unresolved.length > 1) {
                        feedback = "Variables " + String.join(", ", unresolved) + " are unresolved.";
                    } else {
                        feedback = "Variable " + unresolved[0] + " is unresolved.";
                    }
                    return new Feedback(feedback, false);
                }
                return handleCFG(answerCFG, submittedCFG);
            }
            case PushdownAutomaton answerPDA -> {
                if (!(submitted instanceof PushdownAutomaton submittedPDA)) {
                    return submissionTypeError(PushdownAutomaton.class.getName(), submitted);
                }
                if ((maxStates > 0) && (submittedPDA.getStates().length > maxStates)) {
                    return tooManyStates(maxStates, submittedPDA.getStates().length);
                }
                return handlePDA(answerPDA, submittedPDA);
            }
            default -> {
                String text = String.format("ERROR: %s is an unsupported answer type! Please contact your professor.", answer.getClass());
                return new Feedback(text, false);
            }
        }
    }
}
