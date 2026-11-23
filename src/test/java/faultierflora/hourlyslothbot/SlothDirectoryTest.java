package faultierflora.hourlyslothbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlothDirectoryTest {

    @TempDir
    Path directory;

    @Test
    void findSlothImagesShouldReturnOnlyJpgFilesSortedByName() throws IOException {
        Files.createFile(directory.resolve("00002.jpg"));
        Files.createFile(directory.resolve("00002.yaml"));
        Files.createFile(directory.resolve("00001.jpg"));
        Files.createFile(directory.resolve("00001.yaml"));
        Files.createFile(directory.resolve("README.md"));
        Files.createDirectory(directory.resolve("subdirectory.jpg"));

        assertThat(new SlothDirectory(directory.toString()).findSlothImages())
                .extracting(File::getName)
                .containsExactly("00001.jpg", "00002.jpg");
    }

    @Test
    void findSlothImagesShouldAcceptGapsInTheNumbering() throws IOException {
        Files.createFile(directory.resolve("00001.jpg"));
        Files.createFile(directory.resolve("00003.jpg"));
        Files.createFile(directory.resolve("00010.jpg"));

        assertThat(new SlothDirectory(directory.toString()).findSlothImages())
                .extracting(File::getName)
                .containsExactly("00001.jpg", "00003.jpg", "00010.jpg");
    }

    @Test
    void findSlothImagesShouldReturnEmptyListForEmptyDirectory() {
        assertThat(new SlothDirectory(directory.toString()).findSlothImages()).isEmpty();
    }

    @Test
    void findSlothImagesShouldThrowIfDirectoryDoesNotExist() {
        Path missing = directory.resolve("missing");

        assertThatThrownBy(() -> new SlothDirectory(missing.toString()).findSlothImages())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sloth directory " + missing + " doesn't exist or can't be read");
    }

    @Test
    void findSlothImagesShouldThrowIfPathIsAFile() throws IOException {
        Path file = Files.createFile(directory.resolve("00001.jpg"));

        assertThatThrownBy(() -> new SlothDirectory(file.toString()).findSlothImages())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(file.toString());
    }

    @Test
    void descriptionOfShouldReturnYamlWithSameName() {
        SlothDirectory slothDirectory = new SlothDirectory(directory.toString());
        File image = directory.resolve("00042.jpg").toFile();

        assertThat(slothDirectory.descriptionOf(image)).isEqualTo(directory.resolve("00042.yaml").toFile());
    }

    @Test
    void nameOfShouldReturnFileNameWithoutExtension() {
        SlothDirectory slothDirectory = new SlothDirectory(directory.toString());

        assertThat(slothDirectory.nameOf(directory.resolve("00042.jpg").toFile())).isEqualTo("00042");
    }

    @Test
    void getDirectoryShouldReturnConfiguredDirectory() {
        assertThat(new SlothDirectory(directory.toString()).getDirectory()).isEqualTo(directory.toFile());
    }
}
