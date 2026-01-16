package afctevaluator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a single piece of feedback. Holds a string with a feedback message, and a boolean for whether the
 * submission was correct.
 *
 * @author Jesse Burdick-Pless jb4411@rit.edu
 */
public class Feedback {
    public String feedback;
    public boolean correct;
    public ArrayList<String> warnings;
    public ArrayList<String> errors;


    /**
     * Constructor for Feedback.
     *
     * @param feedback the feedback message
     * @param correct whether the submission was correct
     */
    public Feedback(String feedback, boolean correct) {
        this.feedback = feedback;
        this.correct = correct;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    /**
     * Constructor for Feedback.
     *
     * @param feedback the feedback message
     * @param correct whether the submission was correct
     * @param errors any errors
     */
    public Feedback(String feedback, boolean correct, ArrayList<String> errors) {
        this.feedback = feedback;
        this.correct = correct;
        this.warnings = new ArrayList<>();
        this.errors = errors;
    }

    /**
     * Constructor for Feedback.
     *
     * @param feedback the feedback message
     * @param correct whether the submission was correct
     * @param error the error
     */
    public Feedback(String feedback, boolean correct, String error) {
        this.feedback = feedback;
        this.correct = correct;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>(Collections.singletonList(error));
    }

    /**
     * Constructor for Feedback.
     *
     * @param feedback the feedback message
     * @param correct whether the submission was correct
     * @param warnings any warnings
     * @param errors any errors
     */
    public Feedback(String feedback, boolean correct, ArrayList<String> warnings, ArrayList<String> errors) {
        this.feedback = feedback;
        this.correct = correct;
        this.warnings = warnings;
        this.errors = errors;
    }

    public static Feedback contactProfessorError(String error) {
        String feedback = String.format("%s Please contact your professor.", error);
        return new Feedback(feedback, false, error);
    }

    /**
     * A helper method that creates feedback for submissions that are an incorrect type.
     *
     * @param expected the expected type
     * @param submitted the object submitted
     * @return incorrect submission type feedback
     */
    public static <T> Feedback submissionTypeError(Class<T> expected, Serializable submitted) {
        String text = String.format("ERROR: expected submission to be a %s, but got a %s", expected.getSimpleName(), submitted.getClass().getSimpleName());
        return new Feedback(text, false);
    }

    /**
     * A helper method that creates feedback for submissions that have too many states.
     *
     * @param expected the expected maximum number of states
     * @param actual the actual submitted number of states
     * @return too many states feedback
     */
    public static Feedback tooManyStates(int expected, int actual) {
        String text = String.format("Your submission has too many states. (%d > %d)", expected, actual);
        return new Feedback(text, false);
    }


    public void addWarningsAndErrors(ArrayList<String> warnings, ArrayList<String> errors) {
        this.warnings.addAll(warnings);
        this.errors.addAll(errors);
    }
}
