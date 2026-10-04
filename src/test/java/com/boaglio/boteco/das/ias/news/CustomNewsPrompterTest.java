package com.boaglio.boteco.das.ias.news;

import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Subject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomNewsPrompterTest {

    private CustomNewsPrompter prompterFor(String typed) {
        return new CustomNewsPrompter(new BufferedReader(new StringReader(typed)));
    }

    @Test
    void addsPortugueseNewsAsAlreadyTranslated() {
        var added = prompterFor("""
                Minha notícia
                https://www.example.com/post
                Um resumo
                .
                
                
                
                """).prompt(List.of());

        assertThat(added).singleElement().satisfies(news -> {
            assertThat(news.subject()).isEqualTo(Subject.CUSTOM);
            assertThat(news.url()).isEqualTo("https://www.example.com/post");
            assertThat(news.source()).isEqualTo("example.com");
            assertThat(news.titlePt()).isEqualTo("Minha notícia");
            assertThat(news.summaryPt()).isEqualTo("Um resumo");
        });
    }

    @Test
    void leavesEnglishNewsForTheTranslateStage() {
        var added = prompterFor("""
                My news
                
                A summary
                .
                My blog
                n
                
                """).prompt(List.of());

        assertThat(added).singleElement().satisfies(news -> {
            assertThat(news.url()).isNull();
            assertThat(news.source()).isEqualTo("My blog");
            assertThat(news.titlePt()).isNull();
            assertThat(news.summaryPt()).isNull();
        });
    }

    @Test
    void keepsAPastedMultiLineSummaryInOneItem() {
        // The real-world case: a whole post pasted as the summary must not
        // spill into the following prompts and become a second news item.
        var added = prompterFor("""
                Java Meetup SP 45
                https://www.linkedin.com/pulse/java-meetup-45
                O Java Meetup 45 foi sensacional!
                O espaço da FIAP é excelente,
                
                E terminou com Ricardo Mello falando de MongoDB!
                .
                
                
                
                """).prompt(List.of());

        assertThat(added).singleElement().satisfies(news -> {
            assertThat(news.summary()).isEqualTo("""
                    O Java Meetup 45 foi sensacional!
                    O espaço da FIAP é excelente,
                    
                    E terminou com Ricardo Mello falando de MongoDB!""");
            assertThat(news.source()).isEqualTo("linkedin.com");
        });
    }

    @Test
    void asksAgainForAnInvalidLinkOrYesNoAnswer() {
        var added = prompterFor("""
                Minha notícia
                isto não é um link
                https://example.com
                resumo
                .
                
                talvez
                n
                
                """).prompt(List.of());

        assertThat(added).singleElement().satisfies(news -> {
            assertThat(news.url()).isEqualTo("https://example.com");
            assertThat(news.titlePt()).isNull();
        });
    }

    @Test
    void rejectsNewsAlreadyInTheEdition() {
        var gathered = new News(Subject.JAVA, "JDK news", "https://inside.java/x", "inside.java",
                LocalDate.of(2026, 6, 18), "summary", List.of(), null, null, null);

        var added = prompterFor("""
                Same news
                https://inside.java/x
                resumo
                .
                
                
                
                """).prompt(List.of(gathered));

        assertThat(added).isEmpty();
    }

    @Test
    void addsNothingOnBlankOrMissingInput() {
        assertThat(prompterFor("\n").prompt(List.of())).isEmpty();
        assertThat(prompterFor("").prompt(List.of())).isEmpty();
    }
}
