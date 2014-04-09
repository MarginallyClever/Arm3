import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlCompare {
    protected static final String FEATURE_XML_ROUNDTRIP =
        "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

    public static void main (String args[])
        throws XmlPullParserException, IOException
    {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
            System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser pp = factory.newPullParser();
        try {
         pp.setFeature(FEATURE_XML_ROUNDTRIP, false);
        } catch(XmlPullParserException ex) {
            throw new RuntimeException("could nto disable roundtrip feature: "+ex);
        }
        System.out.println("parser implementation class is "+pp.getClass());

        //pp.setInput(new StringReader(SAMPLE_XML));
        final String FILE = "fcked_up2.xml";
        pp.setInput(new FileReader(FILE));
        //final String NS_URI = "http://whappdev1/bscwebservices/";

        String text = getNextElementText(pp);
        //System.out.println("getNextElementText="+text);
        //Writer w = new FileWriter("c:/Forge/homepage/xmlpull/f3.xml");
        //w.write(text);
        //w.close();
        // now just parse input
        //pp.nextTag();
        //System.out.println("current tag namespace="+pp.getNamespace()+" name="+pp.getName());
        //pp.require(XmlPullParser.START_TAG, NS_URI, "string");
        //System.out.println("text='"+pp.nextText()+"'");
        // note nextText() moved parser to END_TAG
        //System.out.println("current tag namespace="+pp.getNamespace()+" name="+pp.getName());
        //pp.require(XmlPullParser.END_TAG, NS_URI, "string");


        compareXmlTwoFiles(new FileReader(FILE),
                           new StringReader(text));
    }

    /**
     * Returns the next Element in the stream (including all sub elements) as a String
     * @param parser
     * @return
     * @throws org.xmlpull.v1.XmlPullParserException
     * @throws java.io.IOException
     */
    public static String getNextElementText(XmlPullParser parser) throws XmlPullParserException, IOException {

        if(parser.getFeature(FEATURE_XML_ROUNDTRIP) == false) {
            throw new RuntimeException("roundtrip feature must be enabled to get tag content");
        }

        int level = 1;
        StringBuffer buf = new StringBuffer();
        parser.nextTag();
        buf.append(parser.getText());
        //int [] holderForStartAndLength = new int[2];
        while (level > 0) {
            int event = parser.next();
            String text = parser.getText();
            if (event == XmlPullParser.START_TAG) {
                buf.append(text);
                if(parser.isEmptyElementTag()) {
                    parser.next(); //skip empty tag
                } else {
                    level++;
                }
            } else if (event == XmlPullParser.END_TAG) {
                // We need to handle <empty/> tags... ;) returns both START_TAG=<empty/> and END_TAG=<empty/>
                //if (!tmp.endsWith("/>")) buf.append(tmp);
                buf.append(text);
                level--;
                //} else if (!parser.isWhitespace()) {
            } else if (text.length() > 0) {
                StringBuffer escapedText = new StringBuffer(text.length());
                for (int i = 0; i < text.length(); i++)
                {
                    char ch = text.charAt(i);
                    if(ch == '<') {
                        escapedText.append("&lt;");
                    } else if(ch == '&') {
                        escapedText.append("&amp;");
                    } else if(ch == '"') {
                        escapedText.append("&quot;");
                    } else if(ch == '\'') {
                        escapedText.append("&apos;");
                    } else {
                        escapedText.append(ch);
                    }

                }
                buf.append(escapedText);
            }
        }
        return buf.toString();
    }


    static void compareXmlTwoFiles(Reader rr, Reader rq) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
            //System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
            "org.xmlpull.mxp1.MXParserFactory", null);
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser r = factory.newPullParser();
        r.setInput(rr);

        XmlPullParser q = factory.newPullParser();
        q.setInput(rq);

        while(true) {
            r.next();
            q.next();
            if(r.getEventType() != q.getEventType()) {
                throw new RuntimeException("inconsistent events");
            }
            if(r.getEventType() == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if(r.getEventType() == XmlPullParser.START_TAG || r.getEventType() == XmlPullParser.END_TAG ) {
                String rName = r.getName();
                String qName =  q.getName();
                if(!rName.equals(qName)) {
                    throw new RuntimeException("element names mismatch");
                }
                if(r.getEventType() == XmlPullParser.START_TAG) {
                }
            } else if(r.getEventType() == XmlPullParser.TEXT) {
                String rText = r.getText();
                String qText =  q.getText();
                if(!rText.equals(qText)) {
                    throw new RuntimeException("text content mismatch '"+rText+"' and '"+qText+"'");
                }

            } else {
                throw new RuntimeException("unknown event type "+r.getEventType()
                                               +r.getPositionDescription());
            }
            System.err.print(".");
        }
        System.err.println("\nOK");

    }

}


