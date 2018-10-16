package pl.gov.sejm.epuap.model;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Confirmation of sending a document.
 *
 * @author Mariusz Jakubowski
 *
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "UPP")
public class EpuapUPP {

    private final String id;
    private final String idUPP;
    private final int status;
    private final String statusMsg;
    private final String uppName;
    private final byte[] uppData;

    /**
     * Constructs a new confirmation of sending a document.
     *
     * @param id        an id of a sent document
     * @param idUPP     an id of this UPP
     * @param status    a status code
     * @param statusMsg a status message
     * @param uppName   a name of the file
     * @param uppData   contents of the UPP
     */
    public EpuapUPP(final String id,
            final String idUPP,
            final int status,
            final String statusMsg,
            final String uppName,
            final byte[] uppData) {
        super();
        this.id = id;
        this.idUPP = idUPP;
        this.status = status;
        this.statusMsg = statusMsg;
        this.uppName = uppName;
        this.uppData = uppData;
    }

    /**
     * Returns an id of sent document.
     *
     * @return id of a document
     */
    public String getId() {
        return id;
    }

    /**
     * Returns an id of this UPP.
     *
     * @return an id of UPP
     */
    public String getIdUPP() {
        return idUPP;
    }

    /**
     * Returns a status code.
     *
     * @return a status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns a status message.
     *
     * @return a status message
     */
    public String getStatusMsg() {
        return statusMsg;
    }

    /**
     * Returns name of the file with UPP.
     * @return a name of the file
     */
    public String getUppName() {
        return uppName;
    }

    /**
     * Returns data for this UPP.
     *
     * @return UPP data
     */
    @XmlTransient
    public byte[] getUppData() {
        return uppData;
    }

    /**
     * Returns data for this UPP converted to a string.
     *
     * @return UPP data
     */
    public String getUppXML() {
        if (uppData != null) {
            try {
                return new String(uppData, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "UPP for " + id + ", UPP ID=" + idUPP + ", status=" + statusMsg;
    }

}
