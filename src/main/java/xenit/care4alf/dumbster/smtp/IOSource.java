package xenit.care4alf.dumbster.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public interface IOSource {

    public BufferedReader getInputStream() throws IOException;

    public PrintWriter getOutputStream() throws IOException;

    public void close() throws IOException;

}
