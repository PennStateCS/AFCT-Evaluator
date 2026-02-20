package afctevaluator;

import automata.*;
import automata.turing.NDTMSimulator;
import automata.turing.TMSimulator;
import automata.turing.TuringMachine;
import grammar.Grammar;
import grammar.GrammarChecker;
import grammar.Production;
import grammar.parse.BruteParser;
import grammar.parse.BruteParserEvent;
import grammar.parse.BruteParserListener;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static automata.SimulatorFactory.getSimulator;

public class CFGAnalyzerInterface {
    boolean errored = false;
    String status = null;
    Integer result = null;
    boolean encounteredTimeout = false;

    private static String quoteRHSTerminals(Production p) {
        String[] terminals = p.getTerminals();
        ArrayList<String> rhs = new ArrayList<>();
        String[] symbols = p.getSymbolsOnRHS();

        for (String str : symbols) {
            boolean isTerm = false;

            for (String t : terminals) {
                if (t.equals(str)) {
                    isTerm = true;
                    break;
                }
            }

            if (isTerm) {
                rhs.add("\"" + str + "\"");
            } else {
                rhs.add(str);
            }
        }

        return String.join(" ", rhs);
    }

    public static String convertGrammar(Serializable input) {
        StringBuilder result = new StringBuilder();

        if (input instanceof Grammar g) {
            ArrayList<Production> productions = new ArrayList<>(Arrays.asList(g.getProductions()));
            final ArrayList<String> lhss = new ArrayList<>();
            String lastLHS = null;
            Iterator<Production> productionIterator = productions.iterator();

            Production p;
            while (productionIterator.hasNext()) {
                p = productionIterator.next();
                if (!lhss.contains(p.getLHS())) {
                    lhss.add(p.getLHS());
                }
            }

            productions.sort(new Comparator<Production>() {
                public int compare(Production p1, Production p2) {
                    return Integer.compare(lhss.indexOf(p1.getLHS()), lhss.indexOf(p2.getLHS()));
                }
            });

            String lhs;
            if (GrammarChecker.isContextFreeGrammar(g)) {
                for (productionIterator = productions.iterator(); productionIterator.hasNext(); result.append(lhs).append(quoteRHSTerminals(p)).append(";\n")) {
                    p = productionIterator.next();
                    lhs = " : ";
                    if (!p.getLHS().equals(lastLHS)) {
                        lastLHS = p.getLHS();
                        lhs = lastLHS + " : ";
                    }
                }
            } else {
                return null;
            }
        }

        return result.toString();
    }

