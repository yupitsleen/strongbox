package org.carlspring.strongbox.providers.repository;

import org.carlspring.strongbox.config.Maven2LayoutProviderTestConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Przemyslaw Fusik
 */
@ActiveProfiles({"MockedRestArtifactResolverTestConfig","test"})
@SpringBootTest
@ContextConfiguration(classes = Maven2LayoutProviderTestConfig.class)
@Execution(CONCURRENT)
public class RetryDownloadArtifactWithPermanentFailureStartingAtSomePointTest
        extends RetryDownloadArtifactTestBase
{

    private PermanentBrokenArtifactInputStream brokenArtifactInputStream;

    @BeforeEach
    public void setup()
    {
        brokenArtifactInputStream = new PermanentBrokenArtifactInputStream(jarArtifact);
        prepareArtifactResolverContext(brokenArtifactInputStream, true);
    }

    @Test
    public void whenProxyRepositoryInputStreamFailsCompletelyArtifactDownloadShouldFail()
    {
        final String storageId = "storage-common-proxies";
        final String repositoryId = "maven-central";
        final String path = getJarPath();
        final Path destinationPath = getVaultDirectoryPath()
                                             .resolve("storages")
                                             .resolve(storageId)
                                             .resolve(repositoryId)
                                             .resolve(path);

        // given
        assertFalse(Files.exists(destinationPath));


        IOException exception = assertThrows(IOException.class, () -> {
            // when
            assertStreamNotNull(storageId, repositoryId, path);
        });

        //then
        assertEquals("Connection lost.", exception.getMessage());
    }

    @Override
    protected String getArtifactVersion()
    {
        return "3.1";
    }

    static class PermanentBrokenArtifactInputStream
            extends RetryDownloadArtifactTestBase.BrokenArtifactInputStream
    {

        private int currentReadSize;

        public PermanentBrokenArtifactInputStream(final Resource jarArtifact)
        {
            super(jarArtifact);
        }

        @Override
        public int read()
                throws IOException
        {

            if (currentReadSize >= BUF_SIZE)
            {
                throw new IOException("Connection lost.");
            }

            currentReadSize++;
            return artifactInputStream.read();
        }

    }
}

