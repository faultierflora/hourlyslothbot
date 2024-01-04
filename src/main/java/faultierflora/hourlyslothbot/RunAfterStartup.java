package faultierflora.hourlyslothbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RunAfterStartup {

    /**
     * The {@link org.slf4j.Logger Logger} for this class.
     * The logger is used for logging as configured for the application.
     *
     * @see "src/main/ressources/logback.xml"
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SlothScheduler.class);

    /**
     * The directory of the sloth images and descriptions.
     * Can be configured in the <code>application.properties</code>.
     */
    @Value("${sloths.dir}")
    private String slothDirectory;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        File slothDirectoryAsFile = new File(slothDirectory);
        long numberOfSloths = Stream.of(slothDirectoryAsFile.listFiles())
                .filter(file -> file.getName().endsWith(".jpg"))
                .count();
        LOGGER.info("Bot started. I have " + numberOfSloths + " sloths found in " + slothDirectory);
    }
}