package pl.gov.sejm.epuap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.gov.epuap.ws.filerepo.OdbierzFaultMsg;
import pl.gov.epuap.ws.skrytka.NadajFaultMsg;
import pl.gov.sejm.epuap.model.DocStatus;
import pl.gov.sejm.epuap.model.EpuapAttachment;
import pl.gov.sejm.epuap.model.EpuapDocument;
import pl.gov.sejm.epuap.model.EpuapUPP;

/**
 * A class that binds together {@link EpuapService} and {@link Store} to
 * allow documents to be read from ePUAP and stored in store.
 * 
 * @author Mariusz Jakubowski
 *
 */
public class EpuapAPIHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(EpuapAPIHelper.class);
    
    private Store store;
    
    private EpuapService service;

    /**
     * Constructs a new API Helper.
     * @param config a configuration object
     * @param store a store to read/save documents
     */
    public EpuapAPIHelper(final EpuapConfig config, final Store store) {
        this.store = store;
        service = new EpuapService(config);
    }
    
    /**
     * Downloads an attachment with specified id.
     * @param id the id of an attachment 
     * @return an attachment
     * @throws OdbierzFaultMsg 
     */
    public EpuapAttachment downloadAttachment(final String id) throws OdbierzFaultMsg {
        EpuapAttachment attachment = service.downloadAttachment(id);
        if (attachment != null) {
        	store.addAttachment(null, attachment);
        }
        return attachment;        
    }
    
    /**
     * Sends a document through the ePUAP.
     * @param docId a document id
     * @return a confirmation of sending a document
     * @throws NadajFaultMsg 
     */
    public EpuapUPP sendDocument(final String docId) {
        EpuapDocument doc = store.getDocumentByStoreId(docId);
		try {
			EpuapUPP upp = service.send(doc);
			store.saveUPP(docId, upp);
	        store.changeDocStatus(doc, DocStatus.UPLOADED);
	        return upp;
		} catch (NadajFaultMsg e) {
			e.printStackTrace();
            LOG.error(e.getFaultInfo().getKomunikat());
            return null;
		}
    }
    
    /**
     * Uploads an attachment to the ePUAP.
     * @param id an id of an attachment in the store to upload
     * @return the id of the uploaded file
     * @throws OdbierzFaultMsg 
     */
    public String uploadAttachment(final String id) throws OdbierzFaultMsg {
        EpuapAttachment att = store.getAttachment(id);
        String fileId = service.upload(att.getFileName(), att.getBytes());
        att.setURI(fileId);
        store.attachmentUploaded(att);
        return fileId;
    }
    
}
