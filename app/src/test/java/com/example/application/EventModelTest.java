package com.example.application;

import static org.junit.Assert.assertEquals;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

public class EventModelTest {

    @Test
    public void eventModel_constructorAndGettersSetters() {
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);
        Event event = new Event("id123", "Hackathon", "Descrição do Hackathon", timestamp, "18:00", "Laboratório", 30, "Tecnologia", -23.55, -46.63);

        assertEquals("id123", event.getId());
        assertEquals("Hackathon", event.getName());
        assertEquals("Descrição do Hackathon", event.getDescription());
        assertEquals(timestamp, event.getDate());
        assertEquals("18:00", event.getTime());
        assertEquals("Laboratório", event.getLocation());
        assertEquals(30, event.getAvailableSlots());
        assertEquals("Tecnologia", event.getCategory());
        assertEquals(-23.55, event.getLatitude(), 0.001);
        assertEquals(-46.63, event.getLongitude(), 0.001);
    }

    @Test
    public void eventModel_nullCoordinatesHandling() {
        Event event = new Event();
        event.setLatitude(null);
        event.setLongitude(null);

        assertEquals("Latitude nula deve retornar 0.0", 0.0, event.getLatitude(), 0.001);
        assertEquals("Longitude nula deve retornar 0.0", 0.0, event.getLongitude(), 0.001);
    }

    @Test
    public void eventModel_statusAndOwnership() {
        Event event = new Event();
        event.setCreatedBy("user_org_123");
        event.setStatus(Event.STATUS_PUBLISHED);

        assertEquals("user_org_123", event.getCreatedBy());
        assertEquals(Event.STATUS_PUBLISHED, event.getStatus());
        assertEquals(false, event.isClosed());

        event.setStatus(Event.STATUS_CLOSED);
        assertEquals(true, event.isClosed());
    }

    @Test
    public void attendeeModel_gettersAndSetters() {
        Timestamp now = Timestamp.now();
        Attendee attendee = new Attendee("sub123", "user456", "Maria Silva", "maria@test.com", now, true, now);

        assertEquals("sub123", attendee.getSubscriptionId());
        assertEquals("user456", attendee.getUserId());
        assertEquals("Maria Silva", attendee.getName());
        assertEquals("maria@test.com", attendee.getEmail());
        assertEquals(now, attendee.getSubscribedAt());
        assertEquals(true, attendee.isCheckedIn());
        assertEquals(now, attendee.getCheckedInAt());
    }
}
