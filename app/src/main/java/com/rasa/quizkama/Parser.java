package com.rasa.quizkama;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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
 * Choices must be lettered consecutively starting from "A." — a line that merely looks like a
 * choice ("E. coli is...", a wrapped "C. Smith") is treated as a continuation of the previous text.
 *
 * Questions that are malformed (fewer than two choices, no "Correct Answer:" line, or a correct
 * answer that doesn't match any choice letter) are dropped instead of corrupting their
 * neighbours; {@link #getSkippedCount()} tells how many.
 */
public class Parser {
    private static final String CORRECT_ANSWER = "Correct Answer:";
    private static final String QUESTION_MARK = "QUESTION ";
    private static final String WATERMARK = "gratisexam";

    private enum State { SKIP, QUESTION, CHOICES }

    private final BufferedReader bufferedReader;

    private State state;
    private StringBuilder questionText;
    private ArrayList<String> choices;
    private StringBuilder choice;
    private char nextLetter; // letter the next choice has to start with
    private int questionHeaders;
    private int skipped;

    Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    List<tectFragen> parse() {
        final List<tectFragen> result = new ArrayList<>();
        state = State.SKIP;
        questionHeaders = 0;
        skipped = 0;

        String line;
        while ((line = readLine()) != null) {
            if (line.contains(WATERMARK)) {
                continue;
            }
            // BOM; non-breaking spaces and tabs (common in PDF-to-text output); surrounding whitespace
            line = line.replace("\uFEFF", "").replace('\u00A0', ' ').replace('\t', ' ').trim();

            if (line.startsWith(QUESTION_MARK)) {
                // A new question always ends the previous one; if that one was still
                // incomplete it is dropped (and counted in getSkippedCount()).
                questionHeaders++;
                startQuestion();
                continue;
            }

            switch (state) {
                case SKIP:
                    break;
                case QUESTION:
                    if (startsWithIgnoreCase(line, CORRECT_ANSWER)) {
                        state = State.SKIP; // answer without choices: nothing to show
                    } else if (isChoiceStart(line)) {
                        startChoice(line);
                        state = State.CHOICES;
                    } else {
                        append(questionText, line);
                    }
                    break;
                case CHOICES:
                    if (startsWithIgnoreCase(line, CORRECT_ANSWER)) {
                        flushChoice();
                        tectFragen q = buildQuestion(line.substring(CORRECT_ANSWER.length()));
                        if (q != null) {
                            result.add(q);
                        }
                        state = State.SKIP;
                    } else if (isChoiceStart(line)) {
                        flushChoice();
                        startChoice(line);
                    } else {
                        append(choice, line);
                    }
                    break;
            }
        }
        skipped = questionHeaders - result.size();
        return result;
    }

    /** Number of questions found in the last parse() that couldn't be used. */
    public int getSkippedCount() {
        return skipped;
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
        nextLetter = 'A';
        state = State.QUESTION;
    }

    private void startChoice(String line) {
        choice = new StringBuilder(line);
        nextLetter++;
    }

    private void flushChoice() {
        if (choice != null) {
            choices.add(choice.toString());
            choice = null;
        }
    }

    /** "A. text" for the expected letter; the line is already trimmed and normalized. */
    private boolean isChoiceStart(String line) {
        return line.length() > 2
                && line.charAt(0) == nextLetter
                && line.charAt(1) == '.'
                && line.charAt(2) == ' ';
    }

    private static boolean startsWithIgnoreCase(String line, String prefix) {
        return line.regionMatches(true, 0, prefix, 0, prefix.length());
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

    /** Returns null if the question is unusable (too few choices, bad or unknown answer letters). */
    private tectFragen buildQuestion(String rawAnswer) {
        if (choices.size() < 2) {
            return null;
        }
        // "AC", "A, C", "ca", "A, A" -> sorted, de-duplicated "AC"/"A"
        TreeSet<Character> letters = new TreeSet<>();
        for (char c : rawAnswer.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                letters.add(c);
            }
        }
        if (letters.isEmpty()) {
            return null;
        }
        StringBuilder answer = new StringBuilder();
        for (char letter : letters) {
            if (letter >= 'A' + choices.size()) { // choices are lettered A, B, C... consecutively
                return null;
            }
            answer.append(letter);
        }
        tectFragen q = new tectFragen();
        q.frageText = questionText.toString();
        q.Vorschlage = choices;
        q.Antworten = answer.toString();
        return q;
    }
}
