package com.example.application.views.upload;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@PageTitle("About")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.FILE)
public class UploadView extends VerticalLayout {
    private Grid<UploadedFile> fileGrid;

    public UploadView() {
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
                // Làm mới danh sách file sau khi upload
                loadFiles();
            } catch (IOException e) {
                e.printStackTrace();
                Notification.show("Upload thất bại!", 3000, Notification.Position.TOP_CENTER);
            }
        });

        Button createFileButton = new Button("Create File", e -> openCreateFileDialog());

        HorizontalLayout uploadLayout = new HorizontalLayout(upload, createFileButton);
        add(uploadLayout);

        fileGrid = new Grid<>(UploadedFile.class, false);
        fileGrid.addColumn(UploadedFile::getFileName).setHeader("Tên File");
        fileGrid.addColumn(UploadedFile::getFileType).setHeader("Loại File");

        fileGrid.addComponentColumn(file -> {
            Button viewButton;
            if ("pdf".equals(file.getFileType())) {
                viewButton = new Button("Xem", ev -> viewPDF(file));
            } else {
                viewButton = new Button("Xem", ev -> viewDOCX(file, true));
            }

            StreamResource resource = new StreamResource(file.getFileName(), 
                    () -> new ByteArrayInputStream(file.getData())); 
            Button downloadButton = new Button("Tải xuống");
            Anchor downloadAnchor = new Anchor(resource, "");
            downloadAnchor.getElement().setAttribute("download", true);
            downloadAnchor.add(downloadButton);

            HorizontalLayout actions = new HorizontalLayout(viewButton, downloadAnchor);
            Button deleteButton = new Button("Xóa", ev -> deleteFile(file));
            actions.add(deleteButton);
            if ("docx".equals(file.getFileType())) {
                Button editButton = new Button("Sửa", ev -> {
                    Dialog editDialog = new Dialog();
                    editDialog.setWidth("100%");
                    editDialog.setHeight("95%");
                    
                    editDialog.setCloseOnOutsideClick(false);
                    editDialog.setCloseOnEsc(false);
                    
                    MainView editMain = new MainView();                   
                    Button cancelButton = new Button("Hủy", event -> editDialog.close());
                                    
                    VerticalLayout dialogLayout = new VerticalLayout();
                    dialogLayout.setSizeFull();
                   
                    dialogLayout.getStyle().set("position", "relative");
                    dialogLayout.add(editMain);
                                                        
                    HorizontalLayout footer = new HorizontalLayout();
                    footer.setWidthFull();
                    
                    footer.getStyle().set("position", "absolute");
                    footer.getStyle().set("bottom", "0");
                    footer.getStyle().set("right", "0");
                    footer.getStyle().set("justify-content", "flex-end");
                    footer.add(cancelButton);
                                    
                    dialogLayout.add(footer);
                    editDialog.add(dialogLayout);
                    editDialog.open();
                });
                actions.add(editButton);
            }


            return actions;
        }).setHeader("Hành động");

        add(new H2("Danh sách file:"));
        add(fileGrid);

        loadFiles();
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
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        try {
            MongoDatabase database = mongoClient.getDatabase("BookStore");
            MongoCollection<Document> collection = database.getCollection("file-editor");

            Document doc = new Document("filename", fileName)
                    .append("fileType", fileType)
                    .append("data", new Binary(fileData));

            collection.insertOne(doc);
        } finally {
            mongoClient.close();
        }
    }

    private void loadFiles() {
        List<UploadedFile> files = new ArrayList<>();
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        try {
            MongoDatabase database = mongoClient.getDatabase("BookStore");
            MongoCollection<Document> collection = database.getCollection("file-editor");

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
        } finally {
            mongoClient.close();
        }
        fileGrid.setItems(files);
    }

    private void viewFile(UploadedFile file) {
        Notification.show("Xem file: " + file.getFileName(), 3000, Notification.Position.TOP_CENTER);
    }

    private void editFile(UploadedFile file) {
        Notification.show("Sửa file: " + file.getFileName(), 3000, Notification.Position.TOP_CENTER);
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
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
            try {
                MongoDatabase database = mongoClient.getDatabase("BookStore");
                MongoCollection<Document> collection = database.getCollection("file-editor");

                collection.deleteOne(new Document("_id", new ObjectId(file.getId())));
                Notification.show("Đã xóa file: " + file.getFileName(), 3000, Notification.Position.TOP_CENTER);
            } finally {
                mongoClient.close();
            }
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
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(file.getData());
            XWPFDocument document = new XWPFDocument(bis);
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            String docText = extractor.getText();
            extractor.close();
            document.close();

            Dialog docxDialog = new Dialog();
            docxDialog.setWidth("80%");
            docxDialog.setHeight("90%");

            // Sử dụng Paragraph để hiển thị văn bản có định dạng dòng
            Paragraph contentParagraph = new Paragraph(docText);
            contentParagraph.getElement().getStyle().set("white-space", "pre-wrap");

            docxDialog.add(contentParagraph);
            docxDialog.open();
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Lỗi khi hiển thị file DOCX", 3000, Notification.Position.TOP_CENTER);
        }
    }

    public static class UploadedFile {
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
}
