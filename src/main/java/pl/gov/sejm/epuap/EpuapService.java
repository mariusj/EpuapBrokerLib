package pl.gov.sejm.epuap;



import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import pl.gov.epuap.ws.filerepo.FileRepoService;
import pl.gov.epuap.ws.filerepo.Filerepo;
import pl.gov.epuap.ws.filerepo.OdbierzFaultMsg;
import pl.gov.epuap.ws.pull.PkPullService;
import pl.gov.epuap.ws.pull.Pull;
import pl.gov.epuap.ws.pull.PullFaultMsg;
import pl.gov.epuap.ws.skrytka.NadajFaultMsg;
import pl.gov.epuap.ws.skrytka.PkSkrytkaService;
import pl.gov.epuap.ws.skrytka.Skrytka;
import pl.gov.epuap.wsdl.filerepocore.DownloadFile;
import pl.gov.epuap.wsdl.filerepocore.DownloadFileParam;
import pl.gov.epuap.wsdl.filerepocore.UploadFileParam;
import pl.gov.epuap.wsdl.obiekty.DaneNadawcyTyp;
import pl.gov.epuap.wsdl.obiekty.DanePodmiotuTyp;
import pl.gov.epuap.wsdl.obiekty.DokumentTyp;
import pl.gov.epuap.wsdl.obiekty.OdpowiedzPullOczekujaceTyp;
import pl.gov.epuap.wsdl.obiekty.OdpowiedzPullPobierzTyp;
import pl.gov.epuap.wsdl.obiekty.OdpowiedzPullPotwierdzTyp;
import pl.gov.epuap.wsdl.obiekty.OdpowiedzSkrytkiTyp;
import pl.gov.epuap.wsdl.obiekty.ZapytaniePullOczekujaceTyp;
import pl.gov.epuap.wsdl.obiekty.ZapytaniePullPobierzTyp;
import pl.gov.epuap.wsdl.obiekty.ZapytaniePullPotwierdzTyp;
import pl.gov.sejm.epuap.model.DocStatus;
import pl.gov.sejm.epuap.model.EpuapAttachment;
import pl.gov.sejm.epuap.model.EpuapDocument;
import pl.gov.sejm.epuap.model.EpuapUPP;


/**
 * A wrapper around ePUAP services.
 * @author Mariusz Jakubowski
 *
 */
public class EpuapService {

    private static final Logger LOG = 
            LoggerFactory.getLogger(EpuapService.class);

    private static final Counter documents_received = Counter.build()
             .name("documents_received")
             .help("Number of received documents.")
             .register();

    private static final Counter documents_sent = Counter.build()
             .name("documents_sent")
             .help("Number of sent documents.")
             .register();

    private static final Gauge documents_received_time = Gauge.build()
             .name("documents_received_time")
             .help("Time of last successfull import of documents.")
             .register();

    private static final Gauge documents_sent_time = Gauge.build()
             .name("documents_sent_time")
             .help("Time of last successfull export of documents.")
             .register();

    /** status returned from ePUAP services */
    private static final int STATUS_OK = 1;

    /** A configuration. */
    private EpuapConfig config;

    /** The Pull service */
    private Pull pull;

    /** The Filerepo service */
    private Filerepo repo;

    /** The Skrytka service */
    private Skrytka skrytka;

    private DocumentBuilderFactory docBuilderFactory = 
            DocumentBuilderFactory.newInstance();
    
    private DocumentBuilder docBuilder;
    
    private TransformerFactory transformerFactory = 
            TransformerFactory.newInstance();

    private Bus bus;

    private org.apache.cxf.ext.logging.LoggingFeature loggingFeature;
    

