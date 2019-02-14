package org.carlspring.strongbox.testing;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.providers.layout.NpmLayoutProvider;
import org.carlspring.strongbox.repository.RepositoryManagementStrategyException;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.storage.repository.NpmRepositoryFactory;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;
import org.carlspring.strongbox.storage.repository.remote.MutableRemoteRepository;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author carlspring
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
public class NpmRepositoryTestCase
        extends TestCaseWithRepositoryManagement
{

    @Inject
    NpmRepositoryFactory npmRepositoryFactory;

    @Inject
    private PropertiesBooter propertiesBooter;


    @Override
    public void createProxyRepository(String storageId,
                                      String repositoryId,
                                      String remoteRepositoryUrl)
            throws IOException,
                   JAXBException,
                   RepositoryManagementStrategyException
    {
        MutableRemoteRepository remoteRepository = new MutableRemoteRepository();
        remoteRepository.setUrl(remoteRepositoryUrl);

        MutableRepository repository = npmRepositoryFactory.createRepository(repositoryId);
        repository.setRemoteRepository(remoteRepository);
        repository.setLayout(NpmLayoutProvider.ALIAS);
        repository.setType(RepositoryTypeEnum.PROXY.getType());

        createRepository(storageId, repository);
    }

    public File getRepositoryBasedir(String storageId, String repositoryId)
    {
        return new File(propertiesBooter.getVaultDirectory() + "/storages/" + storageId + "/" + repositoryId);
    }

}
