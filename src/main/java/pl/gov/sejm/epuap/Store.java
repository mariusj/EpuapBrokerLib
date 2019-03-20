package pl.gov.sejm.epuap;

import pl.gov.sejm.epuap.model.DocStatus;
import pl.gov.sejm.epuap.model.EpuapAttachment;
import pl.gov.sejm.epuap.model.EpuapDocument;
import pl.gov.sejm.epuap.model.EpuapUPP;

/**
 * An interface to a document store.
 *
 * @author Mariusz Jakubowski
 *
 */
public interface Store {

    /**
     * Adds a new document to a store.
     *
     * @param doc data to store on a document
     * @return an id of a new document (the store internal id)
     */
    String addDocument(EpuapDocument doc);

    /**
     * Changes status of a document.
     *
     * @param doc a document to change status in
     * @param status a new status of a document
     */
    void changeDocStatus(EpuapDocument doc, DocStatus status);

    /**
     * Adds a new attachment to a document.
     *
     * @param parent a parent document
     * @param attachment an attachment
     * @return an id of the attachment
     */
    String addAttachment(EpuapDocument parent, EpuapAttachment attachment);

    /**
     * Gets a document from a store by using store internal id.
     *
     * @param storeId an id of a document (the internal id used by the store)
     * @return a document
     */
    EpuapDocument getDocument(String storeId);

    /**
     * Saves a confirmation that a document was delivered.
     *
     * @param parent a parent document
     * @param upp    a UPP info
     */
    void saveUPP(String parent, EpuapUPP upp);

    /**
     * Gets an attachment from a store.
     *
     * @param storeId an id of an attachment (the internal id used by the store)
     * @return an attachment with given id
     */
    EpuapAttachment getAttachment(String storeId);

    /**
     * Called after attachment has been uploaded.
     *
     * @param attachment an uploaded attachment
     */
    void attachmentUploaded(EpuapAttachment attachment);

    /**
     * Logs an error.
     *
     * @param e an exception to log
     */
    void logError(Throwable e);

    /**
     * Called after document has been confirmed as received.
     * @param inbox a name of the inbox 
     * @param sha a sha of the confirmed document
     * @param code a status code
     * @param message a status message
     */
    void confirmed(String inbox, String sha, int code, String message);

    /**
     * Loads a XSL stylesheet with a given id.
     * XSL files can be cached in Store so they are read locally instead of
     * from CRD. The {@link #storeStylesheet(String, String)} saves a 
     * stylesheet in the store.
     * If the stylesheet with a given name is not found in the store then 
     * it returns null.  
     * @param id an id of the stylesheet
     * @return an XSL or null if XSL is not found
     */
    String loadStyleSheet(String id);

    /**
     * Saves a stylesheet in the store. Stylesheets can be cached in the store
     * so they can be read from it later. 
     * @param id an id of the stylesheet
     * @param xsl a stylesheet to save
     */
    void storeStylesheet(String id, String xsl);

    /**
     * Saves a HTML representation of the document.
     * The HTML is produced by transforming XML document using associated XSL.
     * @param doc a document
     * @param html a generated HTML
     */
    void saveHTML(EpuapDocument doc, String html);

}
