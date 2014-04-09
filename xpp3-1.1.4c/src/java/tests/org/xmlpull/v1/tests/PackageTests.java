/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

// -server  -Dorg.xmlpull.v1.XmlPullParserFactory=org.xmlpull.v1.xni2xmlpull1.X2ParserFactory
//  -Dorg.xmlpull.v1.XmlPullParserFactory=org.xmlpull.v1.xni2xmlpull1.X2ParserFactory
//  -Dorg.xmlpull.v1.XmlPullParserFactory=org.xmlpull.mxp1.MXParserFactory
//  -Dorg.xmlpull.v1.tests=org.xmlpull.mxp1.MXParser,org.xmlpull.mxp1_serializer.MXSerializer

/**
 * TODO: add tests for mixed next() with nextToken()
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class PackageTests extends TestRunner {
    private static boolean runAll;
    
    public static boolean runnigAllTests() { return runAll; }
    //public static void setRunnigAllTests(boolean enable) { runAll = enable; }
    
    public PackageTests() {
        super();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite("XmlPull V1 API TESTS");
        
        suite.addTestSuite(TestFactory.class);
        suite.addTestSuite(TestSimple.class);
        suite.addTestSuite(TestSimpleWithNs.class);
        suite.addTestSuite(TestSerialize.class);
        suite.addTestSuite(TestSerializeWithNs.class);
        suite.addTestSuite(TestSimpleToken.class);
        suite.addTestSuite(TestAttributes.class);
        suite.addTestSuite(TestEolNormalization.class);
        suite.addTestSuite(TestEntityReplacement.class);
        suite.addTestSuite(TestCdsect.class);
        suite.addTestSuite(TestEvent.class);
        suite.addTestSuite(TestToken.class);
        suite.addTestSuite(TestMisc.class);
        suite.addTestSuite(TestSetInput.class);
        suite.addTestSuite(TestSimpleProcessDocdecl.class);
        suite.addTestSuite(TestSimpleValidation.class);
        suite.addTestSuite(TestProcessDocdecl.class);
        
        // finally run tests based on XML input files
        suite.addTestSuite(TestBootstrapXmlTests.class);
        suite.addTestSuite(TestXmlSimple.class);
        suite.addTestSuite(TestXmlTypical.class);
        suite.addTestSuite(TestXmlCdsect.class);
        
        
        return suite;
    }
    
    public synchronized void tick() {
        //if(resultPrinter != null) {
        //    resultPrinter.startTest(null);
        //} else {
        System.err.print(".");
        //}
    }
    
    //    public synchronized void startTest(Test test) {
    //        writer()
    //      System.err.print(".");
    //        //if (fColumn++ >= 40) {
    //        //      writer().println();
    //        //      fColumn= 0;
    //        //}
    //    }
    
    private static StringBuffer notes = new StringBuffer();
    public static void addNote(String note) {
        if(PackageTests.runnigAllTests() == false) {
            System.out.print(note);
            System.out.flush();
        }
        notes.append(note);
    }
    
    
    public void runPackageTests(String testFactoryName) {
        //writer()
        System.err.println("Executing XmlPull API tests"
                               +(testFactoryName != null ? " for '"+testFactoryName+"'" : ""));
        notes.setLength(0);
        XmlPullParserFactory f = null;
        try {
            f = UtilTestCase.factoryNewInstance();
            addNote("* factory "+f.getClass()+"\n");//+" created from property "
            //+System.getProperty(XmlPullParserFactory.PROPERTY_NAME)+"\n");
        } catch (Exception ex) {
            System.err.println(
                "ERROR: tests aborted - could not create instance of XmlPullParserFactory:");
            ex.printStackTrace();
            System.exit(1);
        }
        XmlPullParser parser = null;
        try {
            parser=f.newPullParser();
        } catch (Exception ex) {
            System.err.println(
                "ERROR: tests aborted - could not create instance of XmlPullParser from factory "
                    +f.getClass());
            ex.printStackTrace();
            System.exit(2);
        }
        
        XmlSerializer serializer = null;
        try {
            serializer=f.newSerializer();
        } catch (Exception ex) {
            System.err.println(
                "ERROR: tests aborted - could not create instance of XmlSerializer from factory "
                    +f.getClass());
            ex.printStackTrace();
            System.exit(3);
        }
        String name = getClass().getName();
        name = name.substring(name.lastIndexOf('.')+1);
        System.out.println(name+" uses factory="+f.getClass().getName()
                               +" parser="+parser.getClass().getName()
                               +" serializer="+serializer.getClass().getName()
                          );
        
        // now run all tests ...
        //junit.textui.TestRunner.run(suite());
        TestRunner aTestRunner= new TestRunner();
        try {
            TestResult r = doRun(suite(), false);
            if (!r.wasSuccessful())
                System.exit(-1);
            //System.exit(0);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(-2);
        }
        
        if(notes.length() > 0) {
            //writer().
            System.err.println("Test results "
                                   +(testFactoryName != null ? "for '"+testFactoryName+"'" : "")
                                   +"\n"+notes+"\n");
        }
    }
    
    public void printFinalReport() {
        //writer()
        System.err.println("\nAll tests were passed.");
    }
    
    public static void main (String[] args) {
        final PackageTests driver = new PackageTests();
        final ResultPrinter resultPrinter = new ResultPrinter(System.err);
        driver.setPrinter(resultPrinter);
        
        final String listOfTests = System.getProperty("org.xmlpull.v1.tests");
        final String FACTORY_PROPERTY = XmlPullParserFactory.PROPERTY_NAME;
        final String DEFAULT_FACTORY_PROPERTY = System.getProperty(FACTORY_PROPERTY);
        runAll = true;
        if(listOfTests != null) {
            int pos = 0;
            while (pos < listOfTests.length()) {
                int cut = listOfTests.indexOf(':', pos);
                if (cut == -1) cut = listOfTests.length();
                String testFactoryName = listOfTests.substring(pos, cut);
                if("DEFAULT".equals(testFactoryName)) {
                    if(DEFAULT_FACTORY_PROPERTY != null) {
                        System.setProperty(FACTORY_PROPERTY, DEFAULT_FACTORY_PROPERTY);
                    } else {
                        // overcoming limitation of System.setProperty not allowing
                        //  null or empty values (who knows how to unset property ?!)
                        System.setProperty(FACTORY_PROPERTY, "DEFAULT");
                    }
                } else {
                    System.setProperty(FACTORY_PROPERTY, testFactoryName);
                }
                driver.runPackageTests(testFactoryName);
                pos = cut + 1;
            }
            driver.printFinalReport();
            
        } else {
            driver.runPackageTests(null);
        }
        System.exit(0);
    }
    
}

