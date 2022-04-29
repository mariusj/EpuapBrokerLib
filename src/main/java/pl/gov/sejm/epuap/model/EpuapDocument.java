package pl.gov.sejm.epuap.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import pl.gov.epuap.ws.zarzadzaniedokumentami.AdresatTyp;
import pl.gov.epuap.ws.zarzadzaniedokumentami.NadawcaTyp;
import pl.gov.epuap.ws.zarzadzaniedokumentami.SzczegolyDokumentuTyp;
import pl.gov.epuap.wsdl.obiekty.DanePodmiotuTyp;

/**
 * Information about a document.
 *
 * @author Mariusz Jakubowski
 *
 */
public class EpuapDocument {
	
	public static final String FOLDER_RECEIVED = "RECEIVED";

	public static final String FOLDER_SENT = "SENT";

	public static final String FOLDER_DRAFT = "DRAFT";
	
    private static final Logger LOG = 
            LoggerFactory.getLogger(EpuapDocument.class);
    
    private static final Charset UTF8 = Charset.forName("UTF-8");

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
    
    private final String senderBox;

    private final String senderFirstName;

    private final String senderLastName;

    private final String nip;

    private final String pesel;

    private final String regon;

    private final String senderType;

    private final boolean digitalAccept;

    private final String docID;

    private final List<EpuapAttachment> attachments = new ArrayList<>();

    private final String sha;

    private final String sendTo;
    
    private final String sendToName;
    
    private final String formName;
    
    private final String folder;
    
