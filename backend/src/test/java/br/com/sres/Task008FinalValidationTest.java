package br.com.sres;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Task008FinalValidationTest {
    private static final Path BACKEND = Path.of(".").toAbsolutePath().normalize();
    private static final Path ROOT = BACKEND.getParent();

    @Test
    void deliveryImageHasReproducibleNonRootPackagingContract() throws Exception {
        Path dockerfile = BACKEND.resolve("Dockerfile");
        Path dockerignore = ROOT.resolve(".dockerignore");

        assertThat(dockerfile).as("TASK-008 Dockerfile").isRegularFile();
        String content = Files.readString(dockerfile);
        assertThat(content).contains("AS build").contains("FROM eclipse-temurin:21-jre").contains("USER sres");
        assertThat(content).doesNotContain("PASSWORD=").doesNotContain("TOKEN=").doesNotContain(".env");
        assertThat(dockerignore).as("monorepo Docker build context exclusions").isRegularFile();
        assertThat(Files.readString(dockerignore)).contains(".git").contains("target/").contains(".env");
    }

    @Test
    void rootReadmeDocumentsTheOfficialContainerCommand() throws Exception {
        String readme = Files.readString(ROOT.resolve("README.md"));
        assertThat(readme).contains("backend/Dockerfile").contains("docker build").contains("docker run");
    }
}
