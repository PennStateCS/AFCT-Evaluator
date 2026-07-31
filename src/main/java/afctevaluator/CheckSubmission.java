package afctevaluator;

import automata.*;
import automata.fsa.FSATransition;
import automata.turing.NDTMSimulator;
import automata.turing.TMSimulator;
import automata.turing.TuringMachine;
import equivalence.EquivalenceNlgNWitness;
import equivalence.Grader;
import file.DataException;
import file.XMLCodec;

import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

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

import static afctevaluator.CFGAnalyzerInterface.*;
import static afctevaluator.Feedback.getInitialStateMissingFeedbackString;
import static afctevaluator.Main.getTimeTaken;
import static afctevaluator.WitnessFeedbackHelper.getFeedback;
import static automata.SimulatorFactory.getSimulator;
import static conversions.PDAToCFG.setupPDA;
import static conversions.PDAToCFG.transformPDA;

/**
 * Holds methods used to determine if a submission is correct, and returns a "witness string" for incorrect submissions.
 *
 * @author Jesse Burdick-Pless jb4411@rit.edu
 */
public class CheckSubmission {
    private final XMLCodec codec = new XMLCodec();
    //int limit = Integer.parseInt(this.env.getProperty("cfganalyzer.limit"));
    //String analyzer = this.env.getProperty("cfganalyzer.binary");
    // Set to 15 based on the application.properties file from the original AFCT server
    // I have genuinely no idea if this is a good value to use
    // TODO - make these possible to change dynamically from the website
    private static final String CFGANALYZER_LIMIT = "CFGANALYZER_LIMIT";
    private static final String CFGANALYZER_BINARY = "CFGANALYZER_BINARY";
    private static final String UPGRADED_FEEDBACK = "UPGRADED_FEEDBACK";
    private static final String TIMEOUT_SECONDS = "TIMEOUT_SECONDS";
    int limit;
    String analyzer;
    File file;
    public static boolean useUpgradedFeedback = true;
    public static long timeAllowedInSeconds = 30;
    public static long timeAllowedMilli = 30_000;
    public static boolean unknownTimeAllowed = false;
    public ArrayList<String> warnings;
    public ArrayList<String> errors;

