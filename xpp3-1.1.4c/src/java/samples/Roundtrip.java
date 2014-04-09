/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)


//package org.xmlpull.v1.samples;

import java.io.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.xmlpull.v1.XmlSerializer;

/**
 * This sample demonstrates how to roundtrip XML document
 * (roundtrip is not exact but infoset level)
 */

public class Roundtrip {
    
    private final static String PROPERTY_XMLDECL_STANDALONE =
        "http://xmlpull.org/v1/doc/features.html#xmldecl-standalone";
    private final static String PROPERTY_SERIALIZER_INDENTATION =
        "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";
    
    private XmlPullParser parser;
    private XmlSerializer serializer;
    
    private Roundtrip (XmlPullParser parser, XmlSerializer serializer) {
        this.parser = parser;
        this.serializer = serializer;
    }
    
    private void writeStartTag () throws XmlPullParserException, IOException {
        //check for case when feature xml roundtrip is supported
        //if (parser.getFeature (FEATURE_XML_ROUNDTRIP)) {
        if (!parser.getFeature (XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            for (int i = parser.getNamespaceCount (parser.getDepth ()-1);
                     i <= parser.getNamespaceCount (parser.getDepth ())-1; i++) {
                serializer.setPrefix
                    (parser.getNamespacePrefix (i),
                     parser.getNamespaceUri (i));
            }
        }
        serializer.startTag(parser.getNamespace (), parser.getName ());
        
        for (int i = 0; i < parser.getAttributeCount (); i++) {
            serializer.attribute
                (parser.getAttributeNamespace (i),
                 parser.getAttributeName (i),
                 parser.getAttributeValue (i));
        }
        //serializer.closeStartTag();
    }
    
    
    private void writeToken (int eventType) throws XmlPullParserException, IOException {
        switch (eventType) {
            case XmlPullParser.START_DOCUMENT:
                //use Boolean.TRUE to make it standalone
                Boolean standalone = (Boolean) parser.getProperty(PROPERTY_XMLDECL_STANDALONE);
                serializer.startDocument(parser.getInputEncoding(), standalone);
                break;
                
            case XmlPullParser.END_DOCUMENT:
                serializer.endDocument();
                break;
                
            case XmlPullParser.START_TAG:
                writeStartTag ();
                break;
                
            case XmlPullParser.END_TAG:
                serializer.endTag(parser.getNamespace (), parser.getName ());
                break;
                
            case XmlPullParser.IGNORABLE_WHITESPACE:
                //comment it to remove ignorable whtespaces from XML infoset
                String s = parser.getText ();
                serializer.ignorableWhitespace (s);
                break;
                
            case XmlPullParser.TEXT:
                serializer.text (parser.getText ());
                break;
                
            case XmlPullParser.ENTITY_REF:
                serializer.entityRef (parser.getName ());
                break;
                
            case XmlPullParser.CDSECT:
                serializer.cdsect( parser.getText () );
                break;
                
            case XmlPullParser.PROCESSING_INSTRUCTION:
                serializer.processingInstruction( parser.getText ());
                break;
                
            case XmlPullParser.COMMENT:
                serializer.comment (parser.getText ());
                break;
                
            case XmlPullParser.DOCDECL:
                serializer.docdecl (parser.getText ());
                break;
        }
    }
    
    private void roundTrip() throws XmlPullParserException, IOException {
        parser.nextToken(); // read first token
        writeToken (XmlPullParser.START_DOCUMENT);  // write optional XMLDecl if present
        while (parser.getEventType () != XmlPullParser.END_DOCUMENT) {
            writeToken ( parser.getEventType () );
            parser.nextToken ();
        }
        writeToken (XmlPullParser.END_DOCUMENT);
    }

    private static void roundTrip(Reader reader, Writer writer, String indent)
        throws XmlPullParserException, IOException
    {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(reader);
        XmlSerializer serializer = factory.newSerializer();
        serializer.setOutput( writer );
        if(indent != null) {
            serializer.setProperty(PROPERTY_SERIALIZER_INDENTATION, indent);
        }
        (new Roundtrip(pp, serializer)).roundTrip();
        
    }

    
//    public static void main(String[] args) throws Exception {
//        String XML = "<test><foo>fdf</foo></test>";
//        Reader r = new StringReader(XML);
//        Writer w = new StringWriter();
//        roundTrip(r, w, "  ");
//        System.out.println("indented XML="+w);
//    }
    
    private static void roundTrip(XmlPullParserFactory factory, String loc)
        throws XmlPullParserException, IOException
    {
        roundTrip(factory, loc, null);
    }
    
    private static void roundTrip(XmlPullParserFactory factory, String loc, String indent)
        throws XmlPullParserException, IOException
    {
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(new java.net.URL(loc).openStream(), null);
        
        XmlSerializer serializer = factory.newSerializer();
        serializer.setOutput( System.out, null);
        if(indent != null) {
            serializer.setProperty(PROPERTY_SERIALIZER_INDENTATION, indent);
        }
        (new Roundtrip(pp, serializer)).roundTrip();
    }
    
    public static void main(String[] args) throws Exception {
      //for (int i = 0; i < args.length; i++)
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        for (int i = 0; i < 1; i++)
        {
            roundTrip(factory, args[i], "  ");
        }
    }
    
    
}



