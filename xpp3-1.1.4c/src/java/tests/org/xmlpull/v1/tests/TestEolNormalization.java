/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.StringReader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test end-of-line normalization
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestEolNormalization extends UtilTestCase {
    private XmlPullParserFactory factory;

    public TestEolNormalization(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        //factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        factory.setNamespaceAware(true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
    }

    protected void tearDown() {
    }

    public void testNormalizeLine()
        throws IOException, XmlPullParserException
    {

        XmlPullParser pp = factory.newPullParser();

        //-----------------------
        // ---- simple tests for end of line normalization

        final String simpleR = "-\n-\r-\r\n-\n\r-";

        // element content EOL normalizaton

        final String tagSimpleR = "<test>"+simpleR+"</test>";

        final String expectedSimpleN = "-\n-\n-\n-\n\n-";

        parseOneElement(pp, tagSimpleR, true);
        assertEquals(XmlPullParser.TEXT, pp.next());
        assertEquals(printable(expectedSimpleN), printable(pp.getText()));

        // attribute content normalization

        final String attrSimpleR = "<test a=\""+simpleR+"\"/>";

        final String normalizedSimpleN = "- - - -  -";

        parseOneElement(pp, attrSimpleR, true);
        String attrVal = pp.getAttributeValue("","a");

        //TODO Xerces2
        assertEquals(printable(normalizedSimpleN), printable(attrVal));

        //-----------------------
        // --- more complex example with more line engins together

        final String firstR =
            "\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r"+
            "";

        // element content

        final String tagR =
            "<m:test xmlns:m='Some-Namespace-URI'>"+
            firstR+
            "</m:test>\r\n";

        final String expectedN =
            "\n \n \n\n \n\n \n\n \n\n \n\n \n\n\n\n";

        parseOneElement(pp, tagR, true);
        assertEquals(XmlPullParser.TEXT, pp.next());
        assertEquals(printable(expectedN), printable(pp.getText()));

        // attribute value

        final String attrR =
            "<m:test xmlns:m='Some-Namespace-URI' fifi='"+firstR+"'/>";

        final String normalizedN =
            "                       ";

        parseOneElement(pp, attrR, true);
        attrVal = pp.getAttributeValue("","fifi");
        //System.err.println("attrNormalized.len="+normalizedN.length());
        //System.err.println("attrVal.len="+attrVal.length());

        //TODO Xerces2
        assertEquals(printable(normalizedN), printable(attrVal));


        //-----------------------
        // --- even more complex

        final String manyLineBreaks =
            "fifi\r&amp;\r&amp;\r\n foo &amp;\r bar \n\r\n&quot;"+
            firstR;

        final String manyTag =
            "<m:test xmlns:m='Some-Namespace-URI'>"+
            manyLineBreaks+
            "</m:test>\r\n";

        final String manyExpected =
            "fifi\n&\n&\n foo &\n bar \n\n\""+
            expectedN;
        //"\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r";

        parseOneElement(pp, manyTag, true);
        assertEquals(XmlPullParser.TEXT, pp.next());
        assertEquals(printable(manyExpected), printable(pp.getText()));
        assertEquals(manyExpected, pp.getText());

        assertEquals(pp.next(), XmlPullParser.END_TAG);
        assertEquals("test", pp.getName());

        // having \r\n as last characters is the hardest case
        //assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("\n", pp.readContent());
        assertEquals(pp.next(), XmlPullParser.END_DOCUMENT);


        final String manyAttr =
            "<m:test xmlns:m='Some-Namespace-URI' fifi='"+manyLineBreaks+"'/>";

        final String manyNormalized =
            "fifi & &  foo &  bar   \""+
            normalizedN;

        parseOneElement(pp, manyAttr, true);
        attrVal = pp.getAttributeValue("","fifi");
        //TODO Xerces2
        assertEquals(printable(manyNormalized), printable(attrVal));

    }

    private void parseOneElement(
        final XmlPullParser pp,
        final String buf,
        final boolean supportNamespaces)
        throws IOException, XmlPullParserException
    {
        //pp.setInput(buf.toCharArray());
        pp.setInput(new StringReader(buf));
        //pp.setNamespaceAware(supportNamespaces);
        pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, supportNamespaces);
        //pp.setAllowedMixedContent(false);
        pp.next();
        //pp.readStartTag(stag);
        if(supportNamespaces) {
            assertEquals("test", pp.getName());
        } else {
            assertEquals("m:test", pp.getName());
        }
    }


    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestEolNormalization.class));
    }

}

