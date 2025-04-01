package com.example.application.views.docxEditor.component;

import java.util.Base64;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

import com.example.application.views.docxEditor.config.MongoDBConfig;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;



public class DocumentEditor extends VerticalLayout {

    private String base64Docx;
    private String fileId; 

    public DocumentEditor(Runnable backAction, String base64Docx, String fileId) {
        this.base64Docx = base64Docx;
        this.fileId = fileId;

        HorizontalLayout hLayout = new HorizontalLayout();
        Button backButton = new Button("Quay lại", e -> {
            if (backAction != null) {
                backAction.run();
            }
        });
        Button saveButton = new Button("Lưu", e -> {
            UI.getCurrent().getPage().executeJs("return window.getUpdatedFileContent && window.getUpdatedFileContent()")
                .then(String.class, base64Content -> {
                    if (base64Content == null) {
                        Notification.show("Không lấy được dữ liệu từ trình soạn thảo!", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }
                 
                    String base64Data = base64Content.contains(",")
                            ? base64Content.substring(base64Content.indexOf(",") + 1)
                            : base64Content;
                    byte[] updatedData = Base64.getDecoder().decode(base64Data);

                    if (fileId != null && !fileId.isEmpty()) {
                        updateFileInMongo(fileId, updatedData);
                    } else {
                        Notification.show("File ID không xác định!", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }

                    Notification.show("Lưu file thành công!", 3000, Notification.Position.TOP_CENTER);
                    if (backAction != null) {
                        backAction.run();
                    }
                });
        });

        hLayout.add(saveButton, backButton);
        add(hLayout);
        add(new H2("Document Editor"));

        Div editorContainer = new Div();
        Element editorElement = new Element("react-text-input");
        editorElement.setAttribute("fileContent", this.base64Docx);
        editorContainer.getElement().appendChild(editorElement);
        add(editorContainer);
    }

    private void updateFileInMongo(String fileId, byte[] fileData) {      
        var collection = MongoDBConfig.getFileEditorCollection();
        Document filter = new Document("_id", new ObjectId(fileId));
        Document update = new Document("$set", new Document("data", new Binary(fileData)));
        collection.updateOne(filter, update);
    }
}
