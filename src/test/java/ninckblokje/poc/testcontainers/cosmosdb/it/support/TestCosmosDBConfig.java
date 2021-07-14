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

package ninckblokje.poc.testcontainers.cosmosdb.it.support;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.ConnectionMode;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.autoconfigure.cosmos.CosmosProperties;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// never do this in production!!
@Configuration
@EnableConfigurationProperties(CosmosProperties.class)
public class TestCosmosDBConfig extends AbstractCosmosConfiguration {

    private final CosmosProperties properties;

    public TestCosmosDBConfig(CosmosProperties properties) {
        this.properties = properties;
    }

    @Override
    protected String getDatabaseName() {
        return properties.getDatabase();
    }

    @Bean
    public AzureKeyCredential azureKeyCredential() {
        return new AzureKeyCredential(properties.getKey());
    }

    @Bean
    public CosmosClientBuilder cosmosClientBuilder(AzureKeyCredential azureKeyCredential) {
        CosmosClientBuilder cosmosClientBuilder = new CosmosClientBuilder();

        // never do this in production!!
        BridgeInternal.injectConfigs(cosmosClientBuilder, new InsecureConfigs());

        cosmosClientBuilder.credential(azureKeyCredential)
                .consistencyLevel(properties.getConsistencyLevel())
                .endpoint(properties.getUri());
        if (ConnectionMode.GATEWAY == properties.getConnectionMode()) {
            cosmosClientBuilder.gatewayMode();
        }
        return cosmosClientBuilder;
    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(properties.isPopulateQueryMetrics())
                .responseDiagnosticsProcessor(properties.getResponseDiagnosticsProcessor())
                .build();
    }
}
