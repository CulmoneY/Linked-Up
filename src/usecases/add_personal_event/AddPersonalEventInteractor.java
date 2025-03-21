package usecases.add_personal_event;
import entity.Event;
import entity.EventFactory;
import entity.Group;
import entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddPersonalEventInteractor implements AddPersonalEventInputBoundary {
    private AddPersonalEventDataAccessInterface dataAccess;
    private AddPersonalEventOutputBoundary outputBoundary;
    private EventFactory eventFactory;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    public AddPersonalEventInteractor(AddPersonalEventDataAccessInterface dataAccess, AddPersonalEventOutputBoundary outputBoundary, EventFactory eventFactory) {
        this.dataAccess = dataAccess;
        this.outputBoundary = outputBoundary;
        this.eventFactory = eventFactory;
    }

    @Override
    public void executeCreate(AddPersonalEventInputData inputData) {
        if (missingFields(inputData)) {
            outputBoundary.setFailView("Fill in all Fields!");
        } else if (!validTime(inputData.getStartTime(), inputData.getEndTime())) {
            outputBoundary.setFailView("Invalid Time Format!");
        } else {
            Event event = eventFactory.create(inputData.getEventName(), parseDateTime(inputData.getStartTime())
                    ,parseDateTime(inputData.getEndTime()), false);
            dataAccess.addEvent(inputData.getUser(), event);
            for (Group group : inputData.getUser().getGroups()) {
                dataAccess.addGroupEvent(group.getName(), event);
                group.addGroupEvent(event);
            }
            AddPersonalEventOutputData outputData = new AddPersonalEventOutputData(event.getEventName(), event.getStartTime().toString(), event.getEndTime().toString());
            inputData.getUser().getUserCalendar().addEvent(event);
            outputBoundary.setPassView(outputData);

        }
    }

    private boolean missingFields(AddPersonalEventInputData inputData) {
        return inputData.getEventName().isEmpty() || inputData.getStartTime().isEmpty() || inputData.getEndTime().isEmpty();
    }

    private boolean validTime(String startTime, String endTime) {
        System.out.println("In validTime function within the interactor");
        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);
        LocalDateTime startDateTime = parseDateTime(startTime);
        LocalDateTime endDateTime = parseDateTime(endTime);

        if (startDateTime == null || endDateTime == null) {
            return false;
        }

        return startDateTime.isBefore(endDateTime);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            dateTimeStr = dateTimeStr.trim().toUpperCase();
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            System.out.println("Fails in parse");
            return null;
        }
    }
}
