
package org.xmlpull.v1.builder.xpath.impl;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xmlpull.v1.builder.XmlAttribute;
import org.xmlpull.v1.builder.XmlCharacters;
import org.xmlpull.v1.builder.XmlComment;
import org.xmlpull.v1.builder.XmlContainer;
import org.xmlpull.v1.builder.XmlDocument;
import org.xmlpull.v1.builder.XmlElement;
import org.xmlpull.v1.builder.XmlInfosetBuilder;
import org.xmlpull.v1.builder.XmlNamespace;
import org.xmlpull.v1.builder.XmlProcessingInstruction;
import org.xmlpull.v1.builder.xpath.Xb1XPath;
import org.xmlpull.v1.builder.xpath.jaxen.DefaultNavigator;
import org.xmlpull.v1.builder.xpath.jaxen.FunctionCallException;
import org.xmlpull.v1.builder.xpath.jaxen.XPath;
import org.xmlpull.v1.builder.xpath.jaxen.util.SingleObjectIterator;
import org.xmlpull.v1.builder.xpath.saxpath.SAXPathException;

/**
 * Interface for navigating around the XB1 object model
 * to use with Jaxen XPATH implementation.
 *
 * @author Alek
 */
public class DocumentNavigator extends DefaultNavigator
{
    private static DocumentNavigator instance = new DocumentNavigator();

    public static DocumentNavigator getInstance()
    {
        return instance;
    }

    public boolean isElement(Object obj)
    {
        return obj instanceof XmlElement;
    }

    public boolean isComment(Object obj)
    {
        return obj instanceof XmlComment;
    }

    public boolean isText(Object obj)
    {
        return ( obj instanceof String
                    ||
                    obj instanceof XmlCharacters );
    }

    public boolean isAttribute(Object obj)
    {
        return obj instanceof XmlAttribute;
    }

    public boolean isProcessingInstruction(Object obj)
    {
        return obj instanceof XmlProcessingInstruction;
    }

    public boolean isDocument(Object obj)
    {
        return obj instanceof XmlDocument;
    }

    public boolean isNamespace(Object obj)
    {
        return obj instanceof XmlNamespace || obj instanceof XPathNamespace;
    }

    public String getElementName(Object obj)
    {
        XmlElement elem = (XmlElement) obj;

        return elem.getName();
    }

    public String getElementNamespaceUri(Object obj)
    {
        XmlElement elem = (XmlElement) obj;

        String uri = elem.getNamespaceName();
        if ( uri != null && uri.length() == 0 )
            return null;
        else
            return uri;
    }

    public String getAttributeName(Object obj)
    {
        XmlAttribute attr = (XmlAttribute) obj;

        return attr.getName();
    }

    public String getAttributeNamespaceUri(Object obj)
    {
        XmlAttribute attr = (XmlAttribute) obj;

        String uri = attr.getNamespaceName();
        if ( uri != null && uri.length() == 0 )
            return null;
        else
            return uri;
    }

    public Iterator getChildAxisIterator(Object contextNode)
    {
        if ( contextNode instanceof XmlElement )
        {
            return ((XmlElement)contextNode).children();
        }
        else if ( contextNode instanceof XmlDocument )
        {
            return ((XmlDocument)contextNode).children().iterator();
        }

        return null;
    }

