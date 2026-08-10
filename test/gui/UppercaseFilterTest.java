package gui;

import org.junit.Test;
import javax.swing.text.PlainDocument;

import static org.junit.Assert.assertEquals;

/** Tests UppercaseFilter document filtering behavior including newline stripping. */
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
}