    /**
     * Constructor for CheckSubmission.
     */
    public CheckSubmission() {
        // Sets the openOrRead property to true so .jff files are parsed correctly
        // If openOrRead is false, all files appear to be treated as turing machines, and will open a GUI dialog
        // prompting the user for input, which would halt to server
        OpenAction.setOpenOrRead(true);

        warnings = new ArrayList<>();
        errors = new ArrayList<>();

        String limitString;

        boolean useDefaultLimit = true;
        try {
            limitString = System.getenv(CFGANALYZER_LIMIT);
            if (limitString == null) {
                warnings.add(String.format("warning: environment variable '%s' not set", CFGANALYZER_LIMIT));
            } else {
                limit = Integer.parseInt(limitString);
                useDefaultLimit = false;
            }
        } catch (SecurityException e) {
            envVarError("inaccessible", "insufficient permissions to access environment variable", CFGANALYZER_LIMIT);
        } catch (NumberFormatException e) {
            envVarError("invalid", String.format("'%s' must be an integer: unable to convert", CFGANALYZER_LIMIT), System.getenv(CFGANALYZER_LIMIT), "to an integer.");
            //String.format("%s must be an integer: unable to convert", varLimit);
            //System.out.printf("error: invalid environment variable: %s '%s' %s\n", varLimit, System.getenv(varLimit), varLimit, prefix, envVar, suffix);
        }

        if (useDefaultLimit) {
            limit = 15;
            warnings.add(String.format("Using default CFGAnalyzer limit: %d", limit));
        }

        // Check if file was given and if it exists with proper permissions
        try {
            analyzer = System.getenv(CFGANALYZER_BINARY);
            file = new File(analyzer);
            if (analyzer == null) {
                warnings.add(String.format("warning: environment variable '%s' not set", CFGANALYZER_BINARY));
            }
            else if(!file.exists()){{
                    warnings.add(String.format("warning: file '%s' does not exist on system", CFGANALYZER_BINARY));
                }
            }
        } catch (SecurityException e) {
            envVarError("inaccessible", "insufficient permissions to access environment variable", CFGANALYZER_BINARY);
        }

        // Check if upgraded feedback is set
        boolean defaultedToUpgradedFeedback = false;
        String upgradedFeedbackString;
        try {
            upgradedFeedbackString = System.getenv(UPGRADED_FEEDBACK);
            if (upgradedFeedbackString == null) {
                warnings.add(String.format("warning: environment variable '%s' not set", UPGRADED_FEEDBACK));
                defaultedToUpgradedFeedback = true;
            } else {
                if (upgradedFeedbackString.strip().equalsIgnoreCase("true")) {
                    useUpgradedFeedback = true;
                } else if (upgradedFeedbackString.strip().equalsIgnoreCase("false")) {
                    useUpgradedFeedback = false;
                    warnings.add("Upgraded feedback disabled.");
                } else {
                    envVarError("invalid", String.format("'%s' must be a boolean: unable to convert", UPGRADED_FEEDBACK), System.getenv(UPGRADED_FEEDBACK), "to a boolean.");
                    defaultedToUpgradedFeedback = true;
                }
            }
        } catch (SecurityException e) {
            envVarError("inaccessible", "insufficient permissions to access environment variable", CFGANALYZER_LIMIT);
            defaultedToUpgradedFeedback = true;
        }
        if (defaultedToUpgradedFeedback) {
            warnings.add("Defaulting to upgraded feedback.");
            useUpgradedFeedback = true;
        }

        // Check time allowed for evaluation
        String timeAllowedString;
        try {
            timeAllowedString = System.getenv(TIMEOUT_SECONDS);
            if (timeAllowedString == null) {
                warnings.add(String.format("warning: environment variable '%s' not set", TIMEOUT_SECONDS));
                unknownTimeAllowed = true;
            } else {
                timeAllowedInSeconds = Long.parseLong(timeAllowedString);
                timeAllowedMilli = timeAllowedInSeconds * 1000; // Convert from seconds to milliseconds
            }
        } catch (SecurityException e) {
            envVarError("inaccessible", "insufficient permissions to access environment variable", TIMEOUT_SECONDS);
            unknownTimeAllowed = true;
        } catch (NumberFormatException e) {
            envVarError("invalid", String.format("'%s' must be an integer: unable to convert", TIMEOUT_SECONDS), System.getenv(TIMEOUT_SECONDS), "to an integer.");
            unknownTimeAllowed = true;
        }
        if (unknownTimeAllowed) {
            warnings.add("Unknown time allowed, early stopping disabled for upgraded feedback..");
        }
    }

    /**
     * A helper method for displaying an error when issues with environment variables are encountered.
     * The message is formatted as follows:
     *      error: {reason} environment variable: {prefix} '{envVar}'
     *
     * @param reason text to put before 'environment variable'
     * @param prefix text to put before the environment variable
     * @param envVar the environment variable
     */
    private void envVarError(String reason, String prefix, String envVar) {
        this.errors.add(String.format("error: %s environment variable: %s '%s'\n", reason, prefix, envVar));
    }

    /**
     * A helper method for displaying an error when issues with environment variables are encountered.
     * The message is formatted as follows:
     *      error: {reason} environment variable: {prefix} '{envVar} {suffix}'
     *
     * @param reason text to put before 'environment variable'
     * @param prefix text to put before the environment variable
     * @param envVar the environment variable
     * @param suffix text to put after the environment variable
     */
    private void envVarError(String reason, String prefix, String envVar, String suffix) {
        this.errors.add(String.format("error: %s environment variable: %s '%s' %s\n", reason, prefix, envVar, suffix));
    }

    /**
     * A helper method for decoding .jff files.
     *
     * @param file the File object to decode
     * @return the object resulting from decoding the given file, or null if the file is not a .jff file
     */
    public Serializable decode(File file) throws DataException {
        if (!file.getName().endsWith(".jff")) {
            return null;
        }
        return codec.decode(file, null);
    }

