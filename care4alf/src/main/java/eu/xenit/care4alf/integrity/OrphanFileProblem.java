package eu.xenit.care4alf.integrity;

public class OrphanFileProblem extends FileProblem {
    public OrphanFileProblem(String path) {
        super(path);
    }

    @Override
    public String getMessage() {
        return "Found orphan file, no noderef seems to refer to it";
    }
}
