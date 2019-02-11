package pl.gov.sejm.epuap;

import pl.gov.sejm.epuap.model.DocStatus;
import pl.gov.sejm.epuap.model.EpuapAttachment;
import pl.gov.sejm.epuap.model.EpuapDocument;
import pl.gov.sejm.epuap.model.EpuapUPP;

/**
 * A class that binds together {@link EpuapService} and {@link Store} to
 * allow documents to be read from ePUAP and stored in store.
 * @author Mariusz Jakubowski
 *
 */
public class EpuapAPIHelper {
    
    private Store store;
    private EpuapService service;

    public EpuapAPIHelper(EpuapConfig config, Store store) {
        this.store = store;
        service = new EpuapService(config);
    }
    
    /**
     * Downloads an attachment with specified id.
     * @param id the id of an attachment 
     * @return an attachment
     */
    public EpuapAttachment downloadAttachment(final String id) {
        EpuapAttachment attachment = service.downloadAttachment(id);
        store.addAttachment(null, attachment);
        return attachment;        
    }
    
    /**
     * Sends a document through the ePUAP.
     * @param docId a document id
     * @return a confirmation of sending a document
     */
    public EpuapUPP sendDocument(final String docId) {
        EpuapDocument doc = store.getDocument(docId);
        EpuapUPP upp = service.send(doc);
        store.saveUPP(docId, upp);
        store.changeDocStatus(docId, DocStatus.UPLOADED);
        return upp;
    }
    
    /**
     * Uploads an attachment to the ePUAP.
     * @param id an id of an attachment in the store to upload
     * @return the id of the uploaded file
     */
    public String uploadAttachment(final String id) {
        EpuapAttachment att = store.getAttachment(id);
        String fileId = service.upload(att.getFileName(), att.getBytes());
        att.setURI(fileId);
        store.attachmentUploaded(att);
        return fileId;
    }
    
}
