# PoC Testcontainers Cosmos DB

This is a PoC project for combining [Testcontainers](https://www.testcontainers.org)with the
[Linux Cosmos DB emulation](https://docs.microsoft.com/en-us/azure/cosmos-db/linux-emulator) from Microsoft. This way
it is possible to use Cosmos DB in an integration test, where a new Cosmos DB instance is started everytime and
destroyed after the integration test has run.

The class [IntegrationTest](src/test/java/ninckblokje/poc/testcontainers/cosmosdb/it/IntegrationTest.java) contains the integration test.

I have used several workarounds, but in the end it is possible to use this combination.

## Workarounds

- Connection mode
- Random port
- SSL

### Connection mode

The connection mode in the integration test is set to `gateway`. This removes the need for additional ports.

### Random port

Testcontainers uses one or more random ports for every started container, which cannot be controlled. However Cosmos DB
needs to know in advance what the port is. In order to fix this, the following changes have been made:

- A random port number is set in a static variable in the test class
- The random port number is set as system property (for use in Spring's `application.properties`)
- The container is started with a customization for setting the port binding

````java
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
````

### SSL

!! Never do this in production !!

The Cosmos DB container generates an invalid certificate (without a CA) on every startup. Netty checks the validity of
the SSL chain, but fails since the certificate is not valid. Normally it is possible to import the certificate into
a Java keystore. I did not want to script this for every run, instead I have configured the Java Cosmos DB SDK to
use an insecure Netty SSL context using a custom implementation of Microsoft's `CosmosAutoConfiguration` class.

The custom implementation is only available during the integration test and only if the Spring profile `test` is active,
then Microsoft's implementation will not be loaded.

See [TestCosmosDBConfig](src/test/java/ninckblokje/poc/testcontainers/cosmosdb/it/support/TestCosmosDBConfig.java) and
[InsecureConfigs](src/test/java/ninckblokje/poc/testcontainers/cosmosdb/it/support/InsecureConfigs.java) for more
information.

## Documentation

- https://www.testcontainers.org/
- https://www.testcontainers.org/features/advanced_options/#customizing-the-container
- https://docs.microsoft.com/en-us/azure/cosmos-db/linux-emulator
- https://github.com/MicrosoftDocs/azure-docs/blob/master/articles/cosmos-db/sql-sdk-connection-modes.md