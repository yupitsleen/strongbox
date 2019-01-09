package org.carlspring.strongbox.controllers.configuration;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.forms.configuration.*;
import org.carlspring.strongbox.providers.datastore.StorageProviderEnum;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.service.ProxyRepositoryConnectionPoolConfigurationService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.storage.repository.RepositoryStatusEnum;
import org.carlspring.strongbox.xml.configuration.repository.MavenRepositoryConfiguration;

import javax.inject.Inject;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.http.pool.PoolStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpServerErrorException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pablo Tirado
 */
@IntegrationTest
@ExtendWith(SpringExtension.class)
public class StoragesConfigurationControllerTestIT
        extends RestAssuredBaseTest
{

    @Inject
    private ProxyRepositoryConnectionPoolConfigurationService proxyRepositoryConnectionPoolConfigurationService;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl("/api/configuration/strongbox/storages");
    }

    static ProxyConfigurationForm createProxyConfiguration()
    {
        ProxyConfigurationForm proxyConfiguration = new ProxyConfigurationForm();
        proxyConfiguration.setHost("localhost");
        proxyConfiguration.setPort(8080);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("http");
        List<String> nonProxyHosts = Lists.newArrayList();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        return proxyConfiguration;
    }

    @Test
    public void testGetStorages()
    {
        String url = getContextBaseUrl();

        givenCustom().accept(MediaType.APPLICATION_JSON_VALUE)
                     .when()
                     .get(url)
                     .peek()
                     .then()
                     .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testGetStorage()
    {
        String url = getContextBaseUrl() + "/" + STORAGE0;

        givenCustom().accept(MediaType.APPLICATION_JSON_VALUE)
                     .when()
                     .get(url)
                     .peek()
                     .then()
                     .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testGetGroupRepository()
    {
        String url = getContextBaseUrl() + "/storage-common-proxies/group-common-proxies";

        givenCustom().accept(MediaType.APPLICATION_JSON_VALUE)
                     .when()
                     .get(url)
                     .peek()
                     .then()
                     .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testGetMavenRepository()
    {
        String url = getContextBaseUrl() + "/" + STORAGE0 + "/releases";

        givenCustom().accept(MediaType.APPLICATION_JSON_VALUE)
                     .when()
                     .get(url)
                     .peek()
                     .then()
                     .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testAddGetStorage()
    {
        String storageId = "storage1";
        String repositoryId1 = "releases-ags-1-" + System.nanoTime();
        String repositoryId2 = "releases-ags-2-" + System.nanoTime();

        StorageForm storageForm = new StorageForm();
        storageForm.setId(storageId);

        String url = getContextBaseUrl();

        logger.debug("Using storage class " + storageForm.getClass().getName());

        givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                     .accept(MediaType.APPLICATION_JSON_VALUE)
                     .body(storageForm)
                     .when()
                     .put(url)
                     .prettyPeek()
                     .then()
                     .statusCode(HttpStatus.OK.value());

        RepositoryForm repositoryForm1 = new RepositoryForm();
        repositoryForm1.setId(repositoryId1);
        repositoryForm1.setAllowsRedeployment(true);
        repositoryForm1.setSecured(true);
        repositoryForm1.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm1.setType("hosted");
        repositoryForm1.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm1.setImplementation("file-system");
        repositoryForm1.setStatus("In Service");

        RepositoryForm repositoryForm2 = new RepositoryForm();
        repositoryForm2.setId(repositoryId2);
        repositoryForm2.setAllowsForceDeletion(true);
        repositoryForm2.setTrashEnabled(true);
        repositoryForm2.setProxyConfiguration(createProxyConfiguration());
        repositoryForm2.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm2.setType("hosted");
        repositoryForm2.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm2.setImplementation("file-system");
        repositoryForm2.setStatus(RepositoryStatusEnum.IN_SERVICE.getStatus());
        repositoryForm2.setGroupRepositories(ImmutableSet.of(repositoryId1));

        addRepository(repositoryForm1, storageForm);
        addRepository(repositoryForm2, storageForm);

        Storage storage = getStorage(storageId);

        Repository repository0 = storage.getRepositories().get(repositoryId1);
        Repository repository1 = storage.getRepositories().get(repositoryId2);

        assertNotNull(storage, "Failed to get storage (" + storageId + ")!");
        assertFalse(storage.getRepositories().isEmpty(), "Failed to get storage (" + storageId + ")!");
        assertTrue(repository0.allowsRedeployment(), "Failed to get storage (" + storageId + ")!");
        assertTrue(repository0.isSecured(), "Failed to get storage (" + storageId + ")!");
        assertTrue(repository1.allowsForceDeletion(), "Failed to get storage (" + storageId + ")!");
        assertTrue(repository1.isTrashEnabled(), "Failed to get storage (" + storageId + ")!");


        assertNotNull(repository1.getProxyConfiguration().getHost(), "Failed to get storage (" + storageId + ")!");
        assertEquals("localhost", repository1.getProxyConfiguration().getHost(), "Failed to get storage (" + storageId + ")!");

        deleteRepository(storageId, repositoryId1);
        deleteRepository(storageId, repositoryId2);
    }

    @Test
    public void testAddGetRepository()
    {
        String repositoryId1 = "releases-agr-1-" + System.nanoTime();
        String repositoryId2 = "releases-agr-2-" + System.nanoTime();

        StorageForm storageForm = new StorageForm();
        storageForm.setId(STORAGE0);

        MavenRepositoryConfigurationForm mavenRepositoryConfigurationForm = new MavenRepositoryConfigurationForm();
        mavenRepositoryConfigurationForm.setIndexingEnabled(true);
        mavenRepositoryConfigurationForm.setIndexingClassNamesEnabled(false);

        RepositoryForm repositoryForm1 = new RepositoryForm();
        repositoryForm1.setId(repositoryId1);
        repositoryForm1.setAllowsRedeployment(true);
        repositoryForm1.setSecured(true);
        repositoryForm1.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm1.setRepositoryConfiguration(mavenRepositoryConfigurationForm);
        repositoryForm1.setType("hosted");
        repositoryForm1.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm1.setImplementation("file-system");
        repositoryForm1.setStatus("In Service");

        Integer maxConnectionsRepository2 = 30;

        RepositoryForm repositoryForm2 = new RepositoryForm();
        repositoryForm2.setId(repositoryId2);
        repositoryForm2.setAllowsForceDeletion(true);
        repositoryForm2.setTrashEnabled(true);
        repositoryForm2.setProxyConfiguration(createProxyConfiguration());
        repositoryForm2.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm2.setType("proxy");
        repositoryForm2.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm2.setImplementation("file-system");
        repositoryForm2.setStatus(RepositoryStatusEnum.IN_SERVICE.getStatus());
        repositoryForm2.setGroupRepositories(ImmutableSet.of(repositoryId1));
        repositoryForm2.setHttpConnectionPool(maxConnectionsRepository2);

        String secondRepositoryUrl = "http://abc.def";

        RemoteRepositoryForm remoteRepositoryForm = new RemoteRepositoryForm();
        remoteRepositoryForm.setUrl(secondRepositoryUrl);
        remoteRepositoryForm.setCheckIntervalSeconds(1000);

        repositoryForm2.setRemoteRepository(remoteRepositoryForm);

        addRepository(repositoryForm1, storageForm);
        addRepository(repositoryForm2, storageForm);

        Storage storage = getStorage(STORAGE0);
        Repository repository0 = storage.getRepositories().get(repositoryForm1.getId());
        Repository repository1 = storage.getRepositories().get(repositoryForm2.getId());

        assertNotNull(storage, "Failed to get storage (" + STORAGE0 + ")!");
        assertFalse(storage.getRepositories().isEmpty(), "Failed to get storage (" + STORAGE0 + ")!");
        assertTrue(repository0.allowsRedeployment(), "Failed to get storage (" + STORAGE0 + ")!");
        assertTrue(repository0.isSecured(), "Failed to get storage (" + STORAGE0 + ")!");
        assertNotNull(repository0.getRepositoryConfiguration(), "Failed to get storage (" + STORAGE0 + ")!");
        assertTrue(repository0.getRepositoryConfiguration() instanceof MavenRepositoryConfiguration,
                   "Failed to get storage (" + STORAGE0 + ")!");
        assertTrue(((MavenRepositoryConfiguration) repository0.getRepositoryConfiguration()).isIndexingEnabled(),
                   "Failed to get storage (" + STORAGE0 + ")!");
        assertFalse(
                ((MavenRepositoryConfiguration) repository0.getRepositoryConfiguration()).isIndexingClassNamesEnabled(),
                "Failed to get storage (" + STORAGE0 + ")!");

        assertTrue(repository1.allowsForceDeletion(), "Failed to get storage (" + STORAGE0 + ")!");
        assertTrue(repository1.isTrashEnabled(), "Failed to get storage (" + STORAGE0 + ")!");
        assertNotNull(repository1.getProxyConfiguration().getHost(), "Failed to get storage (" + STORAGE0 + ")!");
        assertEquals("localhost",
                     repository1.getProxyConfiguration().getHost(),
                     "Failed to get storage (" + STORAGE0 + ")!");

        PoolStats poolStatsRepository2 = proxyRepositoryConnectionPoolConfigurationService.getPoolStats(
                secondRepositoryUrl);

        assertEquals(maxConnectionsRepository2.intValue(),
                     poolStatsRepository2.getMax(),
                     "Max connections for proxy repository not set accordingly!");

        deleteRepository(storageForm.getId(), repositoryForm1.getId());
        deleteRepository(storageForm.getId(), repositoryForm2.getId());
    }

    private Storage getStorage(String storageId)
    {
        String url = getContextBaseUrl() + "/" + storageId;

        return givenCustom().accept(MediaType.APPLICATION_JSON_VALUE)
                            .when()
                            .get(url)
                            .prettyPeek()
                            .as(Storage.class);
    }

    private int addRepository(RepositoryForm repository,
                              final StorageForm storage)
    {
        String url;
        if (repository == null)
        {
            logger.error("Unable to add non-existing repository.");

            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                               "Unable to add non-existing repository.");
        }

        if (storage == null)
        {
            logger.error("Storage associated with repo is null.");

            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                               "Storage associated with repo is null.");
        }

        try
        {
            url = getContextBaseUrl() + "/" + storage.getId() + "/" + repository.getId();
        }
        catch (RuntimeException e)
        {
            logger.error("Unable to create web resource.", e);

            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        int status = givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                                  .accept(MediaType.APPLICATION_JSON_VALUE)
                                  .body(repository)
                                  .when()
                                  .put(url)
                                  .then()
                                  .statusCode(HttpStatus.OK.value())
                                  .extract()
                                  .statusCode();

        return status;
    }

    private void deleteRepository(String storageId,
                                  String repositoryId)
    {
        String url = getContextBaseUrl() + String.format("/%s/%s", storageId, repositoryId);

        givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                     .accept(MediaType.APPLICATION_JSON_VALUE)
                     .when()
                     .delete(url)
                     .then()
                     .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testCreateAndDeleteStorage()
    {
        String storageId = "storage-cads-" + System.nanoTime();
        String repositoryId1 = "releases-cads-1-" + System.nanoTime();
        String repositoryId2 = "releases-cads-2-" + System.nanoTime();

        StorageForm storageForm = new StorageForm();
        storageForm.setId(storageId);

        String url = getContextBaseUrl();

        givenCustom().contentType(MediaType.APPLICATION_JSON_VALUE)
                     .accept(MediaType.APPLICATION_JSON_VALUE)
                     .body(storageForm)
                     .when()
                     .put(url)
                     .peek() // Use peek() to print the output
                     .then()
                     .statusCode(HttpStatus.OK.value());

        RepositoryForm repositoryForm1 = new RepositoryForm();
        repositoryForm1.setId(repositoryId1);
        repositoryForm1.setAllowsRedeployment(true);
        repositoryForm1.setSecured(true);
        repositoryForm1.setProxyConfiguration(createProxyConfiguration());
        repositoryForm1.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm1.setType("hosted");
        repositoryForm1.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm1.setImplementation(StorageProviderEnum.FILESYSTEM.describe());
        repositoryForm1.setStatus(RepositoryStatusEnum.IN_SERVICE.getStatus());

        RepositoryForm repositoryForm2 = new RepositoryForm();
        repositoryForm2.setId(repositoryId2);
        repositoryForm2.setAllowsRedeployment(true);
        repositoryForm2.setSecured(true);
        repositoryForm2.setLayout(Maven2LayoutProvider.ALIAS);
        repositoryForm2.setType("hosted");
        repositoryForm2.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repositoryForm2.setImplementation("file-system");
        repositoryForm2.setStatus(RepositoryStatusEnum.IN_SERVICE.getStatus());
        repositoryForm2.setProxyConfiguration(createProxyConfiguration());

        addRepository(repositoryForm1, storageForm);
        addRepository(repositoryForm2, storageForm);

        Storage storage = getStorage(storageId);

        assertNotNull(storage, "Failed to get storage (" + storageId + ")!");
        assertFalse(storage.getRepositories().isEmpty(), "Failed to get storage (" + storageId + ")!");

        url = getContextBaseUrl() + "/" + storageId + "/" + repositoryId1;

        logger.debug(url);

        givenCustom().contentType(MediaType.TEXT_PLAIN_VALUE)
                     .accept(MediaType.TEXT_PLAIN_VALUE)
                     .param("force", true)
                     .when()
                     .delete(url)
                     .peek() // Use peek() to print the output
                     .then()
                     .statusCode(HttpStatus.OK.value());

        url = getContextBaseUrl() + "/" + storageId + "/" + repositoryId1;

        logger.debug(storageId);
        logger.debug(repositoryId1);

        givenCustom().contentType(MediaType.TEXT_PLAIN_VALUE)
                     .when()
                     .get(url)
                     .peek() // Use peek() to print the output
                     .then()
                     .statusCode(HttpStatus.NOT_FOUND.value());

        deleteRepository(storageId, repositoryId2);
    }

}
