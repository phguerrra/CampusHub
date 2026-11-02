package com.example.application.e2e;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.application.EventsActivity;
import com.example.application.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventCreationAndDetailsFlowE2ETest {

    @Rule
    public ActivityScenarioRule<EventsActivity> activityRule =
            new ActivityScenarioRule<>(EventsActivity.class);

    @Test
    public void testEventCreationAndDetailsFlow_E2E() throws Exception {
        String uniqueEventName = "E2E Evento " + System.currentTimeMillis();

        //  Confirma que a tela de eventos e a lista principal estão visíveis
        onView(withId(R.id.events_list)).check(matches(isDisplayed()));

        //  Abre a tela de criação de evento
        onView(withId(R.id.create_event_button)).perform(click());

        //  Preenche os campos obrigatórios do formulário de criação
        onView(withId(R.id.create_event_name_input))
                .perform(replaceText(uniqueEventName), closeSoftKeyboard());

        onView(withId(R.id.create_event_category_input))
                .perform(replaceText("Tecnologia"), closeSoftKeyboard());

        onView(withId(R.id.create_event_description_input))
                .perform(replaceText("Descrição completa e válida para o teste E2E do evento."), closeSoftKeyboard());

        // Seleciona Data (abre o DatePickerDialog e confirma "OK")
        onView(withId(R.id.create_event_date_input)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        // Seleciona Horário (abre o TimePickerDialog e confirma "OK")
        onView(withId(R.id.create_event_time_input)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        onView(withId(R.id.create_event_location_input))
                .perform(replaceText("Auditório Principal"), closeSoftKeyboard());

        onView(withId(R.id.create_event_slots_input))
                .perform(replaceText("50"), closeSoftKeyboard());

        // Salva o evento e retorna para a tela principal
        onView(withId(R.id.save_event_button)).perform(click());

        // Aguarda o término da geocodificação e salvamento assíncrono no Firestore
        Thread.sleep(3000);

        //  Filtra a lista pelo nome único do evento criado usando a busca
        onView(withId(R.id.search_events_input))
                .perform(replaceText(uniqueEventName), closeSoftKeyboard());

        Thread.sleep(1000);

        // Confirma que o evento criado aparece na lista
        onView(withText(uniqueEventName)).check(matches(isDisplayed()));

        //  Abre a tela de detalhes do evento recém-criado
        onView(withText(uniqueEventName)).perform(click());

        Thread.sleep(2000);

        //  Valida que os dados do evento aparecem na tela de detalhes
        onView(withId(R.id.event_details_name)).check(matches(withText(uniqueEventName)));
        onView(withId(R.id.event_details_location)).check(matches(withText("Auditório Principal")));
    }
}
