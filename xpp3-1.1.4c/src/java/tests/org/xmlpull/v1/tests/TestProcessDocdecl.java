/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
//import junit.framework.TestCase;
import junit.framework.TestSuite;

//import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test FEATURE_PROCESS_DOCDECL  (when supported)
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestProcessDocdecl extends UtilTestCase {
    private XmlPullParserFactory factory;

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestProcessDocdecl.class));
    }


    public TestProcessDocdecl(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        //assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        //assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
        assertEquals(false, factory.isNamespaceAware());
        assertEquals(false, factory.isValidating());
        //System.out.println("factory="+factory);
    }

    private XmlPullParser newParser(boolean useNamespaces, boolean useValidation)
        throws XmlPullParserException
    {
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, useNamespaces);
        assertEquals(useNamespaces, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        try {
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
        } catch(XmlPullParserException ex) {
            return null;
        }
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL));
        try {
            xpp.setFeature(XmlPullParser.FEATURE_VALIDATION, useValidation);
        } catch(XmlPullParserException ex) {
            return null;
        }
        assertEquals(useValidation, xpp.getFeature(XmlPullParser.FEATURE_VALIDATION));
        return xpp;
    }

    protected void tearDown() {
    }

    public void testSimpleEntity() throws Exception {
        testSimpleEntity(false, false);
        testSimpleEntity(true, false);
        testSimpleEntity(false, true);
        testSimpleEntity(true, true);
    }

    public void testAttributeNormalization() throws Exception {
        //TODO check that Xerces NON-validating normalization of NMTOKEN attributes
        //http://www.w3.org/TR/REC-xml#AVNormalize

    }

    public void testSimpleEntity(boolean useNamespaces, boolean useValidation) throws Exception {
        XmlPullParser xpp = newParser(useNamespaces, useValidation);
        if(xpp == null) return;

        //http://www.w3.org/TR/REC-xml#intern-replacement
        final String XML_SIMPLE_ENT_PROLOG =
            "<?xml version='1.0'?>\n"+
            "<!DOCTYPE test [\n"+
            "<!ENTITY % YN '\"Yes\"' >\n"+
            //"<!ENTITY WhatHeSaid \"He said %YN;\" >\n"+
            "<!ELEMENT test (#PCDATA) >\n"+
            //          "<!ENTITY % pub    \"&#xc9;ditions Gallimard\" >\n"+
            //          "<!ENTITY   rights \"All rights reserved\" >\n"+
            //          "<!ENTITY   book   \"La Peste: Albert Camus,\n"+
            "<!ENTITY   pub    \"&#xc9;ditions Gallimard\" >\n"+
            "<!ENTITY   rights \"All rights reserved\" >\n"+
            "<!ENTITY   book   \"La Peste: Albert Camus,\n"+
            "&#xA9; 1947 &pub;. &rights;\" >\n"+
            "]>\n";
        final String XML_SIMPLE_ENT = XML_SIMPLE_ENT_PROLOG+
            "<test>Publication: &book; </test>\n";

        //        final String PUB_ENTITY_INTERNAL_REPLACEMENT =
        //            "La Peste: Albert Camus,\n"+
        //            "© 1947 Éditions Gallimard. &rights;";
        final String PUB_ENTITY_INTERNAL_REPLACEMENT =
            "La Peste: Albert Camus,\n"+
            "&pub;. &rights;";


        final String PUB_ENTITY_REPLACEMENT =
            "La Peste: Albert Camus,\n"+
            "© 1947 Éditions Gallimard. All rights reserved";

        //next
        xpp.setInput(new StringReader( XML_SIMPLE_ENT ));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "test", null, false/*empty*/, 0);
        xpp.next();
        String expectedContent = "Publication: "+PUB_ENTITY_REPLACEMENT+" ";
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, expectedContent, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "test", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);

        //nextToken
    }

    public void testEntityWithMarkup() throws Exception {
        testEntityWithMarkup(false, false);
        testEntityWithMarkup(true, false);
        testEntityWithMarkup(true, true);
        testEntityWithMarkup(false, true);
    }

    public void testEntityWithMarkup(boolean useNamespaces, boolean useValidation)
        throws Exception
    {
        XmlPullParser xpp = newParser(useNamespaces, useValidation);
        if(xpp == null) return;

        //http://www.w3.org/TR/REC-xml#sec-entexpand
        // derived from
        final String XML_REPLACE_ENT =
            "<?xml version='1.0'?>\n"+
            "<!DOCTYPE test [\n"+
            "<!ELEMENT test (#PCDATA|p)* >\n"+
            "<!ELEMENT p (#PCDATA) >\n"+
            "<!ENTITY example \"<p>An ampersand (&#38;#38;) may be escaped\n"+
            "numerically (&#38;#38;#38;) or with a general entity\n"+
            "(&amp;amp;).</p>\" >\n"+
            "]>\n"+
            "<test>&example; </test>     ";

        final String EXAMPLE_ENTITY_INTERNAL_REPLACEMENT =
            "<p>An ampersand (&#38;) may be escaped\n"+
            "numerically (&#38;#38;) or with a general entity\n"+
            "(&amp;amp;).</p>\n";

        final String EXAMPLE_ENTITY_TEXT_EVENT =
            "An ampersand (&) may be escaped\n"+
            "numerically (&#38;) or with a general entity\n"+
            "(&amp;).";

        //next
        xpp.setInput(new StringReader( XML_REPLACE_ENT ));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "test", null, false/*empty*/, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 0, "", "p", null, false/*empty*/, 0);
        xpp.next();
        String expectedContent = EXAMPLE_ENTITY_TEXT_EVENT;
        checkParserStateNs(xpp, 2, XmlPullParser.TEXT, null, 0, null, null, expectedContent, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 0, "", "p", null, false/*empty*/, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, " ", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "test", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }

    public void testTricky() throws Exception {
        testTricky(false, false);
        testTricky(true, false);
        testTricky(true, true);
        testTricky(false, true);
    }

    public void testTricky(boolean useNamespaces, boolean useValidation)
        throws Exception
    {
        XmlPullParser xpp = newParser(useNamespaces, useValidation);
        if(xpp == null) return;

        // derived from
        final String XML_TRICKY =
            "<?xml version='1.0'?>\n"+
            "<!DOCTYPE test [\n"+
            "<!ELEMENT test (#PCDATA) >\n"+
            "<!ENTITY % xx '&#37;zz;'>\n"+
            "<!ENTITY % zz '&#60;!ENTITY tricky \"error-prone\" >' >\n"+
            "%xx;\n"+
            "]>\n"+
            "<test>This sample shows a &tricky; method.</test>\n";


        final String EXPECTED_XML_TRICKY =
            "This sample shows a error-prone method.";

        //next
        xpp.setInput(new StringReader( XML_TRICKY ));
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "test", null, false/*empty*/, 0);
        xpp.next();
        String expectedContent = EXPECTED_XML_TRICKY;
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, expectedContent, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "test", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);

        //TODO nextToken()

    }

}

