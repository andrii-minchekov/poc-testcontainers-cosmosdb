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

package ninckblokje.poc.testcontainers.cosmosdb.it;

import com.azure.cosmos.CosmosAsyncClient;
import com.github.dockerjava.api.model.PortBinding;
import ninckblokje.poc.testcontainers.cosmosdb.PocTestcontainersCosmosdbApplication;
import ninckblokje.poc.testcontainers.cosmosdb.it.support.TestCosmosDBConfig;
import ninckblokje.poc.testcontainers.cosmosdb.model.Starship;
import ninckblokje.poc.testcontainers.cosmosdb.repository.StarshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static ninckblokje.poc.testcontainers.cosmosdb.model.Franchise.STAR_TREK;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = PocTestcontainersCosmosdbApplication.class, webEnvironment = RANDOM_PORT)
@Import(TestCosmosDBConfig.class)
@AutoConfigureWebTestClient
@Testcontainers
public class IntegrationTest {

    private static final int randomCosmosDBPort = ThreadLocalRandom.current().nextInt(20_000, 30_000);

    {
        System.setProperty("RANDOM_COSMOSDB_PORT", String.valueOf(randomCosmosDBPort));
    }

    @Container
    private static final GenericContainer cosmosDBContainer = new GenericContainer<>("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withExposedPorts(randomCosmosDBPort)
            .withEnv(Map.of(
                    "AZURE_COSMOS_EMULATOR_PARTITION_COUNT", "3",
                    "AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE", "true",
                    "AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE", "127.0.0.1",
                    "AZURE_COSMOS_EMULATOR_ARGS", String.format("-enablepreview -port=%d", randomCosmosDBPort)
            ))
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(PortBinding.parse(String.format("%d:%d", randomCosmosDBPort, randomCosmosDBPort))))
            .waitingFor(Wait.forLogMessage("Started\r\n", 1));

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Autowired
    private CosmosAsyncClient cosmosAsyncClient;
    @Autowired
    private StarshipRepository starshipRepository;
    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    public void beforeEach() {
        assertTrue(cosmosDBContainer.isRunning());
        starshipRepository.deleteAll().block();
    }

    @Test
    public void test() {
        var responseGetAll = webClient.get()
                .uri("/api/starship")
                .exchange();
        responseGetAll.expectStatus().isEqualTo(OK);
        responseGetAll.expectBodyList(Starship.class).hasSize(0);

        var responseCreate = webClient.post()
                .uri("/api/starship")
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(new Starship(STAR_TREK, "Sovereign", "U.S.S. Enterprise", "NCC-1701-E")))
                .exchange();
        responseCreate.expectStatus().isEqualTo(CREATED);
        responseCreate.expectBody(Starship.class).value(starship -> {
            assertNotNull(starship.getId());
            assertEquals(STAR_TREK, starship.getFranchise());
            assertEquals("U.S.S. Enterprise", starship.getName());
            assertEquals("Sovereign", starship.getClassName());
            assertEquals("NCC-1701-E", starship.getRegistration());
        });

        var database = cosmosAsyncClient.getDatabase(databaseName);
        var container = database.getContainer("starships");

        var starships = container.queryItems("select * from c", Starship.class).collectList().block();
        assertEquals(1, starships.size());

        var enterpriseE = starships.get(0);
        assertNotNull(enterpriseE.getId());
        assertEquals(STAR_TREK, enterpriseE.getFranchise());
        assertEquals("U.S.S. Enterprise", enterpriseE.getName());
        assertEquals("Sovereign", enterpriseE.getClassName());
        assertEquals("NCC-1701-E", enterpriseE.getRegistration());

        responseGetAll = webClient.get()
                .uri("/api/starship")
                .exchange();
        responseGetAll.expectStatus().isEqualTo(OK);
        responseGetAll.expectBodyList(Starship.class).hasSize(1).value(allStarships -> {
            var starship = allStarships.get(0);
            assertNotNull(starship.getId());
            assertEquals(STAR_TREK, starship.getFranchise());
            assertEquals("U.S.S. Enterprise", starship.getName());
            assertEquals("Sovereign", starship.getClassName());
            assertEquals("NCC-1701-E", starship.getRegistration());
        });
    }
}
