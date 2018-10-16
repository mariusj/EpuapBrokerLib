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
     * @return an id of a new document
     */
    String addDocument(EpuapDocument doc);

    /**
     * Changes status of a document.
     *
     * @param docId  an id of a document
     * @param status a new status of a document
     */
    void changeDocStatus(String docId, DocStatus status);

    /**
     * Adds a new attachment to a document.
     *
     * @param parent a parent document
     * @param attachment an attachment
     * @return an id of the attachment
     */
    String addAttachment(EpuapDocument parent, EpuapAttachment attachment);

    /**
     * Gets a document from a store.
     *
     * @param id an id of a document
     * @return a document
     */
    EpuapDocument getDocument(String id);

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
     * @param id an id of an attachment
     * @return an attachment with given id
     */
    EpuapAttachment getAttachment(String id);

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

}
