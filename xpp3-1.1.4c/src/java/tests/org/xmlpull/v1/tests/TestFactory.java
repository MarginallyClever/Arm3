/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Simple test ot verify pull parser factory
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestFactory extends UtilTestCase {

    public TestFactory(String name) {
        super(name);
    }

    public void testFactory() throws Exception {
        XmlPullParserFactory factory = factoryNewInstance();
        //System.out.println("factory = "+factory);
        XmlPullParser xpp = factory.newPullParser();
        PackageTests.addNote("* default parser "+xpp.getClass()+"\n");
        assertEquals(false, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        //factory.setNamespaceAware(false);
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

        //assertEquals(false, factory.isNamespaceAware());
        assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        xpp = factory.newPullParser();
        assertEquals(false, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        //factory.setNamespaceAware(true);
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        //assertEquals(true, factory.isNamespaceAware());
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        PackageTests.addNote("* namespace enabled parser "+xpp.getClass()+"\n");

    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestFactory.class));
    }
}

