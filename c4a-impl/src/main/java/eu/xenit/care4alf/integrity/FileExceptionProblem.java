package eu.xenit.care4alf.integrity;

import java.io.IOException;

public class FileExceptionProblem extends FileProblem {
    private IOException exception;

    public FileExceptionProblem(IOException exception) {
        super("[IOException encountered during scan]");
        this.exception = exception;
    }

    @Override
    public String getMessage() {
        return exception.getMessage();
    }
}