    /**
     * A helper method for reading and decoding .jff files.
     *
     * @param filePath the path to the file to read in and decode
     * @return the object resulting from decoding the given file, or null if the file is not a .jff file
     */
    public Serializable readAndDecode(String filePath) throws DataException {
        File file = new File(filePath);
        return this.decode(file);
    }

    /**
     * A helper method that takes two file paths, checks if the submission is equivalent to the answer, and returns the
     * corresponding feedback.
     *
     * @param answerFilePath the path to the file containing the correct answer
     * @param submissionFilePath the path to the file containing the submission to compare to the answer
     * @param maxStates the maximum number of states for the problem (or -1 if there is no maximum)
     * @param deterministic whether the problem is deterministic
     * @return the corresponding feedback
     */
    public Feedback isCorrect(String answerFilePath, String submissionFilePath, int maxStates, boolean deterministic) {
        try {
            Serializable answer = readAndDecode(answerFilePath);
            Serializable submitted = readAndDecode(submissionFilePath);
            return this.isCorrect(answer, submitted, maxStates, deterministic);
        } catch (DataException e) {
            String feedback = "Error: " + e.getMessage();
            String errorMessage = getStackTraceAsString(e);;
            return new Feedback(feedback, false, errorMessage);
        }
    }

    /**
     * A helper method that takes two File objects, checks if the submission is equivalent to the answer, and returns
     * corresponding feedback.
     *
     * @param answerFile the File object containing the correct answer
     * @param submissionFile the File object containing the submission to compare to the answer
     * @param maxStates the maximum number of states for the problem (or -1 if there is no maximum)
     * @param deterministic whether the problem is deterministic
     * @return the corresponding feedback
     */
    public Feedback isCorrect(File answerFile, File submissionFile, int maxStates, boolean deterministic) {
        Serializable answer = decode(answerFile);
        Serializable submitted = decode(submissionFile);
        return this.isCorrect(answer, submitted, maxStates, deterministic);
    }

    /**
     * A helper method for checking if the given automaton accepts the given input.
     *
     * @param automaton the automaton to test
     * @param input the object that represents the input; this is a String in most cases,
     *              but may differ for multiple tape turing machines
     * @return int >= 1 if the input is accepted, 0 if the input is rejected, -1 if the test ended early
     */
    private int testAcceptance(Automaton automaton, Object input) {
        AutomatonSimulator simulator = getSimulator(automaton);

        Configuration[] configs;

        // Get the initial configurations.
        if (automaton instanceof TuringMachine) {
            String[] s = (String[]) input;
            //check for nondeterminism
            NondeterminismDetector d = NondeterminismDetectorFactory.getDetector(automaton);
            State[] nd = d.getNondeterministicStates(automaton);
            if(nd.length > 0) {
                configs = ((NDTMSimulator) simulator).getInitialConfigurations(s);
            } else {
                configs = ((TMSimulator) simulator).getInitialConfigurations(s);
            }
        } else {
            String s = (String) input;
            configs = simulator.getInitialConfigurations(s);
        }

        // How many configurations have we had?
        int numberGenerated = 0;
        // How many have accepted?
        int numberAccepted = 0;
        while (configs.length > 0) {
            numberGenerated += configs.length;

            // Make sure we should continue.
            if (numberGenerated >= 1000) {
                // TODO: determine a better way to handle lots of configs than just stopping after 1000
                //  that still avoids infinite loops...
                return -1;
            }

            // Get the next batch of configurations.
            ArrayList<Configuration> next = new ArrayList<>();
            for (Configuration config : configs) {
                if (config.isAccept()) {
                    numberAccepted++;
                    break;
                } else {
                    next.addAll(simulator.stepConfiguration(config));
                }
            }
            configs = next.toArray(new Configuration[0]);
        }

        return numberAccepted;
    }

    /**
     * A helper method for determining if the given witness string should or should not be accepted.
     *
     * @param answer the correct automaton
     * @param submitted the (incorrect) submitted automaton
     * @param witness the witness string
     * @return int >= 1 if the input should be accepted, 0 if the input should be rejected, -1 if the test ended early
     */
    private int determineWitnessType(Automaton answer, Automaton submitted, Object witness) {
        int answerResult = testAcceptance(answer, witness);

        if (answerResult == -1) {
            int submissionResult = testAcceptance(submitted, witness);

            if (submissionResult == -1) {
                return -1;
            } else if (submissionResult == 0) {
                answerResult = 1;
            } else {
                answerResult = 0;
            }
        }

        return answerResult;
    }

