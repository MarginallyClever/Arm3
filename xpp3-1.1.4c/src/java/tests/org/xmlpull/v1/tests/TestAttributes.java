/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test attribute uniqueness is ensured.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestAttributes extends UtilTestCase {
    private XmlPullParserFactory factory;

    public TestAttributes(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
    }

    protected void tearDown() {
    }

    public void testAttribs() throws IOException, XmlPullParserException
    {
        final String XML_ATTRS =
            "<event xmlns:xsi='http://www.w3.org/1999/XMLSchema/instance' encodingStyle=\"test\">"+
            "<type>my-event</type>"+
            "<handback xsi:type='ns2:string' xmlns:ns2='http://www.w3.org/1999/XMLSchema' xsi:null='1'/>"+
            "</event>";

        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(XML_ATTRS));

        assertEquals(XmlPullParser.START_TAG, pp.next());
        assertEquals("event", pp.getName());
        assertEquals(XmlPullParser.START_TAG, pp.next());
        assertEquals("type", pp.getName());
        assertEquals(XmlPullParser.TEXT, pp.next());
        assertEquals("my-event", pp.getText());
        assertEquals(pp.next(), XmlPullParser.END_TAG);
        assertEquals("type", pp.getName());
        assertEquals(XmlPullParser.START_TAG, pp.next());
        assertEquals("handback", pp.getName());
        //assertEquals(XmlPullParser.CONTENT, pp.next());
        //assertEquals("", pp.readContent());

        String xsiNull = pp.getAttributeValue(
            "http://www.w3.org/1999/XMLSchema/instance", "null");
        assertEquals("1", xsiNull);

        String xsiType = pp.getAttributeValue(
            "http://www.w3.org/1999/XMLSchema/instance", "type");
        assertEquals("ns2:string", xsiType);


        String typeName = getQNameLocal(xsiType);
        assertEquals("string", typeName);
        String typeNS = getQNameUri(pp, xsiType);
        assertEquals("http://www.w3.org/1999/XMLSchema", typeNS);

        assertEquals(pp.next(), XmlPullParser.END_TAG);
        assertEquals("handback", pp.getName());
        assertEquals(pp.next(), XmlPullParser.END_TAG);
        assertEquals("event", pp.getName());

        assertEquals(pp.next(), XmlPullParser.END_DOCUMENT);

    }

    private String getQNameLocal(String qname) {
        if(qname == null) return null;
        int pos = qname.indexOf(':');
        return qname.substring(pos + 1);
    }

    private String getQNameUri(XmlPullParser pp, String qname) throws XmlPullParserException {
        if(qname == null) return null;
        int pos = qname.indexOf(':');
        if(pos == -1) throw new XmlPullParserException(
                "qname des not have prefix");
        String prefix = qname.substring(0, pos);
        return pp.getNamespace(prefix);
    }

    public void testAttribUniq() throws IOException, XmlPullParserException
    {

        final String attribsOk =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='c' n:b='d' n:x='e'"+
            "/>\n"+
            "";

        final String duplicateAttribs =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='a' n:b='b' a='x'"+
            "/>\n"+
            "";

        final String duplicateNsAttribs =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'"+
            " a='a' b='b' m:a='a' n:b='b' n:a='a'"+
            "/>\n"+
            "";

        final String duplicateXmlns =
            "<m:test xmlns:m='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'"+
            ""+
            "/>\n"+
            "";

        final String duplicateAttribXmlnsDefault =
            "<m:test xmlns='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'"+
            " a='a' b='b' m:b='b' m:a='x'"+
            "/>\n"+
            "";

        XmlPullParser pp = factory.newPullParser();
        parseOneElement(pp, attribsOk, false);
        assertEquals("a", pp.getAttributeValue(null, "a"));
        assertEquals("b", pp.getAttributeValue(null, "b"));
        assertEquals("c", pp.getAttributeValue(null, "m:a"));
        assertEquals("d", pp.getAttributeValue(null, "n:b"));
        assertEquals("e", pp.getAttributeValue(null, "n:x"));

        parseOneElement(pp, attribsOk, true);

        assertEquals("a", pp.getAttributeValue("","a"));
        assertEquals("b", pp.getAttributeValue("","b"));
        assertEquals(null, pp.getAttributeValue("", "m:a"));
        assertEquals(null, pp.getAttributeValue("", "n:b"));
        assertEquals(null, pp.getAttributeValue("", "n:x"));

        assertEquals("c", pp.getAttributeValue("Some-Namespace-URI", "a"));
        assertEquals("d", pp.getAttributeValue("Some-Namespace-URI", "b"));
        assertEquals("e", pp.getAttributeValue("Some-Namespace-URI", "x"));

        parseOneElement(pp, duplicateNsAttribs, false);
        parseOneElement(pp, duplicateAttribXmlnsDefault, false);
        parseOneElement(pp, duplicateAttribXmlnsDefault, true);

        Exception ex;

        ex = null;
        try {
            parseOneElement(pp, duplicateAttribs, false);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateAttribs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateXmlns, false);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateXmlns, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        ex = null;
        try {
            parseOneElement(pp, duplicateNsAttribs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

        final String declaringDefaultEmptyNs  =
            "<m:test xmlns='' xmlns:m='uri'/>";

        // allowed when namespaces disabled
        parseOneElement(pp, declaringDefaultEmptyNs, false);

        // allowed to redeclare defaut anemspace to empty string, see:
        //   http://www.w3.org/TR/1999/REC-xml-names-19990114/#ns-decl
        parseOneElement(pp, declaringDefaultEmptyNs, true);

        final String declaringPrefixedEmptyNs  =
            "<m:test xmlns:m='' />";

        // allowed when namespaces disabled
        parseOneElement(pp, declaringPrefixedEmptyNs, false);

        // otherwise it is error to declare '' for non-default NS as described in
        //   http://www.w3.org/TR/1999/REC-xml-names-19990114/#ns-decl
        ex = null;
        try {
            parseOneElement(pp, declaringPrefixedEmptyNs, true);
        } catch(XmlPullParserException rex) {
            ex = rex;
        }
        assertNotNull(ex);

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

    public void testAttribValueNormalization() throws IOException, XmlPullParserException
    {
        final String XML_ATTRS =
            "<javadoc packagenames=\"${packages}\""+
            " bottom=\"&lt;table width='80%%'&gt;&lt;tr&gt;&lt;td width='50%%'&gt;&lt;p "+
            " align='center'&gt;&lt;a href='http://www.xmlpull.org/'&gt;\" "+
            "/>";

        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new StringReader(XML_ATTRS));

        assertEquals(XmlPullParser.START_TAG, pp.next());
        assertEquals("javadoc", pp.getName());
        assertEquals("${packages}", pp.getAttributeValue(0));
        assertEquals("${packages}", pp.getAttributeValue("", "packagenames"));
        assertEquals(
                "<table width='80%%'><tr><td width='50%%'><p  align='center'><a href='http://www.xmlpull.org/'>",
                pp.getAttributeValue("", "bottom")
        );

        //System.out.println(pp.getAttributeValue("", "bottom"));
    }
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestAttributes.class));
    }

}

