package afctevaluator;

import file.XMLCodec;

import java.io.*;

import automata.Automaton;
import automata.fsa.FiniteStateAutomaton;
import automata.mealy.MealyMachine;
import automata.mealy.MooreMachine;
import automata.pda.PushdownAutomaton;
import automata.turing.TuringMachine;
import automata.turing.TuringMachineBuildingBlocks;
import grammar.ConvertedUnrestrictedGrammar;
import grammar.Grammar;
import grammar.UnboundGrammar;
import grammar.UnrestrictedGrammar;
import grammar.cfg.ContextFreeGrammar;
import grammar.reg.RegularGrammar;
import grammar.reg.RightLinearGrammar;
import pumping.ContextFreePumpingLemma;
import pumping.PumpingLemma;
import pumping.RegularPumpingLemma;
import regular.RegularExpression;

public class CheckSubmission {
    private final XMLCodec codec = new XMLCodec();

    public String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    public Serializable readAndDecode(String filePath) {
        File file = new File(filePath);
        return codec.decode(file, null);
    }
    
    
    private void handleAutomaton(Automaton automaton) {
        switch (automaton) {
            case FiniteStateAutomaton finiteStateAutomaton -> {

            }
            case MooreMachine mooreMachine -> {

            }
            case MealyMachine mealyMachine -> {

            }
            case PushdownAutomaton pushdownAutomaton -> {

            }
            case TuringMachineBuildingBlocks turingMachineBuildingBlocks -> {

            }
            case TuringMachine turingMachine -> {

            }
            default -> {
                // TODO - handle everything else
            }
        }
    }

    private void handleGrammar(Grammar grammar) {
        switch (grammar) {
            case ContextFreeGrammar contextFreeGrammar -> {

            }
            case ConvertedUnrestrictedGrammar convertedUnrestrictedGrammar -> {

            }
            case RightLinearGrammar rightLinearGrammar -> {

            }
            case RegularGrammar regularGrammar -> {

            }
            case UnrestrictedGrammar unrestrictedGrammar -> {

            }
            case UnboundGrammar unboundGrammar -> {

            }
            default -> {
                // TODO - handle everything else
            }
        }
    }

    private void handlePumpingLemma(PumpingLemma pumpingLemma) {
        switch (pumpingLemma) {
            case RegularPumpingLemma regularPumpingLemma -> {

            }
            case ContextFreePumpingLemma contextFreePumpingLemma -> {

            }
            default -> {
                // TODO - handle everything else
            }
        }
    }

    private void handleRegularExpression(RegularExpression regularExpression) {

    }

    public void isCorrect(String answerFilePath, String submissionFilePath) {
        Serializable answer = readAndDecode(answerFilePath);
        Serializable submitted = readAndDecode(submissionFilePath);

        switch (answer) {
            case Automaton automaton -> {
                handleAutomaton(automaton);
            }
            case Grammar grammar -> {
                handleGrammar(grammar);
            }
            case PumpingLemma pumpingLemma -> {
                handlePumpingLemma(pumpingLemma);
            }
            case RegularExpression regularExpression -> {
                handleRegularExpression(regularExpression);
            }
            default -> {
                // TODO - handle everything else
            }
        }
    }
}
