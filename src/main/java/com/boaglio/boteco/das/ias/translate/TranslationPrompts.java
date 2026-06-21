package com.boaglio.boteco.das.ias.translate;

/** Builds the prompt that translates a snippet of news text into Brazilian Portuguese. */
public final class TranslationPrompts {

    private TranslationPrompts() {
    }

    /**
     * Asks the model to translate the given text to pt-BR, keeping technical
     * terms (product names, acronyms, code) in their original form.
     */
    public static String toBrazilianPortuguese(String text) {
        return """
                Traduza o texto abaixo para português do Brasil.
                Mantenha os termos técnicos em inglês (nomes de produtos, siglas, código).
                O público é formado por desenvolvedores e estudantes de TI.
                Responda apenas com a tradução — sem preâmbulo, sem aspas, sem comentários.

                Texto:
                %s
                """.formatted(text);
    }
}
