package entity;

public interface Event {
    /**
     * Returns the name of the event.
     * @return the name of the event.
     */
    String getEventName();

    /**
     * Sets the name of the event.
     * @param eventName the new name of the event.
     */
    void setEventName(String eventName);

    /**
     * Returns the time of the event.
     * @return the time of the event.
     */
    String getTime();

    /**
     * Sets the time of the event.
     * @param time the new time of the event.
     */
    void setTime(String time);

}
