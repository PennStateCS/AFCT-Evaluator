package afctevaluator;

import java.util.Arrays;

public class Main {
    private static String answerFilePath;
    private static String submissionFilePath;
    private static int maxStates = -1;
    public static boolean deterministic = false;

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

    private static void invalidCliArg(String prefix, String arg, String suffix) {
        System.out.printf("error: invalid command line argument: %s '%s' %s\n", prefix, arg, suffix);
        showHelp();
        System.exit(1);
    }

    private static void invalidCliArg(String prefix, String arg) {
        System.out.printf("error: invalid command line argument: %s '%s'\n", prefix, arg);
        showHelp();
        System.exit(1);
    }

    private static void invalidCliArg(String arg) {
        System.out.printf("error: invalid command line argument: '%s'\n", arg);
        showHelp();
        System.exit(1);
    }

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

    public static void main(String[] args) {
        handleArgs(args);
        CheckSubmission checkSubmission = new CheckSubmission();
        Feedback feedback = checkSubmission.isCorrect(Main.answerFilePath, Main.submissionFilePath, Main.maxStates, Main.deterministic);
        System.out.println(feedback.correct);
        System.out.println(feedback.feedback);
    }
}
