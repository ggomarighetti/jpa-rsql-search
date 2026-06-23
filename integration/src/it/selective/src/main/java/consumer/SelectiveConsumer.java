package consumer;

import io.github.ggomarighetti.rsqljpasearch.compile.SearchCompiler;
import io.github.ggomarighetti.rsqljpasearch.policy.SearchPolicy;
import io.github.ggomarighetti.rsqljpasearch.rsql.backend.perplexhub.PerplexhubRsqlEngines;

final class SelectiveConsumer {
    private final SearchCompiler compiler =
            new SearchCompiler(PerplexhubRsqlEngines.defaults(), SearchPolicy.defaults());
}
