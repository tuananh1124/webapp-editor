package com.example.application.views.docxEditor.model;

import org.springframework.data.mongodb.core.mapping.Document;

import com.vaadin.flow.component.template.Id;


public class UploadedFile {
	@Id
    private String id;
    private String fileName;
    private byte[] data;
    private String fileType;

    public UploadedFile(String id, String fileName, byte[] data, String fileType) {
        this.id = id;
        this.fileName = fileName;
        this.data = data;
        this.fileType = fileType;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileType() {
        return fileType;
    }
}
