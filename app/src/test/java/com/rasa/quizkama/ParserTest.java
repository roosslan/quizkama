package com.rasa.quizkama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class ParserTest {

    private static List<tectFragen> parse(String text) {
        return new Parser(new BufferedReader(new StringReader(text))).parse();
    }

    @Test
    public void parsesSimpleQuestions() {
        List<tectFragen> r = parse(
                "garbage before\n"
                        + "QUESTION 1\nWhat is X?\nA. one\nB. two\nCorrect Answer: B\n"
                        + "Explanation: blah\n"
                        + "QUESTION 2\nQ2?\nA. a\nB. b\nCorrect Answer: A\n");
        assertEquals(2, r.size());
        assertEquals("What is X?", r.get(0).frageText);
        assertEquals(Arrays.asList("A. one", "B. two"), r.get(0).Vorschlage);
        assertEquals("B", r.get(0).Antworten);
        assertEquals("A", r.get(1).Antworten);
    }

    @Test
    public void joinsMultiLineQuestionAndChoices() {
        List<tectFragen> r = parse(
                "QUESTION 1\nline one\nline two\n"
                        + "A. long choice\ncontinues here. And ends\n"
                        + "B. two\nCorrect Answer: B\n");
        assertEquals(1, r.size());
        assertEquals("line one line two", r.get(0).frageText);
        assertEquals("A. long choice continues here. And ends", r.get(0).Vorschlage.get(0));
        assertEquals(2, r.get(0).Vorschlage.size());
    }

    @Test
    public void normalizesAnswerLetters() {
        assertEquals("B", parse("QUESTION 1\nQ\nA. a\nB. b\nCorrect Answer: B \n").get(0).Antworten);
        assertEquals("AC", parse("QUESTION 1\nQ\nA. a\nB. b\nC. c\nCorrect Answer: A, C\n").get(0).Antworten);
        assertEquals("AC", parse("QUESTION 1\nQ\nA. a\nB. b\nC. c\nCorrect Answer: ac\n").get(0).Antworten);
    }

    @Test
    public void supportsMoreThanSixChoices() {
        List<tectFragen> r = parse(
                "QUESTION 1\nQ\nA. a\nB. b\nC. c\nD. d\nE. e\nF. f\nG. g\nH. h\nCorrect Answer: GH\n");
        assertEquals(8, r.get(0).Vorschlage.size());
        assertEquals("GH", r.get(0).Antworten);
    }

    @Test
    public void questionWithoutChoicesDoesNotSwallowNextOne() {
        List<tectFragen> r = parse(
                "QUESTION 1\nDrag and drop task\nCorrect Answer: see exhibit\n"
                        + "QUESTION 2\nQ2?\nA. a\nB. b\nCorrect Answer: A\n");
        assertEquals(1, r.size());
        assertEquals("Q2?", r.get(0).frageText);
    }

    @Test
    public void questionWithoutCorrectAnswerIsDropped() {
        List<tectFragen> r = parse(
                "QUESTION 1\nQ1?\nA. a\nB. b\n"
                        + "QUESTION 2\nQ2?\nA. a\nB. b\nCorrect Answer: B\n");
        assertEquals(1, r.size());
        assertEquals("Q2?", r.get(0).frageText);
    }

    @Test
    public void answerReferringToMissingChoiceIsDropped() {
        assertTrue(parse("QUESTION 1\nQ\nA. a\nB. b\nCorrect Answer: C\n").isEmpty());
        assertTrue(parse("QUESTION 1\nQ\nA. a\nB. b\nCorrect Answer: \n").isEmpty());
    }

    @Test
    public void truncatedFileDoesNotCrash() {
        assertTrue(parse("QUESTION 1\nQ\nA. a\nB. b").isEmpty());
        assertTrue(parse("QUESTION 1").isEmpty());
        assertTrue(parse("").isEmpty());
    }

    @Test
    public void ignoresWatermarkLinesIncludingFirst() {
        List<tectFragen> r = parse(
                "www.gratisexam.com\n"
                        + "QUESTION 1\nQ?\nhttp://www.gratisexam.com/\nA. a\nB. b\nCorrect Answer: A\n");
        assertEquals(1, r.size());
        assertEquals("Q?", r.get(0).frageText);
    }

    @Test
    public void handlesWindowsLineEndings() {
        List<tectFragen> r = parse("QUESTION 1\r\nQ?\r\nA. a\r\nB. b\r\nCorrect Answer: B\r\n");
        assertEquals(1, r.size());
        assertEquals("B", r.get(0).Antworten);
    }

    @Test
    public void ignoresByteOrderMark() {
        List<tectFragen> r = parse("\uFEFFQUESTION 1\nQ?\nA. a\nB. b\nCorrect Answer: A\n");
        assertEquals(1, r.size());
    }
}