    /**
     * Ctor.
     * @param config a configuration
     */
    public EpuapService(final EpuapConfig config) {
        LOG.info("Creating ePUAP service");
        this.config = config;
        this.bus = BusFactory.getDefaultBus();
        this.loggingFeature = new org.apache.cxf.ext.logging.LoggingFeature();
        loggingFeature.setVerbose(true);
        loggingFeature.setPrettyLogging(true);
        if (config.isLoggingEnabled()) {
            Collection<Feature> features = bus.getFeatures();
            features.add(loggingFeature);
            bus.setFeatures(features);
        }
        
        PkPullService pullSvc = new PkPullService();
        pull = pullSvc.getPull();
        initializeService((BindingProvider) pull, config.getPullService());

        FileRepoService repoSvc = new FileRepoService();
        repo = repoSvc.getFilerepo();
        initializeService((BindingProvider) repo, config.getFileRepoService());

        PkSkrytkaService skrytkaSvc = new PkSkrytkaService();
        skrytka = skrytkaSvc.getSkrytka();
        initializeService((BindingProvider) skrytka, config.getSkrytkaService());
    }

    /**
     * Configures a SOAP service (e.g. port address).
     * @param port a service port
     * @param url a service URL address
     */
    protected void initializeService(
            final BindingProvider port,
            final String url) {
        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(port);
        if (config.isLoggingEnabled()) {
            loggingFeature.initialize(client, bus);
        }
        Map<String, Object> ctx = port.getRequestContext();
        ctx.put("security.signature.properties", config.getSignatureProperties());
        ctx.put("security.callback-handler", config.getPasswordCallback().getName());        
        ctx.put("ws-security.asymmetric.signature.algorithm", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        if (url != null) {
            ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        }
    }

    /**
     * Returns full address of the inbox in format /podmiot/skrytka.
     * @param inboxName a name of the inbox
     * @return address of the inbox
     */
    private String getInboxAddr(final String inboxName) {
        return "/" + config.getOrg() + "/" + inboxName;
    }

    /**
     * Returns number of documents in a given inbox.
     * @param inbox a name of an inbox
     * @return a number of documents in this inbox
     */
    public int getNumDocuments(final String inbox) throws PullFaultMsg {
        LOG.info("getNumDocuments");
        ZapytaniePullOczekujaceTyp q = new ZapytaniePullOczekujaceTyp();
        q.setNazwaSkrytki(inbox);
        q.setPodmiot(config.getOrg());
        q.setAdresSkrytki(getInboxAddr(inbox));

        OdpowiedzPullOczekujaceTyp dokumenty = pull.oczekujaceDokumenty(q);
        LOG.info("num of documents in {}={}", 
                inbox, dokumenty.getOczekujace());
        return dokumenty.getOczekujace();
    }

    /**
     * Downloads documents from inbox and saves them in a store.
     * @param store a store
     * @param inbox a name of an inbox
     * @throws PullFaultMsg 
     */
    public int importInbox(final Store store, final String inbox) 
            throws PullFaultMsg {
        LOG.info("importing documents from {}", inbox);
        int docs = getNumDocuments(inbox);
        if (docs == 0) {
            LOG.info("no documents to import from {}", inbox);
            return 0;
        }
        LOG.info("importing {} documents from {}", docs, inbox);

        int imported = 0;
        for (int i = 0; i < docs; i++) {
            EpuapDocument doc = importAndSave(store, inbox);
            if (doc == null) {
                LOG.info("no more documents in {}", inbox);
                break;
            }
            imported++;
        }
        LOG.info("import from {} completed", inbox);
        return imported;
    }

    /**
     * Imports one document from a given inbox.
     * @param inbox a name of an inbox to import document from.
     * @return a imported document saved in a store
     * @throws PullFaultMsg 
     */
    public EpuapDocument importOneDoc(final String inbox) throws PullFaultMsg {
        LOG.info("importing document from {}", inbox);
        ZapytaniePullPobierzTyp q = new ZapytaniePullPobierzTyp();
        q.setNazwaSkrytki(inbox);
        q.setPodmiot(config.getOrg());
        q.setAdresSkrytki(getInboxAddr(inbox));

        OdpowiedzPullPobierzTyp meta = pull.pobierzNastepny(q);
        if (meta == null) {
            return null;
        }

        if (meta.getStatus().getKod() != STATUS_OK) {
            if (meta.getStatus().getKod() == 0) {
                LOG.warn("no documents to import from {}", inbox);
                return null;
            }
            LOG.error("error downloading documents from {}, code={}, {}",
                    inbox,
                    meta.getStatus().getKod(),
                    meta.getStatus().getKomunikat());
            return null;
        }

        String adresOdpowiedzi = meta.getAdresOdpowiedzi();
        String adresSkrytki = meta.getAdresSkrytki();
        byte[] daneDodatkowe = meta.getDaneDodatkowe();

        DaneNadawcyTyp daneNadawcy = meta.getDaneNadawcy();
        String system = daneNadawcy.getSystem();
        String uzytkownik = daneNadawcy.getUzytkownik();
        String sender = (system != null && !system.isEmpty()) ? 
                system : uzytkownik;

        DanePodmiotuTyp danePodmiotu = meta.getDanePodmiotu();

        XMLGregorianCalendar dataNadania = meta.getDataNadania();
        GregorianCalendar sendDate = dataNadania.toGregorianCalendar();

        DokumentTyp dok = meta.getDokument();
        String nazwaPliku = dok.getNazwaPliku();
        String typPliku = dok.getTypPliku();
        byte[] bytes = dok.getZawartosc();

        EpuapDocument documentInfo = new EpuapDocument(sender,
                danePodmiotu,
                adresOdpowiedzi,
                adresSkrytki,
                sendDate,
                daneDodatkowe,
                nazwaPliku,
                typPliku,
                bytes);
        LOG.info("imported document from={}, id={}",
                sender, documentInfo.getDocID());

        documents_received.inc();
        documents_received_time.setToCurrentTime();

        return documentInfo;
    }

    /**
     * Imports and saves one document.
     * @param store a store
     * @param inbox a name of inbox
     * @return a saved document
     * @throws PullFaultMsg 
     */
    public EpuapDocument importAndSave(final Store store, final String inbox) 
            throws PullFaultMsg {
        EpuapDocument edoc = importOneDoc(inbox);
        if (edoc == null) {
            return null;
        }
        store.addDocument(edoc);
        confirmReceive(store, inbox, edoc.getSHA());
        saveAttachments(store, edoc);
        String html = toHTML(store, edoc);
        if (html != null) {
            store.saveHTML(edoc, html);
        }
        return edoc;
    }

    /**
     * Converts a document in the XML format to the HTML format.
     * using associated XSL.
     * @param store a store
     * @param doc a document to transform
     * @return a transformed document
     */
    public String toHTML(final Store store, final EpuapDocument doc) {
        LOG.info("Generating HTML for {}", doc.getDocID());
        if (docBuilder == null) {
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                LOG.error(e.getMessage());
                return null;
            }
        } else {
            docBuilder.reset();
        }
        DOMSource xmlSource = toXMLSource(doc.getDataXML());
        Source stylesheet = getXSLFor(xmlSource, store);
        try {
            Transformer transformer = 
                    transformerFactory.newTransformer(stylesheet);
            StringWriter writer = new StringWriter();
            transformer.transform(xmlSource, new StreamResult(writer));
            return writer.toString();            
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            return null;
        }
    }

