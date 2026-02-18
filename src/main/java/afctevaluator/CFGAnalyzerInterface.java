package afctevaluator;

import grammar.CNFConverter;
import grammar.Grammar;
import grammar.GrammarChecker;
import grammar.Production;

import java.io.*;
import java.util.*;

public class CFGAnalyzerInterface {
    private static String quoteRHSTerminals(Production p) {
        String[] terminals = p.getTerminals();
        ArrayList<String> rhs = new ArrayList<>();
        String[] symbols = CNFConverter.separateString(p.getRHS());

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
            lhss.add(g.getStartVariable());
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
            if (true || GrammarChecker.isContextFreeGrammar(g)) {
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
        sb.append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private static Feedback getGrade(File answerTempFile, File submittedTempFile, String analyzer, String limit) {
        String feedback = null;
        String error = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(analyzer, "--equivalence", "--maxbound", limit, answerTempFile.getAbsolutePath(), submittedTempFile.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process p = pb.start();

            if (true || p.waitFor() != 2) {
                feedback = new String(p.getInputStream().readAllBytes());
            }
            System.out.println("CFGA SAYS: " + feedback);
        } catch (IOException e) {
            error = getStackTraceAsString(e);
            Feedback errorFeedback = Feedback.contactProfessorError("CFGAnalyzer error!");
            //Feedback errorFeedback = Feedback.contactProfessorError(error);
            errorFeedback.errors.add(error);
            return errorFeedback;
            //e.printStackTrace();
        } catch (InterruptedException ie) {
            error = getStackTraceAsString(ie);
            Feedback errorFeedback = Feedback.contactProfessorError("CFGAnalyzer timed out!");
            //Feedback errorFeedback = Feedback.contactProfessorError(error);
            errorFeedback.errors.add(error);
            return errorFeedback;
            //ie.printStackTrace();
        }

        return new Feedback(feedback, false, error);
    }

    public static Feedback gradeCFG(String answerStr, String submittedStr, String analyzer, int limit) {
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

            if (feedback.feedback == null) {
                feedback = getGrade(submittedTempFile, answerTempFile, analyzer, limitStr);
                trackWarningsAndErrors.addWarningsAndErrors(feedback.warnings, feedback.errors);
            }

            if (feedback.feedback.isEmpty()) {
                correct = true;
                text = "Correct!";
            } else {
                text = feedback.feedback;
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
}
