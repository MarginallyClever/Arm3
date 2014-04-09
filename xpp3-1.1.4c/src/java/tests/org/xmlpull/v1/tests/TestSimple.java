/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Simple test ot verify pull parser factory
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSimple extends UtilTestCase {
    private XmlPullParserFactory factory;

    public TestSimple(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        //assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        //assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
        assertEquals(false, factory.isNamespaceAware());
        assertEquals(false, factory.isValidating());
    }

    protected void tearDown() {
    }

    public void testSimple() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(false, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        // this SHOULD always be OK
        assertEquals("START_DOCUMENT", XmlPullParser.TYPES[XmlPullParser.START_DOCUMENT]);
        assertEquals("END_DOCUMENT", XmlPullParser.TYPES[XmlPullParser.END_DOCUMENT]);
        assertEquals("START_TAG", XmlPullParser.TYPES[XmlPullParser.START_TAG]);
        assertEquals("END_TAG", XmlPullParser.TYPES[XmlPullParser.END_TAG]);
        assertEquals("TEXT", XmlPullParser.TYPES[XmlPullParser.TEXT]);
        assertEquals("CDSECT", XmlPullParser.TYPES[XmlPullParser.CDSECT]);
        assertEquals("ENTITY_REF", XmlPullParser.TYPES[XmlPullParser.ENTITY_REF]);
        assertEquals("IGNORABLE_WHITESPACE", XmlPullParser.TYPES[XmlPullParser.IGNORABLE_WHITESPACE]);
        assertEquals("PROCESSING_INSTRUCTION", XmlPullParser.TYPES[XmlPullParser.PROCESSING_INSTRUCTION]);
        assertEquals("COMMENT", XmlPullParser.TYPES[XmlPullParser.COMMENT]);
        assertEquals("DOCDECL", XmlPullParser.TYPES[XmlPullParser.DOCDECL]);

        // check setInput semantics
        assertEquals(XmlPullParser.START_DOCUMENT, xpp.getEventType());
        try {
            xpp.next();
            fail("exception was expected of next() if no input was set on parser");
        } catch(XmlPullParserException ex) {}

        xpp.setInput(null);
        assertEquals(XmlPullParser.START_DOCUMENT, xpp.getEventType());
        try {
            xpp.next();
            fail("exception was expected of next() if no input was set on parser");
        } catch(XmlPullParserException ex) {}

        try {
            xpp.setInput(null, null);
            fail("exception was expected of setInput() if input stream is null");
        } catch(IllegalArgumentException ex) {}

        xpp.setInput(null);
        assertTrue("line number must be -1 or >= 1 not "+xpp.getLineNumber(),
                   xpp.getLineNumber() == -1 || xpp.getLineNumber() >= 1);
        assertTrue("column number must be -1 or >= 0 not "+xpp.getColumnNumber(),
                   xpp.getColumnNumber() == -1 || xpp.getColumnNumber() >= 0);

        // check the simplest possible XML document - just one root element
        xpp.setInput(new StringReader("<foo></foo>"));
        assertEquals(null, xpp.getInputEncoding());
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, false /*empty*/, 0);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);


        //check taking input form input stream
        byte[] binput = "<foo/>".getBytes("UTF-8"); //Xerces2 doe snot like UTF8 ...
        xpp.setInput(new ByteArrayInputStream( binput ), "UTF-8" );
        assertEquals("UTF-8", xpp.getInputEncoding());
        //xpp.setInput(new StringReader( "<foo/>" ) );
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, true /*empty*/, 0);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);

        // one step further - it has an attribute and content ...
        xpp.setInput(new StringReader("<foo attrName='attrVal'>bar<p:t>\r\n\t </p:t></foo>"));
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, false, 1);
        checkAttrib(xpp, 0, "attrName", "attrVal");
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.TEXT, null, "bar", false, -1);
        assertEquals(false, xpp.isWhitespace());
        xpp.next();
        checkParserState(xpp, 2, XmlPullParser.START_TAG, "p:t", null, false, 0);
        xpp.next();
        checkParserState(xpp, 2, XmlPullParser.TEXT, null, "\n\t ", false, -1);
        assertTrue(xpp.isWhitespace());
        xpp.next();
        checkParserState(xpp, 2, XmlPullParser.END_TAG, "p:t", null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);


    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSimple.class));
    }

}

