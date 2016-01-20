package com.rasa.quizkama;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Parser {
    private static final String CORRECT_ANSWER = "Correct Answer: ";
    private static final String CHOICE_LETTER_DELIMITER = ". ";
    private static final String QUESTION_MARK = "QUESTION ";
    private static final String ANSWER_MARK = "A. ";
    final List<tectFragen> result = new LinkedList<>();
    private final BufferedReader bufferedReader;
    // context
    private String lastLine;
    private StringBuffer buffer;
    private tectFragen laufendeFrage;

    Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    private static boolean crap_checkEndOfSection(String line) {
        return line.startsWith(QUESTION_MARK);
    }

    private static boolean question_checkEndOfSection(String line) {
        return line.startsWith(ANSWER_MARK);
    }

    List<tectFragen> parse() {
        result.clear();

        readFirstLine();

        if (null != lastLine) {
            skipCrap();
            while (null != lastLine) {
                readQuestion();
                readChoices();
                readAnswer();
                skipCrap();
            }
        }

        return result;
    }

    private void readAnswer() {
        if (null == lastLine) {
            return;
        }

        answer_doSomethingWithLastLine();

        readLine("Couldn't read line after answer (next crappy line).");
    }

    private void answer_doSomethingWithLastLine() {
        laufendeFrage.Vorschlage.add(buffer.toString());
        laufendeFrage.Antworten = lastLine.substring(CORRECT_ANSWER.length());
        result.add(laufendeFrage);
    }

    private void readChoices() {
        if (null == lastLine) {
            return;
        }

        choices_doSomethingWithLastLine();
        while (choices_doSomethingWithNextLine(lastLine)) {
            readLine("Couldn't read question line.");
        }
    }

    private boolean choices_checkEndOfSection(String line) {
        return line.startsWith(CORRECT_ANSWER);
    }

    private boolean choices_doSomethingWithNextLine(String line) {
        if (null == lastLine || choices_checkEndOfSection(lastLine)) {
            return false;
        }

        final int position = line.indexOf(CHOICE_LETTER_DELIMITER);
        if (-1 == position) {
            buffer.append(" ").append(lastLine);
        } else {
            if (null != buffer) {
                laufendeFrage.Vorschlage.add(buffer.toString());
            }
            buffer = new StringBuffer();
//            buffer.append(lastLine.substring(CHOICE_LETTER_DELIMITER.length() + lastLine.indexOf(CHOICE_LETTER_DELIMITER)));
            buffer.append(lastLine);
        }
        return true;
    }

    private void choices_doSomethingWithLastLine() {
        laufendeFrage.frageText = buffer.toString();
        laufendeFrage.Vorschlage = new ArrayList<>();
        buffer = null;
    }

    private void readQuestion() {
        // TODO: check that question number is correct.

        if (null == lastLine) {
            return;
        }

        question_doSomethingWithLastLine();
        do {
            readLine("Couldn't read question line.");
        } while (question_doSomethingWithNextLine());
    }

    private void readLine(String s) {
        try {
            lastLine = bufferedReader.readLine();
            if (lastLine != null && lastLine.contains("gratisexam")) { lastLine = ""; }
        } catch (IOException e) {
            System.err.println(s);
            throw new RuntimeException();
        }
    }

    private boolean question_doSomethingWithNextLine() {
        if (null == lastLine || question_checkEndOfSection(lastLine)) {
            return false;
        }

        buffer.append(" ").append(lastLine);
        return true;
    }

    private void question_doSomethingWithLastLine() {
        laufendeFrage = new tectFragen();
        buffer = new StringBuffer();
    }

    private void readFirstLine() {
        try {
            lastLine = bufferedReader.readLine();
        } catch (IOException e) {
            System.err.println("Couldn't read first line.");
            throw new RuntimeException(e.getMessage());
        }
    }

    private void skipCrap() {
        while (null != lastLine && !crap_checkEndOfSection(lastLine)) {
            readLine("Couldn't read crappy line.");
        }
    }
}
