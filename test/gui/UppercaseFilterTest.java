package gui;

import org.junit.Test;
import javax.swing.text.PlainDocument;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/** Tests UppercaseFilter document filtering behavior including newline stripping and non-English character filtering. */
public class UppercaseFilterTest {

    @Test
    public void testUppercaseAndNewlineFilter() throws Exception {
        PlainDocument doc = new PlainDocument();
        doc.setDocumentFilter(new UppercaseFilter());

        doc.insertString(0, "hello\nworld\rtest", null);
        assertEquals("HELLOWORLDTEST", doc.getText(0, doc.getLength()));

        doc.replace(0, doc.getLength(), "line1\r\nline2", null);
        assertEquals("LINE1LINE2", doc.getText(0, doc.getLength()));
    }

    @Test
    public void testNonEnglishCharactersFilteredOut() throws Exception {
        PlainDocument doc = new PlainDocument();
        doc.setDocumentFilter(new UppercaseFilter());

        // Non-English characters such as 'İ', 'ö', 'ü', 'ç', 'é', 'ñ' should be filtered out
        doc.insertString(0, "test-123_ç", null);
        assertEquals("TEST-123_", doc.getText(0, doc.getLength()));

        doc.replace(0, doc.getLength(), "HİLL TOP öü", null);
        assertEquals("HLL TOP ", doc.getText(0, doc.getLength()));

        // Cyrillic and emojis
        doc.replace(0, doc.getLength(), "MOSKVA МОСКВА ✈️", null);
        assertEquals("MOSKVA  ", doc.getText(0, doc.getLength()));
    }

    @Test
    public void testEnglishUppercaseUnderTurkishWindowsLocale() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            PlainDocument doc = new PlainDocument();
            doc.setDocumentFilter(new UppercaseFilter());

            // On Turkish Windows, 'i' would become 'İ' if locale wasn't enforced to ENGLISH
            doc.insertString(0, "istanbul", null);
            assertEquals("ISTANBUL", doc.getText(0, doc.getLength()));

            // Directly typed 'İ' should be filtered out
            doc.replace(0, doc.getLength(), "İSTANBUL", null);
            assertEquals("STANBUL", doc.getText(0, doc.getLength()));
        } finally {
            Locale.setDefault(original);
        }
    }
}

