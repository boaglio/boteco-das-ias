package com.boaglio.boteco.das.ias.render;

import com.boaglio.boteco.das.ias.config.BotecoProperties;
import com.boaglio.boteco.das.ias.model.Magazine;
import com.boaglio.boteco.das.ias.model.News;
import com.boaglio.boteco.das.ias.model.Opinion;
import com.boaglio.boteco.das.ias.model.Reviewer;
import com.boaglio.boteco.das.ias.storage.MagazineStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Stage 4 of the build process: render the finished {@link Magazine} as a
 * self-contained HTML page written next to its JSON, in the release directory.
 *
 * <p>The layout follows the brief: the title, then one block per news item that
 * alternates the image side (image-left for even items, image-right for odd
 * ones), each followed by its opinions laid out as a chat — the human and Llama
 * on the left, GPT-OSS and Claude on the right — and finally a social footer.
 */
@Service
public class MagazineRenderer {

    private static final Logger log = LoggerFactory.getLogger(MagazineRenderer.class);
    private static final String HTML_FILE = "magazine.html";
    private static final String LOGO_FILE = "logo.png";
    private static final String AVATAR_FILE = "avatar.png";

    private final MagazineStore store;
    private final BotecoProperties properties;

    public MagazineRenderer(MagazineStore store, BotecoProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    /** Renders the magazine to {@code magazine.html} in its release dir and returns the path. */
    public Path renderToFile(Magazine magazine) {
        var releaseDir = store.releaseDir(magazine.releaseDate());
        var htmlPath = releaseDir.resolve(HTML_FILE);
        try {
            Files.createDirectories(releaseDir);
            copyAssetsInto(releaseDir);
            Files.writeString(htmlPath, render(magazine), StandardCharsets.UTF_8);
            writeCards(magazine, releaseDir);
            log.info("Rendered magazine to {}", htmlPath);
            return htmlPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write magazine HTML to " + htmlPath, e);
        }
    }

    /**
     * Writes one standalone HTML card per news item ({@code card-N-subject.html}),
     * each holding the news plus its full conversation — the source for the
     * per-news LinkedIn images (rasterized by scripts/linkedin-images.sh).
     */
    private void writeCards(Magazine magazine, Path releaseDir) throws IOException {
        var news = magazine.news();
        for (var i = 0; i < news.size(); i++) {
            var item = news.get(i);
            var name = "card-%d-%s.html".formatted(
                    i + 1, item.subject().name().toLowerCase(Locale.ROOT));
            Files.writeString(releaseDir.resolve(name), renderCard(magazine, item),
                    StandardCharsets.UTF_8);
        }
        log.info("Wrote {} LinkedIn card(s) to {}", news.size(), releaseDir);
    }

    /** A standalone, portrait-friendly card: one news item and its conversation. */
    private String renderCard(Magazine magazine, News news) {
        var brand = logoSource() == null ? ""
                : "<img src=\"%s\" alt=\"\">".formatted(LOGO_FILE);
        return """
                <!DOCTYPE html>
                <html lang="pt-br">
                <head><meta charset="utf-8"><style>%s</style></head>
                <body class="card">
                %s
                %s
                <div class="opinions">%s</div>
                <div class="card-brand">%s<span>Boteco das IAs · %s</span></div>
                </body>
                </html>
                """.formatted(css() + cardCss(), renderImage(news), renderText(news),
                renderOpinions(news), brand, magazine.releaseDate());
    }

    /** Copies the brand logo and author avatar into the release dir, if present. */
    private void copyAssetsInto(Path releaseDir) throws IOException {
        if (logoSource() != null) {
            Files.copy(logoSource(), releaseDir.resolve(LOGO_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
        if (avatarSource() != null) {
            Files.copy(avatarSource(), releaseDir.resolve(AVATAR_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The configured logo file, or null when none is set or it doesn't exist. */
    private Path logoSource() {
        return existingFile(properties.logoFile());
    }

    /** The author avatar file, or null when none is set or it doesn't exist. */
    private Path avatarSource() {
        var author = properties.author();
        return author == null ? null : existingFile(author.avatarFile());
    }

    private static Path existingFile(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        var path = Path.of(configured);
        return Files.exists(path) ? path : null;
    }

    /** Builds the full HTML document for the magazine. */
    public String render(Magazine magazine) {
        var body = new StringBuilder();
        var news = magazine.news();
        for (var i = 0; i < news.size(); i++) {
            body.append(renderNews(news.get(i), i % 2 == 0));
        }
        return """
                <!DOCTYPE html>
                <html lang="pt-br">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>%s</style>
                </head>
                <body>
                <header class="magazine-title">%s<h1>%s</h1></header>
                <main>
                %s</main>
                <footer class="magazine-footer">
                <p>Boteco das IAs — seu boteco semanal de notícias com IA.</p>
                <nav class="footer-links">%s</nav>
                </footer>
                </body>
                </html>
                """.formatted(escape(magazine.title()), css(), logoBanner(),
                escape(magazine.title()), body, renderFooterLinks());
    }

    /** The header logo image tag, or empty when no logo is configured/present. */
    private String logoBanner() {
        return logoSource() == null ? ""
                : "<img class=\"logo-banner\" src=\"%s\" alt=\"Boteco das IAs\">".formatted(LOGO_FILE);
    }

    private String renderNews(News news, boolean imageLeft) {
        var image = renderImage(news);
        var text = renderText(news);
        var left = imageLeft ? image : text;
        var right = imageLeft ? text : image;
        return """
                <article class="news">
                <div class="news-row">
                <div class="news-cell">%s</div>
                <div class="news-cell">%s</div>
                </div>
                <div class="opinions">
                %s</div>
                </article>
                """.formatted(left, right, renderOpinions(news));
    }

    private String renderImage(News news) {
        if (news.imagePath() == null) {
            return "<div class=\"image placeholder\">sem imagem</div>";
        }
        return "<img class=\"image\" src=\"%s\" alt=\"%s\">".formatted(
                escape(news.imagePath()), escape(news.title()));
    }

    private String renderText(News news) {
        var headline = news.titlePt() != null && !news.titlePt().isBlank()
                ? news.titlePt() : news.title();
        var summary = news.summaryPt() != null && !news.summaryPt().isBlank()
                ? news.summaryPt() : news.summary();
        // Show the original English headline beneath the translation, when it differs.
        var original = news.titlePt() != null && !news.titlePt().isBlank()
                && !news.title().equalsIgnoreCase(news.titlePt())
                ? "<p class=\"original-title\">%s</p>".formatted(escape(news.title()))
                : "";
        return """
                <div class="news-text">
                <span class="subject">%s</span>
                <h2><a href="%s">%s</a></h2>
                %s<p class="summary">%s</p>
                <p class="source">Fonte: %s</p>
                </div>
                """.formatted(
                escape(news.subject().name().replace('_', ' ')), escape(news.url()), escape(headline),
                original, escape(summary), escape(news.source()));
    }

    private String renderFooterLinks() {
        var sb = new StringBuilder("<ul class=\"footer-list\">");
        for (var link : properties.footerLinks()) {
            sb.append("<li><a href=\"%s\">%s<span>%s</span></a></li>".formatted(
                    escape(link.url()), FooterIcons.forSlug(link.icon()), escape(link.label())));
        }
        return sb.append("</ul>").toString();
    }

    private String renderOpinions(News news) {
        var sb = new StringBuilder();
        for (var opinion : news.opinions()) {
            var reviewer = opinion.reviewer();
            var maker = makerOf(reviewer);
            var makerHtml = maker.isEmpty() ? ""
                    : "<span class=\"maker\">%s</span>".formatted(escape(maker));
            sb.append("""
                    <div class="opinion %s">
                    <span class="speaker">%s %s %s</span>
                    <p>%s</p>
                    </div>
                    """.formatted(sideOf(reviewer), iconFor(reviewer),
                    escape(modelLabel(reviewer)), makerHtml, escape(opinion.text())));
        }
        return sb.toString();
    }

    /** Conversation side: human and Llama on the left, Phi-4 and Claude on the right. */
    private static String sideOf(Reviewer reviewer) {
        return switch (reviewer) {
            case HUMAN, OLLAMA_LLAMA -> "left";
            case OLLAMA_PHI, CLAUDE_CLI -> "right";
        };
    }

    /** The model name shown for the opinion (the human speaks as themselves). */
    private String modelLabel(Reviewer reviewer) {
        return switch (reviewer) {
            case HUMAN -> authorName();
            case OLLAMA_PHI -> properties.reviewers().ollama().phiModel();
            case OLLAMA_LLAMA -> properties.reviewers().ollama().llamaModel();
            case CLAUDE_CLI -> "Claude";
        };
    }

    /** The human reviewer's display name, falling back to "Eu". */
    private String authorName() {
        var author = properties.author();
        return author != null && author.name() != null && !author.name().isBlank()
                ? author.name() : "Eu";
    }

    /** The speaker icon: the author's avatar for the human, a maker badge otherwise. */
    private String iconFor(Reviewer reviewer) {
        if (reviewer == Reviewer.HUMAN && avatarSource() != null) {
            return "<img class=\"logo avatar\" src=\"%s\" alt=\"%s\">".formatted(
                    AVATAR_FILE, escape(authorName()));
        }
        return MakerLogos.forReviewer(reviewer);
    }

    /** The big-tech maker behind each model (empty for the human). */
    private static String makerOf(Reviewer reviewer) {
        return switch (reviewer) {
            case HUMAN -> "";
            case OLLAMA_PHI -> "Microsoft";
            case OLLAMA_LLAMA -> "Meta";
            case CLAUDE_CLI -> "Anthropic";
        };
    }

    private static String css() {
        return """
                :root{--bg:#16151b;--card:#23222b;--card-right:#1f2a39;--ink:#ece7df;--muted:#9a948a;--accent:#e07a5f;--line:#332f3a}
                body{margin:0;background:var(--bg);color:var(--ink);font-family:system-ui,sans-serif;line-height:1.5}
                .magazine-title{text-align:center;padding:2rem 1rem;border-bottom:3px solid var(--accent)}
                .magazine-title h1{margin:0;font-size:2rem}
                .logo-banner{width:60%;max-width:320px;height:auto;display:block;margin:0 auto 1rem;border-radius:14px}
                main{max-width:880px;margin:0 auto;padding:1rem}
                .news{padding:1.5rem 0;border-bottom:1px solid var(--line)}
                .news-row{display:flex;gap:1.5rem;align-items:center;flex-wrap:wrap}
                .news-cell{flex:1 1 280px}
                .image{width:100%;border-radius:10px;display:block}
                .image.placeholder{aspect-ratio:1;display:flex;align-items:center;justify-content:center;background:#2a2833;color:var(--muted)}
                .subject{font-size:.75rem;letter-spacing:.1em;color:var(--accent);font-weight:700}
                .news-text h2{margin:.3rem 0}
                .news-text a{color:inherit;text-decoration:none}
                .original-title{margin:.1rem 0 .4rem;font-size:.85rem;font-style:italic;color:var(--muted)}
                .source{font-size:.8rem;color:var(--muted)}
                .opinions{margin-top:1rem;display:flex;flex-direction:column;gap:.6rem}
                .opinion{max-width:75%;padding:.6rem .9rem;border-radius:14px;background:var(--card);box-shadow:0 1px 3px rgba(0,0,0,.4)}
                .opinion.left{align-self:flex-start;border-top-left-radius:2px}
                .opinion.right{align-self:flex-end;border-top-right-radius:2px;background:var(--card-right)}
                .opinion .speaker{display:flex;align-items:center;gap:.3rem;font-size:.7rem;font-weight:700;color:var(--muted);margin-bottom:.2rem}
                .opinion .logo{flex:none}
                .opinion .avatar{width:16px;height:16px;border-radius:50%;object-fit:cover}
                .opinion .maker{font-weight:400;color:#7d776e;font-size:.65rem}
                .opinion p{margin:0}
                .magazine-footer{text-align:center;padding:2rem 1rem;color:var(--muted);font-size:.85rem;border-top:1px solid var(--line)}
                .footer-list{list-style:none;margin:1rem auto 0;padding:0;max-width:300px;display:inline-block;text-align:left}
                .footer-list li{margin:.4rem 0}
                .footer-list a{display:flex;align-items:center;gap:.55rem;color:var(--ink);text-decoration:none}
                .footer-list a:hover{color:var(--accent)}
                .footer-list .flogo{flex:none}
                """;
    }

    /** Extra styling for a standalone per-news LinkedIn card (fixed width, stacked). */
    private static String cardCss() {
        return """
                body.card{width:1080px;box-sizing:border-box;padding:40px 44px}
                .card .news{border:none;padding:0}
                .card .image{width:100%;max-height:560px;object-fit:cover;border-radius:16px;margin-bottom:1.3rem}
                .card .subject{font-size:.9rem}
                .card .news-text h2{font-size:1.7rem}
                .card .summary{font-size:1.1rem}
                .card .opinions{margin-top:1.4rem;gap:.7rem}
                .card .opinion{max-width:88%;font-size:1.02rem}
                .card-brand{display:flex;align-items:center;gap:.6rem;margin-top:1.6rem;padding-top:1rem;border-top:1px solid var(--line);color:var(--muted);font-weight:700}
                .card-brand img{width:36px;height:36px;border-radius:9px}
                """;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
