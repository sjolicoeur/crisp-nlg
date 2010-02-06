package crisp.main;

/**
 * The standard Exception that is thrown by CRISP if anything goes wrong.
 * @author dbauer
 */
public class CrispGeneratorException extends Exception{

    public CrispGeneratorException(String string) {
        super(string);
    }

    public CrispGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
