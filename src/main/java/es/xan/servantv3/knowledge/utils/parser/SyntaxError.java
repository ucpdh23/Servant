package es.xan.servantv3.knowledge.utils.parser;

public class SyntaxError extends RuntimeException {
    public SyntaxError(String message) {
        super(message);
    }

    public SyntaxError() {
        super();
    }
}
