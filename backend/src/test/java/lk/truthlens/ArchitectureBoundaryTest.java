package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.nio.file.*;
import lk.truthlens.model.Submission;
import lk.truthlens.model.UserAccount;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

    @Test
    void controllersDoNotReadTheDatabaseDirectly() throws Exception {
        try (var files = Files.list(Path.of("src/main/java/lk/truthlens/controller"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("JdbcTemplate"), file.toString());
                assertFalse(source.contains("import lk.truthlens.repository."), file.toString());
            }
        }
    }

    @Test
    void servicesAndRepositoriesDoNotDependOnControllers() throws Exception {
        for (String layer : new String[] { "service", "repository" })
            try (var files = Files.list(Path.of("src/main/java/lk/truthlens", layer))) {
                for (var file : files.filter(p -> p.toString().endsWith(".java")).toList())
                    assertFalse(
                        Files.readString(file).contains("import lk.truthlens.controller."),
                        file.toString()
                    );
            }
    }

    @Test
    void persistedModelFieldsArePrivate() {
        for (Class<?> model : new Class<?>[] { UserAccount.class, Submission.class })
            for (var field : model.getDeclaredFields())
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) assertTrue(
                    Modifier.isPrivate(field.getModifiers()),
                    field.toString()
                );
    }
}
