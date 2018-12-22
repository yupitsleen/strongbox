package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.rest.common.MavenRestAssuredBaseTest;
import org.carlspring.strongbox.storage.repository.MavenRepositoryFactory;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.xml.configuration.repository.MutableMavenRepositoryConfiguration;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Martin Todorov
 * @author Alex Oreshkevich
 * @author Pablo Tirado
 */
@IntegrationTest
@ExtendWith(SpringExtension.class)
@Execution(CONCURRENT)
public class TrashControllerTest
        extends MavenRestAssuredBaseTest
{

    @Inject
    private MavenRepositoryFactory mavenRepositoryFactory;


    @BeforeAll
    public static void cleanUp()
            throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
    }

    public static Set<MutableRepository> getRepositoriesToClean()
    {
        Set<MutableRepository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE0, "tct-tfdana-releases", Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, "tct-tfdaa-releases", Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, "tct-tdaatfdaaetfrwtah-releases", Maven2LayoutProvider.ALIAS));
        repositories.add(createRepositoryMock(STORAGE0, "tct-tdaaetfrwtah-releases", Maven2LayoutProvider.ALIAS));


        return repositories;
    }

    @Test
    public void testForceDeleteArtifactNotAllowed()
            throws Exception
    {
        // Test resources initialization start:
        String repositoryId = "tct-tfdana-releases";
        String repositoryBaseDir = getRepositoryBasedir(STORAGE0, repositoryId).getAbsolutePath();

        MutableMavenRepositoryConfiguration mavenRepositoryConfiguration = new MutableMavenRepositoryConfiguration();
        mavenRepositoryConfiguration.setIndexingEnabled(false);

        MutableRepository repositoryWithTrash = mavenRepositoryFactory.createRepository(repositoryId);
        repositoryWithTrash.setAllowsForceDeletion(false);
        repositoryWithTrash.setTrashEnabled(true);
        repositoryWithTrash.setRepositoryConfiguration(mavenRepositoryConfiguration);

        createRepository(STORAGE0, repositoryWithTrash);

        generateArtifact(repositoryBaseDir, "org.carlspring.strongbox:test-artifact-to-trash:1.0");

        // Test resources initialization end.

        final String artifactPath = "org/carlspring/strongbox/test-artifact-to-trash/1.0/test-artifact-to-trash-1.0.jar";

        // Delete the artifact (this one should get placed under the .trash)
        client.delete(STORAGE0, repositoryId, artifactPath, false);

        final Path repositoryDir = Paths.get(repositoryBaseDir + "/.trash");
        final Path artifactFile = repositoryDir.resolve(artifactPath);

        logger.debug("Artifact file: " + artifactFile.toAbsolutePath());

        assertTrue(Files.exists(artifactFile),
                   "Should have moved the artifact to the trash during a force delete operation, " +
                   "when allowsForceDeletion is not enabled!");
    }

    @Test
    public void testForceDeleteArtifactAllowed()
            throws Exception
    {
        // Test resources initialization start:
        String repositoryId = "tct-tfdaa-releases";
        String repositoryBaseDir = getRepositoryBasedir(STORAGE0, repositoryId).getAbsolutePath();

        MutableMavenRepositoryConfiguration mavenRepositoryConfiguration = new MutableMavenRepositoryConfiguration();
        mavenRepositoryConfiguration.setIndexingEnabled(false);

        MutableRepository repositoryWithTrash = mavenRepositoryFactory.createRepository(repositoryId);
        repositoryWithTrash.setAllowsForceDeletion(true);
        repositoryWithTrash.setTrashEnabled(true);
        repositoryWithTrash.setRepositoryConfiguration(mavenRepositoryConfiguration);

        createRepository(STORAGE0, repositoryWithTrash);

        generateArtifact(repositoryBaseDir, "org.carlspring.strongbox:test-artifact-to-trash:1.1");

        // Test resources initialization end.

        final String artifactPath = "org/carlspring/strongbox/test-artifact-to-trash/1.1/test-artifact-to-trash-1.1.jar";

        // Delete the artifact (this one shouldn't get placed under the .trash)
        client.delete(STORAGE0, repositoryId, artifactPath, true);

        final Path repositoryTrashDir = Paths.get(equals(repositoryBaseDir)+ "/.trash");

        final Path repositoryDir = Paths.get(repositoryId);

        assertFalse(Files.exists(repositoryTrashDir.resolve(artifactPath)),
                    "Failed to delete artifact during a force delete operation!");
        assertFalse(Files.exists(repositoryDir.resolve(artifactPath)),
                    "Failed to delete artifact during a force delete operation!");
    }

    @Test
    @Execution(CONCURRENT)
    public void testDeleteArtifactAndEmptyTrashForRepositoryWithTextAcceptHeader()
            throws Exception
    {
        // Test resources initialization start:
        String repositoryId = "tct-tdaaetfrwtah-releases";
        String repositoryBaseDir = getRepositoryBasedir(STORAGE0, repositoryId).getAbsolutePath();

        MutableMavenRepositoryConfiguration mavenRepositoryConfiguration = new MutableMavenRepositoryConfiguration();
        mavenRepositoryConfiguration.setIndexingEnabled(false);

        MutableRepository repositoryWithTrash = mavenRepositoryFactory.createRepository(repositoryId);
        repositoryWithTrash.setTrashEnabled(true);
        repositoryWithTrash.setRepositoryConfiguration(mavenRepositoryConfiguration);

        createRepository(STORAGE0, repositoryWithTrash);

        generateArtifact(repositoryBaseDir, "org.carlspring.strongbox:test-artifact-to-trash:1.2");

        // Test resources initialization end.

        String url = getContextBaseUrl() + "/api/trash/" + STORAGE0 + "/" + repositoryId;

        given().header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
               .when()
               .delete(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(equalTo("The trash for '" + STORAGE0 + ":" + repositoryId + "' was removed successfully."));

        Path pathInTrash = Paths.get(repositoryBaseDir + "/.trash/" +
                                     "org/carlspring/strongbox/test-artifact-to-trash/1.2/" +
                                     "test-artifact-to-trash-1.2.jar");

        assertFalse(Files.exists(pathInTrash),
                    "Failed to empty trash for repository '" + repositoryId + "'!");
    }

    @Test
    @Execution(CONCURRENT)
    public void testDeleteArtifactAndEmptyTrashForRepositoryWithJsonAcceptHeader()
            throws Exception
    {
        // Test resources initialization start:
        String repositoryId = "tct-tdaaetfrwtah-releases";
        String repositoryBaseDir = getRepositoryBasedir(STORAGE0, repositoryId).getAbsolutePath();

        MutableMavenRepositoryConfiguration mavenRepositoryConfiguration = new MutableMavenRepositoryConfiguration();
        mavenRepositoryConfiguration.setIndexingEnabled(false);

        MutableRepository repositoryWithTrash = mavenRepositoryFactory.createRepository(repositoryId);
        repositoryWithTrash.setTrashEnabled(true);
        repositoryWithTrash.setRepositoryConfiguration(mavenRepositoryConfiguration);

        createRepository(STORAGE0, repositoryWithTrash);

        generateArtifact(repositoryBaseDir, "org.carlspring.strongbox:test-artifact-to-trash:1.3");

        // Test resources initialization end.

        String url = getContextBaseUrl() + "/api/trash/" + STORAGE0 + "/" + repositoryId;

        given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .delete(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("message", equalTo(
                       "The trash for '" + STORAGE0 + ":" + repositoryId + "' was removed successfully."));

        Path pathInTrash = Paths.get(repositoryBaseDir + "/.trash/" +
                                     "org/carlspring/strongbox/test-artifact-to-trash/1.3/" +
                                     "test-artifact-to-trash-1.3.jar");

        assertFalse(Files.exists(pathInTrash),
                    "Failed to empty trash for repository '" + repositoryId + "'!");
    }

}
