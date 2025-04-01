package com.example.application.views.docxEditor.config;



import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBConfig {
    
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "BookStore";
    private static final String COLLECTION_NAME = "file-editor";
    
    
    private static final MongoClient mongoClient = MongoClients.create(CONNECTION_STRING);

    
    public static MongoDatabase getDatabase() {
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    
    public static MongoCollection<Document> getFileEditorCollection() {
        return getDatabase().getCollection(COLLECTION_NAME);
    }
    
   
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}

