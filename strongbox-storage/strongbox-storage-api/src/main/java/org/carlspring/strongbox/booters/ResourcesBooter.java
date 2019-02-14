package org.carlspring.strongbox.booters;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @author mtodorov
 */
public class ResourcesBooter
{

    private static final Logger logger = LoggerFactory.getLogger(ResourcesBooter.class);

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Inject
    private PropertiesBooter propertiesBooter;


    @PostConstruct
    public void execute()
            throws IOException
    {
        copyConfigurationFilesFromClasspath("etc/conf");
    }

    public Resource[] getConfigurationResourcesFromClasspath(String resourcesBasedir)
            throws IOException
    {
        return resolver.getResources("classpath*:" + resourcesBasedir + "/**/*");
    }

    public Resource[] getResourcesExistingOnClasspathOnly(Resource[] resources)
            throws IOException
    {
        List<Resource> diff = new ArrayList<>();

        for (Resource resource : resources)
        {
            logger.debug(resource.getURL().toString());
            diff.add(resource);
        }

        return diff.toArray(new Resource[diff.size()]);
    }

    public void copyConfigurationFilesFromClasspath(String resourcesBasedir)
            throws IOException
    {
        final Resource[] resources = getResourcesExistingOnClasspathOnly(
                getConfigurationResourcesFromClasspath(resourcesBasedir));

        final Path configDir = Paths.get(propertiesBooter.getHomeDirectory(), resourcesBasedir);
        if (!Files.exists(configDir))
        {
            Files.createDirectories(configDir);
        }

        for (Resource resource : resources)
        {
            Path destFile = configDir.resolve(resource.getFilename());

            if (Files.exists(destFile))
            {
                logger.info(String.format("Resource already exists, skip [%s].", destFile));
                continue;
            }

            Files.copy(resource.getInputStream(), destFile);
        }
    }

    public PathMatchingResourcePatternResolver getResolver()
    {
        return resolver;
    }

    public void setResolver(PathMatchingResourcePatternResolver resolver)
    {
        this.resolver = resolver;
    }

}
