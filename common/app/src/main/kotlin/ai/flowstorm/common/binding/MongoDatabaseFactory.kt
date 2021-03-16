package ai.flowstorm.common.binding

import ai.flowstorm.common.config.Config
import ai.flowstorm.common.dsuffix
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import org.glassfish.hk2.api.Factory
import org.litote.kmongo.KMongo
import javax.inject.Inject

class MongoDatabaseFactory : Factory<MongoDatabase> {

    @Inject
    lateinit var config: Config

    override fun provide(): MongoDatabase = KMongo.createClient(ConnectionString(config["database.url"]))
        .getDatabase(config["name"] + "-" + config.dsuffix)

    override fun dispose(instance: MongoDatabase) {}
}