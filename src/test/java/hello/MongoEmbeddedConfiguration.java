package hello;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.Mongo;

import cz.jirutka.spring.embedmongo.EmbeddedMongoBuilder;
import de.flapdoodle.embed.mongo.MongoImportExecutable;
import de.flapdoodle.embed.mongo.MongoImportProcess;
import de.flapdoodle.embed.mongo.MongoImportStarter;
import de.flapdoodle.embed.mongo.config.IMongoImportConfig;
import de.flapdoodle.embed.mongo.config.MongoImportConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.File;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

@Configuration
public class MongoEmbeddedConfiguration {

    private String DBNAME = "testGenerics";
    
    //NBA : this version should match with the one defined in pom.xml in the embedmongo-maven-plugin section
    private static final Version VERSION = Version.V3_0_5;
    private static final Logger L = LoggerFactory.getLogger(MongoEmbeddedConfiguration.class);

    @Bean(destroyMethod = "close")
    @Primary
    public Mongo mongo() throws IOException, InterruptedException {
        Mongo mongoClient = launchMongodAndImportData();
        return mongoClient;
    }

    private Mongo launchMongodAndImportData() throws IOException, InterruptedException {
        final int port = 37017;
        Mongo mongo = new EmbeddedMongoBuilder().version(VERSION.asInDownloadPath()).bindIp("127.0.0.1").port(port).build();
        //int port = mongo.getServerAddressList().get(0).getPort();
        setUpMongo(port);
        L.info("Running MongoDB on port " + port);
        return mongo;
    }

    private void setUpMongo(int port) throws IOException, InterruptedException {
        File folder = new File(getClass().getClassLoader().getResource("mongodata").getFile());
        MongoImportStarter instance = MongoImportStarter.getDefaultInstance();
        for(File file: folder.listFiles()) {
            startMongoImport(instance, port, DBNAME, parseFileCollectionName(file), file.getPath(), false, false).waitFor(); //both false(upsert must be false in order to avoid incosistent DBRef problems, refered https://jira.mongodb.org/browse/SERVER-14024)
        }
    }

    private String parseFileCollectionName(File jsonFile) {
        String jsonFilename = jsonFile.getName();
        String file = jsonFilename.substring(0, jsonFilename.lastIndexOf('.'));
        /*
         *  PD: LazyLoadingProxySerializer trick because we have DBRef to entities with locale ??, which is
         *  for unlocalized data. We can't create a file with ? on it, so we use @
         *  and substitute it with ?? to give a name to the collection.
         */
        return file.replace(".@@", ".??");
    }
    
    private MongoImportProcess startMongoImport(MongoImportStarter instance, int port, String dbName, String collection, String jsonFile, Boolean upsert, Boolean drop) throws UnknownHostException,
            IOException {
        IMongoImportConfig mongoImportConfig = new MongoImportConfigBuilder()
                .version(VERSION)
                .net(new Net(port, Network.localhostIsIPv6()))
                .db(dbName)
                .collection(collection)
                .upsert(upsert)
                .dropCollection(drop)
                .importFile(jsonFile)
                .jsonArray(true)
                .build();
        MongoImportExecutable mongoImportExecutable = instance.prepare(mongoImportConfig);
        MongoImportProcess mongoImport = mongoImportExecutable.start();
        return mongoImport;
    }

}