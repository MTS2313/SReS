package br.com.sres.telegram;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String name;
    private final String contentType;
    ByteArrayMultipartFile(byte[] content, String name, String contentType) { this.content = content; this.name = name; this.contentType = contentType; }
    public String getName() { return name; }
    public String getOriginalFilename() { return name; }
    public String getContentType() { return contentType; }
    public boolean isEmpty() { return content.length == 0; }
    public long getSize() { return content.length; }
    public byte[] getBytes() { return content; }
    public InputStream getInputStream() { return new ByteArrayInputStream(content); }
    public void transferTo(java.io.File destination) throws IOException { java.nio.file.Files.write(destination.toPath(), content); }
}
