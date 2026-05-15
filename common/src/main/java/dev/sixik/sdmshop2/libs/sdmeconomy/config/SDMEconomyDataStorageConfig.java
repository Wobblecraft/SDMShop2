package dev.sixik.sdmshop2.libs.sdmeconomy.config;

import net.shadowking21.shadowconfig.annotation.ConfigComment;

public class SDMEconomyDataStorageConfig {

    public StorageType type = StorageType.JSON;

    public MongoConfig mongodb = new MongoConfig();

    public enum StorageType {
        JSON,
        MONGODB,
        CUSTOM
    }

    public static class MongoConfig {

        @ConfigComment("Connection string\nLocal: mongodb://127.0.0.1:27017/?replicaSet=rs0\nCloud: mongodb+srv://user:password@cluster.mongodb.net/")
        public String uri = "mongodb://127.0.0.1:27017/?replicaSet=rs0";

        @ConfigComment("Data base name")
        public String database = "sdm_economy";

        public String accountsCollection = "accounts";

        @ConfigComment("A unique name for this server (eg: \"survival_1\", \"lobby\").\nUsed for echo protection (to prevent the server from updating itself).\nIf left blank, a random UUID will be generated.")
        public String serverName = "server_1";
    }
}
