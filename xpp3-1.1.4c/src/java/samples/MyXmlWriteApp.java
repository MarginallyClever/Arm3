/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * An example of an application that uses XmlPull v1 API to write XML.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class MyXmlWriteApp {

    private final static String NAMESPACE = "http://www.megginson.com/ns/exp/poetry";

    public static void main (String args[])
        throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
            System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        XmlSerializer serializer = factory.newSerializer();
        System.out.println("serializer implementation class is "+serializer.getClass());


        boolean addNewLine = false;
        int argPos = 0;
        while(argPos < args.length)
        {
            String arg = args[argPos];
            if("-n".equals(arg) ) {
                addNewLine = true;
            } else if("-i".equals(arg)) {
                // set indentation to number of spaces passed as argument
                String s = args[++argPos];
                int indentSize = Integer.parseInt(s);
                StringBuffer buf = new StringBuffer(indentSize);
                for (int i = 0; i < indentSize; i++)
                {
                    buf.append(' ');
                }
                String indent = buf.toString();
                // this is optional propery
                serializer.setProperty(
                    "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", indent);
            }
            ++argPos;
        }
        // set output
        serializer.setOutput(new PrintWriter( System.out ));

        // first write XML declaration
        serializer.startDocument(null, null);
        // add some empty lines before first start tag
        serializer.ignorableWhitespace("\n\n");

        // if prefix is not set serializer will generate automatically prefix
        // we overwrite this mechanism and manually namespace to be default (empty prefix)
        serializer.setPrefix("", NAMESPACE);

        serializer.startTag(NAMESPACE, "poem");
        if(addNewLine) serializer.text("\n");

        serializer.startTag(NAMESPACE, "title");
        serializer.text("Roses are Red");
        serializer.endTag(NAMESPACE, "title");
        if(addNewLine) serializer.text("\n");

        serializer.startTag(NAMESPACE, "l")
            .text("Roses are red,")
            .endTag(NAMESPACE, "l");
        if(addNewLine) serializer.text("\n");

        //multiple operations can be also combined to get shorter syntax
        serializer.startTag(NAMESPACE, "l").text("Violets are blue;").endTag(NAMESPACE, "l");
        if(addNewLine) serializer.text("\n");

        // or writing can be delegate to specialized functions
        writeLine(serializer, "Sugar is sweet,", addNewLine);
        writeLine(serializer, "And I love you.,", addNewLine);

        serializer.endTag(NAMESPACE, "poem");


        // this will ensure that output is flushed and prevent from writing to serializer
        serializer.endDocument();


    }

    private static void writeLine(XmlSerializer serializer, String line, boolean addNewLine)
        throws IOException {
        serializer.startTag(NAMESPACE, "l");
        serializer.text(line);
        serializer.endTag(NAMESPACE, "l");
        if(addNewLine) serializer.text("\n");
    }


}