    public Iterator getNamespaceAxisIterator(Object contextNode)
    {
        if ( ! ( contextNode instanceof XmlElement ) )
        {
            return null;
        }

        XmlElement elem = (XmlElement) contextNode;

        Map nsMap = new HashMap();

        XmlElement current = elem;

        while ( current != null ) {

            XmlNamespace ns = current.getNamespace();

            //if ( ns != Namespace.NO_NAMESPACE ) {
            if ( ns != null && ns.getPrefix() != null ) {
                if ( !nsMap.containsKey(ns.getPrefix()) )
                    nsMap.put( ns.getPrefix(), new XPathNamespace(elem, ns) );
            }

            Iterator declaredNamespaces = current.namespaces();

            while ( declaredNamespaces.hasNext() ) {

                ns = (XmlNamespace)declaredNamespaces.next();
                if ( !nsMap.containsKey(ns.getPrefix()) )
                    nsMap.put( ns.getPrefix(), new XPathNamespace(elem, ns) );
            }

            if(current.getParent() instanceof XmlElement) {
                current = (XmlElement) current.getParent();
            } else {
                current = null;
            }
        }

        //TODO: should we really add it or is it already added by XPP3?
        //nsMap.put( "xml", new XPathNamespace(elem, Namespace.XML_NAMESPACE) );

        return nsMap.values().iterator();
    }

    public Iterator getParentAxisIterator(Object contextNode)
    {
        Object parent = null;

        if ( contextNode instanceof XmlDocument )
        {
            parent = contextNode;
        }
        else if ( contextNode instanceof XmlElement )
        {
            parent = ((XmlElement)contextNode).getParent();

//            if ( parent == null )
//            {
//                if ( ((XmlElement)contextNode).isRootElement() )
//                {
//                    parent = ((Element)contextNode).getDocument();
//                }
//            }
        }
        else if ( contextNode instanceof XmlAttribute )
        {
            parent = ((XmlAttribute)contextNode).getOwner();
        }
        else if ( contextNode instanceof XPathNamespace )
        {
            parent = ((XPathNamespace)contextNode).getElement();
        }
        else if ( contextNode instanceof XmlProcessingInstruction )
        {
            parent = ((XmlProcessingInstruction)contextNode).getParent();
        }
        else if ( contextNode instanceof XmlComment )
        {
            parent = ((XmlComment)contextNode).getParent();
        }
        else if ( contextNode instanceof XmlCharacters ) {
            parent = ((XmlCharacters)contextNode).getParent();
        }
        else if ( contextNode instanceof String )
        {
            throw new IllegalStateException("XB1 XML tree shoul dnot contain Strings directly");
        }
        if ( parent != null )
        {
            return new SingleObjectIterator( parent );
        }

        return null;

    }

    public Iterator getAttributeAxisIterator(Object contextNode)
    {
        if ( ! ( contextNode instanceof XmlElement ) )
        {
            return null;
        }

        XmlElement elem = (XmlElement) contextNode;

        return elem.attributes();
    }

    public XPath parseXPath (String xpath) throws SAXPathException
    {
        return new Xb1XPath(xpath);
    }

    private static XmlDocument getDocument(XmlElement el) {
        //TODO recursively work until parent document
        XmlElement root = el;
        while(root.getParent() instanceof XmlElement) {
            root = (XmlElement) root.getParent();
        }
        return (XmlDocument) root.getParent();
    }

    public Object getDocumentNode(Object contextNode)
    {
        if ( contextNode instanceof XmlDocument )
        {
            return contextNode;
        }

        XmlElement elem = (XmlElement) contextNode;

        return getDocument(elem);
    }

    public String getElementQName(Object obj)
    {
        XmlElement elem = (XmlElement) obj;

        String prefix = null;
        if(elem.getNamespace() != null) {
            //TODO REVISIT: needs to declare prefix if namesapce name != null ... or not?

            prefix = elem.getNamespace().getPrefix();
        }

        if ( prefix == null || "".equals( prefix ) )
        {
            return elem.getName();
        }

        return prefix + ":" + elem.getName();
    }

    public String getAttributeQName(Object obj)
    {
        XmlAttribute attr = (XmlAttribute) obj;

        //String prefix = attr.getNamespacePrefix();
        String prefix = null;

        if(attr.getNamespace() != null) {
            //TODO REVISIT: needs to declare prefix if namesapce name != null ... or not?

            prefix = attr.getNamespace().getPrefix();
        }

        if ( prefix == null || "".equals( prefix ) )
        {
            return attr.getName();
        }

        return prefix + ":" + attr.getName();
    }

