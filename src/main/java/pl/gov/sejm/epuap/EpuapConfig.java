package pl.gov.sejm.epuap;

import java.util.List;

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
     * Returns a list of inboxes to import.
     *
     * @return a list of inboxes
     */
    List<String> getInboxes();

}
