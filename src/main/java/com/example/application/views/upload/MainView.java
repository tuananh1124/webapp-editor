package com.example.application.views.upload;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
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
import org.bson.Document;

import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.dialog.Dialog;

import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



@Tag("react-text-input")
@JsModule("/view/react-text-input.tsx")
public class MainView extends AbstractSinglePropertyField<MainView, String> {
	public MainView() {
		super("value", "", false);
	}
}
