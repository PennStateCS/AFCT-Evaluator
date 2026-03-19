package afctevaluator;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;

import static afctevaluator.TestExtended.getNumberedTestFileNames;

public class TestMain {
    private static ArrayList<TestExtended.TestCase> getTestCases() {
        ArrayList<TestExtended.TestCase> testCases = new ArrayList<>();

        // Homework 0
        testCases.add(new TestExtended.TestCase("Homework 0", "Hw-00-solution.jff", false, getNumberedTestFileNames("Hw-00-incorrect-answer-", 3)));


        // Homework 2 - Problem 2
        testCases.add(new TestExtended.TestCase("Homework 2 - Problem 2", "Hw-02-p02-solution.jff", true, getNumberedTestFileNames("Hw-02-p02-incorrect-answer-", 3)));

        // Regular Expression
        ArrayList<String> reTestFileNames = new ArrayList<>();
        reTestFileNames.add("1.18a-no.jff");
        reTestFileNames.add("1.18a.jff");
        testCases.add(new TestExtended.TestCase("Regular Expressions", "1.18a.jff", false, reTestFileNames));

        // CFG
        ArrayList<String> testFileNames = new ArrayList<>();
        testFileNames.add("2.4b_better.jff");
        //testCases.add(new TestCase("CFG", "2.4b.jff", false, testFileNames));
        //testCases.add(new TestCase("CFG", "Hw-05 - p03.jff", false, "brent broken for hw5-p3.jff"));

        return testCases;
    }

    public static void main(String[] args) {
        Main.testMode = true;

        ArrayList<TestExtended.TestCase> testCases = getTestCases();

        for (TestExtended.TestCase testCase : testCases) {
            System.out.println("\n" + testCase.name);
            for (String submissionFilePath : testCase.submissionFiles) {
                System.out.println("\t" + (new File(submissionFilePath)).getName());
                String[] mainArgs = new String[]{"-j", testCase.solutionFilePath, submissionFilePath, "-1", String.valueOf(testCase.deterministic)};
                Main.main(mainArgs);
                System.out.println();
            }
        }
    }

}
