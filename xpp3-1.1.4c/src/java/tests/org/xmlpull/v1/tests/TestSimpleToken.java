/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Simple test for minimal XML tokenizing
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSimpleToken extends UtilTestCase {
    private XmlPullParserFactory factory;


    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSimpleToken.class));
    }

    public TestSimpleToken(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        //System.out.println(getClass()+"-factory="+factory);
    }

    protected void tearDown() {
    }

    public void testVerySimpleToken() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        xpp.setInput(new StringReader("<foo><!--comment--><?target pi?></foo>"));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 0);
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.COMMENT, null, 0, null, null, "comment", false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.PROCESSING_INSTRUCTION, null, 0, null, null, "target pi", false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }

    public void testSimpleToken() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        // check setInput semantics
        assertEquals(XmlPullParser.START_DOCUMENT, xpp.getEventType());
        try {
            xpp.nextToken();
            fail("exception was expected of nextToken() if no input was set on parser");
        } catch(XmlPullParserException ex) {}

        xpp.setInput(null);
        assertEquals(XmlPullParser.START_DOCUMENT, xpp.getEventType());
        try {
            xpp.nextToken();
            fail("exception was expected of next() if no input was set on parser");
        } catch(XmlPullParserException ex) {}

        xpp.setInput(null); //reset parser
        // attempt to set roundtrip
        try {
            xpp.setFeature(FEATURE_XML_ROUNDTRIP, true);
        } catch(Exception ex) {
        }
        // did we succeeded?
        boolean roundtripSupported = xpp.getFeature(FEATURE_XML_ROUNDTRIP);


        // check the simplest possible XML document - just one root element
        for(int i = 1; i <= 2; ++i) {
            xpp.setInput(new StringReader(i == 1 ? "<foo/>" : "<foo></foo>"));
            boolean empty = (i == 1);
            checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
            xpp.nextToken();
            checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, empty, 0);
            if(roundtripSupported) {
                if(empty) {
                    //              System.out.println("tag='"+xpp.getText()+"'");
                    //              String foo ="<foo/>";
                    //              String foo2 = xpp.getText();
                    //              System.out.println(foo.equals(foo2));
                    assertEquals("empty tag roundtrip",
                                 printable("<foo/>"),
                                 printable(xpp.getText()));
                } else {
                    assertEquals("start tag roundtrip",
                                 printable("<foo>"),
                                 printable(xpp.getText()));
                }
            }
            xpp.nextToken();
            checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
            if(roundtripSupported) {
                if(empty) {
                    assertEquals("empty tag roundtrip",
                                 printable("<foo/>"),
                                 printable(xpp.getText()));
                } else {
                    assertEquals("end tag roundtrip",
                                 printable("</foo>"),
                                 printable(xpp.getText()));
                }
            }
            xpp.nextToken();
            checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        }
    }


    public void testNormalization() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        testNormalization(xpp);
        try{ xpp.setFeature(FEATURE_XML_ROUNDTRIP, false); } catch(Exception ex) {}
        testNormalization(xpp);
        try{ xpp.setFeature(FEATURE_XML_ROUNDTRIP, true); } catch(Exception ex) {}
        testNormalization(xpp);
    }

    public void testNormalization(XmlPullParser xpp) throws Exception {
        xpp.setInput(new StringReader("<foo>\n \r\n \n\r</foo>"));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false/*empty*/, 0);

        boolean roundtrip = xpp.getFeature(FEATURE_XML_ROUNDTRIP) == false;
        String text = nextTokenGathered(xpp, XmlPullParser.TEXT, true);
        if(roundtrip) {
            assertEquals(printable("\n \n \n\n"), printable(text));
            assertEquals("\n \n \n\n", text);
        } else {
            assertEquals(printable("\n \r\n \n\r"), printable(text));
            assertEquals("\n \r\n \n\r", text);
        }

        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.nextToken();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
}

