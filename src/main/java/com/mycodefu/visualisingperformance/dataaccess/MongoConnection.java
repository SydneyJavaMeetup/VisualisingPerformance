package com.mycodefu.visualisingperformance.dataaccess;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import org.apache.commons.lang3.StringUtils;

public class MongoConnection {
    private static MongoClient mongoClient = null;

    public static final String DATABASE_NAME = "SydneyJavaMeetup";
    public static final String STATS_COLLECTION_NAME = "PerfStats";

    public static MongoClient get() {
        if (mongoClient == null) {
            synchronized (MongoConnection.class) {
                if (mongoClient == null) {
                    String connectionString = getEnvSetting("MONGO_CONNECTION_STRING", "mongodb://localhost:27017/" + DATABASE_NAME);
                    mongoClient = MongoClients.create(connectionString);
                }
            }
        }
        return mongoClient;
    }

    private static String getEnvSetting(String name, String defaultValue) {
        return StringUtils.isNotBlank(System.getenv(name)) ? System.getenv(name) : defaultValue;
    }
}