    /**
     * A helper method that checks if the submitted Finite State Automaton (FSA) is equivalent to the answer FSA, and
     * returns corresponding feedback.
     *
     * @param answerFSA the correct FSA
     * @param submittedFSA the submitted FSA
     * @return the corresponding feedback
     */
    private Feedback handleFSA(FiniteStateAutomaton answerFSA, FiniteStateAutomaton submittedFSA) {
        Feedback feedback = new Feedback();

        EquivalenceNlgNWitness grader = new EquivalenceNlgNWitness(answerFSA, submittedFSA, true);

        boolean correct = grader.areAutomataEquivalent();
        Instant equivalenceDone = Instant.now();
        //long timeTakenEquivalence = getTimeTaken();
        feedback.info.add("Equivalence - time taken: " + getTimeTaken(equivalenceDone) + " ms.");
        String feedbackStr = "Correct!";

        if (grader.getHasInputError()) {
            feedbackStr = grader.getInputErrorMessage();
            if (feedbackStr.equals("Initial state missing")) {
                feedbackStr = getInitialStateMissingFeedbackString();
            }
        } else if (!correct) {
            String witness = grader.getWitness();

            Integer witnessType;
            if (useUpgradedFeedback) {
                witnessType = determineWitnessType(answerFSA, submittedFSA, witness);
                long timeTakenWitnessType = Duration.between(equivalenceDone, Instant.now()).toMillis();
                feedback.info.add("Witness type check - time taken: " + timeTakenWitnessType + " ms.");
            } else {
                witnessType = null;
            }

            feedbackStr = getFeedback(false, witnessType, witness);
        }

        feedback.feedback = feedbackStr;
        feedback.correct = correct;
        return feedback;
    }

    /**
     * A helper method for converting a Regular Expression (RE) into a Nondeterministic Finite Automaton (NFA).
     *
     * @param re the RE to convert
     * @return the resulting NFA
     */
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

    /**
     * A helper method that converts the given answer and submission Regular Expressions (REs) into Nondeterministic
     * Finite Automatons (NFAs), then calls handleFSA() to check if they are equivalent, and returns corresponding
     * feedback.
     *
     * @param answer the correct RE
     * @param submitted the submitted RE
     * @return the corresponding feedback
     */
    private Feedback handleRE(RegularExpression answer, RegularExpression submitted) {
        FiniteStateAutomaton answerFSA = REToNFA(answer);
        FiniteStateAutomaton submittedFSA = REToNFA(submitted);

        return handleFSA(answerFSA, submittedFSA);
    }

    /**
     * A helper method that converts the given answer and submission Context Free Grammars (CFGs) into strings formatted
     * for CFGAnalyzer, calls gradeCFG() to check if they are equivalent, and returns corresponding feedback.
     *
     * @param answer the correct CFG
     * @param submitted the submitted CFG
     * @return the corresponding feedback
     */
    private Feedback handleCFG(ContextFreeGrammar answer, ContextFreeGrammar submitted) {
        return CFGAnalyzerInterface.handleGrammar(answer, submitted, this.analyzer, this.limit);
    }

    private Feedback handleGrammar(Grammar answer, Grammar submitted) {
        return CFGAnalyzerInterface.handleGrammar(answer, submitted, this.analyzer, this.limit);
    }

    private Feedback handleGrammar(Grammar answer, Grammar submitted, PushdownAutomaton pdaAnswer, PushdownAutomaton pdaSubmission) {
        return CFGAnalyzerInterface.handleGrammar(answer, submitted, this.analyzer, this.limit, pdaAnswer, pdaSubmission);
    }

    /**
     * A helper method for converting a Pushdown Automaton (PDA) into a Context Free Grammar (CFG).
     *
     * @param pda the PDA to convert
     * @return the resulting CFG
     */
    private static ContextFreeGrammar PDAToCFG(PushdownAutomaton pda) {
        setupPDA(pda);
        return transformPDA(pda);
    }

