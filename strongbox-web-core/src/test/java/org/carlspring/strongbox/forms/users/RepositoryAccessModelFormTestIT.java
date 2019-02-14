package org.carlspring.strongbox.forms.users;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.users.domain.Privileges;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pablo Tirado
 */
@IntegrationTest
@SpringBootTest
public class RepositoryAccessModelFormTestIT
        extends RestAssuredBaseTest
{

    private static final String STORAGE_ID_VALID = "storage0";
    private static final String REPOSITORY_ID_VALID = "releases";
    private Collection<String> privileges;

    @Inject
    private Validator validator;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();

        privileges = Lists.newArrayList(Privileges.r());
    }

    @Test
    void testRepositoryAccessModelFormValid()
    {
        // given
        RepositoryAccessModelForm repositoryAccessModelForm = new RepositoryAccessModelForm();
        repositoryAccessModelForm.setStorageId(STORAGE_ID_VALID);
        repositoryAccessModelForm.setRepositoryId(REPOSITORY_ID_VALID);
        repositoryAccessModelForm.setPrivileges(privileges);

        // when
        Set<ConstraintViolation<RepositoryAccessModelForm>> violations = validator.validate(repositoryAccessModelForm);

        // then
        assertTrue(violations.isEmpty(), "Violations are not empty!");
    }

    @Test
    void testRepositoryAccessModelFormInvalidEmptyStorageId()
    {
        // given
        RepositoryAccessModelForm repositoryAccessModelForm = new RepositoryAccessModelForm();
        repositoryAccessModelForm.setStorageId(StringUtils.EMPTY);
        repositoryAccessModelForm.setRepositoryId(REPOSITORY_ID_VALID);
        repositoryAccessModelForm.setPrivileges(privileges);

        // when
        Set<ConstraintViolation<RepositoryAccessModelForm>> violations = validator.validate(repositoryAccessModelForm);

        // then
        assertFalse(violations.isEmpty(), "Violations are empty!");
        assertEquals(violations.size(), 1);
        assertThat(violations).extracting("message").containsAnyOf("A storage id must be specified.");
    }

    @Test
    void testRepositoryAccessModelFormInvalidEmptyRepositoryId()
    {
        // given
        RepositoryAccessModelForm repositoryAccessModelForm = new RepositoryAccessModelForm();
        repositoryAccessModelForm.setStorageId(STORAGE_ID_VALID);
        repositoryAccessModelForm.setRepositoryId(StringUtils.EMPTY);
        repositoryAccessModelForm.setPrivileges(privileges);

        // when
        Set<ConstraintViolation<RepositoryAccessModelForm>> violations = validator.validate(repositoryAccessModelForm);

        // then
        assertFalse(violations.isEmpty(), "Violations are empty!");
        assertEquals(violations.size(), 1);
        assertThat(violations).extracting("message").containsAnyOf("A repository id must be specified.");
    }

    @Test
    void testRepositoryAccessModelFormInvalidEmptyPrivileges()
    {
        // given
        RepositoryAccessModelForm repositoryAccessModelForm = new RepositoryAccessModelForm();
        repositoryAccessModelForm.setStorageId(STORAGE_ID_VALID);
        repositoryAccessModelForm.setRepositoryId(REPOSITORY_ID_VALID);
        repositoryAccessModelForm.setPrivileges(Collections.emptyList());

        // when
        Set<ConstraintViolation<RepositoryAccessModelForm>> violations = validator.validate(repositoryAccessModelForm);

        // then
        assertFalse(violations.isEmpty(), "Violations are empty!");
        assertEquals(violations.size(), 1);
        assertThat(violations).extracting("message").containsAnyOf("A collection of privileges must be specified.");
    }
}
