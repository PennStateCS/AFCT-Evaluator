package afctevaluator;

public class Main {
    public static void main(String[] args) {
        String answerFilePath = args[0];
        String submissionFilePath = args[1];

        int maxStates = Integer.MAX_VALUE;
        boolean deterministic = false;
        if (args.length > 3) {
            maxStates = Integer.parseInt(args[2]);
            deterministic = args[3].equalsIgnoreCase("true");
        } else if (args.length > 2) {
            maxStates = Integer.parseInt(args[2]);
        }
        CheckSubmission checkSubmission = new CheckSubmission();
        Feedback feedback = checkSubmission.isCorrect(answerFilePath, submissionFilePath, maxStates, deterministic);
        System.out.println(feedback.correct);
        System.out.println(feedback.feedback);
    }
}
