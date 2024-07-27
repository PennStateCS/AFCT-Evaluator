package afctevaluator;

public class Main {
    public static void main(String[] args) {
        String answerFilePath = args[0];
        String submissionFilePath = args[1];
        boolean deterministic = false;
        if (args.length > 2) {
            deterministic = args[2].equalsIgnoreCase("true");
        }
        CheckSubmission checkSubmission = new CheckSubmission();
        checkSubmission.isCorrect(answerFilePath, submissionFilePath, deterministic);
    }
}
