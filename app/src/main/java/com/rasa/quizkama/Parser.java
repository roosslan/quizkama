package com.rasa.quizkama;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the plain-text question format:
 *
 * <pre>
 * QUESTION 1
 * question text, possibly spanning several lines
 * A. first choice
 * B. second choice
 * Correct Answer: B
 * ...anything up to the next "QUESTION " line is ignored (explanations etc.)
 * </pre>
 *
 * Questions that are malformed (no choices, no "Correct Answer:" line, or a correct answer
 * that doesn't match any choice letter) are dropped instead of corrupting their neighbours.
 */
public class Parser {
    private static final String CORRECT_ANSWER = "Correct Answer:";
    private static final String QUESTION_MARK = "QUESTION ";
    private static final String WATERMARK = "gratisexam";
    private static final Pattern CHOICE_START = Pattern.compile("^[A-Z]\\.\\s.*");

    private enum State { SKIP, QUESTION, CHOICES }

    private final BufferedReader bufferedReader;

    private State state;
    private StringBuilder questionText;
    private ArrayList<String> choices;
    private StringBuilder choice;

    Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    List<tectFragen> parse() {
        final List<tectFragen> result = new ArrayList<>();
        state = State.SKIP;

        String line;
        while ((line = readLine()) != null) {
            if (line.contains(WATERMARK)) {
                continue;
            }
            line = line.replace("\uFEFF", "").trim(); // BOM + surrounding whitespace

            if (line.startsWith(QUESTION_MARK)) {
                // A new question always ends the previous one; if that one was still
                // incomplete it is silently dropped.
                startQuestion();
                continue;
            }

            switch (state) {
                case SKIP:
                    break;
                case QUESTION:
                    if (line.startsWith(CORRECT_ANSWER)) {
                        state = State.SKIP; // answer without choices: nothing to show
                    } else if (CHOICE_START.matcher(line).matches()) {
                        startChoice(line);
                        state = State.CHOICES;
                    } else {
                        append(questionText, line);
                    }
                    break;
                case CHOICES:
                    if (line.startsWith(CORRECT_ANSWER)) {
                        flushChoice();
                        tectFragen q = buildQuestion(line.substring(CORRECT_ANSWER.length()));
                        if (q != null) {
                            result.add(q);
                        }
                        state = State.SKIP;
                    } else if (CHOICE_START.matcher(line).matches()) {
                        flushChoice();
                        startChoice(line);
                    } else {
                        append(choice, line);
                    }
                    break;
            }
        }
        return result;
    }

    private String readLine() {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read question file", e);
        }
    }

    private void startQuestion() {
        questionText = new StringBuilder();
        choices = new ArrayList<>();
        choice = null;
        state = State.QUESTION;
    }

    private void startChoice(String line) {
        choice = new StringBuilder(line);
    }

    private void flushChoice() {
        if (choice != null) {
            choices.add(choice.toString());
            choice = null;
        }
    }

    private static void append(StringBuilder sb, String line) {
        if (sb == null || line.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(line);
    }

    /** Returns null if the answer doesn't refer to existing choice letters. */
    private tectFragen buildQuestion(String rawAnswer) {
        // "AC", "A, C", "a c " -> "AC"
        String answer = rawAnswer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (answer.isEmpty() || choices.isEmpty()) {
            return null;
        }
        for (int i = 0; i < answer.length(); i++) {
            boolean found = false;
            for (String c : choices) {
                if (c.charAt(0) == answer.charAt(i)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        tectFragen q = new tectFragen();
        q.frageText = questionText.toString();
        q.Vorschlage = choices;
        q.Antworten = answer;
        return q;
    }
}
