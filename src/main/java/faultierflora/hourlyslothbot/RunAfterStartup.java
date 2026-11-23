package faultierflora.hourlyslothbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Checks the sloth directory after the application started.
 *
 * @author faultierflora
 */
@Component
public class RunAfterStartup {

    /**
     * The {@link org.slf4j.Logger Logger} for this class.
     * The logger is used for logging as configured for the application.
     *
     * @see "src/main/ressources/logback.xml"
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(RunAfterStartup.class);

    /**
     * The directory of the sloth images and descriptions.
     */
    private final SlothDirectory slothDirectory;

    /**
     * The sole constructor for this class.
     *
     * @param slothDirectory The directory of the sloth images and descriptions.
     */
    public RunAfterStartup(final SlothDirectory slothDirectory) {
        this.slothDirectory = slothDirectory;
    }

    /**
     * Logs the number of sloths after the application started.
     * Fails the startup, if the sloth directory can't be read, because the bot can't post anything without it.
     *
     * @throws IllegalStateException if the sloth directory doesn't exist or can't be read.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        int numberOfSloths = slothDirectory.findSlothImages().size();
        if (numberOfSloths == 0) {
            LOGGER.warn("Bot started, but I found no sloths in " + slothDirectory.getDirectory());
        } else {
            LOGGER.info("Bot started. I have " + numberOfSloths + " sloths found in " + slothDirectory.getDirectory());
        }
    }
}
