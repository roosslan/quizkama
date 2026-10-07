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

    // ---- choice detection -------------------------------------------------------------

    @Test
    public void questionTextLineThatLooksLikeAChoiceDoesNotStartChoices() {
        List<tectFragen> r = parse(
                "QUESTION 1\nWhich bacteria?\nE. coli is found in\nthe gut.\nA. yes\nB. no\nCorrect Answer: A\n");
        assertEquals(1, r.size());
        assertEquals("Which bacteria? E. coli is found in the gut.", r.get(0).frageText);
        assertEquals(Arrays.asList("A. yes", "B. no"), r.get(0).Vorschlage);
    }

    @Test
    public void wrappedLineThatLooksLikeAChoiceStaysInPreviousChoice() {
        List<tectFragen> r = parse(
                "QUESTION 1\nWho?\nA. Ask Dr.\nC. Smith about it\nB. no\nCorrect Answer: B\n");
        assertEquals(Arrays.asList("A. Ask Dr. C. Smith about it", "B. no"), r.get(0).Vorschlage);
    }

    @Test
    public void choicesMustBeLetteredConsecutively() {
        // A missing letter means the rest is treated as continuation; one choice is not a quiz.
        assertTrue(parse("QUESTION 1\nQ\nA. a\nC. c\nCorrect Answer: A\n").isEmpty());
    }

    @Test
    public void singleChoiceQuestionIsDropped() {
        assertTrue(parse("QUESTION 1\nQ?\nA. only\nCorrect Answer: A\n").isEmpty());
    }

    // ---- normalization ----------------------------------------------------------------

    @Test
    public void nonBreakingSpacesAndTabsAreNormalized() {
        List<tectFragen> r = parse(
                "QUESTION\u00A01\nQ?\nA.\u00A0one\nB.\ttwo\nCorrect\u00A0Answer:\u00A0B\n");
        assertEquals(1, r.size());
        assertEquals(Arrays.asList("A. one", "B. two"), r.get(0).Vorschlage);
    }

    @Test
    public void answerLettersAreSortedAndDeduplicated() {
        assertEquals("AC", parse("QUESTION 1\nQ\nA. a\nB. b\nC. c\nCorrect Answer: CA\n").get(0).Antworten);
        assertEquals("A", parse("QUESTION 1\nQ\nA. a\nB. b\nCorrect Answer: A, A\n").get(0).Antworten);
    }

    @Test
    public void correctAnswerPrefixIsCaseInsensitive() {
        assertEquals("B", parse("QUESTION 1\nQ\nA. a\nB. b\nCorrect answer: B\n").get(0).Antworten);
        assertEquals("B", parse("QUESTION 1\nQ\nA. a\nB. b\nCORRECT ANSWER:B\n").get(0).Antworten);
    }

    // ---- skipped counter --------------------------------------------------------------

    @Test
    public void skippedCountIsZeroForCleanFile() {
        Parser p = new Parser(new BufferedReader(new StringReader(
                "QUESTION 1\nQ\nA. a\nB. b\nCorrect Answer: A\n")));
        assertEquals(1, p.parse().size());
        assertEquals(0, p.getSkippedCount());
    }

    @Test
    public void skippedCountReportsEveryDroppedQuestion() {
        Parser p = new Parser(new BufferedReader(new StringReader(
                "QUESTION 1\nno choices\nCorrect Answer: see exhibit\n"   // dropped
                        + "QUESTION 2\nQ\nA. a\nB. b\n"                    // no answer line
                        + "QUESTION 3\nQ\nA. a\nB. b\nCorrect Answer: Z\n" // unknown letter
                        + "QUESTION 4\nQ\nA. a\nB. b\nCorrect Answer: B\n" // ok
                        + "QUESTION 5\nQ\nA. a\nB. b")));                   // truncated
        assertEquals(1, p.parse().size());
        assertEquals(4, p.getSkippedCount());
    }

    // ---- a realistic dump -------------------------------------------------------------

    @Test
    public void realisticDump() {
        String dump = String.join("\n",
                "Exam ABC-123",
                "www.gratisexam.com",
                "",
                "QUESTION 1",
                "You need to configure a server. Which two actions should you",
                "perform? (Choose two.)",
                "A. Install the role.",
                "B. Reboot the server. Then verify",
                "the configuration.",
                "C. Delete the logs",
                "D. Update the firmware",
                "Correct Answer: AB",
                "Section: (none)",
                "Explanation/Reference:",
                "Explanation:",
                "A. is needed because the role is required.",
                "http://www.gratisexam.com/",
                "",
                "QUESTION 2",
                "DRAG DROP",
                "Match the items.",
                "Select and Place:",
                "Correct Answer:",
                "Section: (none)",
                "",
                "QUESTION 3",
                "Which command lists files?",
                "A. ls",
                "B. cd",
                "C. rm",
                "Correct Answer: A",
                "");
        Parser p = new Parser(new BufferedReader(new StringReader(dump)));
        List<tectFragen> r = p.parse();

        assertEquals(2, r.size());
        assertEquals(1, p.getSkippedCount());

        tectFragen q1 = r.get(0);
        assertEquals("You need to configure a server. Which two actions should you perform? (Choose two.)",
                q1.frageText);
        assertEquals(4, q1.Vorschlage.size());
        assertEquals("B. Reboot the server. Then verify the configuration.", q1.Vorschlage.get(1));
        assertEquals("AB", q1.Antworten);

        assertEquals("Which command lists files?", r.get(1).frageText);
        assertEquals("A", r.get(1).Antworten);
    }
}