    private final Integer idUPO;
    
    
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
    public EpuapDocument(
    		final String fromID,
            final DanePodmiotuTyp senderInfo,
            final String replyTo,
            final String inbox,
            final Calendar date,
            final byte[] addData,
            final String fileName,
            final String fileType,
            final byte[] data) {
        this.fromID = fromID;
        this.senderID = senderInfo.getIdentyfikator();
        this.senderFirstName = senderInfo.getImieSkrot();
        this.senderLastName = senderInfo.getNazwiskoNazwa();
        this.senderBox = null;
        this.nip = senderInfo.getNip();
        this.pesel = senderInfo.getPesel();
        this.regon = senderInfo.getRegon();
        this.senderType = senderInfo.getTypOsoby();
        this.digitalAccept = senderInfo.isZgoda();
        this.replyTo = replyTo;
        this.sendTo = null;
        this.sendToName = null;

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
        this.formName = null;
        this.folder = null;
        this.idUPO = null;

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
        this.sendToName = null;
        this.fileName = fileName;
        this.data = data;
        this.sha = Base64.encodeBase64String(DigestUtils.sha1(data));
        String dataXML = null;
        try {
            dataXML = new String(this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.dataXML = dataXML;
        this.senderType = null;
        this.senderID = this.fromID;
        this.senderBox = null;
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
        this.formName = null;
        this.folder = null;
        this.idUPO = null;
    }

    /**
     * Creates a document from data obtained from <code>ZarzadzanieDokumentami</code> service.
     * @param meta a metadata of a document
     * @param body a body of a document
     */
    public EpuapDocument(SzczegolyDokumentuTyp meta, Source body) {
		this.docID = Integer.toString(meta.getId());
    	this.idUPO = meta.getIdUPO() != null ? meta.getIdUPO() : null;
	    this.fromID = null;

	    this.senderFirstName = null;
		NadawcaTyp nadawca = meta.getNadawca();
	    this.replyTo = nadawca != null ? nadawca.getAdres() : null;
	    this.senderLastName = nadawca != null ? nadawca.getNazwa() : null;
		if (nadawca != null && nadawca.getAdres() != null && !nadawca.getAdres().isEmpty()) {
			this.senderID = nadawca.getAdres().split("/")[1];
			this.senderBox = nadawca.getAdres().split("/")[2];
		} else {
			this.senderID = null;
			this.senderBox = null;
		}
		AdresatTyp adresat = meta.getAdresat();
	    this.sendTo = adresat != null ? adresat.getAdres() : null;
	    this.sendToName = adresat != null ? adresat.getNazwa() : null;
	    
	    this.inbox = meta.getAdresat() != null ? meta.getAdresat().getAdres() : null;
	    this.addData = null;
	    this.addDataXML = readFromSource(meta.getMetadane());
	    this.date = meta.getDataNadania() != null ? meta.getDataNadania().toGregorianCalendar() : null;
	    this.fileName = meta.getNazwa();
	    this.fileType = null;
	    this.dataXML = readFromSource(body);
	    this.data = dataXML != null ? dataXML.getBytes(Charset.forName("UTF-8")) : null;
	    this.nip = null;
	    this.pesel = null;
	    this.regon = null;
	    this.senderType = null;
	    this.digitalAccept = true;
	    this.sha = null;
	    if (meta.getFormularz() != null) {
	    	// probably bug in ePUAP: nazwa - org id, podmiot - form name
	    	this.formName = meta.getFormularz().getNazwa() + "/" + meta.getFormularz().getPodmiot();
	    } else {
	    	this.formName = null;
	    }
	    this.folder = meta.getFolder();
        this.extractAttachments();
	}

    /**
     * Ctor.
     * @param docID
     * @param storeID
     * @param fileName
     * @param date
     * @param inbox
     * @param senderID
     * @param senderFirstName
     * @param senderLastName
     * @param fromID
     * @param replyTo
     * @param senderBox
     * @param sendTo
     * @param sendToName
     * @param formName
     * @param idUPO
     * @param data
     * @param addData
     */
    public EpuapDocument(
			String docID, 
    		String storeID, 
			String fileName, 
			Calendar date, 
    		String inbox, 
			String senderID,
			String senderFirstName, 
			String senderLastName, 
    		String fromID, 
    		String replyTo, 
			String senderBox, 
			String sendTo, 
			String sendToName, 
			String formName, 
			Integer idUPO,
			byte[] data, 
    		byte[] addData 
			) {
		this.storeID = storeID;
		this.fromID = fromID;
		this.replyTo = replyTo;
		this.inbox = inbox;
		this.addData = addData;
		this.addDataXML = new String(addData, UTF8);
		this.date = date;
		this.fileName = fileName;
		this.fileType = null;
		this.data = data;
		this.dataXML = new String(data, UTF8);
		this.senderID = senderID;
		this.senderBox = senderBox;
		this.senderFirstName = senderFirstName;
		this.senderLastName = senderLastName;
		this.nip = null;
		this.pesel = null;
		this.regon = null;
		this.senderType = null;
		this.digitalAccept = true;
		this.docID = docID;
		this.sha = Base64.encodeBase64String(DigestUtils.sha1(data));
		this.sendTo = sendTo;
		this.sendToName = sendToName;
		this.formName = formName;
		this.folder = null;
		this.idUPO = idUPO;
		extractAttachments();
	}

	/**
     * Reads contents of a XML source.
     * @param source
     * @return
     */
	private String readFromSource(Source source) {
		if (source == null) {
			return null;
		}
		try {
		    if (source instanceof StreamSource) {
		    	StreamSource ss = (StreamSource) source;
		    	BufferedReader br;
		    	if (ss.getReader() != null) {
		    		br = new BufferedReader(ss.getReader());
		    	} else {
			    	InputStream is = ss.getInputStream();
			    	if (is == null) {
			    		LOG.error("StreamSource InputStream is null");
			    		return null;
			    	}
			    	br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));			    	
		    	}		    	
		    	String line = br.readLine();
		    	StringBuilder out = new StringBuilder();
		    	while (line != null) {
		    		out.append(line).append("\n");
		    		line = br.readLine();
		    	}
		    	return out.toString();
		    } else {
		    	LOG.error("Source is not StreamSource: {}", source.getClass().getName());
		    }
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return null;
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
     * Returns name of the recipient.
     * @return a name of recipient
     */
    public String getSendToName() {
		return sendToName;
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
     * A name of a box from where sender sent this message.
     * @return a name of a box
     */
    public String getSenderBox() {
		return senderBox;
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
     * Returns name of the form in which this document was created.
     * @return
     */
    public String getFormName() {
		return formName;
	}
    
    /**
     * Returns a folder in the ePUAP inbox.
     * @return
     */
    public String getFolder() {
		return folder;
	}
    
    /**
     * Id of the source document.
     * @return
     */
    public Integer getIdUPO() {
		return idUPO;
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
