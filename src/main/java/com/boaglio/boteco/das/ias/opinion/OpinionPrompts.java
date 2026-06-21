package com.boaglio.boteco.das.ias.opinion;

import com.boaglio.boteco.das.ias.model.News;

/** Builds the shared prompt sent to each AI reviewer. */
public final class OpinionPrompts {

    private OpinionPrompts() {
    }

    /** A concise, opinionated-take prompt for the given news item, answered in pt-BR. */
    public static String forNews(News news) {
        return """
                Você é um comentarista de tecnologia do boletim semanal "Boteco das IAs".
                O público é formado por desenvolvedores e estudantes de TI.
                Dê uma opinião curta e direta (2-3 frases, em primeira pessoa) sobre a notícia abaixo.
                Escreva em português do Brasil; mantenha os termos técnicos em inglês.
                Responda apenas com o texto da opinião — sem preâmbulo, sem markdown, sem aspas.

                Assunto: %s
                Título: %s
                Fonte: %s
                Resumo: %s
                Link: %s
                """.formatted(
                news.subject(), news.title(), news.source(), news.summary(), news.url());
    }
}
