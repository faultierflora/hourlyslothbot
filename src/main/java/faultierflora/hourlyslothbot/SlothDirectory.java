package faultierflora.hourlyslothbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The directory with the sloth images and their descriptions.
 * Every sloth consists of an image <code>&lt;name&gt;.jpg</code> and a description <code>&lt;name&gt;.yaml</code>.
 * The names don't need to be consecutive.
 *
 * @author faultierflora
 */
@Component
public class SlothDirectory {

    /**
     * The file extension of the sloth images.
     */
    static final String IMAGE_EXTENSION = ".jpg";

    /**
     * The file extension of the sloth descriptions.
     */
    static final String DESCRIPTION_EXTENSION = ".yaml";

    /**
     * The directory of the sloth images and descriptions.
     */
    private final File directory;

    /**
     * The sole constructor for this class.
     *
     * @param slothDirectory The directory of the sloth images and descriptions.
     *                       Can be configured in the <code>application.properties</code>.
     */
    public SlothDirectory(@Value("${sloths.dir}") final String slothDirectory) {
        this.directory = new File(slothDirectory);
    }

    /**
     * Returns the configured directory.
     *
     * @return The directory of the sloth images and descriptions.
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Finds all sloth images in the directory, sorted by name.
     *
     * @return All sloth images. The list is empty, if the directory contains no images.
     * @throws IllegalStateException if the directory doesn't exist, is no directory or can't be read.
     */
    public List<File> findSlothImages() {
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IllegalStateException("Sloth directory " + directory + " doesn't exist or can't be read");
        }
        return Arrays.stream(files)
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(IMAGE_EXTENSION))
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    /**
     * Returns the description file belonging to a sloth image.
     *
     * @param image A sloth image of this directory.
     * @return The yaml file with the same name as the image.
     */
    public File descriptionOf(final File image) {
        return new File(image.getParentFile(), nameOf(image) + DESCRIPTION_EXTENSION);
    }

    /**
     * Returns the name of a sloth image without the file extension.
     *
     * @param image A sloth image of this directory.
     * @return The name of the sloth, e.g. <code>00001</code> for <code>00001.jpg</code>.
     */
    public String nameOf(final File image) {
        String fileName = image.getName();
        return fileName.substring(0, fileName.length() - IMAGE_EXTENSION.length());
    }
}
