package afctevaluator;

import java.io.File;
import java.io.Serializable;

public class Test {
    public static void main(String[] args) {
        // Specify the directory path
        String directoryPath = "TESTINPUT";

        // Create a File object for the directory
        File directory = new File(directoryPath);

        // Get all files in the directory
        File[] filesList = directory.listFiles();

        assert filesList != null;
        CheckSubmission checkSubmission = new CheckSubmission();
        File submissionFile = filesList[0];
        Serializable submission = checkSubmission.decode(submissionFile);
        for (File answerFile : filesList) {
            if (!answerFile.getName().endsWith(".jff")) {
                continue;
            }
            System.out.print(answerFile.getName());
            System.out.print("\t");
            Serializable answer = checkSubmission.decode(answerFile);
            checkSubmission.isCorrect(answer, submission, true);
        }
    }
}
