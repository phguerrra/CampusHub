package com.example.application.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EventFirestoreIntegrationTest {

    private FirebaseFirestore database;
    private String testEventId;
    private String testUserId;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        database = FirebaseFirestore.getInstance();
        testUserId = "test_user_integration_" + System.currentTimeMillis();
        testEventId = "test_event_integration_" + System.currentTimeMillis();
    }

    @Test
    public void testCreateEvent() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Evento de Teste Integração");
        event.put("category", "Tecnologia");
        event.put("description", "Descrição do evento de teste de integração no Firestore.");
        event.put("location", "Auditório Central");
        event.put("availableSlots", 50L);
        event.put("createdBy", testUserId);
        event.put("createdAt", FieldValue.serverTimestamp());

        Task<Void> setTask = database.collection("events").document(testEventId).set(event);
        Tasks.await(setTask, 10, TimeUnit.SECONDS);

        Task<DocumentSnapshot> getTask = database.collection("events").document(testEventId).get();
        DocumentSnapshot snapshot = Tasks.await(getTask, 10, TimeUnit.SECONDS);

        assertTrue("O documento do evento deve existir no Firestore", snapshot.exists());
        assertEquals("Evento de Teste Integração", snapshot.getString("name"));
        assertEquals("Tecnologia", snapshot.getString("category"));
    }

    @Test
    public void testFetchEvents() throws Exception {
        testCreateEvent();

        Task<QuerySnapshot> queryTask = database.collection("events").get();
        QuerySnapshot querySnapshot = Tasks.await(queryTask, 10, TimeUnit.SECONDS);

        assertNotNull("QuerySnapshot não deve ser nulo", querySnapshot);
        assertFalse("Coleção de eventos não deve estar vazia", querySnapshot.isEmpty());
    }

    @Test
    public void testSubscribeToEvent() throws Exception {
        String subDocId = testEventId + "_" + testUserId;
        Map<String, Object> subData = new HashMap<>();
        subData.put("eventId", testEventId);
        subData.put("userId", testUserId);
        subData.put("subscribedAt", FieldValue.serverTimestamp());

        Task<Void> setTask = database.collection("subscriptions").document(subDocId).set(subData);
        Tasks.await(setTask, 10, TimeUnit.SECONDS);

        Task<DocumentSnapshot> getTask = database.collection("subscriptions").document(subDocId).get();
        DocumentSnapshot snapshot = Tasks.await(getTask, 10, TimeUnit.SECONDS);

        assertTrue("Inscrição deve existir no Firestore", snapshot.exists());
        assertEquals(testEventId, snapshot.getString("eventId"));
        assertEquals(testUserId, snapshot.getString("userId"));
    }

    @Test
    public void testUnsubscribeFromEvent() throws Exception {
        testSubscribeToEvent();

        String subDocId = testEventId + "_" + testUserId;
        Task<Void> deleteTask = database.collection("subscriptions").document(subDocId).delete();
        Tasks.await(deleteTask, 10, TimeUnit.SECONDS);

        Task<DocumentSnapshot> getTask = database.collection("subscriptions").document(subDocId).get();
        DocumentSnapshot snapshot = Tasks.await(getTask, 10, TimeUnit.SECONDS);

        assertFalse("Inscrição não deve mais existir no Firestore após o cancelamento", snapshot.exists());
    }

    @After
    public void tearDown() throws Exception {
        if (database != null && testEventId != null) {
            database.collection("events").document(testEventId).delete();
            database.collection("subscriptions").document(testEventId + "_" + testUserId).delete();
        }
    }
}
