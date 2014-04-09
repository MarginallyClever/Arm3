/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test FEATURE_VALIDATION  (when supported)
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSimpleValidation extends UtilTestCase {
    private XmlPullParserFactory factory;

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSimpleValidation.class));
    }


    public TestSimpleValidation(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        assertEquals(false, factory.isNamespaceAware());
        assertEquals(false, factory.isValidating());
        //System.out.println("factory="+factory);
    }

    public void testValidation() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        try {
            xpp.setFeature(XmlPullParser.FEATURE_VALIDATION, true);
        } catch(XmlPullParserException ex) {
            return;
        }
        PackageTests.addNote("* feature "+XmlPullParser.FEATURE_VALIDATION+" is supported\n");
        // setting validation MUST enables also PROCESS_DOCDECL
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL));


        //http://www.w3.org/TR/REC-xml#NT-extSubsetDecl
        // minimum validation
        final String XML_MIN_PROLOG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"+
            "<!DOCTYPE greeting [\n"+
            "<!ELEMENT greeting (#PCDATA)>\n"+
            "]>\n";

        final String XML_MIN_VALID = XML_MIN_PROLOG+
            "<greeting>Hello, world!</greeting>\n";

        xpp.setInput(new StringReader( XML_MIN_VALID ));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        assertNull(xpp.getProperty(PROPERTY_XMLDECL_VERSION));
        assertNull(xpp.getProperty(PROPERTY_XMLDECL_STANDALONE));
        assertNull(xpp.getProperty(PROPERTY_XMLDECL_CONTENT));

        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "greeting", null, false/*empty*/, 0);

        //XMLDecl support is required when PROCESS DOCDECL enabled
        assertEquals("1.0", xpp.getProperty(PROPERTY_XMLDECL_VERSION));
        assertEquals(null, xpp.getProperty(PROPERTY_XMLDECL_STANDALONE));

        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, "Hello, world!", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "greeting", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);



        //AND WRONG
        final String XML_MIN_INVALID = XML_MIN_PROLOG+
            "<greet>Hello, world!</greet>\n";
        xpp.setInput(new StringReader( XML_MIN_INVALID ));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        try {
            xpp.next();
            fail("exception was expected of next() for invalid document element root");
        } catch(XmlPullParserException ex) {}

    }

}

