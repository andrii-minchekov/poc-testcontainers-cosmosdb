/*
 * Copyright (c) 2021, ninckblokje
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ninckblokje.poc.testcontainers.cosmosdb.provisioning;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class CosmosDBProvisioner {

    private final static Logger logger = LoggerFactory.getLogger(CosmosDBProvisioner.class);

    private final CosmosAsyncClient cosmosAsyncClient;

    private final String containerName = "starships";
    private final String databaseName;

    public CosmosDBProvisioner(CosmosAsyncClient cosmosAsyncClient, @Value("${azure.cosmos.database}") String databaseName) {
        this.cosmosAsyncClient = cosmosAsyncClient;
        this.databaseName = databaseName;
    }

    @PostConstruct
    public void postContruct() {
        logger.info("Provisioning CosmosDB {}", databaseName);

        var databaseResponse = cosmosAsyncClient.createDatabaseIfNotExists(databaseName).block();
        switch (databaseResponse.getStatusCode()) {
            case 200:
                logger.info("Database {} already exists", databaseName);
                break;
            case 201:
                logger.info("Database {} created", databaseName);
                break;
            default:
                var error = String.format("Unknown response %d when creating database %s", databaseResponse.getStatusCode(), databaseName);
                logger.error(error);
                throw new RuntimeException(error);
        }

        var database = cosmosAsyncClient.getDatabase(databaseName);

        var containerProperties = new CosmosContainerProperties(containerName, "/franchise");

        var indexingPolicy = new IndexingPolicy();
        containerProperties.setIndexingPolicy(indexingPolicy);
        indexingPolicy.setIncludedPaths(List.of(
                new IncludedPath("/franchise/?"),
                new IncludedPath("/name/?"),
                new IncludedPath("/className/?")
        ));
        indexingPolicy.setExcludedPaths(List.of(
                new ExcludedPath("/\"_etag\"/?"),
                new ExcludedPath("/*")
        ));

        var containerResponse = database.createContainerIfNotExists(containerProperties, ThroughputProperties.createAutoscaledThroughput(4000)).block();
        switch (containerResponse.getStatusCode()) {
            case 200:
                logger.info("Container {} already exists", containerName);
                break;
            case 201:
                logger.info("Container {} created", containerName);
                break;
            default:
                var error = String.format("Unknown response %d when creating container %s", databaseResponse.getStatusCode(), containerName);
                logger.error(error);
                throw new RuntimeException(error);
        }
    }
}
