package afctevaluator;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static afctevaluator.CFGAnalyzerInterface.getStackTraceAsString;

/**
 * The main AFCT-Evaluator class is run as afct-evaluator.jar, usage message is below in showHelp().
 *
 *  @author Jesse Burdick-Pless jb4411@rit.edu
 */
public class Main {
    private static String answerFilePath;
    private static String submissionFilePath;
    private static int maxStates = -1;
    public static boolean deterministic = false;
    private static boolean outputJson = false;

    /**
     * A helper method for displaying the help/usage message for AFCT-Evaluator.
     */
    private static void showHelp() {
        String helpMessage = """
                usage: afct-evaluator.jar [-h] [-v] [-j] answerFilePath submissionFilePath [maxStates] [deterministic]
                                
                positional arguments:
                    answerFilePath          path to the file with the correct answer
                    submissionFilePath      path to the file submitted
                    
                optional positional arguments:
                    maxStates               maximum number of states for PDAs and FAs (-1 means no limit)
                    deterministic           whether the answer FA is deterministic (valid input: {true,false})
                                
                options:
                    -h, --help              show this help message and exit
                    -v, --version           show the afct evaluator version and exit
                    -j, --json              display output in JSON format
                """;
        System.out.println(helpMessage);
    }

    /**
     * A helper method for displaying an error when invalid command line arguments are given.
     * The message is formatted as follows:
     *      error: invalid command line argument: {prefix} '{arg}' {suffix}
     *
     * @param prefix text to put before the invalid argument
     * @param arg the invalid argument
     * @param suffix text to put after the invalid argument
     */
    private static void invalidCliArg(String prefix, String arg, String suffix) {
        System.out.printf("error: invalid command line argument: %s '%s' %s\n", prefix, arg, suffix);
        showHelp();
        System.exit(1);
    }

    /**
     * A helper method for displaying an error when invalid command line arguments are given.
     * The message is formatted as follows:
     *      error: invalid command line argument: {prefix} '{arg}'
     *
     * @param prefix text to put before the invalid argument
     * @param arg the invalid argument
     */
    private static void invalidCliArg(String prefix, String arg) {
        System.out.printf("error: invalid command line argument: %s '%s'\n", prefix, arg);
        showHelp();
        System.exit(1);
    }

    /**
     * A helper method for displaying an error when invalid command line arguments are given.
     * The message is formatted as follows:
     *      error: invalid command line argument: '{arg}'
     *
     * @param arg the invalid argument
     */
    private static void invalidCliArg(String arg) {
        System.out.printf("error: invalid command line argument: '%s'\n", arg);
        showHelp();
        System.exit(1);
    }

    /**
     * A helper method for handling the command line argument [deterministic], and setting Main.deterministic accordingly.
     *
     * @param arg the value given for [deterministic] on the command line
     * @return true if arg is the string "true" or the string "false" (ignoring case), false otherwise
     */
    private static boolean handleDeterministic(String arg) {
        if (arg.equalsIgnoreCase("true")) {
            Main.deterministic = true;
            return true;
        } else if (arg.equalsIgnoreCase("false")) {
            Main.deterministic = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * A helper method for parsing and handling command line arguments.
     *
     * @param args the given command line arguments
     */
    private static void handleArgs(String[] args) {
        List<String> argList = Arrays.asList(args);

        if (argList.contains("-h") || argList.contains("--help")) {
            showHelp();
            System.exit(0);
        }

        if (argList.contains("-vj") || argList.contains("-jv")) {
            Gson gson = new Gson();
            String json = gson.toJson(new Version());
            System.out.println(json);
            System.exit(0);
        }

        if (argList.contains("-v") || argList.contains("--version")) {
            if (argList.contains("-j") || argList.contains("--json")) {
                Gson gson = new Gson();
                String json = gson.toJson(new Version());
                System.out.println(json);
            } else {
                System.out.println(Main.class.getPackage().getImplementationVersion());
            }
            System.exit(0);
        }

        argList = new ArrayList<>(argList);

        if (argList.contains("-j")) {
            argList.removeAll(Collections.singleton("-j"));
            Main.outputJson = true;
        }
        if (argList.contains("--json")) {
            argList.removeAll(Collections.singleton("--json"));
            Main.outputJson = true;
        }

        if (argList.size() < 2) {
            System.out.print("error: not enough command line arguments: ");
            if (argList.isEmpty()) {
                System.out.println("answerFilePath and submissionFilePath are required.");
            } else if (argList.size() == 1) {
                System.out.println("submissionFilePath is required.");
            }
            showHelp();
            System.exit(1);
        }

        Main.answerFilePath = argList.get(0);
        Main.submissionFilePath = argList.get(1);

        if (argList.size() > 3) {
            try {
                Main.maxStates = Integer.parseInt(argList.get(2));
            } catch (NumberFormatException ignored) {
                invalidCliArg("maxStates must be an integer: unable to convert", argList.get(2), "to an integer.");
            }
            if (!handleDeterministic(argList.get(3))) {
                invalidCliArg("deterministic must be 'true' or 'false' but got:", argList.get(3));
            }
        } else if (argList.size() > 2) {
            try {
                Main.maxStates = Integer.parseInt(argList.get(2));
            } catch (NumberFormatException ignored) {
                if (!handleDeterministic(argList.get(2))) {
                    invalidCliArg("maxStates must be an integer: unable to convert", argList.get(2), "to an integer.");
                }
            }
        }
    }

    /**
     * The main entry point for the AFCT-Evaluator program. Prints feedback to System.out.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        handleArgs(args);
        CheckSubmission checkSubmission = new CheckSubmission();
        Feedback feedback;
        try {
            feedback = checkSubmission.isCorrect(Main.answerFilePath, Main.submissionFilePath, Main.maxStates, Main.deterministic);
        } catch (Exception e) {
            String feedbackMessage = "An error occurred while checking your answer. Please try again. If this occurs repeatedly, please contact your professor.";
            String errorMessage = getStackTraceAsString(e);
            feedback = new Feedback(feedbackMessage, false, errorMessage);
        }
        feedback.addWarningsAndErrors(checkSubmission.warnings, checkSubmission.errors);

        if (Main.outputJson) {
            Gson gson = new Gson();
            String json = gson.toJson(feedback);
            System.out.println(json);
        } else {
            System.out.println("Feedback: ");
            System.out.println(feedback.correct);
            System.out.println(feedback.feedback);

            if (!feedback.warnings.isEmpty()) {
                System.out.println("\nWarnings:");
                for (String warning : feedback.warnings) {
                    System.out.println(warning);
                }
            }

            if (!feedback.errors.isEmpty()) {
                System.out.println("\nErrors:");
                for (String error : feedback.errors) {
                    System.out.println(error);
                }
            }
        }
    }
}
