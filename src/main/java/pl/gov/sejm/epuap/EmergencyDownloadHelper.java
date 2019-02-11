package pl.gov.sejm.epuap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;

import pl.gov.epuap.EmergencyDownload.DokumentType;
import pl.gov.epuap.EmergencyDownload.Dokumenty;
import pl.gov.epuap.wsdl.obiekty.DanePodmiotuTyp;
import pl.gov.sejm.epuap.model.EpuapDocument;

/**
 * Helper for handling emergency download archive.
 * @author Mariusz Jakubowski
 *
 */
public class EmergencyDownloadHelper {
    
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss.SSS");
    
    private Path baseDir;

    /**
     * Ctor.
     * @param baseDir a directory where files will be extracted
     */
    public EmergencyDownloadHelper(java.nio.file.Path baseDir) {
        this.baseDir = baseDir;
    }
    
    
    /**
     * Loads a descriptor file.
     * @return a list of documents in descriptor
     * @throws JAXBException
     */
    public Dokumenty getDescriptor() throws JAXBException {
        java.nio.file.Path desc = baseDir.resolve("Deskryptor.xml");
        JAXBContext ctx = JAXBContext.newInstance(Dokumenty.class);
        Unmarshaller jaxbUnmarshaller = ctx.createUnmarshaller();
        Dokumenty docsType = (Dokumenty) jaxbUnmarshaller.unmarshal(desc.toFile());
        return docsType;
    }
    
    /**
     * Converts a document from emergency download descriptor to the 
     * {@link EpuapDocument} class.
     * @param doc a document loaded from emergency archive
     * @return an ePuap document
     * @throws ParseException
     * @throws IOException
     */
    public EpuapDocument loadEmergencyDoc(DokumentType doc)
            throws ParseException, IOException {
        DanePodmiotuTyp senderInfo = new DanePodmiotuTyp();
        senderInfo.setIdentyfikator(doc.getDanePodmiotu().getIdentyfikator());
        senderInfo.setImieSkrot(doc.getDanePodmiotu().getImieSkrot());
        senderInfo.setNazwiskoNazwa(doc.getDanePodmiotu().getNazwiskoNazwa());
        senderInfo.setNip(doc.getDanePodmiotu().getNip());
        senderInfo.setPesel(doc.getDanePodmiotu().getPesel());
        senderInfo.setRegon(doc.getDanePodmiotu().getRegon());
        senderInfo.setTypOsoby(doc.getDanePodmiotu().getTypOsoby());
        senderInfo.setZgoda(doc.getDanePodmiotu().isZgoda());
        java.nio.file.Path path = baseDir.resolve(doc.getSciezkaWArchiwum());
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(doc.getDataNadania()));
        byte[] data = Files.readAllBytes(path);
        System.out.println(doc.getDataNadania());
        byte[] addData = Base64.decodeBase64(doc.getDaneDodatkowe());
        EpuapDocument eDoc = new EpuapDocument(doc.getDanePodmiotu().getIdentyfikator(), 
                senderInfo, 
                doc.getAdresOdpowiedzi(), 
                doc.getAdresSkrytki(), 
                cal, 
                addData, 
                doc.getNazwaPliku(), 
                "application/xml", 
                data);
        return eDoc;
    }
    

}
