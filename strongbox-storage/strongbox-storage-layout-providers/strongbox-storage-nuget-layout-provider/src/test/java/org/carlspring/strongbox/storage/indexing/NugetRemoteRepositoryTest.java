package org.carlspring.strongbox.storage.indexing;

import org.carlspring.strongbox.artifact.coordinates.NugetArtifactCoordinates;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.config.NugetLayoutProviderTestConfig;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.RemoteArtifactEntry;
import org.carlspring.strongbox.nuget.NugetSearchRequest;
import org.carlspring.strongbox.providers.layout.NugetLayoutProvider;
import org.carlspring.strongbox.repository.NugetRepositoryFeatures;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.storage.repository.NugetRepositoryFactory;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.remote.MutableRemoteRepository;
import org.carlspring.strongbox.testing.TestCaseWithRepository;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Sergey Bespalov
 *
 */
@ContextConfiguration(classes = NugetLayoutProviderTestConfig.class)
@SpringBootTest
@ActiveProfiles(profiles = "test")
@Execution(CONCURRENT)
public class NugetRemoteRepositoryTest
        extends TestCaseWithRepository
{

    private static final String NUGET_COMMON_STORAGE = "storage-nuget";

    private static final String REPOSITORY_PROXY = "nrrt-proxy";

    @Inject
    private ConfigurationManager configurationManager;

    @Inject
    private RepositoryManagementService repositoryManagementService;

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private NugetRepositoryFeatures features;

    @Inject
    private NugetRepositoryFactory nugetRepositoryFactory;

    @BeforeAll
    public static void cleanUp()
        throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    public static Set<MutableRepository> getRepositoriesToClean()
    {
        Set<MutableRepository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(NUGET_COMMON_STORAGE, REPOSITORY_PROXY, NugetLayoutProvider.ALIAS));

        return repositories;
    }

    @BeforeEach
    public void initialize()
        throws Exception
    {
        MutableRepository repository = nugetRepositoryFactory.createRepository(REPOSITORY_PROXY);
        repository.setType("proxy");
        repository.setRemoteRepository(new MutableRemoteRepository());
        repository.getRemoteRepository().setUrl("https://www.nuget.org/api/v2");

        configurationManagementService.saveRepository(NUGET_COMMON_STORAGE, repository);
        repositoryManagementService.createRepository(NUGET_COMMON_STORAGE, repository.getId());
    }

    @AfterEach
    public void removeRepositories()
        throws IOException,
        JAXBException
    {
        for (MutableRepository repository : getRepositoriesToClean())
        {
            configurationManagementService.removeRepository(repository.getStorage().getId(), repository.getId());
        }
    }

    @Test
    public void testRepositoryIndexFetching()
        throws ArtifactTransportException,
        IOException
    {
        Storage storage = configurationManager.getConfiguration().getStorage(NUGET_COMMON_STORAGE);
        Repository repository = storage.getRepository(REPOSITORY_PROXY);

        NugetSearchRequest nugetSearchRequest = new NugetSearchRequest();
        nugetSearchRequest.setFilter(String.format("Id eq '%s'", "NHibernate"));
        
        features.downloadRemoteFeed(storage.getId(),
                                    repository.getId(),
                                    nugetSearchRequest);

        NugetArtifactCoordinates c = new NugetArtifactCoordinates("NHibernate", "4.0.4.4000", "nupkg");
        Optional<ArtifactEntry> artifactEntry = Optional.ofNullable(artifactEntryService.findOneArtifact(NUGET_COMMON_STORAGE,
                                                                                                         REPOSITORY_PROXY,
                                                                                                         c.toPath()));

        assertTrue(artifactEntry.isPresent());
        assertFalse(((RemoteArtifactEntry) artifactEntry.get()).getIsCached());
    }

}
