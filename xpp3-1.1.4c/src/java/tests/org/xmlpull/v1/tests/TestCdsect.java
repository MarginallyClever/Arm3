/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Simple test for CDATA parsing.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestCdsect extends UtilTestCase {
    private XmlPullParserFactory factory;
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestCdsect.class));
    }
    
    public TestCdsect(String name) {
        super(name);
    }
    
    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
    }
    
    protected void tearDown() {
    }
    
    
    public void testCdsect() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        
        final String XML =
            "<t><![CDATA[ f]]>o<![CDATA[o ]]></t>";
        
        xpp.setInput(new StringReader(XML));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "t", null, false/*empty*/, 0);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, " foo ", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "t", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
    
    public void testCdsectWithEol() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        
        final String XML =
            "<t> \n<![CDATA[fo]]>o<![CDATA[ \r\n\r]]>\n</t>";
        
        xpp.setInput(new StringReader(XML));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "t", null, false/*empty*/, 0);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, " \nfoo \n\n\n", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "t", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
    
    public void testCdsectWithComment() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        
        final String XML =
            "<t> \n<![CDATA[foo<![CDATA[ \r\n\r]]-->]]>\r</t>";
        
        xpp.setInput(new StringReader(XML));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "t", null, false/*empty*/, 0);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, " \nfoo<![CDATA[ \n\n]]-->\n", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "t", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
    
    public void testCdsectSoleContent() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        
        final String XML =
            "<t><![CDATA[foo<![CDATA[ \r\n\r]]-->]]></t>";
        
        xpp.setInput(new StringReader(XML));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "t", null, false/*empty*/, 0);
        xpp.next();
        //System.err.println(getClass()+" text="+printable(xpp.getText()));
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, "foo<![CDATA[ \n\n]]-->", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "t", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
    
    
    public void testBigCdsect() throws Exception {
        testBigCdsect(1);
        testBigCdsect(10);
        testBigCdsect(15);
        testBigCdsect(16);
        testBigCdsect(100);
        testBigCdsect(127);
        testBigCdsect(128);
        testBigCdsect(1023);
        testBigCdsect(1024);
        testBigCdsect(1025);
        testBigCdsect(16*1024-1);
        testBigCdsect(16*1024);
        testBigCdsect(16*1024+1);
    }
    
    public void testBigCdsect(int size) throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        
        // inspired by http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=238
        char[] oneToTen = "0123456789".toCharArray();
        StringBuffer cdataContent = new StringBuffer(size+16);
        for (int i = 0; i < size; i++) {
            char c = oneToTen[i % 10];
            cdataContent.append(c);
        }

        StringBuffer XML = new StringBuffer(size+16);
        XML.append("<foo><![CDATA[");
        XML.append(cdataContent);
        XML.append("]]></foo>");
        
        xpp.setInput(new StringReader(XML.toString()));
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false/*empty*/, 0);
        xpp.next();
        //System.err.println(getClass()+" text="+printable(xpp.getText()+" expected="+cdataContent.toString()));
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, cdataContent.toString(), false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        
    }
}

