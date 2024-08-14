package afctevaluator;

/**
 * Represents a single piece of feedback. Holds a string with a feedback message, and a boolean for whether the
 * submission was correct.
 *
 * @author Jesse Burdick-Pless jb4411@rit.edu
 */
public class Feedback {
    public String feedback;
    public boolean correct;

    /**
     * Constructor for Feedback.
     *
     * @param feedback the feedback message
     * @param correct whether the submission was correct
     */
    public Feedback(String feedback, boolean correct) {
        this.feedback = feedback;
        this.correct = correct;
    }
}
