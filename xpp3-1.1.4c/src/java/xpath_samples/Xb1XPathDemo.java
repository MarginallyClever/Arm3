/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Xb1XPathDemo.java,v 1.3 2006/04/22 22:51:26 aslom Exp $
 */

import java.io.StringReader;
import java.util.Iterator;
import org.xmlpull.v1.builder.XmlBuilderException;
import org.xmlpull.v1.builder.XmlDocument;
import org.xmlpull.v1.builder.XmlElement;
import org.xmlpull.v1.builder.XmlInfosetBuilder;
import org.xmlpull.v1.builder.xpath.Xb1XPath;
import org.xmlpull.v1.builder.xpath.jaxen.XPathSyntaxException;

/**
 * Simpe demos of XPath suuprt in XB1.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 *
 * @version $Revision: 1.3 $
 */
public class Xb1XPathDemo
{
    public static void main(String[] args)
    {
        String location = "<elem attr='value'><sub-elem>Text</sub-elem></elem>";
        String xpathExpr = "//";
        if ( args.length != 2 ) {
            System.err.println("usage: "+Xb1XPathDemo.class.getName()+" <document url or XML content> <xpath expr>");
            //System.exit( 1 );
        }
        if(args.length > 0) {
            location = args[0];
        }
        if(args.length > 1) {
            xpathExpr = args[1];
        }
        int pos = location.indexOf("://");
        boolean useUrl =  pos >= 0 && pos < 6; //simple heuristic to find "http://" and similiar
        try {
            XmlInfosetBuilder builder = XmlInfosetBuilder.newInstance();
            
            System.out.println("Evaluating XPath '"+xpathExpr+"' against '" + location
                                   +"'"+(useUrl ? " URL" : "") );
            
            XmlDocument doc = useUrl ? builder.parseLocation( location )
                : builder.parseReader(new StringReader( location ) );
            
            Xb1XPath xpath = new Xb1XPath( xpathExpr );
            
            Iterator resultsIter = xpath.selectNodes( doc ).iterator();
            
            //XmlElement root = doc.getDocumentElement();
            //Iterator resultsIter = xpath.selectNodes( root ).iterator();
            
            System.out.println("Results:" );
            while ( resultsIter.hasNext() ) {
                Object infosetItem = resultsIter.next();
                System.out.println("----------------------------------"); //+ infosetItem.getClass());
                System.out.println(builder.serializeToString(infosetItem)); //what about attributes, namespaces????
            }
            System.out.println("----------------------------------");
        } catch (XPathSyntaxException e) {
            System.err.println( e.getMultilineMessage() );
        } catch (XmlBuilderException e) {
            e.printStackTrace();
        }
    }
}


/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (c) 2002-2004 The Trustees of Indiana University.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1) All redistributions of source code must retain the above
 *    copyright notice, the list of authors in the original source
 *    code, this list of conditions and the disclaimer listed in this
 *    license;
 *
 * 2) All redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the disclaimer
 *    listed in this license in the documentation and/or other
 *    materials provided with the distribution;
 *
 * 3) Any documentation included with all redistributions must include
 *    the following acknowledgement:
 *
 *      "This product includes software developed by the Indiana
 *      University Extreme! Lab.  For further information please visit
 *      http://www.extreme.indiana.edu/"
 *
 *    Alternatively, this acknowledgment may appear in the software
 *    itself, and wherever such third-party acknowledgments normally
 *    appear.
 *
 * 4) The name "Indiana University" or "Indiana University
 *    Extreme! Lab" shall not be used to endorse or promote
 *    products derived from this software without prior written
 *    permission from Indiana University.  For written permission,
 *    please contact http://www.extreme.indiana.edu/.
 *
 * 5) Products derived from this software may not use "Indiana
 *    University" name nor may "Indiana University" appear in their name,
 *    without prior written permission of the Indiana University.
 *
 * Indiana University provides no reassurances that the source code
 * provided does not infringe the patent or any other intellectual
 * property rights of any other entity.  Indiana University disclaims any
 * liability to any recipient for claims brought by any other entity
 * based on infringement of intellectual property rights or otherwise.
 *
 * LICENSEE UNDERSTANDS THAT SOFTWARE IS PROVIDED "AS IS" FOR WHICH
 * NO WARRANTIES AS TO CAPABILITIES OR ACCURACY ARE MADE. INDIANA
 * UNIVERSITY GIVES NO WARRANTIES AND MAKES NO REPRESENTATION THAT
 * SOFTWARE IS FREE OF INFRINGEMENT OF THIRD PARTY PATENT, COPYRIGHT, OR
 * OTHER PROPRIETARY RIGHTS.  INDIANA UNIVERSITY MAKES NO WARRANTIES THAT
 * SOFTWARE IS FREE FROM "BUGS", "VIRUSES", "TROJAN HORSES", "TRAP
 * DOORS", "WORMS", OR OTHER HARMFUL CODE.  LICENSEE ASSUMES THE ENTIRE
 * RISK AS TO THE PERFORMANCE OF SOFTWARE AND/OR ASSOCIATED MATERIALS,
 * AND TO THE PERFORMANCE AND VALIDITY OF INFORMATION GENERATED USING
 * SOFTWARE.
 */

