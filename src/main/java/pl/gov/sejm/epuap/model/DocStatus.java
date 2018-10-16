package pl.gov.sejm.epuap.model;

/**
 * Status of a document in database.
 *
 * @author Mariusz Jakubowski
 *
 */
public enum DocStatus {
    
    /** Document is downloaded from ePUAP. */
    DOWNLOADED("D"),
    /** Document and attachments are downloaded. */
    ATTACHMENTS_DOWNLOADED("A"),
    /** Document is ready to be send. */
    READY_TO_SEND("R"),
    /** Document has been uploaded to ePUAP. */
    UPLOADED("U");

    /** A value stored in database for given status. */
    private String symbol;

    /**
     * Ctor.
     * @param symbol a value stored in database
     */
    DocStatus(final String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns a value stored in database for given status.
     * @return
     */
    public String getSymbol() {
        return symbol;
    }
}
