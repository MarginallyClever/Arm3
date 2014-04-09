/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.StringReader;
import java.io.StringWriter;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Conformance test to verify nextToken() behavior.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestToken extends UtilTestCase {
    private XmlPullParserFactory factory;

    public TestToken(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
    }

    protected void tearDown() {
        factory = null;
    }


    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestToken.class));
    }

    private static final String FOO_XML =
        "<!DOCTYPE titlepage "+
        "[<!ENTITY % active.links \"INCLUDE\">"+
        "  <!ENTITY   test \"This is test! Do NOT Panic!\" >"+
        "]>"+
        "<foo attrName='attrVal'>bar<!--comment\r\ntest-->"+
        "&test;&test;&lt;&#32;"+
        "&amp;&gt;&apos;&quot;&#x20;&#x3C;"+
        "<?pi ds\r\nda?><![CDATA[ vo<o ]]></foo>";
    private static final String MISC_XML =
        //"\n \r\n \n\r<!DOCTYPE titlepage SYSTEM \"http://www.foo.bar/dtds/typo.dtd\""+
        //"<!--c2-->"+
        //"<?xml version='1.0'?><!--c-->"+
        //"   \r\n <?c x?>"+
        "\n \r\n \n\r"+
        "<!--c-->  \r\n"+
        FOO_XML+
        " \r\n";



    public void testTokenEventEquivalency() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        xpp.setInput(new StringReader(MISC_XML));
        boolean processDocdecl = xpp.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL);

        // make sure entity "test" can be resolved even when parser is not parsing DOCDECL
        if(!processDocdecl) {
            xpp.defineEntityReplacementText("test", "This is test! Do NOT Panic!");
        }

        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 1);
        checkAttribNs(xpp, 0, null, "", "attrName", "attrVal");
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null,
                           "barThis is test! Do NOT Panic!This is test! Do NOT Panic!< &>'\" < vo<o ",
                           false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);

    }

    public void testTokenTypes() throws Exception {
        testTokenTypes(false, false);
        testTokenTypes(false, true);
    }

    public void testTokenTypesInPrologAndEpilog() throws Exception {
        testTokenTypes(true, false);
        testTokenTypes(true, false);
    }

    // one step further - it has content ...
    public void testTokenTypes(boolean checkPrologAndEpilog,
                               boolean useRoundtrip) throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        //System.out.println("using "+xpp);

        if(checkPrologAndEpilog) {
            xpp.setInput(new StringReader(MISC_XML));
        } else {
            xpp.setInput(new StringReader(FOO_XML));
        }

        // attempt to set roundtrip
        try {
            xpp.setFeature(FEATURE_XML_ROUNDTRIP, useRoundtrip);
        } catch(Exception ex) {  // make sure we ignore if failed to set roundtrip ...
        }
        // did we succeeded?
        boolean roundtripSupported = xpp.getFeature(FEATURE_XML_ROUNDTRIP);
        //boolean unnormalizedSupported = xpp.getFeature(FEATURE_UNNORMALIZED_XML);
        if(!useRoundtrip && roundtripSupported != false) {
            throw new RuntimeException(
                "disabling feature "+FEATURE_XML_ROUNDTRIP+" must be supported");
        }

        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for START_DOCUMENT");
        } catch(XmlPullParserException ex) {
        }

        if(checkPrologAndEpilog) {
            xpp.nextToken();
            if(xpp.getEventType() == XmlPullParser.IGNORABLE_WHITESPACE) {
                //              xpp.nextToken();
                //              checkParserStateNs(xpp, 0, xpp.IGNORABLE_WHITESPACE, null, 0, null, null,
                //                                 "\n \r\n \n\r", false, -1);
                //              assertTrue(xpp.isWhitespace());
                String text = gatherTokenText(xpp, XmlPullParser.IGNORABLE_WHITESPACE, true);
                if(roundtripSupported) {
                    assertEquals(printable("\n \r\n \n\r"), printable(text));
                    assertEquals("\n \r\n \n\r", text);
                } else {
                    assertEquals(printable("\n \n \n\n"), printable(text));
                    assertEquals("\n \n \n\n", text);
                }
            }

            checkParserStateNs(xpp, 0, XmlPullParser.COMMENT, null, 0, null, null, "c", false, -1);
            try {
                xpp.isWhitespace();
                fail("whitespace function must fail for START_DOCUMENT");
            } catch(XmlPullParserException ex) {
            }

            xpp.nextToken();
            if(xpp.getEventType() == XmlPullParser.IGNORABLE_WHITESPACE) {
                //              xpp.nextToken();
                //              checkParserStateNs(xpp, 0, xpp.IGNORABLE_WHITESPACE, null, 0, null, null, "  \r\n", false, -1);
                //              assertTrue(xpp.isWhitespace());
                //String text = nextTokenGathered(xpp, xpp.IGNORABLE_WHITESPACE, true);
                String text = gatherTokenText(xpp, XmlPullParser.IGNORABLE_WHITESPACE, true);
                if(roundtripSupported) {
                    assertEquals(printable("  \r\n"), printable(text));
                } else {
                    assertEquals(printable("  \n"), printable(text));
                }
            }


        } else {
            xpp.nextToken();
        }
        checkParserStateNs(xpp, 0, XmlPullParser.DOCDECL, null, 0, null, null, false, -1);
        String expectedDocdecl = " titlepage "+
            //"SYSTEM \"http://www.foo.bar/dtds/typo.dtd\""+
            "[<!ENTITY % active.links \"INCLUDE\">"+
            "  <!ENTITY   test \"This is test! Do NOT Panic!\" >]";
        String gotDocdecl = xpp.getText();
        if(roundtripSupported && gotDocdecl == null) {
            fail("when roundtrip is enabled DOCDECL content must be reported");
        }
        if(gotDocdecl != null) {
            assertEquals("DOCDECL content", expectedDocdecl, gotDocdecl);
        }

        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for DOCDECL");
        } catch(XmlPullParserException ex) {
        }

        // now parse elements
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 1);
        if(roundtripSupported) {
            assertEquals("start tag roundtrip", "<foo attrName='attrVal'>", xpp.getText());
        }
        checkAttribNs(xpp, 0, null, "", "attrName", "attrVal");
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for START_DOCUMENT");
        } catch(XmlPullParserException ex) {
        }

        {
            String text = nextTokenGathered(xpp, XmlPullParser.TEXT, false);
            assertEquals(printable("bar"), printable(text));
        }

        //xpp.nextToken();

        checkParserStateNs(xpp, 1, XmlPullParser.COMMENT, null, 0, null, null, false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for COMMENT");
        } catch(XmlPullParserException ex) {
        }
        {
            String text = xpp.getText();
                if(roundtripSupported) {
                    assertEquals(printable("comment\r\ntest"), printable(text));
                } else {
                    assertEquals(printable("comment\ntest"), printable(text));
                }
            }

        boolean processDocdecl = xpp.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL);

        // uresolved entity must be reurned as null by nextToken()
        xpp.nextToken();
        if(!processDocdecl) {
            checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null,
                               "test", null, false, -1);
        } else {
            checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null,
                               "test", "This is test! Do NOT Panic!", false, -1);

        }

        // now we check if we can resolve entity
        if(!processDocdecl) {
            xpp.defineEntityReplacementText("test", "This is test! Do NOT Panic!");
        }
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null,
                           "test", "This is test! Do NOT Panic!", false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for ENTITY_RED");
        } catch(XmlPullParserException ex) {
        }

        // check standard entities and char refs
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "lt", "<", false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for ENTITY_REF");
        } catch(XmlPullParserException ex) {
        }

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "#32", " ", false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for ENTITY_REF");
        } catch(XmlPullParserException ex) {
        }

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "amp", "&", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "gt", ">", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "apos", "'", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "quot", "\"", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "#x20", " ", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "#x3C", "<", false, -1);

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.PROCESSING_INSTRUCTION, null, 0, null, null, false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for START_DOCUMENT");
        } catch(XmlPullParserException ex) {
        }
        {
            String text = xpp.getText();
                if(roundtripSupported) {
                    assertEquals(printable("pi ds\r\nda"), printable(text));
                } else {
                    assertEquals(printable("pi ds\nda"), printable(text));
                }
            }

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.CDSECT, null, 0, null, null, " vo<o ", false, -1);
        assertEquals(false, xpp.isWhitespace());

        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        if(roundtripSupported) {
            assertEquals("end tag roundtrip", "</foo>", xpp.getText());
        }
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for END_TAG");
        } catch(XmlPullParserException ex) {
        }

        xpp.nextToken();
        if(checkPrologAndEpilog) {
            //if(unnormalizedSupported) {
            if(xpp.getEventType() == XmlPullParser.IGNORABLE_WHITESPACE) {

                //              xpp.nextToken();
                //              checkParserStateNs(xpp, 0, xpp.IGNORABLE_WHITESPACE, null, 0, null, null,
                //                                 " \r\n", false, -1);
                //              assertTrue(xpp.isWhitespace());
                //String text = nextTokenGathered(xpp, xpp.IGNORABLE_WHITESPACE, true);
                String text = gatherTokenText(xpp, XmlPullParser.IGNORABLE_WHITESPACE, true);
                if(roundtripSupported) {
                    assertEquals(printable(" \r\n"), printable(text));
                } else {
                    assertEquals(printable(" \n"), printable(text));
                }
            }
        }
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        try {
            xpp.isWhitespace();
            fail("whitespace function must fail for END_DOCUMENT");
        } catch(XmlPullParserException ex) {
        }

    }

    // one step further - it has content ...
    public void testXmlRoundtrip() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));


        // reset parser
        xpp.setInput(null);
        // attempt to set roundtrip
        try {
            xpp.setFeature(FEATURE_XML_ROUNDTRIP, true);
        } catch(Exception ex) {
        }
        // did we succeeded?
        boolean roundtripSupported = xpp.getFeature(FEATURE_XML_ROUNDTRIP);

        if(!roundtripSupported) {
            return;
        }
        PackageTests.addNote("* optional feature  "+FEATURE_XML_ROUNDTRIP+" is supported\n");

        StringWriter sw = new StringWriter();
        String s;
        //StringWriter st = new StringWriter();
        xpp.setInput(new StringReader(MISC_XML));
        int[] holderForStartAndLength = new int[2];
        char[] buf;
        while(xpp.nextToken() != XmlPullParser.END_DOCUMENT) {
            switch(xpp.getEventType()) {
                case XmlPullParser.START_TAG:
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip START_TAG", xpp.getText(), s);
                    sw.write(s);
                    break;
                case XmlPullParser.END_TAG:
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip END_TAG", xpp.getText(), s);
                    sw.write(s);
                    break;
                case XmlPullParser.TEXT:
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip TEXT", xpp.getText(), s);
                    sw.write(s);
                    break;
                case XmlPullParser.IGNORABLE_WHITESPACE:
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip IGNORABLE_WHITESPACE", xpp.getText(), s);
                    sw.write(s);
                    break;
                case XmlPullParser.CDSECT:
                    sw.write("<![CDATA[");
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip CDSECT", xpp.getText(), s);
                    sw.write(s);
                    sw.write("]]>");
                    break;
                case XmlPullParser.PROCESSING_INSTRUCTION:
                    sw.write("<?");
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip PROCESSING_INSTRUCTION", xpp.getText(), s);
                    sw.write(s);
                    sw.write("?>");
                    break;
                case XmlPullParser.COMMENT:
                    sw.write("<!--");
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip COMMENT", xpp.getText(), s);
                    sw.write(s);
                    sw.write("-->");
                    break;
                case XmlPullParser.ENTITY_REF:
                    sw.write("&");
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip ENTITY_REF", xpp.getName(), s);
                    sw.write(s);
                    sw.write(";");
                    break;
                case XmlPullParser.DOCDECL:
                    sw.write("<!DOCTYPE");
                    buf = xpp.getTextCharacters(holderForStartAndLength);
                    s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                    assertEquals("roundtrip DOCDECL", xpp.getText(), s);
                    sw.write(s);
                    sw.write(">");
                    break;
                default:
                    throw new RuntimeException("unknown token type");
            }
        }
        sw.close();
        String RESULT_XML_BUF = sw.toString();
        assertEquals("rountrip XML", printable(MISC_XML), printable(RESULT_XML_BUF));
    }


}

