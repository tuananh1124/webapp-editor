package com.example.application.views.docxEditor.ui;


import com.example.application.views.docxEditor.component.DocumentEditor;
import com.example.application.views.docxEditor.config.MongoDBConfig;
import com.example.application.views.docxEditor.model.UploadedFile;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@PageTitle("UploadView")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.FILE)
public class UploadView extends VerticalLayout {
    private Grid<UploadedFile> fileGrid;
    private VerticalLayout mainLayout;

    public UploadView() {
        mainLayout = new VerticalLayout();
        add(mainLayout);
        buildUploadLayout();
        loadFiles();
    }

    private void buildUploadLayout() {
        mainLayout.removeAll();

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".docx", ".pdf");

        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                byte[] fileData = buffer.getInputStream().readAllBytes();
                String extension = getFileExtension(fileName);
                saveFileToMongo(fileName, fileData, extension);
                Notification.show("Upload thành công: " + fileName, 3000, Notification.Position.TOP_CENTER);
                loadFiles();
            } catch (IOException e) {
                e.printStackTrace();
                Notification.show("Upload thất bại!", 3000, Notification.Position.TOP_CENTER);
            }
        });

        Button createFileButton = new Button("Create File", e -> openCreateFileDialog());
        HorizontalLayout uploadLayout = new HorizontalLayout(upload, createFileButton);
        mainLayout.add(uploadLayout);

        fileGrid = new Grid<>(UploadedFile.class, false);
       
        fileGrid.addColumn(file -> getFileNameWithoutExtension(file.getFileName())).setHeader("Tên File");
        fileGrid.addColumn(UploadedFile::getFileType).setHeader("Loại File");

        fileGrid.addComponentColumn(file -> {
            Button viewButton;
            if ("pdf".equals(file.getFileType())) {
                viewButton = new Button("Xem", ev -> viewPDF(file));
            } else {
                viewButton = new Button("Xem", ev -> viewDOCX(file, true));
            }

         
            String baseName = getFileNameWithoutExtension(file.getFileName());
            String downloadFileName = baseName + "." + file.getFileType().toLowerCase();

            StreamResource resource = new StreamResource(downloadFileName,
                    () -> new ByteArrayInputStream(file.getData()));
            Anchor downloadAnchor = new Anchor(resource, "");
            downloadAnchor.getElement().setAttribute("download", downloadFileName);
            Button downloadButton = new Button("Tải xuống");
            downloadAnchor.add(downloadButton);

            HorizontalLayout actions = new HorizontalLayout(viewButton, downloadAnchor);
            Button deleteButton = new Button("Xóa", ev -> deleteFile(file));
            actions.add(deleteButton);

            if ("docx".equals(file.getFileType())) {
                Button editButton = new Button("Sửa", ev -> loadDocumentEditor(file));
                actions.add(editButton);
            }
            return actions;
        }).setHeader("Hành động");

        mainLayout.add(new H2("Danh sách file:"));
        mainLayout.add(fileGrid);
    }

    private void loadDocumentEditor(UploadedFile file) {
        mainLayout.removeAll();
        String base64Docx = Base64.getEncoder().encodeToString(file.getData());
      
        DocumentEditor editorPanel = new DocumentEditor(() -> {
            buildUploadLayout();
            loadFiles();
        }, base64Docx, file.getId());
        mainLayout.add(editorPanel);
    }

    private void openCreateFileDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        TextField fileNameField = new TextField("Tên File");
        fileNameField.setWidthFull();

        ComboBox<String> fileTypeCombo = new ComboBox<>("Loại File");
        fileTypeCombo.setItems("docx");
        fileTypeCombo.setValue("docx");

        Button createButton = new Button("Tạo", event -> {
            String fileName = fileNameField.getValue();
            String fileType = fileTypeCombo.getValue();
            if (fileName == null || fileName.trim().isEmpty()) {
                Notification.show("Tên file không được để trống!", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            saveFileToMongo(fileName, new byte[0], fileType);
            Notification.show("Tạo file thành công: " + fileName + " (" + fileType + ")", 3000, Notification.Position.TOP_CENTER);
            dialog.close();
            loadFiles();
        });
        Button cancelButton = new Button("Hủy", event -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(createButton, cancelButton);
        VerticalLayout dialogLayout = new VerticalLayout(fileNameField, fileTypeCombo, buttons);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void saveFileToMongo(String fileName, byte[] fileData, String fileType) {
        MongoCollection<Document> collection = MongoDBConfig.getFileEditorCollection();
        Document doc = new Document("filename", fileName)
                .append("fileType", fileType)
                .append("data", new org.bson.types.Binary(fileData));
        collection.insertOne(doc);
    }

    private void loadFiles() {
        List<UploadedFile> files = new ArrayList<>();

        MongoCollection<Document> collection = MongoDBConfig.getFileEditorCollection();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                ObjectId id = doc.getObjectId("_id");
                String fileName = doc.getString("filename");
                String fileType = doc.getString("fileType");
                Binary binaryData = doc.get("data", Binary.class);
                byte[] data = binaryData.getData();
                files.add(new UploadedFile(id.toHexString(), fileName, data, fileType));
            }
        }
        fileGrid.setItems(files);
    }

    private void viewPDF(UploadedFile file) {
        Dialog pdfDialog = new Dialog();
        pdfDialog.setWidth("40%");
        pdfDialog.setHeight("20%");
        pdfDialog.setCloseOnOutsideClick(false);
        pdfDialog.setCloseOnEsc(false);

        StreamResource resource = new StreamResource(file.getFileName(),
                () -> new ByteArrayInputStream(file.getData()));
        Anchor pdfAnchor = new Anchor(resource, "Nhấn vào đây để xem PDF");
        pdfAnchor.getElement().setAttribute("target", "_blank");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.add(pdfAnchor);

        Button cancelButton = new Button("Hủy", event -> pdfDialog.close());
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.getStyle().set("justify-content", "flex-end");
        footer.add(cancelButton);

        layout.add(footer);
        pdfDialog.add(layout);
        pdfDialog.open();
    }

    private void viewDOCX(UploadedFile file, boolean readOnly) {
        if (file.getData() == null || file.getData().length == 0) {
            Notification.show("File trống, không có nội dung, vui lòng thử lại!", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        String base64Docx = Base64.getEncoder().encodeToString(file.getData());

        Dialog docxDialog = new Dialog();
        docxDialog.setWidth("90%");
        docxDialog.setHeight("90%");

        // Create the container for the React component
        Div customElementContainer = new Div();
        customElementContainer.getElement().setProperty("innerHTML",
            "<react-text-input fileContent='" + base64Docx + "' readOnly='" + readOnly + "'></react-text-input>");

        // Add the container to the dialog before opening
        docxDialog.add(customElementContainer);

        // Open the dialog
        docxDialog.open();
    }

    private void deleteFile(UploadedFile file) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");

        H2 confirmTitle = new H2("Xác nhận xóa");
        TextField message = new TextField();
        message.setValue("Bạn có chắc chắn muốn xóa file: " + file.getFileName() + " ?");
        message.setReadOnly(true);
        message.setWidthFull();

        Button confirmButton = new Button("Có", e -> {
            MongoCollection<Document> collection = MongoDBConfig.getFileEditorCollection();
            collection.deleteOne(new Document("_id", new ObjectId(file.getId())));
            Notification.show("Đã xóa file: " + file.getFileName(), 3000, Notification.Position.TOP_CENTER);
            confirmDialog.close();
            loadFiles();
        });

        Button cancelButton = new Button("Không", e -> confirmDialog.close());
        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout dialogLayout = new VerticalLayout(confirmTitle, message, buttons);
        confirmDialog.add(dialogLayout);
        confirmDialog.open();
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

 
    private String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}