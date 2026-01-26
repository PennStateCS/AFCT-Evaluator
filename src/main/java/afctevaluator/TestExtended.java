package afctevaluator;

import java.io.File;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

// TODO: create a better test suite that covers more - better test cases, including some with longer witness strings
public class TestExtended {
    static String sep = FileSystems.getDefault().getSeparator();
    static String directoryPath = "TestInputExtended";

    public static class TestCase {
        String name;
        File solutionFile;
        boolean deterministic;
        ArrayList<File> submissionFiles;

        public TestCase(String name, File solutionFile, boolean deterministic, ArrayList<File> submissionFiles) {
            this.name = name;
            this.solutionFile = solutionFile;
            this.deterministic = deterministic;
            this.submissionFiles = submissionFiles;
        }

        public TestCase(String name, String solutionFileName, boolean deterministic, String submissionFileName) {
            this.name = name;
            solutionFileName = includeDirectory(solutionFileName);
            this.solutionFile = new File(solutionFileName);
            this.deterministic = deterministic;

            submissionFileName = includeDirectory(submissionFileName);
            this.submissionFiles = new ArrayList<>();
            this.submissionFiles.add(new File(submissionFileName));
        }

        public TestCase(String name, String solutionFileName, boolean deterministic, List<String> submissionFileNames) {
            this.name = name;
            solutionFileName = includeDirectory(solutionFileName);
            this.solutionFile = new File(solutionFileName);
            this.deterministic = deterministic;

            this.submissionFiles = new ArrayList<>();
            for (String fileName : submissionFileNames) {
                this.submissionFiles.add(new File(includeDirectory(fileName)));
            }
        }
    }

    public static String includeDirectory(String fileName) {
        if (!fileName.contains(sep)) {
            fileName = directoryPath + sep + fileName;
        }
        return fileName;
    }

    private static ArrayList<String> getNumberedTestFileNames(String baseName) {
        ArrayList<String> fileNames = new ArrayList<>();
        return fileNames;
    }

    private static ArrayList<TestCase> getTestCases() {
        ArrayList<TestCase> testCases = new ArrayList<>();

        // Homework 2 - Problem 2
        ArrayList<String> hw02p02Submissions = new ArrayList<>();
        hw02p02Submissions.add("Hw-02-p02-incorrect-answer-1.jff");
        hw02p02Submissions.add("Hw-02-p02-incorrect-answer-2.jff");
        testCases.add(new TestCase("Homework 2 - Problem 2", "Hw-02-p02-solution.jff", true, hw02p02Submissions));

        return testCases;
    }

    public static void main(String[] args) {
        CheckSubmission checkSubmission = new CheckSubmission();
        ArrayList<TestCase> testCases = getTestCases();
        Serializable answer;
        Serializable submission;
        for (TestCase testCase : testCases) {
            System.out.println(testCase.name);
            answer = checkSubmission.decode(testCase.solutionFile);
            for (File submissionFile : testCase.submissionFiles) {
                System.out.println("\t" + submissionFile.getName());
                submission = checkSubmission.decode(submissionFile);
                Feedback feedback = checkSubmission.isCorrect(answer, submission, -1, testCase.deterministic);
                System.out.println("\t\t" + feedback.correct);
                System.out.println("\t\t" + feedback.feedback);
            }
        }
    }
}
