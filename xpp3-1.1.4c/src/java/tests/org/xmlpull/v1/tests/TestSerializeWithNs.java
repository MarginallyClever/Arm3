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
 * Simple test to verify serializer (with namespaces)
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSerializeWithNs extends UtilTestCase {
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSerializeWithNs.class));
    }
    
    public TestSerializeWithNs(String name) {
        super(name);
    }
    
    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setNamespaceAware(true);
        // now validate that can be deserialzied
        xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
    }
    
    protected void tearDown() {
    }
    
    
    private void checkSimpleWriterResult(String textContent) throws Exception {
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, xpp.isEmptyElementTag() /*empty*/, 0);
        if(textContent != null) {
            xpp.next();
            checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, textContent, false, -1);
        }
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }
    
    public void testSimpleWriter() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        //assert there is error if trying to write
        
        //assert there is error if trying to write
        try {
            ser.startTag("", "foo");
            fail("exception was expected of serializer if no input was set");
        } catch(Exception ex) {}
        
        ser.setOutput(null);
        
        //assert there is error if trying to write
        try {
            ser.startTag("", "foo");
            fail("exception was expected of serializer if no input was set");
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
        ser.startTag("", "foo");
        
        //TODO: check that startTag(null, ...) is allowed
        
        ser.endTag("", "foo");
        ser.endDocument();
        
        
        //xpp.setInput(new StringReader("<foo></foo>"));
        String serialized = sw.toString();
        xpp.setInput(new StringReader(serialized));
        
        assertEquals(null, xpp.getInputEncoding());
        checkSimpleWriterResult(null);
    }
    
    
    public void testSimpleOutputStream() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF-8");
        ser.startDocument("UTF-8", null);
        ser.startTag("", "foo");
        final String text = "\"test<&>&amp;";
        ser.text(text);
        ser.endTag("", "foo");
        ser.endDocument();
        
        //check taking input form input stream
        //byte[] binput = "<foo>test</foo>".getBytes("UTF8");
        
        byte[] binput = baos.toByteArray();
        
        xpp.setInput(new ByteArrayInputStream( binput ), "UTF-8" );
        assertEquals("UTF-8", xpp.getInputEncoding());
        
        //xpp.setInput(new StringReader( "<foo/>" ) );
        
        checkSimpleWriterResult(text);
        
    }
    
    public void testNamespaceGeneration() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        //System.out.println(getClass()+" ser="+ser);
        
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        //assertEquals(null, ser.getOutputEncoding());
        
        ser.startDocument("ISO-8859-1", Boolean.TRUE);
        
        ser.setPrefix("boo", "http://example.com/boo");
        ser.startTag("", "foo");
        ser.attribute("http://example.com/boo", "attr", "val");
        
        String booPrefix = ser.getPrefix("http://example.com/boo", false);
        assertEquals("boo", booPrefix);
        
        //check that new prefix may be generated after startTag and used as attribbute value
        String newPrefix = ser.getPrefix("http://example.com/bar", true);
        ser.attribute("http://example.com/bar", "attr", "val");
        
        ser.startTag("http://example.com/bar", "foo");
        ser.endTag("http://example.com/bar", "foo");
        
        String checkPrefix = ser.getPrefix("http://example.com/bar", false);
        assertEquals(newPrefix, checkPrefix);
        
        ser.endTag("", "foo");
        checkPrefix = ser.getPrefix("http://example.com/bar", false);
        assertEquals(null, checkPrefix);
        
        ser.endDocument();
        
        
        //xpp.setInput(new StringReader("<foo></foo>"));
        String serialized = sw.toString();
        //System.out.println(getClass()+" serialized="+serialized);
        //xpp.setInput(new StringReader(serialized));
    }
    
    public void testMisc() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        // all comments etc
        ser.startDocument(null, Boolean.TRUE);
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        
        final String docdecl = " foo [\n"+
            "<!ELEMENT foo (#PCDATA|bar)* >\n"+
            "<!ELEMENT pbar (#PCDATA) >\n"
            +"]";
        ser.docdecl(docdecl);
        ser.processingInstruction("pi test");
        final String iws = "\n\t";
        ser.ignorableWhitespace(iws);
        
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        ser.startTag(null, "foo");
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        
        //check escaping & < > " '
        final String attrVal = "attrVal&<>\"''&amp;";
        //final String attrVal = "attrVal&;";
        ser.attribute(null, "attrName", attrVal);
        
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.entityRef("amp");
        final String cdsect = "hello<test>\"test";
        ser.cdsect(cdsect);
        
        ser.setPrefix("ns1", "usri2");
        
        assertEquals(1, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals("foo", ser.getName());
        
        ser.startTag("uri1", "bar");
        assertEquals(2, ser.getDepth());
        assertEquals("uri1", ser.getNamespace());
        assertEquals("bar", ser.getName());
        
        final String text = "test\n\ntest";
        char[] buf = text.toCharArray();
        ser.text(buf, 0, buf.length);
        
        final String comment = "comment B- ";
        ser.comment(comment);
        assertEquals(2, ser.getDepth());
        assertEquals("uri1", ser.getNamespace());
        assertEquals("bar", ser.getName());
        
        assertEquals(2, ser.getDepth());
        assertEquals("uri1", ser.getNamespace());
        assertEquals("bar", ser.getName());
        
        
        ser.endDocument(); // should close unclosed foo and bar start tag
        assertEquals(0, ser.getDepth());
        assertEquals(null, ser.getNamespace());
        assertEquals(null, ser.getName());
        
        // -- now check that we get back what we serialized ...
        
        String serialized = sw.toString();
        //System.out.println(getClass()+" serialized="+serialized);
        xpp.setInput(new StringReader(serialized));
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        
        xpp.nextToken();
        checkParserStateNs(xpp, 0, XmlPullParser.DOCDECL, null, 0, null, null, false, -1);
        String gotDocdecl = xpp.getText();
        if(gotDocdecl != null) {
            assertEquals(printable(docdecl), printable(gotDocdecl));
        }
        
        xpp.nextToken();
        checkParserStateNs(xpp, 0, XmlPullParser.PROCESSING_INSTRUCTION, null, 0, null, null, "pi test", false, -1);
        
        
        xpp.nextToken();
        if(xpp.getEventType() == XmlPullParser.IGNORABLE_WHITESPACE) {
            String expectedIws = gatherTokenText(xpp, XmlPullParser.IGNORABLE_WHITESPACE, true);
            assertEquals(printable(iws), printable(expectedIws));
        }
        
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 1);
        checkAttribNs(xpp, 0, null, "", "attrName", attrVal);
        
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.ENTITY_REF, null, 0, null, "amp", "&", false, -1);
        
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.CDSECT, null, 0, null, null, cdsect, false, -1);
        assertEquals(false, xpp.isWhitespace());
        
        xpp.nextToken();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, 2, "uri1", "bar", false, 0);
        
        String gotText = nextTokenGathered(xpp, XmlPullParser.TEXT, false);
        //System.out.println(getClass()+" text="+printable(text)+" got="+printable(gotText));
        assertEquals(printable(text), printable(gotText));
        
        //xpp.nextToken();
        checkParserStateNs(xpp, 2, XmlPullParser.COMMENT, null, 2, null, null, comment, false, -1);
        
        xpp.nextToken();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, 2, "uri1", "bar", false, -1);
        
        xpp.nextToken();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, 0, "", "foo", false, -1);
        
    }
    
    private static final String ENV = "http://www.w3.org/2002/06/soap-envelope";
    private static final String ALERTCONTROL = "http://example.org/alertcontrol";
    private static final String ALERT = "http://example.org/alert";
    private static final String EXPIRES = "2001-06-22T14:00:00-05:00";
    private static final String MSG = "Pick up Mary at school at 2pm";
    private static final String ROLE = "http://www.w3.org/2002/06/soap-envelope/role/ultimateReceiver";
    
    // based on example from SOAP 1.2 spec http://www.w3.org/TR/soap12-part1/
    private static final String SOAP12 =
        "<env:Envelope xmlns:env=\""+ENV+"\">"+
        "<env:Header>"+
        "<n:alertcontrol xmlns:n=\""+ALERTCONTROL+"\""+
        " env:mustUnderstand=\"true\""+
        " env:role=\""+ROLE+"\">"+
        "<n:priority>1</n:priority>"+
        "<n:expires>"+EXPIRES+"</n:expires>"+
        "</n:alertcontrol>"+
        "</env:Header>"+
        "<env:Body>"+
        "<m:alert xmlns:m=\""+ALERT+"\" >"+
        "<m:msg>"+MSG+"</m:msg>"+
        "</m:alert>"+
        "</env:Body>"+
        "</env:Envelope>";
    
    
    private String generateSoapEnvelope(String envPrefix,
                                        String alertcontrolPrefix,
                                        String alertPrefix) throws Exception
    {
        return generateSoapEnvelope(envPrefix, alertcontrolPrefix, alertPrefix,
                                    null, null, null);
    }
    
    private final String PROPERTY_SERIALIZER_INDENTATION =
        "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";
    private final String PROPERTY_SERIALIZER_LINE_SEPARATOR =
        "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator";
    private final String FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE =
        "http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe";
    
    private boolean serializerIndentationSupported;
    private boolean serializerLineSeparatorSupported;
    private boolean serializerUseApostropheSupported;
    
    
    /**
     * Test optional support pretty printing
     */
    public void testUseApostrophe() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        try {
            ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE, true);
        } catch(Exception ex) {
            // ignore test if optional property not supported
            return;
        }
        PackageTests.addNote("* optional feature  "+FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE+" is supported\n");
        
        boolean useApost = ser.getFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE);
        assertEquals(true, useApost);
        checkAttributeQuot(true, ser);
        
        ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE, false);
        useApost = ser.getFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE);
        assertEquals(false, useApost);
        
        checkAttributeQuot(false, ser);
        useApost = ser.getFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE);
        assertEquals(false, useApost);
        
        ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE, true);
        useApost = ser.getFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE);
        assertEquals(true, useApost);
        checkAttributeQuot(true, ser);
        
        checkAttributeQuotMix(ser);
    }
    
    /**
     * Check that attribute was quoted correctly
     */
    private void checkAttributeQuot(boolean useApostrophe, XmlSerializer ser) throws Exception {
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        ser.startTag("", "test");
        ser.attribute(null, "att", "value");
        ser.endTag("", "test");
        ser.endDocument();
        
        String s = sw.toString();
        if(useApostrophe) {
            assertTrue("use apostrophe for attribute value", s.indexOf("'value'") !=-1);
        } else {
            assertTrue("use quotation for attribute value", s.indexOf("\"value\"") !=-1);
        }
        
        // some validaiton of serialized XML
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(s));
        pp.nextTag();
        pp.require(XmlPullParser.START_TAG, null, "test");
        assertEquals("value", pp.getAttributeValue(XmlPullParser.NO_NAMESPACE, "att"));
        pp.nextTag();
        pp.require(XmlPullParser.END_TAG, null, "test");
    }
    
    /**
     * Check that attribute quotations can be changed _during_ serialization
     */
    private void checkAttributeQuotMix(XmlSerializer ser) throws Exception {
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        ser.startTag("", "test");
        ser.attribute(null, "att", "value");
        ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE, true);
        ser.attribute(null, "attA", "valueA");
        ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE, false);
        ser.attribute(null, "attQ", "valueQ");
        ser.endTag("", "test");
        ser.endDocument();
        
        String s = sw.toString();
        assertTrue("use apostrophe for attribute value", s.indexOf("'valueA'") !=-1);
        assertTrue("use apostrophe for attribute value", s.indexOf("\"valueQ\"") !=-1);
        
        // some validaiton of serialized XML
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(s));
        pp.nextTag();
        pp.require(XmlPullParser.START_TAG, null, "test");
        assertEquals("value", pp.getAttributeValue(XmlPullParser.NO_NAMESPACE, "att"));
        assertEquals("valueA", pp.getAttributeValue(XmlPullParser.NO_NAMESPACE, "attA"));
        assertEquals("valueQ", pp.getAttributeValue(XmlPullParser.NO_NAMESPACE, "attQ"));
        pp.nextTag();
        pp.require(XmlPullParser.END_TAG, null, "test");
    }
    
    
    public void testIndentation() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        try {
            ser.setProperty(PROPERTY_SERIALIZER_INDENTATION, " ");
        } catch(Exception ex) {
            // ignore test if optional property not supported
            return;
        }
        PackageTests.addNote("* optional property "+PROPERTY_SERIALIZER_INDENTATION+" is supported\n");
        
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        ser.startTag("", "S1");
        ser.startTag("", "S2");
        ser.text("T");
        ser.endTag("", "S2");
        ser.startTag("", "M2");
        ser.startTag("", "M3");
        ser.endTag("", "M3");
        ser.endTag("", "M2");
        ser.endTag("", "S1");
        ser.endDocument();
        
        String xml = sw.toString();
        //System.out.println(getClass()+" xml="+xml);
        checkFormatting(" ", 0, "\n", "<S1", xml);
        checkFormatting(" ", 1, "\n", "<S2", xml);
        checkFormatting("T", 1, null, "</S2", xml); //special case set that no indent but content
        checkFormatting(" ", 1, "\n", "<M2", xml);
        checkFormatting(" ", 2, "\n", "<M3", xml);
        checkFormatting(" ", 1, "\n", "</M2", xml);
        checkFormatting(" ", 0, "\n", "</S1", xml);
        
        //TODO check if line separators property is supported ...
    }
    
    private void checkFormatting(String indent, int level, String lineSeparator,
                                 String s, String xml) throws Exception {
        // check that s is on output XML
        int pos = xml.indexOf(s);
        assertTrue(pos >= 0);
        // check that indent string is used at level
        for (int i = 0; i < level; i++)
        {
            for (int j = indent.length() - 1; j >= 0 ; j--)
            {
                --pos;
                if(pos < 0) {
                    fail("not enough indent for "+printable(s)+" in "+printable(xml));
                }
                char indentCh = indent.charAt(j);
                char ch = xml.charAt(pos);
                assertEquals(
                    "expected indentation character '"+printable(indent)+"'"
                        +" pos="+pos+" s='"+printable(s)
                        +"' xml="+printable(xml),
                    printable(indentCh), printable(ch));
            }
        }
        // check that indent is of exact size and line ending is as expected
        if(pos > 0) {
            --pos;
            char ch = xml.charAt(pos);
            if(lineSeparator != null) {
                for (int i = lineSeparator.length() - 1; i >=0 ; i--)
                {
                    char lineSepCh = lineSeparator.charAt(i);
                    assertEquals(
                        "expected end of line at pos="+pos+" s='"+printable(s)
                            +"' xml="+printable(xml),
                        printable(lineSepCh), printable(ch));
                    --pos;
                    ch = xml.charAt(pos);
                    
                }
            } else {
                char indentCh = indent.charAt(indent.length() - 1);
                assertTrue(
                    "expected character that is different from '"+printable(indent)+"'"
                        +" used for indentation "
                        +"pos="+pos+" s="+printable(s)+" xml="+printable(xml),
                    indentCh != ch);
            }
        }
    }
    
    public void testLineSeparator() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        try {
            ser.setProperty(PROPERTY_SERIALIZER_LINE_SEPARATOR, "\n");
        } catch(Exception ex) {
            // ignore test if optional property not supported
            return;
        }
        PackageTests.addNote("* optional property "+PROPERTY_SERIALIZER_LINE_SEPARATOR+" is supported\n");
    }
    
    /** generate SOAP 1.2 envelope
     try to use indentation
     
     and check automtic namespace prefix declaration
     and auto-generation of prefixes
     
     */
    private String generateSoapEnvelope(String envPrefix,
                                        String alertcontrolPrefix,
                                        String alertPrefix,
                                        Boolean attvalueUseApostrophe,
                                        String indentation,
                                        String lineSeparator
                                       )
        throws Exception
    {
        XmlSerializer ser = factory.newSerializer();
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        
        if(attvalueUseApostrophe !=null) {
            try {
                ser.setFeature(FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE,
                               attvalueUseApostrophe.booleanValue());
                serializerUseApostropheSupported = true;
            } catch(Exception ex) {
                // ignore if optional feature not supported
            }
        }
        if(indentation !=null) {
            try {
                ser.setProperty(PROPERTY_SERIALIZER_INDENTATION, indentation);
                serializerIndentationSupported = true;
            } catch(Exception ex) {
                // ignore if optional property not supported
            }
        }
        if(lineSeparator !=null) {
            try {
                ser.setProperty(PROPERTY_SERIALIZER_LINE_SEPARATOR, lineSeparator);
                serializerLineSeparatorSupported = true;
            } catch(Exception ex) {
                // ignore if optional property not supported
            }
        }
        
        // all comments etc
        ser.startDocument(null, Boolean.TRUE);
        
        if(envPrefix != null) ser.setPrefix(envPrefix, ENV);
        ser.startTag(ENV, "Envelope");
        ser.startTag(ENV, "Header");
        
        if(alertcontrolPrefix != null) ser.setPrefix(alertcontrolPrefix, ALERTCONTROL);
        ser.startTag(ALERTCONTROL, "alertcontrol");
        ser.attribute(ENV, "mustUnderstand", "true");
        ser.attribute(ENV, "role", ROLE);
        
        ser.startTag(ALERTCONTROL, "priority");
        ser.text("1");
        ser.endTag(ALERTCONTROL, "priority");
        
        ser.startTag(ALERTCONTROL, "expires");
        ser.text(EXPIRES);
        ser.endTag(ALERTCONTROL, "expires");
        
        ser.endTag(ALERTCONTROL, "alertcontrol");
        
        ser.endTag(ENV, "Header");
        
        ser.startTag(ENV, "Body");
        
        if(alertPrefix != null) ser.setPrefix(alertPrefix, ALERT);
        ser.startTag(ALERT, "alert");
        
        ser.startTag(ALERT, "msg");
        ser.text(MSG);
        ser.endTag(ALERT, "msg");
        
        ser.endTag(ALERT, "alert");
        
        
        ser.endTag(ENV, "Body");
        
        ser.endTag(ENV, "Envelope");
        
        ser.endDocument();
        
        String s = sw.toString();
        
        return s;
    }
    
    public void testSetPrefix(String prefix) throws Exception {
        XmlSerializer ser = factory.newSerializer();
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        final String NS = "http://example.com/test";
        ser.setPrefix(prefix, NS);
        ser.startTag(NS, "foo");
        ser.endDocument();
        
        String serialized = sw.toString();
        //System.out.println(getClass()+" sw="+sw);
        xpp.setInput(new StringReader(serialized));
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        String expectedPrefix = (prefix != null && prefix.length() == 0) ? null : prefix;
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, expectedPrefix, 1, NS, "foo", null, xpp.isEmptyElementTag() /*empty*/, 0);
    }
    
    public void testSetPrefix() throws Exception {
        testSetPrefix("ns");
        testSetPrefix("");
        testSetPrefix(null);
    }

    /**
     * Testing for case described in http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=241
     */
    public void testAttrPrefix(String prefix, boolean extraPrefix) throws Exception {
        XmlSerializer ser = factory.newSerializer();
        //System.err.println(getClass()+" ser="+ser.getClass());
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        final String NS = "http://example.com/test";
        ser.setPrefix(prefix, NS);
        if(extraPrefix) ser.setPrefix("p1", NS);
        ser.startTag(NS, "foo");
        ser.attribute(NS, "attr", "aar");
        ser.endDocument();
        
        String serialized = sw.toString();
        xpp.setInput(new StringReader(serialized));
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        String expectedPrefix = (prefix != null && prefix.length() == 0) ? null : prefix;
        if(extraPrefix) expectedPrefix = "p1";
        int nsCount = 1;
        if(extraPrefix) ++nsCount;
        if(expectedPrefix == null) ++nsCount;
        //System.err.println(getClass()+" nsCount="+nsCount+" extraPrefix="+extraPrefix+" expectedPrefix="+expectedPrefix+" sw="+sw);
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, expectedPrefix, nsCount, NS, "foo", null, xpp.isEmptyElementTag() /*empty*/, 1);
        checkAttribNs(xpp, 0, NS, "attr", "aar");
    }

    public void testAttrPrefix() throws Exception {
        testAttrPrefix("ns", false);
        testAttrPrefix("", false);
        testAttrPrefix(null, false);
        testAttrPrefix("ns", true);
        testAttrPrefix("", true);
        testAttrPrefix(null, true);
    }

    
    /** setPrefix check that prefix is not duplicated ... */
    public void testSetPrefixAdv() throws Exception {
        //TODO check redeclaring defult namespace
        
        
        checkTestSetPrefixSoap(SOAP12);
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m"));
        checkTestSetPrefixSoap(generateSoapEnvelope(null, null, "m"));
        checkTestSetPrefixSoap(generateSoapEnvelope("env", null, "m"));
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "", ""));
        
        
        String generated = generateSoapEnvelope("", "n", "m");
        //System.err.println(getClass()+" generated="+generated);
        
        // 1 is for one extra namespace must be added to declare xmlns namespace
        //    for attrbute mustUnderstan in SOAP-ENV namespace
        checkTestSetPrefixSoap(generated, 1,false);
        
        
        checkTestSetPrefixSoap(generateSoapEnvelope("", null, "m"),1,false);
        
        //check optional pretty printing
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m", Boolean.FALSE, null, null));
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m", Boolean.TRUE, null, null));
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m", null, " ", null), true);
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m", null, "\t", null), true);
        checkTestSetPrefixSoap(generateSoapEnvelope("env", "n", "m", null, "    ", null), true);
        String s = generateSoapEnvelope("env", "n", "m", Boolean.TRUE, " ", "\n");
        //System.out.println(getClass()+" envelope="+generateSoapEnvelope("", "n", "m"));
        checkTestSetPrefixSoap(s, true);
    }
    
    /** check that ti is possible to select which prefix should be used for namespace
     * For more details check http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=169
     */
    public void testSetPrefixPreferences() throws Exception {
        testSetPrefixPreferences("", null);
        testSetPrefixPreferences("saml", "saml");
        
        // check case when prefix in Infoset is invalid -- prefix preoprty is optional after all
        // http://www.w3.org/TR/xml-infoset/#infoitem.element
        testSetPrefixPreferences("sample", "saml");
        
        testSetPrefixPreferences("", null);
        testSetPrefixPreferences("saml", "saml");
        
    }
    
    private void testSetPrefixPreferences(String preferredPrefix, String expectedPrefix) throws Exception {
        
        XmlSerializer ser = factory.newSerializer();
        StringWriter sw = new StringWriter();
        ser.setOutput(sw);
        //<Assertion xmlns="urn:oasis:names:tc:SAML:1.0:assertion"
        final String NS = "urn:oasis:names:tc:SAML:1.0:assertion";
        boolean deferred = false;
        if("".equals(preferredPrefix)) {
            deferred = true;
        } else {
            ser.setPrefix("", NS);
        }
        if("saml".equals(preferredPrefix)) {
            deferred = true;
        } else {
            ser.setPrefix("saml", NS);
            
        }
        ser.setPrefix("samlp", "urn:oasis:names:tc:SAML:1.0:protocol");
        if(deferred) {
            ser.setPrefix(preferredPrefix, NS);
        }
        ser.startTag(NS, "Assertion");
        boolean serAtt = !"".equals(preferredPrefix);
        if(serAtt) {
            ser.attribute(NS, "test", "1");
        }
        ser.endTag(NS, "Assertion");
        ser.endDocument();
        
        sw.close();
        String serialized = sw.toString();
        //System.out.println("\n"+getClass()+" sw="+sw);
        xpp.setInput(new StringReader(serialized));
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        //String expectedPrefix = (prefix2 != null && prefix2.length() == 0) ? null : prefix2;
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, expectedPrefix, 3,
                           NS, "Assertion", null, xpp.isEmptyElementTag() /*empty*/, serAtt ? 1 : 0);
        if(serAtt) {
            assertEquals(expectedPrefix, xpp.getAttributePrefix(0)); //NS, "test
        }
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, expectedPrefix, 3,
                           NS, "Assertion", null, false, -1);
        
    }
    
    private void checkTestSetPrefixSoap(String soapEnvelope) throws Exception {
        checkTestSetPrefixSoap(soapEnvelope, 0, false);
    }
    
    private void checkTestSetPrefixSoap(String soapEnvelope, boolean indented) throws Exception {
        checkTestSetPrefixSoap(soapEnvelope, 0, indented);
    }
    
    // run test using checkParserStateNs()
    private void checkTestSetPrefixSoap(String soapEnvelope, int extraNs, boolean indented)
        throws Exception
    {
        
        //compare that XML representation of soapEnvelope is as desired
        if(!indented) {
            assertXmlEquals(SOAP12, soapEnvelope);
        }
        
        xpp.setInput(new StringReader(soapEnvelope));
        
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, 1, ENV, "Envelope", false /*empty*/, 0);
        xpp.nextTag();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, 1, ENV, "Header", false /*empty*/, 0);
        xpp.nextTag();
        checkParserStateNs(xpp, 3, XmlPullParser.START_TAG,
                           2+extraNs, ALERTCONTROL, "alertcontrol", false /*empty*/, 2);
        checkAttribNs(xpp, 0, ENV, "mustUnderstand", "true");
        checkAttribNs(xpp, 1, ENV, "role", ROLE);
        
        xpp.nextTag();
        checkParserStateNs(xpp, 4, XmlPullParser.START_TAG, 2+extraNs, ALERTCONTROL, "priority", false /*empty*/, 0);
        String text = xpp.nextText();
        assertEquals("1", text);
        xpp.nextTag();
        checkParserStateNs(xpp, 4, XmlPullParser.START_TAG, 2+extraNs, ALERTCONTROL, "expires", false /*empty*/, 0);
        text = xpp.nextText();
        assertEquals(EXPIRES, text);
        xpp.nextTag();
        checkParserStateNs(xpp, 3, XmlPullParser.END_TAG, 2+extraNs, ALERTCONTROL, "alertcontrol", false, -1);
        xpp.nextTag();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, 1, ENV, "Header", false, -1);
        xpp.nextTag();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, 1, ENV, "Body", false /*empty*/, 0);
        
        xpp.nextTag();
        checkParserStateNs(xpp, 3, XmlPullParser.START_TAG, 2, ALERT, "alert", false /*empty*/, 0);
        xpp.nextTag();
        checkParserStateNs(xpp, 4, XmlPullParser.START_TAG, 2, ALERT, "msg", false /*empty*/, 0);
        text = xpp.nextText();
        assertEquals(MSG, text);
        xpp.nextTag();
        checkParserStateNs(xpp, 3, XmlPullParser.END_TAG, 2, ALERT, "alert", false, -1);
        xpp.nextTag();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, 1, ENV, "Body", false, -1);
        xpp.nextTag();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, 1, ENV, "Envelope", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        
        checkTestSetPrefixSoap2(soapEnvelope);
    }
    
    // run test using parser.require()
    private void checkTestSetPrefixSoap2(String soapEnvelope) throws Exception {
        xpp.setInput(new StringReader(soapEnvelope));
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        
        xpp.require(XmlPullParser.START_DOCUMENT, null, null);
        
        xpp.next(); // essentially moveToContent()
        xpp.require(XmlPullParser.START_TAG, ENV, "Envelope");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ENV, "Header");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ALERTCONTROL, "alertcontrol");
        String mustUderstand = xpp.getAttributeValue(ENV, "mustUnderstand");
        assertEquals("true", mustUderstand);
        String role = xpp.getAttributeValue(ENV, "role");
        assertEquals(ROLE, role);
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ALERTCONTROL, "priority");
        String text = xpp.nextText();
        assertEquals("1", text);
        //Integer.parseInt(text);
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ALERTCONTROL, "expires");
        text = xpp.nextText();
        assertEquals(EXPIRES, text);
        
        xpp.nextTag();
        xpp.require(XmlPullParser.END_TAG, ALERTCONTROL, "alertcontrol");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.END_TAG, ENV, "Header");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ENV, "Body");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ALERT, "alert");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.START_TAG, ALERT, "msg");
        
        text = xpp.nextText();
        assertEquals(MSG, text);
        
        xpp.nextTag();
        xpp.require(XmlPullParser.END_TAG, ALERT, "alert");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.END_TAG, ENV, "Body");
        
        xpp.nextTag();
        xpp.require(XmlPullParser.END_TAG, ENV, "Envelope");
        
        xpp.next();
        xpp.require(XmlPullParser.END_DOCUMENT, null, null);
    }
    
    
    public void testConflictingDefaultNs() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF8");
        
        ser.setPrefix("", "namesp");
        ser.setPrefix("ns1", "namesp1");
        ser.setPrefix("ns2", "namesp2");
        try {
            ser.startTag("", "foo");
            fail("exception was expected when default namespace can not be declared");
        } catch(IllegalStateException ex) {
            //
        }
    }
    
    
    public void testMultipleOverlappingNamespaces() throws Exception {
        XmlSerializer ser = factory.newSerializer();
        
        //<section xmlns='urn:com:books-r-us'>
        //  <!-- 2 -->   <title>Book-Signing Event</title>
        //  <!-- 3 -->   <signing>
        //  <!-- 4 -->     <author title="Mr" name="Vikram Seth" />
        //  <!-- 5 -->     <book title="A Suitable Boy" price="$22.95" />
        //               </signing>
        //             </section>
        
        
        // check namespaces generation with explicit prefixes
        
        //        byte[] binput = ("<foo xmlns='namesp' xmlns:ns1='namesp1' xmlns:ns2='namesp2'>"+
        //                             "<ns1:bar xmlns:ns1='x1' xmlns:ns3='namesp3' xmlns='n1'>"+
        //                             "<ns2:gugu a1='v1' ns2:a2='v2' xml:lang='en' ns1:a3=\"v3\"/>"+
        //                             "<baz xmlns:ns1='y1'></baz>"+
        //                             "</ns1:bar></foo>").getBytes("US-ASCII");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ser.setOutput(baos, "UTF8");
        
        ser.startDocument(null, null);
        ser.setPrefix("", "namesp");
        ser.setPrefix("ns1", "namesp1");
        ser.setPrefix("ns2", "namesp2");
        ser.startTag("namesp", "foo");
        
        ser.setPrefix("ns1", "x1");
        ser.setPrefix("ns3", "namesp3");
        ser.setPrefix("", "namesp1");
        ser.startTag("x1", "bar");
        
        ser.startTag("namesp2", "gugu");
        ser.attribute("", "a1", "v1");
        ser.attribute("namesp2", "a2", "v2" );
        ser.attribute("http://www.w3.org/XML/1998/namespace", "lang", "en");
        ser.attribute("x1", "a3", "v3");
        
        ser.endTag("namesp2", "gugu");
        
        ser.setPrefix("ns1", "y1");
        ser.startTag("namesp1", "baz");
        
        ser.endTag("namesp1", "baz");
        
        ser.endTag("x1", "bar");
        
        ser.endTag("namesp", "foo");
        ser.endDocument();
        
        
        byte[] binput = baos.toByteArray();
        
        //System.out.println(getClass().getName()+"serialized="+new String(binput, "US-ASCII"));
        
        xpp.setInput(new ByteArrayInputStream( binput ), "US-ASCII" );
        assertEquals("US-ASCII", xpp.getInputEncoding());
        
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 3, "namesp", "foo", null, false, 0);
        assertEquals(0, xpp.getNamespaceCount(0));
        assertEquals(3, xpp.getNamespaceCount(1));
        checkNamespace(xpp, 0, null, "namesp", true);
        checkNamespace(xpp, 1, "ns1", "namesp1", true);
        checkNamespace(xpp, 2, "ns2", "namesp2", true);
        
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, "ns1", 6, "x1", "bar", null, false, 0);
        assertEquals(0, xpp.getNamespaceCount(0));
        assertEquals(3, xpp.getNamespaceCount(1));
        assertEquals(6, xpp.getNamespaceCount(2));
        checkNamespace(xpp, 3, "ns1", "x1", true);
        checkNamespace(xpp, 4, "ns3", "namesp3", true);
        checkNamespace(xpp, 5, null, "namesp1", true);
        
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.START_TAG, "ns2", 6, "namesp2", "gugu", null, true, 4);
        assertEquals(6, xpp.getNamespaceCount(2));
        assertEquals(6, xpp.getNamespaceCount(3));
        assertEquals("x1", xpp.getNamespace("ns1"));
        assertEquals("namesp2", xpp.getNamespace("ns2"));
        assertEquals("namesp3", xpp.getNamespace("ns3"));
        checkAttribNs(xpp, 0, null, "", "a1", "v1");
        checkAttribNs(xpp, 1, "ns2", "namesp2", "a2", "v2");
        checkAttribNs(xpp, 2, "xml", "http://www.w3.org/XML/1998/namespace", "lang", "en");
        checkAttribNs(xpp, 3, "ns1", "x1", "a3", "v3");
        
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.END_TAG, "ns2", 6, "namesp2", "gugu", null, false, -1);
        
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.START_TAG, null, 7, "namesp1", "baz", null, xpp.isEmptyElementTag(), 0);
        assertEquals(0, xpp.getNamespaceCount(0));
        assertEquals(3, xpp.getNamespaceCount(1));
        assertEquals(6, xpp.getNamespaceCount(2));
        assertEquals(7, xpp.getNamespaceCount(3));
        checkNamespace(xpp, 6, "ns1", "y1", true);
        assertEquals("y1", xpp.getNamespace("ns1"));
        assertEquals("namesp2", xpp.getNamespace("ns2"));
        assertEquals("namesp3", xpp.getNamespace("ns3"));
        
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.END_TAG, null, 7, "namesp1", "baz", null, false, -1);
        assertEquals("y1", xpp.getNamespace("ns1"));
        assertEquals("namesp2", xpp.getNamespace("ns2"));
        assertEquals("namesp3", xpp.getNamespace("ns3"));
        
        // check that declared namespaces can be accessed for current end tag
        assertEquals(3, xpp.getDepth());
        assertEquals(6, xpp.getNamespaceCount(2));
        assertEquals(7, xpp.getNamespaceCount(3));
        
        // chekc that namespace is accessible by direct addresssing
        assertEquals(null, xpp.getNamespacePrefix(0));
        assertEquals("namesp", xpp.getNamespaceUri(0));
        assertEquals("ns1", xpp.getNamespacePrefix(1));
        assertEquals("namesp1", xpp.getNamespaceUri(1));
        assertEquals("ns1", xpp.getNamespacePrefix(3));
        assertEquals("x1", xpp.getNamespaceUri(3));
        assertEquals("ns1", xpp.getNamespacePrefix(6));
        assertEquals("y1", xpp.getNamespaceUri(6));
        
        
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, "ns1", 6, "x1", "bar", null, false, -1);
        // check that namespace is undelcared
        assertEquals("x1", xpp.getNamespace("ns1"));
        
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 3, "namesp", "foo", null, false, -1);
        
        assertEquals("namesp1", xpp.getNamespace("ns1"));
        assertEquals("namesp2", xpp.getNamespace("ns2"));
        assertEquals(null, xpp.getNamespace("ns3"));
        
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        assertEquals(null, xpp.getNamespace("ns1"));
        assertEquals(null, xpp.getNamespace("ns2"));
        assertEquals(null, xpp.getNamespace("ns3"));
        
        
    }
    
    private void assertXmlEquals(String expectedXml, String actualXml)
        throws Exception
    {
        XmlPullParser expect = factory.newPullParser();
        expect.setInput(new StringReader(expectedXml));
        XmlPullParser actual = factory.newPullParser();
        actual.setInput(new StringReader(actualXml));
        while(true) {
            expect.next();
            actual.next();
            assertXml("inconsistent event type", expect, actual,
                      XmlPullParser.TYPES[ expect.getEventType() ],
                      XmlPullParser.TYPES[ actual.getEventType() ]
                     );
            if(expect.getEventType() == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if(expect.getEventType() == XmlPullParser.START_TAG
                   || expect.getEventType() == XmlPullParser.END_TAG )
            {
                assertXml("tag names", expect, actual,
                          expect.getName(), actual.getName());
                assertXml("tag namespaces", expect, actual,
                          expect.getNamespace(), actual.getNamespace());
                if(expect.getEventType() == XmlPullParser.START_TAG) {
                    // check consisteny of attributes -- allow them to be in any order
                    int expectAttrCount = expect.getAttributeCount();
                    assertXml("attributes count", expect, actual,
                              ""+expectAttrCount, ""+actual.getAttributeCount());
                    for (int i = 0; i < expectAttrCount; i++)
                    {
                        String expectAttrNamespace = expect.getAttributeNamespace(i);
                        String expectAttrName = expect.getAttributeName(i);
                        String expectAttrType = expect.getAttributeType(i);
                        String expectAttrValue = expect.getAttributeValue(i);
                        boolean expectAttrDefault = expect.isAttributeDefault(i);
                        
                        // find this attribute actual position
                        int actualPos = -1;
                        for (int j = 0; j < expectAttrCount; j++)
                        {
                            if(expectAttrNamespace.equals(actual.getAttributeNamespace(j))
                                   && expectAttrName.equals(actual.getAttributeName(j)))
                            {
                                actualPos = j;
                                break;
                            }
                        }
                        String expectN = expectAttrNamespace+":"+expectAttrName;
                        if(actualPos == -1) {
                            System.err.println("expected:\n"+expectedXml
                                                   +"\nactual:\n"+actualXml);
                            fail("could not find expected attribute "+expectN
                                     +" actual parser "+actual.getPositionDescription());
                        }
                        
                        //and compare ...
                        assertXml("attribute "+expectN+" namespace", expect, actual,
                                  expectAttrNamespace, actual.getAttributeNamespace(actualPos) );
                        assertXml("attribute "+expectN+" name", expect, actual,
                                  expectAttrName, actual.getAttributeName(actualPos) );
                        assertXml("attribute "+expectN+" type", expect, actual,
                                  expectAttrType, actual.getAttributeType(actualPos) );
                        assertXml("attribute "+expectN+" value", expect, actual,
                                  expectAttrValue, actual.getAttributeValue(actualPos) );
                        assertXml("attribute "+expectN+" default", expect, actual,
                                  ""+expectAttrDefault, ""+actual.isAttributeDefault(actualPos) );
                    }
                }
            } else if(expect.getEventType() == XmlPullParser.TEXT) {
                assertXml("text content", expect, actual,
                          expect.getText(), actual.getText());
            } else {
                fail("unexpected event type "+expect.getEventType()+" "+expect.getPositionDescription());
            }
            //System.err.print(".");
        }
        //System.err.println("\nOK");
    }
    
    private static void assertXml(String formatted,
                                  XmlPullParser pExpected, XmlPullParser pActual,
                                  String expected, String actual
                                 )
    {
        if((expected != null && !expected.equals(actual))
               || (expected == null && actual != null))
        {
            fail(formatted
                     +" expected:<"+expected+"> but was:<"+actual+">"
                     +" (expecte parser position:"+pExpected.getPositionDescription()
                     +" and actual parser positon:"+pActual.getPositionDescription()
                     
                );
        }
    }
}