    /**
     * Converts XML string to a {@link DOMSource}.
     * @param xml contents of the XML file
     * @return a {@link DOMSource}
     */
    private DOMSource toXMLSource(final String xml) {
        StringReader r = new StringReader(xml);
        InputSource source = new InputSource(r);
        try {
            org.w3c.dom.Document xdoc = docBuilder.parse(source);
            return new DOMSource(xdoc);
        } catch (Exception e) {           
            e.printStackTrace();
            LOG.error(e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a stylesheet for a given XML.
     * @param xmlSource a XML with associated XSL
     * @return a stylesheet
     */
    private Source getXSLFor(final DOMSource xmlSource, final Store store) {
        try {
            Source stylesheet = transformerFactory.getAssociatedStylesheet(
                    xmlSource, null, null, null);
            String ssId = stylesheet.getSystemId();
            String xsl = store.loadStyleSheet(ssId);
            if (xsl == null) {
                saveStylesheet(store, ssId, stylesheet);
            } else {                
                stylesheet = new StreamSource(new StringReader(xsl));
            }
            return stylesheet;
        } catch (Exception e) {           
            e.printStackTrace();
            LOG.error(e.getMessage());
            return null;
        }
    }

    /**
     * Saves a stylesheet in the store.
     * @param store a store where stylesheet will be saved
     * @param ssId an id of the stylesheet
     * @param stylesheet a stylesheet
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    private void saveStylesheet(
            final Store store, 
            final String ssId, 
            final Source stylesheet)
            throws TransformerConfigurationException, TransformerException {
        LOG.info("Saving XSL {}", ssId);
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(stylesheet, new StreamResult(writer));
        store.storeStylesheet(ssId, writer.toString());
    }

    /**
     * Saves all attachments for a specified document.
     * @param store a store
     * @param edoc a document
     */
    public void saveAttachments(final Store store, final EpuapDocument edoc) {
        for (EpuapAttachment att : edoc.getAttachments()) {
            if (att.getURI() != null) {
                // for documents that are not embedded in XML but are
                // stored in the ePUAP big file storage
                // we have to download them
            	EpuapAttachment att2 = downloadAttachment(att.getURI());
                if (att2 == null) {
                	// There is an error downloading attachment from the ePUAP
                	// maybe we have downloaded this attachment earlier
                	// try to get it from the store.
                	// The ePUAP service doesn't allow to download the same
                	// big file twice (even if it were attached to two 
                	// different documents).
                	att2 = store.getAttachmentByURI(att.getURI());
                } 
                if (att2 != null) {
                    att.setStream(att2.getStream());
                }
            }
            store.addAttachment(edoc, att);
            if (config.isExtractZIP() 
                    && att.getFileName().toLowerCase().endsWith(".zip")) {
                extractZIP(store, edoc, att);
            }
        }
        store.changeDocStatus(edoc, DocStatus.ATTACHMENTS_DOWNLOADED);
    }

    /**
     * Extracts a zip and adds all attachments from it.
     * @param store a store
     * @param parent a parent document
     * @param zipAtt a zipped file
     */
    private void extractZIP(
            final Store store, 
            final EpuapDocument parent, 
            final EpuapAttachment zipAtt) {
        try {
        	LOG.info("extracting zip file {}", zipAtt.getFileName());
            java.nio.file.Path tempDir = Files.createTempDirectory("epuap");
            try {
            	Utils.extractZip(tempDir, zipAtt.getStream());
            } catch (IllegalArgumentException e) {
            	// extract files packed in DOS encoding
            	Utils.extractZip(tempDir, zipAtt.getStream(), Charset.forName("CP852"));
            }
            DirectoryStream<Path> dir = Files.newDirectoryStream(tempDir);
            for (Path path : dir) {
                byte[] bytes = Files.readAllBytes(path);
                EpuapAttachment unzipped = new EpuapAttachment(
                        path.getFileName().toString(), 
                        null, null, bytes, null);
                LOG.info("adding extracted attachment {}", path.getFileName().toString());
                store.addAttachment(parent, unzipped);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LOG.error("error extracting zip file {} - {}", zipAtt.getFileName(), e.getMessage());
        }
        
    }

    /**
     * Confirms receiving of a document.
     * After it's confirmed it's removed from the waiting 
     * documents in the inbox.
     * @param store a store
     * @param inbox an inbox name
     * @param sha a SHA-1 of document to confirm
     * @throws PullFaultMsg 
     */
    public void confirmReceive(
            final Store store, 
            final String inbox, 
            final String sha) throws PullFaultMsg {
        LOG.info("confirm receive in {} for {}", inbox, sha);
        ZapytaniePullPotwierdzTyp q = new ZapytaniePullPotwierdzTyp();
        q.setNazwaSkrytki(inbox);
        q.setPodmiot(config.getOrg());
        q.setAdresSkrytki(getInboxAddr(inbox));
        q.setSkrot(sha);
        OdpowiedzPullPotwierdzTyp resp = pull.potwierdzOdebranie(q);
        store.confirmed(inbox, sha, 
                resp.getStatus().getKod(), 
                resp.getStatus().getKomunikat());
    }

    /**
     * Downloads an attachment with given id.
     * @param docId an id of an attachment
     * @return a InputStream with attachment data
     * @throws OdbierzFaultMsg 
     */
    public EpuapAttachment downloadAttachment(String docId) {
        LOG.info("downloading attachment {}", docId);
        DownloadFileParam download = new DownloadFileParam();
        if (docId.startsWith("http")) {
            int idx = docId.indexOf("fileId=") + 7;
            docId = docId.substring(idx);
        }
        download.setFileId(docId);
        download.setSubject(config.getOrg());
        try {
        	DownloadFile downloaded = repo.downloadFile(download);
            LOG.info("download complete {} {}",
                    downloaded.getFilename(), downloaded.getMimeType());
            EpuapAttachment attachment = new EpuapAttachment(
                    downloaded.getFilename(),
                    downloaded.getEncoding(),
                    downloaded.getMimeType(),
                    docId,
                    null);
            DataHandler file = downloaded.getFile();
            try {
                InputStream stream = file.getInputStream();
                attachment.setStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error(e.toString());
            }
            return attachment;
        } catch (OdbierzFaultMsg e) {
            e.printStackTrace();
            LOG.error(e.getFaultInfo().getKomunikat());
        } catch (Throwable e) {
        	e.printStackTrace();
        	LOG.error(e.getMessage());
        }
        return null;
    }

    /**
     * Sends a document.
     * @param sendTo an address where to send document to
     * @param replyTo an address where recipient should send a response
     * @param fileName a name of a file to send
     * @param data a document to send
     * @return a confirmation that a document was sent
     * @throws NadajFaultMsg 
     */
    public EpuapUPP send(final String sendTo,
            final String replyTo,
            final String fileName,
            final byte[] data) throws NadajFaultMsg {
        LOG.info("sending {} to {} from {}", fileName, sendTo, replyTo);
        String podmiot = config.getOrg();
        DokumentTyp dokument = new DokumentTyp();
        dokument.setNazwaPliku(fileName);
        dokument.setZawartosc(data);
        OdpowiedzSkrytkiTyp resp = skrytka.nadaj(podmiot,
                sendTo,
                replyTo,
                false,
                null,
                dokument);
        LOG.info("document sent id: {}, upp: {}", 
                resp.getIdentyfikatorDokumentu(), 
                resp.getIdentyfikatorUpp());
        String uppName = null;
        byte[] uppData = null;
        if (resp.getZalacznik() != null && 
                resp.getZalacznik().getZawartosc() != null) {
            uppName = resp.getZalacznik().getNazwaPliku();
            uppData = resp.getZalacznik().getZawartosc();
        }

        documents_sent.inc();
        documents_sent_time.setToCurrentTime();

        return new EpuapUPP(resp.getIdentyfikatorDokumentu(),
                resp.getIdentyfikatorUpp(),
                resp.getStatus().getKod(),
                resp.getStatus().getKomunikat(),
                uppName,
                uppData);
    }

    /**
     * Sends a document.
     * @param doc a document to send
     * @return a confirmation that a document was sent
     * @throws NadajFaultMsg 
     */
    public EpuapUPP send(final EpuapDocument doc) throws NadajFaultMsg {
        EpuapUPP upp = send(doc.getSendTo(),
                doc.getReplyTo(),
                doc.getFileName(),
                doc.getData());
        return upp;
    }


    /**
     * Uploads a big file to the ePUAP.
     * @param fileName name of a file
     * @param data contents of a file
     * @return the id of a file in the repository
     * @throws OdbierzFaultMsg 
     */
    public String upload(final String fileName, final byte[] data) 
            throws OdbierzFaultMsg {
        LOG.info("uploading big file {} {}", fileName, data.length);
        UploadFileParam upload = new UploadFileParam();
        upload.setFilename(fileName);
        String mime = EpuapAttachment.guessMIME(fileName);
        upload.setMimeType(mime);
        upload.setSubject(config.getOrg());
        DataSource ds = new ByteDataSource(data, mime);
        DataHandler dataHandler = new DataHandler(ds);
        upload.setFile(dataHandler);
        String uploaded = repo.uploadFile(upload);
        LOG.info("big file {} uploaded to {}", fileName, uploaded);
        return uploaded;
    }

}
