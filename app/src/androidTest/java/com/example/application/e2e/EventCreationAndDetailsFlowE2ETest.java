package com.example.application.e2e;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.application.EventsActivity;
import com.example.application.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventCreationAndDetailsFlowE2ETest {

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Task<AuthResult> task = auth.signInWithEmailAndPassword("aluno@exemplo.com", "123456");
            Tasks.await(task, 10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testEventCreationAndDetailsFlow_E2E() throws Exception {
        try (ActivityScenario<EventsActivity> scenario = ActivityScenario.launch(EventsActivity.class)) {
            String uniqueEventName = "E2E Evento " + System.currentTimeMillis();

            // Confirma que a tela de eventos e a lista principal estão visíveis
            onView(withId(R.id.events_list)).check(matches(isDisplayed()));

            // Abre a tela de criação de evento
            onView(withId(R.id.create_event_button)).perform(click());

            // Preenche os campos obrigatórios do formulário de criação
            onView(withId(R.id.create_event_name_input))
                    .perform(replaceText(uniqueEventName), closeSoftKeyboard());

            onView(withId(R.id.create_event_category_input))
                    .perform(replaceText("Tecnologia"), closeSoftKeyboard());

            onView(withId(R.id.create_event_description_input))
                    .perform(replaceText("Descrição completa e válida para o teste E2E do evento."), closeSoftKeyboard());

            // Preenche Data
            onView(withId(R.id.create_event_date_input))
                    .perform(replaceText("15/10/2026"), closeSoftKeyboard());

            // Preenche Horário
            onView(withId(R.id.create_event_time_input))
                    .perform(replaceText("19:00"), closeSoftKeyboard());

            onView(withId(R.id.create_event_location_input))
                    .perform(replaceText("Auditório Principal"), closeSoftKeyboard());

            onView(withId(R.id.create_event_slots_input))
                    .perform(replaceText("50"), closeSoftKeyboard());

            // Salva o evento e retorna para a tela principal
            onView(withId(R.id.save_event_button)).perform(click());

            // Aguarda o término da geocodificação e salvamento assíncrono no Firestore
            Thread.sleep(3000);

            // Filtra a lista pelo nome único do evento criado usando a busca
            onView(withId(R.id.search_events_input))
                    .perform(replaceText(uniqueEventName), closeSoftKeyboard());

            Thread.sleep(1000);

            // Confirma que o evento criado aparece no card da lista
            onView(allOf(withId(R.id.event_name), withText(uniqueEventName))).check(matches(isDisplayed()));

            // Abre a tela de detalhes do evento recém-criado
            onView(allOf(withId(R.id.event_name), withText(uniqueEventName))).perform(click());

            Thread.sleep(2000);

            // Valida que os dados do evento aparecem na tela de detalhes
            onView(withId(R.id.event_details_name)).check(matches(withText(uniqueEventName)));
            onView(withId(R.id.event_details_location)).check(matches(withText("Auditório Principal")));
        }
    }
}
