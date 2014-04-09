/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import java.io.*;

import junit.framework.TestSuite;

import org.xmlpull.v1.*;

public class TestXmlSimple extends XmlTestCase {

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestXmlSimple.class));
    }

    public TestXmlSimple(String name) {
        super(name);
    }

    public void testSimple()
        throws IOException, XmlPullParserException
    {
        testXml("simple.xml");
    }

    public void testSimple2()
        throws IOException, XmlPullParserException
    {
        testXml("simple2.xml");
    }
}

