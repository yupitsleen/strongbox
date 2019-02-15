package org.carlspring.strongbox.providers.search;

import org.carlspring.strongbox.config.MavenIndexerEnabledCondition;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.search.SearchRequest;
import org.carlspring.strongbox.storage.search.SearchResult;
import org.carlspring.strongbox.storage.search.SearchResults;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 */
@Conditional(MavenIndexerEnabledCondition.class)
public class MavenIndexerSearchProvider
        extends AbstractSearchProvider
{

    private static final Logger logger = LoggerFactory.getLogger(MavenIndexerSearchProvider.class);

    public static final String ALIAS = "Maven Indexer";

    @Inject
    private SearchProviderRegistry searchProviderRegistry;

    @Inject
    private RepositoryIndexManager repositoryIndexManager;

    @Inject
    private ConfigurationManager configurationManager;


    @PostConstruct
    @Override
    public void register()
    {
        searchProviderRegistry.addProvider(ALIAS, this);

        logger.info("Registered search provider '" + getClass().getCanonicalName() + "' with alias '" + ALIAS + "'.");
    }

    @Override
    public String getAlias()
    {
        return ALIAS;
    }

    @Override
    public SearchResults search(SearchRequest searchRequest)
            throws SearchException
    {
        SearchResults searchResults = new SearchResults();

        final String repositoryId = searchRequest.getRepositoryId();

        try
        {
            final Collection<Storage> storages = getConfiguration().getStorages().values();
            if (StringUtils.isNotBlank(repositoryId))
            {
                logger.debug("Repository: {}", repositoryId);

                final String storageId = searchRequest.getStorageId();
                if (StringUtils.isBlank(storageId))
                {
                    for (Storage storage : storages)
                    {
                        if (storage.containsRepository(repositoryId))
                        {
                            performSearch(searchResults, storage.getId(), repositoryId, searchRequest);
                        }
                    }
                }
                else
                {

                    performSearch(searchResults, storageId, repositoryId, searchRequest);
                }
            }
            else
            {
                for (Storage storage : storages)
                {
                    for (Repository r : storage.getRepositories().values())
                    {
                        logger.debug("Repository: {}", r.getId());

                        performSearch(searchResults, storage.getId(), r.getId(), searchRequest);
                    }
                }
            }

            logger.debug("Results: {}", searchResults.getResults().size());

            return searchResults;

        }
        catch (ParseException | IOException e)
        {
            logger.error(e.getMessage(), e);

            throw new SearchException(e.getMessage(), e);
        }
    }

    private void performSearch(final SearchResults searchResults,
                               final String storageId,
                               final String repositoryId,
                               final SearchRequest searchRequest)
            throws IOException, ParseException
    {
        final String indexType = StringUtils.defaultString(searchRequest.getOption("indexType"),
                                                           IndexTypeEnum.LOCAL.getType());

        final String contextId = storageId + ":" + repositoryId + ":" + indexType;

        final RepositoryIndexer repositoryIndexer = repositoryIndexManager.getRepositoryIndexer(contextId);
        if (repositoryIndexer != null)
        {
            final Set<SearchResult> sr = repositoryIndexer.search(searchRequest.getQuery());

            if (CollectionUtils.isNotEmpty(sr))
            {
                searchResults.getResults().addAll(sr);
            }
        }
    }

    @Override
    public boolean contains(SearchRequest searchRequest)
            throws SearchException
    {
        return !search(searchRequest).getResults().isEmpty();
    }

    public Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

}