    public String getNamespaceStringValue(Object obj)
    {
        if (obj instanceof XmlNamespace) {

            XmlNamespace ns = (XmlNamespace) obj;
            return ns.getNamespaceName();
        } else {

            XPathNamespace ns = (XPathNamespace) obj;
            return ns.getNamespace().getNamespaceName();
        }
    }

    public String getNamespacePrefix(Object obj)
    {
        if (obj instanceof XmlNamespace) {

            XmlNamespace ns = (XmlNamespace) obj;
            return ns.getPrefix();
        } else {

            XPathNamespace ns = (XPathNamespace) obj;
            return ns.getNamespace().getPrefix();
        }
    }

    public String getTextStringValue(Object obj)
    {
        if ( obj instanceof XmlCharacters )
        {
            return ((XmlCharacters)obj).getText();
        }

        if ( obj instanceof String )
        {
            return ((String)obj);
        }

        return "";
    }

    public String getAttributeStringValue(Object obj)
    {
        XmlAttribute attr = (XmlAttribute) obj;

        return attr.getValue();
    }

    public String getElementStringValue(Object obj)
    {
        XmlElement elem = (XmlElement) obj;

        StringBuffer buf = new StringBuffer();

        Iterator contentIter = elem.children();
        Object   each        = null;

        while ( contentIter.hasNext() )
        {
            each = contentIter.next();

            if ( each instanceof String )
            {
                buf.append( ((String)each) );
            }
            else if ( each instanceof XmlCharacters )
            {
                buf.append( ((XmlCharacters)each).getText() );
            }
            else if ( each instanceof XmlElement )
            {
                buf.append( getElementStringValue( each ) );
            }
        }

        return buf.toString();
    }

    public String getProcessingInstructionTarget(Object obj)
    {
        XmlProcessingInstruction pi = (XmlProcessingInstruction) obj;

        return pi.getTarget();
    }

    public String getProcessingInstructionData(Object obj)
    {
        XmlProcessingInstruction pi = (XmlProcessingInstruction) obj;

        return pi.getContent();
    }

    public String getCommentStringValue(Object obj)
    {
        XmlComment cmt = (XmlComment) obj;

        return cmt.getContent();
    }

    public String translateNamespacePrefixToUri(String prefix, Object context)
    {
        XmlElement element = null;
        if ( context instanceof XmlElement )
        {
            element = (XmlElement) context;
        }
        else if ( context instanceof XmlCharacters )
        {
            element = (XmlElement) ((XmlCharacters)context).getParent();
        }
        else if ( context instanceof XmlAttribute )
        {
            element = ((XmlAttribute)context).getOwner();
        }
        else if ( context instanceof XPathNamespace )
        {
            element = ((XPathNamespace)context).getElement();
        }
        else if ( context instanceof XmlComment )
        {
            XmlContainer container = ((XmlComment)context).getParent();
            if(container instanceof XmlElement) {
                element = (XmlElement) container;
            }
        }
        else if ( context instanceof XmlProcessingInstruction )
        {
            XmlContainer container = ((XmlProcessingInstruction)context).getParent();
            if(container instanceof XmlElement) {
                element = (XmlElement) container;
            }
        }
        else if ( context instanceof String )
        {
            //TODO REVISIT to do automatic wrapping ....
            //element = ((String)context).getParent();
            throw new IllegalArgumentException("could not determine parent string in "+context);
        }

        if ( element != null )
        {
            XmlNamespace namespace = element.lookupNamespaceByPrefix( prefix );

            if ( namespace != null )
            {
                return namespace.getNamespaceName();
            }
        }
        return null;
    }

    public Object getDocument(String url) throws FunctionCallException
    {
        try
        {
            XmlInfosetBuilder builder = XmlInfosetBuilder.newInstance();

            return builder.parseLocation( url );
        }
        catch (Exception e)
        {
            throw new FunctionCallException( e.getMessage() );
        }
    }
}
