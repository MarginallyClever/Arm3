/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Simple test to verify serializer (with no namespaces)
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSerialize extends UtilTestCase {
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSerialize.class));
    }
    
    public TestSerialize(String name) {
        super(name);
    }
    
    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        xpp = factory.newPullParser();
        assertEquals(false, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
    }
    
    protected void tearDown() {
    }
    
    public void testSimpleWriter() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        PackageTests.addNote("* default serializer "+ser.getClass()+"\n");
        
        //assert there is error if trying to write
        
        //assert there is error if trying to write
        try {
            ser.startTag(null, "foo");
            fail("exception was expected of serializer if no input was set on parser");
        } catch(Exception ex) {}
        
        ser.setOutput(null);
        
        //assert there is error if trying to write
        try {
            ser.startTag(null, "foo");
            fail("exception was expected of serializer if no input was set on parser");
        } catch(Exception ex) {}
        
        StringWriter sw = new StringWriter();
        
        ser.setOutput(sw);
        
        try {
            ser.setOutput(null, null);
            fail("exception was expected of setOutput() if output stream is null");
        } catch(IllegalArgumentException ex) {}
        
        //check get property
        
        ser.setOutput(sw);
        
        //assertEquals(null, ser.getOutputEncoding());
        
        ser.startDocument("ISO-8859-1", Boolean.TRUE);
        ser.startTag(null, "foo");
        
        ser.endTag(null, "foo");
        ser.endDocument();
        
        // now validate that can be deserialzied
        
        //xpp.setInput(new StringReader("<foo></foo>"));
        String serialized = sw.toString();
        xpp.setInput(new StringReader(serialized));
        
        assertEquals(null, xpp.getInputEncoding());
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, xpp.isEmptyElementTag() /*empty*/, 0);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);
    }
    
    public void testSimpleStream() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        ser.text("test");
        ser.endTag("", "foo");
        ser.endDocument();
        
        //check taking input form input stream
        //byte[] binput = "<foo>test</foo>".getBytes("UTF8");
        
        byte[] binput = baos.toByteArray();
        
        xpp.setInput(new ByteArrayInputStream( binput ), "UTF-8" );
        assertEquals("UTF-8", xpp.getInputEncoding());
        
        //xpp.setInput(new StringReader( "<foo/>" ) );
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, false /*empty*/, 0);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.TEXT, null, "test", false, -1);
        assertEquals(false, xpp.isWhitespace());
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);
    }
    
    public void testSimpleSerWithAttribute() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        // one step further - it has an attribute and content ...
        
        StringWriter sw = new StringWriter();
        
        assertEquals(0, ser.getDepth());
        ser.setOutput(sw);
        assertEquals(0, ser.getDepth());
        
        ser.startDocument(null, null);
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        ser.startTag(null, "foo");
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.attribute(null, "attrName", "attrVal");
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.text("bar");
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.startTag("", "p:t");
        assertEquals(2, ser.getDepth());
        assertEquals("", ser.getNamespace());
        assertEquals("p:t", ser.getName());
        
        ser.text("\n\t ");
        
        ser.endTag("", "p:t");
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.endTag(null, "foo");
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        ser.endDocument();
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        //xpp.setInput(new StringReader("<foo attrName='attrVal'>bar<p:t>\r\n\t </p:t></foo>"));
        String serialized = sw.toString();
        xpp.setInput(new StringReader(serialized));
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
    
    public void testEscaping() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        String s = "test\u0008\t\r\n";
        try {
            ser.attribute(null, "att", s);
            fail("expected to fail for control character (<32)");
        } catch(IllegalArgumentException e) {
        } catch(IllegalStateException e){
        }
        
        ser = factory.newSerializer();
        baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        try {
            ser.text(s);
        } catch(IllegalArgumentException e) {
        } catch(IllegalStateException e){
        }
        
        ser = factory.newSerializer();
        baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        s = "test\u0009\t\r\n";
        String expectedS = "test\u0009\t\n";
        ser.attribute(null, "att", s);
        ser.text(s);
        
        ser.endTag("", "foo");
        ser.endDocument();
        
        //check taking input form input stream
        //byte[] binput = "<foo>test</foo>".getBytes("UTF8");
        
        byte[] binput = baos.toByteArray();
        //System.out.println(getClass()+" binput="+printable(new String(binput)));
        
        xpp.setInput(new ByteArrayInputStream( binput ), "UTF-8" );
        assertEquals("UTF-8", xpp.getInputEncoding());
        
        //xpp.setInput(new StringReader( "<foo/>" ) );
        checkParserState(xpp, 0, XmlPullParser.START_DOCUMENT, null, null, false, -1);
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.START_TAG, "foo", null, false /*empty*/, 1);
        assertEquals(printable(s), printable(xpp.getAttributeValue(null, "att")));
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.TEXT, null, expectedS, false, -1);
        assertEquals(false, xpp.isWhitespace());
        xpp.next();
        checkParserState(xpp, 1, XmlPullParser.END_TAG, "foo", null, false, -1);
        xpp.next();
        checkParserState(xpp, 0, XmlPullParser.END_DOCUMENT, null, null, false, -1);
        
        baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        s = "test\u0000\t\r\n";
        try {
            ser.attribute(null, "att", s);
            fail("expected to fail as zero chartacter is illegal both in XML 1.0 and 1.1");
        } catch(Exception e) {}
        try {
            ser.text(s);
            fail("expected to fail as zero chartacter is illegal both in XML 1.0 and 1.1");
        } catch(Exception e) {}
    }
}

