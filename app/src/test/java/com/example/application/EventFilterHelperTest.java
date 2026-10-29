package com.example.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventFilterHelperTest {

    private List<Event> sampleEvents;
    private long nowTimestamp;

    @Before
    public void setUp() {
        nowTimestamp = System.currentTimeMillis();
        long futureTime = nowTimestamp + (1000L * 60 * 60 * 24); // +1 dia
        long pastTime = nowTimestamp - (1000L * 60 * 60 * 24);   // -1 dia

        sampleEvents = new ArrayList<>();
        sampleEvents.add(new Event("1", "Feira de Tecnologia", "Desc", new Timestamp(new Date(futureTime)), "19:00", "Local A", 50, "Tecnologia", -23.5, -46.6));
        sampleEvents.add(new Event("2", "Torneio de Futebol", "Desc", new Timestamp(new Date(futureTime)), "10:00", "Quadra", 20, "Esportes", 0.0, 0.0));
        sampleEvents.add(new Event("3", "Palestra Acadêmica", "Desc", new Timestamp(new Date(pastTime)), "14:00", "Auditório", 10, "Acadêmico", -23.5, -46.6));
    }

    @Test
    public void filterEvents_searchByTitleSubstring() {
        List<Event> result = EventFilterHelper.filterEvents(sampleEvents, "tecnologia", "", "", nowTimestamp, "Próximos", "Encerrados");
        assertEquals("Busca por 'tecnologia' deve retornar 1 evento", 1, result.size());
        assertEquals("Feira de Tecnologia", result.get(0).getName());
    }

    @Test
    public void filterEvents_filterByCategory() {
        List<Event> result = EventFilterHelper.filterEvents(sampleEvents, "", "Esportes", "", nowTimestamp, "Próximos", "Encerrados");
        assertEquals("Filtro por categoria 'Esportes' deve retornar 1 evento", 1, result.size());
        assertEquals("Torneio de Futebol", result.get(0).getName());
    }

    @Test
    public void filterEvents_filterByStatusUpcomingAndEnded() {
        List<Event> upcoming = EventFilterHelper.filterEvents(sampleEvents, "", "", "Próximos", nowTimestamp, "Próximos", "Encerrados");
        assertEquals("Devem existir 2 eventos próximos", 2, upcoming.size());

        List<Event> ended = EventFilterHelper.filterEvents(sampleEvents, "", "", "Encerrados", nowTimestamp, "Próximos", "Encerrados");
        assertEquals("Deve existir 1 evento encerrado", 1, ended.size());
        assertEquals("Palestra Acadêmica", ended.get(0).getName());
    }

    @Test
    public void filterEvents_noMatchesReturnsEmpty() {
        List<Event> result = EventFilterHelper.filterEvents(sampleEvents, "Inexistente", "", "", nowTimestamp, "Próximos", "Encerrados");
        assertTrue("Busca sem correspondência deve retornar lista vazia", result.isEmpty());
    }
}
