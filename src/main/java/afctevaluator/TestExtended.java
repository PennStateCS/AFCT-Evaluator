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
        String solutionFilePath;
        boolean deterministic;
        ArrayList<String> submissionFiles;

        public TestCase(String name, String solutionFileName, boolean deterministic, String submissionFileName) {
            this.name = name;
            this.solutionFilePath = includeDirectory(solutionFileName);
            this.deterministic = deterministic;

            this.submissionFiles = new ArrayList<>();
            this.submissionFiles.add(includeDirectory(submissionFileName));
        }

        public TestCase(String name, String solutionFileName, boolean deterministic, List<String> submissionFileNames) {
            this.name = name;
            this.solutionFilePath = includeDirectory(solutionFileName);
            this.deterministic = deterministic;

            this.submissionFiles = new ArrayList<>();
            for (String fileName : submissionFileNames) {
                this.submissionFiles.add(includeDirectory(fileName));
            }
        }
    }

    public static String includeDirectory(String fileName) {
        if (!fileName.contains(sep)) {
            fileName = directoryPath + sep + fileName;
        }
        return fileName;
    }

    private static ArrayList<String> getNumberedTestFileNames(String baseName, int numFiles) {
        ArrayList<String> fileNames = new ArrayList<>();

        for (int i = 1; i <= numFiles; i++) {
            fileNames.add(baseName + i + ".jff");
        }

        return fileNames;
    }

    private static ArrayList<TestCase> getTestCases() {
        ArrayList<TestCase> testCases = new ArrayList<>();

        // Homework 0
        //testCases.add(new TestCase("Homework 0", "Hw-00-solution.jff", false, getNumberedTestFileNames("Hw-00-incorrect-answer-", 3)));


        // Homework 2 - Problem 2
        //testCases.add(new TestCase("Homework 2 - Problem 2", "Hw-02-p02-solution.jff", true, getNumberedTestFileNames("Hw-02-p02-incorrect-answer-", 3)));

        // Regular Expression
        ArrayList<String> reTestFileNames = new ArrayList<>();
        reTestFileNames.add("1.18a-no.jff");
        reTestFileNames.add("1.18a.jff");
        //testCases.add(new TestCase("Regular Expressions", "1.18a.jff", false, reTestFileNames));

        // CFG
        ArrayList<String> testFileNames = new ArrayList<>();
        testFileNames.add("2.4b_better.jff");
        //testCases.add(new TestCase("CFG", "2.4b.jff", false, testFileNames));

        // CFG - Counterexample
        //testCases.add(new TestCase("CFG", "zcounter1.jff", false, "zcounter1.jff"));
        //testCases.add(new TestCase("CFG", "zcounter-complex-2.6d.jff", false, "zcounter-complex-2.6d-Mod.jff"));
        //testCases.add(new TestCase("CFG", "zcounter-sol.jff", false, "zcounter-incorrect.jff"));
        testCases.add(new TestCase("CFG", "zcounter-complex-2.6d - Copy.jff", false, "zcounter-complex-2.6d-Mod - Copy.jff"));

        return testCases;
    }

    public static void main(String[] args) {
        CheckSubmission checkSubmission = new CheckSubmission();
        ArrayList<TestCase> testCases = getTestCases();

        for (TestCase testCase : testCases) {
            System.out.println("\n" + testCase.name);
            for (String submissionFilePath : testCase.submissionFiles) {
                System.out.println("\t" + (new File(submissionFilePath)).getName());
                Feedback feedback = checkSubmission.isCorrect(testCase.solutionFilePath, submissionFilePath, -1, testCase.deterministic);
                //System.out.println("\t\t" + feedback.correct);
                //System.out.println("\t\tCorrect: " + feedback.correct);
                System.out.println("\t\t" + feedback.feedback);
            }
        }
    }
}
