package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.config.Maven2LayoutProviderCronTasksTestConfig;
import org.carlspring.strongbox.data.CacheManagerTestExecutionListener;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.providers.repository.ProxyRepositoryProvider;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.time.DateUtils;
import org.codehaus.plexus.util.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Przemyslaw Fusik
 */
@ContextConfiguration(classes = Maven2LayoutProviderCronTasksTestConfig.class)
@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestExecutionListeners(listeners = { CacheManagerTestExecutionListener.class }, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class CleanupExpiredArtifactsFromProxyRepositoriesCronJobTestIT
        extends BaseCronJobWithMavenIndexingTestCase
{

    @Inject
    private RepositoryPathResolver repositoryPathResolver;

    @Inject
    private ProxyRepositoryProvider proxyRepositoryProvider;

    @Inject
    private ArtifactEntryService artifactEntryService;

    private String storageId = "storage-common-proxies";

    private String repositoryId = "maven-central";

    private String path = "org/carlspring/properties-injector/1.5/properties-injector-1.5.jar";

    @Override
    @BeforeEach
    public void init(TestInfo testInfo)
            throws Exception
    {
        super.init(testInfo);
    }

    @BeforeEach
    @AfterEach
    public void cleanup()
            throws Exception
    {
        deleteDirectoryRelativeToVaultDirectory(
                "storages/storage-common-proxies/maven-central/org/carlspring/properties-injector/1.5");

        artifactEntryService.delete(
                artifactEntryService.findArtifactList(storageId,
                                                      repositoryId,
                                                      ImmutableMap.of("groupId", "org.carlspring",
                                                                      "artifactId", "properties-injector",
                                                                      "version", "1.5"),
                                                      true));
    }

    @Test
    public void expiredArtifactsCleanupCronJobShouldCleanupDatabaseAndStorage()
            throws Exception
    {
        final String storageId = "storage-common-proxies";
        final String repositoryId = "maven-central";
        final String path = "org/carlspring/properties-injector/1.5/properties-injector-1.5.jar";

        Optional<ArtifactEntry> artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                                                 repositoryId,
                                                                                                                 path));
        assertThat(artifactEntryOptional, CoreMatchers.equalTo(Optional.empty()));

        Path repositoryPath = proxyRepositoryProvider.fetchPath(repositoryPathResolver.resolve(storageId, repositoryId,
                                                                                               path));
        try (final InputStream ignored = proxyRepositoryProvider.getInputStream(repositoryPath))
        {
        }

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId, repositoryId,
                                                                                         path));
        ArtifactEntry artifactEntry = artifactEntryOptional.orElse(null);
        assertThat(artifactEntry, CoreMatchers.notNullValue());
        assertThat(artifactEntry.getLastUpdated(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getLastUsed(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getSizeInBytes(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getSizeInBytes(), Matchers.greaterThan(0l));

        artifactEntry.setLastUsed(
                DateUtils.addDays(artifactEntry.getLastUsed(), -10));
        final Long sizeInBytes = artifactEntry.getSizeInBytes();

        artifactEntryService.save(artifactEntry);

        final String jobName = expectedJobName;
        jobManager.registerExecutionListener(jobName, (jobName1, statusExecuted) ->
        {
            if (jobName1.equals(jobName) && statusExecuted)
            {
                Optional<ArtifactEntry> optionalArtifactEntryFromDb = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                                                               repositoryId,
                                                                                                                               path));
                assertThat(optionalArtifactEntryFromDb, CoreMatchers.equalTo(Optional.empty()));

                final Storage storage = getConfiguration().getStorage(artifactEntry.getStorageId());
                final Repository repository = storage.getRepository(artifactEntry.getRepositoryId());
                final LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());

                try
                {
                    assertFalse(RepositoryFiles.artifactExists(repositoryPathResolver.resolve(repository, path)));
                    assertTrue(RepositoryFiles.artifactExists(repositoryPathResolver.resolve(repository,
                                                                                          StringUtils.replace(path,
                                                                                                              "1.5/properties-injector-1.5.jar",
                                                                                                              "maven-metadata.xml"))));
                }
                catch (IOException e)
                {
                    throw new UndeclaredThrowableException(e);
                }

            }
        });

        addCronJobConfig(jobName, CleanupExpiredArtifactsFromProxyRepositoriesCronJob.class, storageId, repositoryId,
                         properties ->
                         {
                             properties.put("lastAccessedTimeInDays", "5");
                             properties.put("minSizeInBytes", Long.valueOf(sizeInBytes - 1).toString());
                         });

        await().atMost(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilTrue(receivedExpectedEvent());
    }

}
