package afctevaluator;

public class WitnessFeedbackHelper {

    public static String getFeedback(boolean correct, int witnessType, String witness) {
        String feedback = "Correct!";

        if (!correct) {
            feedback = "Your answer is incorrect. ";

            if (witnessType == -1) {
                if (witness.contentEquals("")) {
                    feedback = feedback + "The empty string is an example.";
                } else {
                    feedback = feedback + "The string \"" + witness + "\" is an example.";
                }
            } else if (witnessType == 0) {
                if (witness.contentEquals("")) {
                    feedback = feedback + "The empty string should NOT be accepted.";
                } else {
                    feedback = feedback + "The string \"" + witness + "\" should NOT be accepted.";
                }
            } else {
                if (witness.contentEquals("")) {
                    feedback = feedback + "The empty string SHOULD be accepted.";
                } else {
                    feedback = feedback + "The string \"" + witness + "\" SHOULD be accepted.";
                }
            }
        }

        return feedback;
    }
}
