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

import java.io.InputStream;
import java.util.Map;

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
     * The sole constructor for this class.
     * The needed classes are {@link org.springframework.beans.factory.annotation.Autowired autowired} by Spring.
     *
     * @param statusRepository The  {@link faultierflora.hourlyslothbot.mastodon.StatusRepository StatusRepository} for mastodon.
     */
    public SlothScheduler(@Autowired StatusRepository statusRepository) {
        this.repo = statusRepository;
    }

    /**
     * Schedules the posting of new sloth pictures via mastodon toots.
     * postSloth will be run according to the {@link org.springframework.scheduling.annotation.Scheduled Scheduled annotation}.
     * It generates a new post and creates a new toot on mastodon via the {@link faultierflora.hourlyslothbot.mastodon.StatusRepository StatusRepository}.
     * <p>
     * Exceptions are logged as errors and suppressed. No further error handling is applied.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void postSloth() {
        LOGGER.info("Going to post new sloth");

        String yamlFilename = "sloths/00001.yaml";
        String imageFilename = "sloths/00001.jpg";

        Map<String, Object> yaml = loadYaml(yamlFilename);

        LOGGER.info("Posting " + imageFilename);
        try {
            Status status = this.repo.postStatus(yaml, imageFilename);
            LOGGER.info("Sloth successfully postet with id " + status.getId());
        } catch (BigBoneRequestException e) {
            LOGGER.error("An error occurred. Status code: " + e.getHttpStatusCode() + "; message: " + e.getMessage() + "; cause:" + e.getCause());
        }
    }

    private Map<String, Object> loadYaml(String filename) {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(filename);
        Map<String, Object> statusText = yaml.load(inputStream);
        return statusText;
    }
}