    public static String OLDgradeCFG(String answerStr, String submittedStr, String analyzer, int limit) {
        String ret = null;

        try {
            File answerTempFile = File.createTempFile("cfganalyzer", ".cfg");
            try (PrintStream answerOutput = new PrintStream(answerTempFile)) {
                answerOutput.println(answerStr);
            }

            File submittedTempFile = File.createTempFile("cfganalyzer", ".cfg");
            try (PrintStream submittedOutput = new PrintStream(submittedTempFile)) {
                submittedOutput.println(submittedStr);
            }

            ProcessBuilder pb = new ProcessBuilder(analyzer, "--equivalence", "--maxbound", Integer.toString(limit), answerTempFile.getAbsolutePath(), submittedTempFile.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process p = pb.start();

            if (p.waitFor() != 2) {
                ret = p.getInputStream().toString();
            }

            answerTempFile.delete();
            submittedTempFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        return ret;
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getSimpleName()).append(": ").append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private Feedback getGrade(File answerTempFile, File submittedTempFile, String analyzer, String limit) {
        String feedback = null;
        String error = null;
        boolean badWaitFor = false;

        try {
            ProcessBuilder pb = new ProcessBuilder(analyzer, "--equivalence", "--maxbound", limit, answerTempFile.getAbsolutePath(), submittedTempFile.getAbsolutePath());

            // This should never be done. It literally just allows for arbitrary code execution.
            // ONLY use for local testing.
//            String[] parts = analyzer.split(" ");
//            List<String> partsList = Arrays.asList(parts);
//            ProcessBuilder pb = new ProcessBuilder(partsList);

            pb.redirectErrorStream(true);

            Process p = pb.start();

            if (p.waitFor() == 2) {
                badWaitFor = true;
            }
            feedback = new String(p.getInputStream().readAllBytes());

        } catch (IOException e) {
            error = getStackTraceAsString(e);
            //Feedback errorFeedback = Feedback.contactProfessorError("CFGAnalyzer error!");
            Feedback errorFeedback = Feedback.contactProfessorError(error);
            errorFeedback.errors.add(error);
            return errorFeedback;
            //e.printStackTrace();
        } catch (InterruptedException ie) {
            error = getStackTraceAsString(ie);
            //Feedback errorFeedback = Feedback.contactProfessorError("CFGAnalyzer timed out!");
            Feedback errorFeedback = Feedback.contactProfessorError(error);
            errorFeedback.errors.add(error);
            return errorFeedback;
            //ie.printStackTrace();
        }

        Feedback result = new Feedback(feedback, false, error);
        if (badWaitFor) {
            result.warnings.add("p.waitFor() = 2");
        }
        return result;
    }

    public Feedback gradeCFG(String answerStr, String submittedStr, String analyzer, int limit) {
        Feedback trackWarningsAndErrors = new Feedback(null, false);
        Feedback feedback;
        String text;
        String error = null;
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<String> errors = new ArrayList<>();
        boolean correct = false;
        try {
            File answerTempFile = File.createTempFile("cfganalyzer", ".cfg");
            try (PrintStream answerOutput = new PrintStream(answerTempFile)) {
                answerOutput.println(answerStr);
            }

            File submittedTempFile = File.createTempFile("cfganalyzer", ".cfg");
            try (PrintStream submittedOutput = new PrintStream(submittedTempFile)) {
                submittedOutput.println(submittedStr);
            }
            String limitStr = Integer.toString(limit);
            feedback = getGrade(answerTempFile, submittedTempFile, analyzer, limitStr);
            trackWarningsAndErrors.addWarningsAndErrors(feedback.warnings, feedback.errors);
            boolean errored1 = !feedback.errors.isEmpty();
            this.errored = errored1;

            boolean errored2 = false;
            if (feedback.feedback == null) {
                feedback = getGrade(submittedTempFile, answerTempFile, analyzer, limitStr);
                trackWarningsAndErrors.addWarningsAndErrors(feedback.warnings, feedback.errors);
                errored2 = !feedback.errors.isEmpty();
                this.errored = errored2;
            }

            if (feedback.feedback == null) {
                // This is bad...
                text = "Error: CFGAnalyzer feedback was null both times. Please contact your professor.";
            } else {
                if (feedback.feedback.isEmpty()) {
                    correct = true;
                    text = "Correct!";
                } else {
                    text = feedback.feedback;
                }
            }

            feedback = new Feedback(text, correct);
            feedback.addWarningsAndErrors(trackWarningsAndErrors.warnings, trackWarningsAndErrors.errors);
            return feedback;
        } catch (FileNotFoundException fne) {
            error = getStackTraceAsString(fne);
            //fne.printStackTrace();
        } catch (IOException e) {
            error = getStackTraceAsString(e);
            //e.printStackTrace();
        }
        feedback = new Feedback("A server error occurred. Please contact your professor.", false);
        trackWarningsAndErrors.errors.add(error);
        feedback.addWarningsAndErrors(trackWarningsAndErrors.warnings, trackWarningsAndErrors.errors);
        return feedback;
    }

    private int doBruteForceParse(Grammar grammar, String input) throws InterruptedException {
        BruteParser parser = BruteParser.get(grammar, input);

        parser.addBruteParserListener(new BruteParserListener() {
            public void bruteParserStateChange(BruteParserEvent e) {
                synchronized (e.getParser()) {
                    switch (e.getType()) {
                        case BruteParserEvent.START:
                            break;
                        case BruteParserEvent.REJECT:
                            status = "String rejected.";
                            break;
                        case BruteParserEvent.PAUSE:
                            status = "Parser paused.";
                            break;
                        case BruteParserEvent.ACCEPT:
                            status = "String accepted!";
                            break;
                    }
                    if (parser.isFinished()) {
                        if (e.isAccept()) {
                            // Accepted
                            result = Math.max(parser.getTotalNodeCount(), 1);
                        } else if (e.isReject()) {
                            // Rejected!
                            result = 0;
                        } else {
                            result = -1;
                        }
                    }
                }
            }
        });
        parser.start();

        Thread parseThread = parser.getParseThread();
        parseThread.join(Duration.ofSeconds(10));
//        if (parseThread.isAlive()) {
//
//        }
        String temp = "";
        if (parser.isFinished()) {
            temp = "temp1" + temp + "temp2" + this.status;
        }

        if (parser.getAnswer() != null) {
            return result;
        }
        return result;
    }

    /**
     * A helper method for checking if the given grammar accepts the given input.
     *
     * @param grammar the grammar to test
     * @param input the object that represents the input
     * @return int >= 1 if the input is accepted, 0 if the input is rejected, -1 if the test ended early
     */
    private int testAcceptance(Grammar grammar, String input) {
        // TODO: pick which parseer to use intelligently
        //  - i.e. pick the one that is likely to be the fastest
        int result;
        try {
            result = doBruteForceParse(grammar, input);
        } catch (InterruptedException e) {
            if (this.result != null) {
                return this.result;
            }
            return -1;
        } catch (IllegalArgumentException ie) {
            // TODO: add this to the tracked errors for the feedback
            return -1;
        }
        if (this.result != null) {
            return this.result;
        }
        return result;
    }

    private int parallelParse(Grammar answer, Grammar submitted, String witness) {
        // TODO: maybe start parsing on both submission and answer at once?
        return -1;
    }


    /**
     * A helper method for determining if the given witness string should or should not be accepted.
     *
     * @param answer the correct grammar
     * @param submitted the (incorrect) submitted grammar
     * @param witness the witness string
     * @return int >= 1 if the input should be accepted, 0 if the input should be rejected, -1 if the test ended early
     */
    private int determineWitnessType(Grammar answer, Grammar submitted, String witness) {
        int answerResult = testAcceptance(answer, witness);

        if (answerResult == -1) {
            encounteredTimeout = true;
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


    public static Feedback handleGrammar(Grammar answer, Grammar submitted, String analyzer, int limit) {
        String answerStr = convertGrammar(answer);
        String submittedStr = convertGrammar(submitted);
        CFGAnalyzerInterface grader = new CFGAnalyzerInterface();
        Feedback feedback = grader.gradeCFG(answerStr, submittedStr, analyzer, limit);

//        if (grader.errored) {
//            feedback.feedback = feedback.feedback + "Errored";
//            return feedback;
//        }
        if (!feedback.correct) {
            String[] parts = feedback.feedback.split("\"");
            if (parts.length < 2) {
                return feedback;
            }

            String feedbackStr = "Your answer is incorrect. ";

            if (feedback.warnings.contains("p.waitFor() = 2")) {
                Pattern pattern = Pattern.compile("nonterminal\\s+(.+?)\\s+seems to have no definition");
                Matcher matcher = pattern.matcher(feedback.feedback);

                if (matcher.find()) {
                    // TODO: maybe look into way to auto adjust input so that capital letters can be treated as non-terminals
                    //      like maybe check left hand side of each rule, and is "A" for example is on the right hand
                    //      side of some rule, but never on the left hand side, treat it as a non-terminal?
                    String nonterminal = matcher.group(1);
                    //feedback.feedback = feedbackStr + "The variable \"" + nonterminal + "\" is not defined in your grammar.";
                    //feedback.feedback = feedbackStr + "The variable \"" + nonterminal + "\" has no production rules in your grammar.";
                    feedback.feedback = feedbackStr + "Your grammar has a variable, \"" + nonterminal + "\", with no defined production rules.";
                    return feedback;
                }
            }

            String witness = parts[1];
            int witnessType = grader.determineWitnessType(answer, submitted, witness);

            if (witnessType == -1) {
//                if (witness.contentEquals("")) {
//                    feedbackStr = feedbackStr + "The empty string is an example.";
//                } else {
//                    feedbackStr = feedbackStr + "The string \"" + witness + "\" is an example.";
//                }
                //feedbackStr = "-1 -- ";
                feedbackStr += feedback.feedback;
            } else if (witnessType == 0) {
                if (witness.contentEquals("")) {
                    feedbackStr = feedbackStr + "The empty string should NOT be accepted.";
                } else {
                    feedbackStr = feedbackStr + "The string \"" + witness + "\" should NOT be accepted.";
                }
            } else {
                if (witness.contentEquals("")) {
                    feedbackStr = feedbackStr + "The empty string SHOULD be accepted.";
                } else {
                    feedbackStr = feedbackStr + "The string \"" + witness + "\" SHOULD be accepted.";
                }
            }

            feedback.feedback = feedbackStr;// + feedback.feedback;
        }

        return feedback;
    }
}
