package pl.gov.sejm.epuap.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.io.IOUtils;

/**
 * Information about an attachment.
 * @author Mariusz Jakubowski
 *
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "attachment")
public class EpuapAttachment {

    private String storeID;
    private final String fileName;
    private final String encoding;
    private final String mime;
    private String URI;
    private final String description;
    private byte[] bytes;
    private InputStream stream;

    /**
     * Constructs an attachment object based on byte data.
     * @param fileName a name of a file
     * @param encoding an encoding
     * @param mime a MIME type
     * @param bytes an array of bytes
     * @param description a description of the attachment
     */
    public EpuapAttachment(final String fileName,
            final String encoding,
            final String mime,
            final byte[] bytes,
            final String description) {
        this.fileName = fileName;
        this.encoding = encoding;
        this.mime = mime;
        this.URI = null;
        this.bytes = bytes;
        this.description = description;
    }

    /**
     * Constructs an attachment object based on the address in the ePUAP.
     * @param fileName a name of a file
     * @param encoding an encoding
     * @param mime a MIME type
     * @param URI the URI of the attachment
     * @param description a description of the attachment
     */
    public EpuapAttachment(final String fileName,
            final String encoding,
            final String mime,
            final String URI,
            final String description) {
        this.fileName = fileName;
        this.encoding = encoding;
        this.mime = mime;
        this.URI = URI;
        this.bytes = null;
        this.description = description;
    }

    /**
     * Returns an id of the attachment in store.
     * @return an id
     */
    public String getStoreID() {
        return storeID;
    }

    /**
     * Sets an id of the attachment saved in store.
     * @param storeID a new id
     */
    public void setStoreID(final String storeID) {
        this.storeID = storeID;
    }

    /**
     * Returns a file name of the attachment.
     * @return a name of the file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns an encoding of the attachment.
     * @return an encoding of the attachment
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns a MIME type of the attachment.
     * @return a MIME type
     */
    public String getMime() {
        return mime;
    }

    /**
     * Returns a stream of bytes of this attachment.
     * @return a stream with contents of this attachment
     */
    @XmlTransient
    public InputStream getStream() {
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }
        return stream;
    }

    /**
     * Sets a stream of bytes for this attachment.
     * @param stream a stream of bytes
     */
    @XmlTransient
    public void setStream(final InputStream stream) {
        this.stream = stream;
    }

    /**
     * Returns an array of bytes for this attachment.
     * @return
     */
    @XmlTransient
    public byte[] getBytes() {
        if (this.bytes != null) {
            return this.bytes;
        }
        try {
            this.bytes = IOUtils.toByteArray(this.stream);
            this.stream.close();
            return this.bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a description of this attachment.
     * @return a description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns an URI of this attachment.
     * @return an URI
     */
    public String getURI() {
        return URI;
    }

    /**
     * Sets the URI of a document after uploading it to the ePUAP.
     * @param uRI
     */
    public void setURI(final String uRI) {
        URI = uRI;
    }

    /**
     * Tries to guess the MIME type of the attachment based on the file name.
     * @param fileName a name of the attachment
     * @return a MIME type
     */
    public static String guessMIME(final String fileName) {
        String mime = URLConnection.guessContentTypeFromName(fileName);
        if (mime == null) {
            if (fileName.endsWith(".docx")) {
                mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else {
                mime = "application/octet-stream";
            }
        }
        return mime;
    }

    @Override
    public String toString() {
        return "EpuapAttachment [fileName=" + fileName
                + ", encoding=" + encoding + ", mime=" + mime + ", URI=" + URI
                + ", description=" + description + ", bytes=" + bytes
                + ", stream=" + stream + "]";
    }


}
