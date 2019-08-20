package eu.xenit.care4alf.integrity;

import java.nio.file.Path;

public abstract class FileProblem implements Problem {
    private String path;

    public FileProblem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
