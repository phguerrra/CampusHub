package com.example.application.e2e;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.application.MainActivity;
import com.example.application.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserAuthAndProfileE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testUserAuthenticationAndNavigationFlow() throws Exception {
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

    @Test
    public void testInvalidCredentials_showsErrorMessage() throws Exception {
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
