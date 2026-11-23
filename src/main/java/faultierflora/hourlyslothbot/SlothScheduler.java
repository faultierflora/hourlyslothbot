package faultierflora.hourlyslothbot;

import faultierflora.hourlyslothbot.mastodon.StatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import social.bigbone.api.entity.Status;
import social.bigbone.api.exception.BigBoneRequestException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The SlothScheduler is responsible for scheduling toots with a new sloth picture.
 *
 * @author faultierflora
 */
@Service
public class SlothScheduler {

    /**
     * The {@link org.slf4j.Logger Logger} for this class.
     * The logger is used for logging as configured for the application.
     *
     * @see "src/main/ressources/logback.xml"
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SlothScheduler.class);

    /**
     * The {@link faultierflora.hourlyslothbot.mastodon.StatusRepository StatusRepository} of this class.
     * The repository is used to create new toots at mastodon.
     */
    private final StatusRepository repo;

    /**
     * The directory of the sloth images and descriptions.
     */
    private final SlothDirectory slothDirectory;

    /**
     * The random generator to choose the next sloth.
     */
    private final Random random = new Random();

    /**
     * The sole constructor for this class.
     * The needed classes are {@link org.springframework.beans.factory.annotation.Autowired autowired} by Spring.
     *
     * @param statusRepository The  {@link faultierflora.hourlyslothbot.mastodon.StatusRepository StatusRepository} for mastodon.
     * @param slothDirectory   The directory of the sloth images and descriptions.
     */
    public SlothScheduler(@Autowired StatusRepository statusRepository, @Autowired SlothDirectory slothDirectory) {
        this.repo = statusRepository;
        this.slothDirectory = slothDirectory;
    }

    /**
     * Schedules the posting of new sloth pictures via mastodon toots.
     * postSloth will be run according to the {@link org.springframework.scheduling.annotation.Scheduled Scheduled annotation}.
     * It generates a new post and creates a new toot on mastodon via the {@link faultierflora.hourlyslothbot.mastodon.StatusRepository StatusRepository}.
     * <p>
     * Exceptions are logged as errors and suppressed. No further error handling is applied.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void postSloth() throws FileNotFoundException {
        LOGGER.info("Going to post new sloth");
        List<File> sloths;
        try {
            sloths = slothDirectory.findSlothImages();
        } catch (IllegalStateException e) {
            LOGGER.error("Can't post a sloth: " + e.getMessage());
            return;
        }
        if (sloths.isEmpty()) {
            LOGGER.error("Can't post a sloth: No sloths found in " + slothDirectory.getDirectory());
            return;
        }

        File image = sloths.get(random.nextInt(sloths.size()));
        Map<String, Object> yaml = loadYaml(slothDirectory.descriptionOf(image));
        LOGGER.info("Posting sloth number " + slothDirectory.nameOf(image));
        try {
            Status status = this.repo.postStatus(yaml, image);
            LOGGER.info("Sloth successfully postet with id " + status.getId());
        } catch (BigBoneRequestException e) {
            LOGGER.error("An error occurred. Status code: " + e.getHttpStatusCode() + "; message: " + e.getMessage() + "; cause:" + e.getCause());
        }
    }

    private Map<String, Object> loadYaml(final File file) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        return yaml.load(new FileInputStream(file));
    }
}
