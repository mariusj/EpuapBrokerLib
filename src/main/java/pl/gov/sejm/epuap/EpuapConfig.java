package pl.gov.sejm.epuap;

import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

/**
 * Provides configuration for the broker.
 *
 * @author Mariusz Jakubowski
 *
 */
public interface EpuapConfig {

    /**
     * Returns identifier of our organization on ePUAP platform.
     *
     * @return id of an organization
     */
    String getOrg();

    /**
     * Returns address of the pull service.
     *
     * @return pull service address
     */
    String getPullService();

    /**
     * Returns address of the file repo service.
     *
     * @return file repo service address
     */
    String getFileRepoService();

    /**
     * Returns address of the skrytka service.
     *
     * @return skrytka service address
     */
    String getSkrytkaService();
    
    /**
     * Returns address of the ZarzadzanieDokumentami service.
     *
     * @return ZarzadzanieDokumentami service address
     */
    String getZarzadzanieDokumentamiService();

    /**
     * Returns a list of inboxes to import.
     *
     * @return a list of inboxes
     */
    List<String> getInboxes();
    
    /**
     * A flag indicating that downloaded .zip files should be extracted.
     * @return true if .zip files should be extracted
     */
    boolean isExtractZIP();
    
    /**
     * If this flag is set to true then logging of messages will be enabled.
     * @return
     */
    boolean isLoggingEnabled();
    
    /**
     * Returns a class that is used to authenticate to the keystore.
     * @return
     */
    Class<? extends CallbackHandler> getPasswordCallback();

    /**
     * Returns properties required for signing a message.
     * @return
     */
    Properties getSignatureProperties();

}
