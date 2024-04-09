package io.intelliflow.services.helper;

import io.intelliflow.services.centralcustomexceptionhandler.CustomException;
import io.quarkus.scheduler.Scheduled;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class EventScheduleMonitor {

    @Inject
    EventQueueHandler eventQueueHandler;

    @Scheduled(every = "5s", delay = 30, delayUnit = TimeUnit.SECONDS)
    void monitor() throws IOException, CustomException {
        eventQueueHandler.processAllTheQueuedEvents();
    }

}