    /**
     * A helper method that converts the given answer and submission Pushdown Automatons (PDAs) into Context Free
     * Grammars (CFGs), then calls handleCFG() to check if they are equivalent, and returns corresponding feedback.
     *
     * @param answer the correct PDA
     * @param submitted the submitted PDA
     * @return the corresponding feedback
     */
    private Feedback handlePDA(PushdownAutomaton answer, PushdownAutomaton submitted) {
        Grammar answerCFG = PDAToCFG(answer);
        Grammar submittedCFG = PDAToCFG(submitted);
        return handleGrammar(answerCFG, submittedCFG, answer, submitted);
    }

    /**
     * Takes a correct answer and a submission, determines the type of problem, calls the method for handling that type
     * of problem to check if the submission is equivalent to the answer, and returns the corresponding feedback.
     *
     * @param answer the correct answer
     * @param submitted the submission to check
     * @param maxStates the maximum number of states for the problem (or -1 if there is no maximum)
     * @param deterministic whether the problem is deterministic
     * @return the corresponding feedback
     */
    public Feedback isCorrect(Serializable answer, Serializable submitted, int maxStates, boolean deterministic) {
        if (answer == null && submitted == null) {
            return Feedback.contactProfessorError("ERROR: both the submission and the answer are null!");
        } else if (answer == null) {
            return Feedback.contactProfessorError("ERROR: answer is null!");
        } else if (submitted == null) {
            return new Feedback("Your submission is null!", false);
        }

        switch (answer) {
            case FiniteStateAutomaton answerFSA -> {
                if (!(submitted instanceof FiniteStateAutomaton submittedFSA)) {
                    return Feedback.submissionTypeError(FiniteStateAutomaton.class, submitted);
                }
                if ((maxStates > 0) && (submittedFSA.getStates().length > maxStates)) {
                    return Feedback.tooManyStates(maxStates, submittedFSA.getStates().length);
                }

                Feedback fsaFeedback = handleFSA(answerFSA, submittedFSA);

                // Check for determinism AFTER everything else.
                if (deterministic && !Grader.isSipserDFA(submittedFSA)) {
                    String nonDeterministicMsg = "Your submission is not deterministic.";
                    // If correct, replace feedback message completely
                    if (fsaFeedback.correct) {
                        fsaFeedback.feedback = nonDeterministicMsg;
                    }
                    // If incorrect, append nonDeterministicMsg to feedback
                    else {
                        fsaFeedback.feedback = fsaFeedback.feedback + " " + nonDeterministicMsg;
                    }
                    fsaFeedback.correct = false;
                }
                return fsaFeedback;
            }
            case RegularExpression answerRE -> {
                if (!(submitted instanceof RegularExpression submittedRE)) {
                    return Feedback.submissionTypeError(RegularExpression.class, submitted);
                }
                return handleRE(answerRE, submittedRE);
            }
            case ContextFreeGrammar answerCFG -> {
                if (!(submitted instanceof ContextFreeGrammar submittedCFG)) {
                    return Feedback.submissionTypeError(ContextFreeGrammar.class, submitted);
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
            case Grammar answerGrammar -> {
                if (!(submitted instanceof Grammar submittedGrammar)) {
                    return Feedback.submissionTypeError(ContextFreeGrammar.class, submitted);
                }
                return handleGrammar(answerGrammar, submittedGrammar);
            }
            case PushdownAutomaton answerPDA -> {
                if (!(submitted instanceof PushdownAutomaton submittedPDA)) {
                    return Feedback.submissionTypeError(PushdownAutomaton.class, submitted);
                }
                if ((maxStates > 0) && (submittedPDA.getStates().length > maxStates)) {
                    return Feedback.tooManyStates(maxStates, submittedPDA.getStates().length);
                }
                if (submittedPDA.getInitialState() == null) {
                    return Feedback.initialStateMissing();
                }
                return handlePDA(answerPDA, submittedPDA);
            }
            default -> {
                String error = String.format("ERROR: %s is an unsupported answer type!", answer.getClass().getSimpleName());
                return Feedback.contactProfessorError(error);
            }
        }
    }
}
