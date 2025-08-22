package afctevaluator;

import com.google.gson.Gson;

import java.util.Arrays;

/**
 * Tha main AFCT-Evaluator class is run as afct-evaluator.jar, usage message is below in showHelp().
 *
 *  @author Jesse Burdick-Pless jb4411@rit.edu
 */
public class Main {
    private static String answerFilePath;
    private static String submissionFilePath;
    private static int maxStates = -1;
    public static boolean deterministic = false;

    /**
     * A helper method for displaying the help/usage message for AFCT-Evaluator.
     */
    private static void showHelp() {
        String helpMessage = """
                usage: afct-evaluator.jar [-h] answerFilePath submissionFilePath [maxStates] [deterministic]
                                
                positional arguments:
                    answerFilePath          path to the file with the correct answer
                    submissionFilePath      path to the file submitted
                    
                optional positional arguments:
                    maxStates               maximum number of states for PDAs and FAs (-1 means no limit)
                    deterministic           whether the answer FA is deterministic (valid input: {true,false})
                                
                options:
                    -h, --help              show this help message and exit
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
        if (Arrays.asList(args).contains("-h") || Arrays.asList(args).contains("--help")) {
            showHelp();
            System.exit(0);
        }

        if (args.length < 2) {
            System.out.print("error: not enough command line arguments: ");
            if (args.length == 0) {
                System.out.println("answerFilePath and submissionFilePath are required.");
            } else if (args.length == 1) {
                System.out.println("submissionFilePath is required.");
            }
            showHelp();
            System.exit(1);
        }

        Main.answerFilePath = args[0];
        Main.submissionFilePath = args[1];

        if (args.length > 3) {
            try {
                Main.maxStates = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                invalidCliArg("maxStates must be an integer: unable to convert", args[2], "to an integer.");
            }
            if (!handleDeterministic(args[3])) {
                invalidCliArg("deterministic must be 'true' or 'false' but got:", args[3]);
            }
        } else if (args.length > 2) {
            try {
                Main.maxStates = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                if (!handleDeterministic(args[2])) {
                    invalidCliArg(args[2]);
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
        Feedback feedback = checkSubmission.isCorrect(Main.answerFilePath, Main.submissionFilePath, Main.maxStates, Main.deterministic);
        feedback.addWarningsAndErrors(checkSubmission.warnings, checkSubmission.errors);

        Gson gson = new Gson();
        String json = gson.toJson(feedback);
        System.out.println(json);
    }
}
