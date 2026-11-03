package com.example.application.e2e;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.application.MainActivity;
import com.example.application.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserAuthAndProfileE2ETest {

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    @Test
    public void testUserAuthenticationAndNavigationFlow() throws Exception {
        FirebaseAuth.getInstance().signOut();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Assegura que a tela inicial de autenticação exibe os campos de entrada
            onView(withId(R.id.email_input)).check(matches(isDisplayed()));
            onView(withId(R.id.password_input)).check(matches(isDisplayed()));

            // Preenche as credenciais e executa a ação de login
            onView(withId(R.id.email_input))
                    .perform(replaceText("aluno@exemplo.com"), closeSoftKeyboard());
            onView(withId(R.id.password_input))
                    .perform(replaceText("123456"), closeSoftKeyboard());

            onView(withId(R.id.login_button)).perform(click());

            // Aguarda a resposta do Firebase Auth e transição para a tela principal
            Thread.sleep(3000);

            // Confirma que o login e a navegação para a EventsActivity autenticada ocorreram com sucesso
            onView(withId(R.id.events_title)).check(matches(isDisplayed()));
            onView(withId(R.id.events_list)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testUserRegistrationAndNavigationFlow() throws Exception {
        FirebaseAuth.getInstance().signOut();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            String testEmail = "aluno_e2e_" + System.currentTimeMillis() + "@exemplo.com";

            // Clica no botão para abrir a tela de cadastro
            onView(withId(R.id.open_register_button)).perform(click());

            // Verifica que os campos da tela de registro são exibidos
            onView(withId(R.id.name_input)).check(matches(isDisplayed()));
            onView(withId(R.id.email_input)).check(matches(isDisplayed()));
            onView(withId(R.id.password_input)).check(matches(isDisplayed()));

            // Preenche o formulário de cadastro com e-mail único
            onView(withId(R.id.name_input))
                    .perform(replaceText("Aluno E2E"), closeSoftKeyboard());
            onView(withId(R.id.email_input))
                    .perform(replaceText(testEmail), closeSoftKeyboard());
            onView(withId(R.id.password_input))
                    .perform(replaceText("123456"), closeSoftKeyboard());

            // Executa a criação da conta
            onView(withId(R.id.register_button)).perform(click());

            // Aguarda a criação da conta no Firebase Auth e redirecionamento
            Thread.sleep(3500);

            // Confirma que o cadastro efetuou o login e abriu a EventsActivity autenticada
            onView(withId(R.id.events_title)).check(matches(isDisplayed()));
            onView(withId(R.id.events_list)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testInvalidCredentials_showsErrorMessage() throws Exception {
        FirebaseAuth.getInstance().signOut();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Preenche e-mail e senha inválidos
            onView(withId(R.id.email_input))
                    .perform(replaceText("aluno_inexistente@exemplo.com"), closeSoftKeyboard());
            onView(withId(R.id.password_input))
                    .perform(replaceText("senhaErrada123"), closeSoftKeyboard());

            onView(withId(R.id.login_button)).perform(click());

            // Aguarda o retorno de erro do Firebase Auth
            Thread.sleep(2500);

            // Verifica se a mensagem de erro foi exibida na tela de login
            onView(withId(R.id.auth_error)).check(matches(isDisplayed()));
        }
    }
}
