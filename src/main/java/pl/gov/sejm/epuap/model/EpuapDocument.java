package pl.gov.sejm.epuap.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import pl.gov.epuap.wsdl.obiekty.DanePodmiotuTyp;

/**
 * Information about a document.
 *
 * @author Mariusz Jakubowski
 *
 */
public class EpuapDocument {

    private String storeID;

    private final String fromID;
 
    private final String replyTo;

    private final String inbox;

    private final byte[] addData;

    private final String addDataXML;

    private final Calendar date;

    private final String fileName;

    private final String fileType;

    private final byte[] data;

    private final String dataXML;

    private final String senderID;

    private final String senderFirstName;

    private final String senderLastName;

    private final String nip;

    private final String pesel;

    private final String regon;

    private final String senderType;

    private final boolean digitalAccept;

    private final String docID;

    private final List<EpuapAttachment> attachments;

    private final String sha;

    private final String sendTo;
    
    /**
     * Creates a document received from the ePUAP service.
     *
     * @param fromID identifier of the sender
     * @param senderInfo information about a sender
     * @param replyTo a name of inbox to reply to
     * @param inbox a name of the inbox where the message arrived
     * @param date a date of document
     * @param addData additional data for this document
     * @param fileName a name of the document
     * @param fileType a type of the document
     * @param data an array of bytes with content of the file
     */
    public EpuapDocument(final String fromID,
            final DanePodmiotuTyp senderInfo,
            final String replyTo,
            final String inbox,
            final Calendar date,
            final byte[] addData,
            final String fileName,
            final String fileType,
            final byte[] data) {
        super();
        this.fromID = fromID;
        this.senderID = senderInfo.getIdentyfikator();
        this.senderFirstName = senderInfo.getImieSkrot();
        this.senderLastName = senderInfo.getNazwiskoNazwa();
        this.nip = senderInfo.getNip();
        this.pesel = senderInfo.getPesel();
        this.regon = senderInfo.getRegon();
        this.senderType = senderInfo.getTypOsoby();
        this.digitalAccept = senderInfo.isZgoda();
        this.replyTo = replyTo;
        this.sendTo = null;

        this.inbox = inbox;
        this.date = date;

        this.addData = addData;
        String addDataXML = null;
        String docId = null;
        if (addData != null && addData.length > 0) {
            try {
                addDataXML = new String(addData, "UTF-8");
                docId = extractDocId(addDataXML);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        this.addDataXML = addDataXML;
        this.docID = docId;

        this.fileName = fileName;
        this.fileType = fileType;

        this.data = data;
        this.sha = Base64.encodeBase64String(DigestUtils.sha1(data));

        String dataXML = null;
        if ("application/xml".equals(fileType) || "XML".equals(fileType)) {
            try {
                dataXML = new String(this.data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        this.dataXML = dataXML;

        this.attachments = new ArrayList<>();
        this.extractAttachments();
    }

    /**
     * Creates a document that will be send using the ePUAP service.
     *
     * @param fromID sender identifier in ePUAP
     * @param replyTo address of sender inbox
     * @param sendTo address of recipient inbox
     * @param fileName a name of the file
     * @param data an array of bytes with content of the file
     */
    public EpuapDocument(final String fromID,
            final String replyTo,
            final String sendTo,
            final String fileName,
            final byte[] data) {
        this.fromID = fromID;
        this.replyTo = replyTo;
        this.sendTo = sendTo;
        this.fileName = fileName;
        this.data = data;
        this.sha = Base64.encodeBase64String(DigestUtils.sha1(data));
        this.attachments = new ArrayList<>();
        String dataXML = null;
        try {
            dataXML = new String(this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.dataXML = dataXML;
        this.senderType = null;
        this.senderID = this.fromID;
        this.senderFirstName = null;
        this.senderLastName = null;
        this.regon = null;
        this.pesel = null;
        this.nip = null;
        this.inbox = null;
        this.fileType = null;
        this.digitalAccept = true;
        this.addData = null;
        this.addDataXML = null;
        this.date = null;
        this.docID = null;
    }

    /**
     * Returns an id of the document in the store.
     * @return an id of the document
     */
    public String getStoreID() {
        return storeID;
    }

    /**
     * Sets an id of the document in the store.
     * @param storeID an id of the document
     */
    public void setStoreID(final String storeID) {
        this.storeID = storeID;
    }

    /**
     * Returns id of the sender.
     * @return id of the sender
     */
    public String getFromID() {
        return fromID;
    }

    /**
     * Returns address of the inbox to where send reply.
     * @return address of the inbox
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Returns address of the inbox where the message arrived.
     * @return address of the inbox
     */
    public String getInbox() {
        return inbox;
    }

    /**
     * Returns additional data for this document.
     * @return additional data
     */
    public byte[] getAddData() {
        return addData;
    }

    /**
     * Returns additional data for this document as a XML string.
     * @return additional data
     */
    public String getAddDataXML() {
        return addDataXML;
    }

    /**
     * Returns date when this document was send.
     * @return a date of document
     */
    public Calendar getDate() {
        return date;
    }


    /**
     * Returns address to where send reply.
     * @return an address of inbox to reply
     */
    public String getSendTo() {
        return sendTo;
    }

    /**
     * Returns name of the file.
     * @return a name of file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns a MIME type of this document.
     * @return a MIME type
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Returns contents of this document.
     * @return contents of this document
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns contents of this document as XML string.
     * @return contents of this document
     */
    public String getDataXML() {
        return dataXML;
    }

    /**
     * Returns id of the sender.
     * @return id of the sender
     */
    public String getSenderID() {
        return senderID;
    }

    /**
     * Returns first name of the sender.
     * @return the first name of a sender
     */
    public String getSenderFirstName() {
        return senderFirstName;
    }

    /**
     * Returns last name of the sender.
     * @return the last name of a sender
     */
    public String getSenderLastName() {
        return senderLastName;
    }

    /**
     * Returns NIP of the sender.
     * @return NIP identifier
     */
    public String getNip() {
        return nip;
    }

    /**
     * Returns Pesel of the sender.
     * @return Pesel identifier
     */
    public String getPesel() {
        return pesel;
    }

    /**
     * Returns Regon of the sender.
     * @return Regon identifier
     */
    public String getRegon() {
        return regon;
    }

    /**
     * Returns type of the sender.
     * @return a type of a sender
     */
    public String getSenderType() {
        return senderType;
    }

    /**
     * Returns acceptance for digital contact.
     * @return true if digital contact is accepted
     */
    public boolean isDigitalAccept() {
        return digitalAccept;
    }

    /**
     * Returns an id of a document (in ePUAP).
     * @return id of a document
     */
    public String getDocID() {
        return docID;
    }

    /**
     * Returns a list of attachments.
     * @return a list of attachments.
     */
    public List<EpuapAttachment> getAttachments() {
        return attachments;
    }

    /**
     * Returns an SHA of this document.
     * @return an SHA of a document
     */
    public String getSHA() {
        return sha;
    }

    /**
     * Extracts a document id from additional data.
     * @param addDataXML 
     */
    private String extractDocId(String addDataXML) {
        try {
            org.w3c.dom.Document xmlDoc = parseXML(addDataXML);
            XPathExpression xID =
                    setupXPath("(//IdentyfikatorDokumentu)[last()]/text()");
            String id = (String) xID.evaluate(xmlDoc, XPathConstants.STRING);
            //System.out.println("eval " + id);
            if (id != null && !id.isEmpty()) {
                return id;
            }
        } catch (ParserConfigurationException | SAXException
                | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Constructs a list of attachments embedded or referenced in this document.
     */
    private void extractAttachments() {
        if (this.dataXML == null) {
            return;
        }
        try {
            org.w3c.dom.Document xmlDoc = parseXML(this.dataXML);
            XPathExpression xAttExt =
                    setupXPath("//str:Zalaczniki/str:Zalacznik");
            org.w3c.dom.NodeList attListExt =
                    (org.w3c.dom.NodeList)
                    xAttExt.evaluate(xmlDoc, XPathConstants.NODESET);
            for (int a = 0; a < attListExt.getLength(); a++) {
                Node att = attListExt.item(a);
                EpuapAttachment attachment = extractAttachment(att);
                if (attachment != null) {
                    attachments.add(attachment);
                }
            }
        } catch (ParserConfigurationException | SAXException
                | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts an attachment from a XML node.
     *
     * @param att a node with attachment
     * @return an attachment
     */
    private EpuapAttachment extractAttachment(final Node att) {
        NamedNodeMap attributes = att.getAttributes();
        Node nameNode = attributes.getNamedItem("nazwaPliku");
        if (nameNode == null) {
            return null;
        }
        String fileName = nameNode.getNodeValue();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        String encoding = attributes.getNamedItem("kodowanie").getNodeValue();
        Node format = attributes.getNamedItem("format");
        String mime = "";
        if (format != null) {
            mime = format.getNodeValue();
        }
        String description = null;
        String fileEncoded = null;
        Node child = att.getFirstChild();
        while (child != null) {
            String nodeName = child.getLocalName();
            if ("DaneZalacznika".equals(nodeName)) {
                fileEncoded = getNodeValue(child);
            } else if ("OpisZalacznika".equals(nodeName)) {
                description = getNodeValue(child);
            }
            child = child.getNextSibling();
        }
        if ("base64".equals(encoding)) {
            byte[] bytes = Base64.decodeBase64(fileEncoded);
            EpuapAttachment attachment = new EpuapAttachment(fileName,
                    encoding,
                    mime,
                    bytes,
                    description);
            return attachment;
        } else if ("URI".equals(encoding)) {
            EpuapAttachment attachment = new EpuapAttachment(fileName,
                    encoding,
                    mime,
                    fileEncoded,
                    description);
            return attachment;
        }
        return null;
    }

    /**
     * Returns node value or if node value is null returns node value of first
     * child.
     *
     * @param node a node to check
     * @return a node value
     */
    private String getNodeValue(final Node node) {
        if (node.getNodeValue() != null) {
            return node.getNodeValue();
        }
        Node child = node.getFirstChild();
        if (child != null) {
            return child.getNodeValue();
        }
        return null;
    }

    /**
     * Constructs a XML document from a string.
     * @param xml a string with XML document
     * @return an instance of XML Document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private org.w3c.dom.Document parseXML(final String xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        StringReader sr = new StringReader(xml);
        InputSource source = new InputSource(sr);
        org.w3c.dom.Document doc = builder.parse(source);
        return doc;
    }

    /**
     * Set up the XPath configuration.
     * @param expression an expression to initialize
     * @return an object with XPath expression
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private synchronized XPathExpression setupXPath(final String expression)
            throws ParserConfigurationException, XPathExpressionException {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(final String prefix) {
                if ("str".equals(prefix)) {
                    return "http://crd.gov.pl/xml/schematy/struktura/2009/11/16/";
                }
                return XMLConstants.NULL_NS_URI;
            }

            // This method isn't necessary for XPath processing.
            public String getPrefix(final String uri) {
                throw new UnsupportedOperationException();
            }

            // This method isn't necessary for XPath processing either.
            public Iterator<String> getPrefixes(final String uri) {
                throw new UnsupportedOperationException();
            }
        });

        XPathExpression expr = xpath.compile(expression);
        return expr;
    }

    @Override
    public String toString() {
        return "EpuapDocument [fromID=" + fromID
                + ", replyTo=" + replyTo
                + ", inbox=" + inbox
                + ", addDataXML=" + addDataXML
                + ", date=" + date
                + ", fileName=" + fileName
                + ", fileType=" + fileType
                + ", dataXML=" + dataXML
                + ", senderID=" + senderID
                + ", senderFirstName=" + senderFirstName
                + ", senderLastName=" + senderLastName
                + ", nip=" + nip
                + ", pesel=" + pesel
                + ", regon=" + regon
                + ", senderType=" + senderType
                + ", digitalAccept=" + digitalAccept
                + ", docID=" + docID
                + ", attachments=" + attachments + "]";
    }

}
