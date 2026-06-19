package io.github.ggomarighetti.jparsqlsearch.compile;

import io.github.ggomarighetti.jparsqlsearch.definition.SearchDefinition;
import io.github.ggomarighetti.jparsqlsearch.definition.SearchField;
import io.github.ggomarighetti.jparsqlsearch.page.validation.SearchPageableValidationException;
import io.github.ggomarighetti.jparsqlsearch.path.SearchPath;
import io.github.ggomarighetti.jparsqlsearch.policy.SearchPolicy;
import io.github.ggomarighetti.jparsqlsearch.query.SearchQuery;
import io.github.ggomarighetti.jparsqlsearch.query.validation.SearchQueryValidationException;
import io.github.ggomarighetti.jparsqlsearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.jparsqlsearch.definition.validation.SearchDefinitionValidator;
import io.github.ggomarighetti.jparsqlsearch.sort.SearchSorting;
import io.github.ggomarighetti.jparsqlsearch.validation.RuleViolation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Facade that validates and compiles one complete search request.
 *
 * <p>The compiler coordinates filtering, free-text query, paging, sorting, and
 * cross-component protection rules under a single request context. Applications
 * should use this type instead of invoking internal validators independently.
 */
public final class SearchCompiler {
    private final RsqlSearchGuard rsqlSearchGuard;
    private final SearchQueryGuard searchQueryGuard;
    private final SearchPageableGuard searchPageableGuard;
    private final SearchPolicy policy;

    /**
     * Creates a compiler with an explicit engine and policy.
     *
     * @param engine configured RSQL engine
     * @param policy global protection policy
     */
    public SearchCompiler(SearchRsqlEngine engine, SearchPolicy policy) {
        this(engine, policy, List.of());
    }

    /**
     * Creates a compiler with runtime definition validators.
     *
     * @param engine configured RSQL engine
     * @param policy global protection policy
     * @param definitionValidators validators executed once per definition instance
     */
    public SearchCompiler(
            SearchRsqlEngine engine,
            SearchPolicy policy,
            Collection<? extends SearchDefinitionValidator> definitionValidators) {
        Objects.requireNonNull(engine, "engine must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.rsqlSearchGuard = new RsqlSearchGuard(engine, policy, definitionValidators);
        this.searchQueryGuard = new SearchQueryGuard(policy);
        this.searchPageableGuard = new SearchPageableGuard(policy);
    }

    /**
     * Compiles a request intended for a paged query with a count.
     *
     * @param filter optional RSQL filter
     * @param query optional free-text query
     * @param pageable requested page and sort
     * @param definition allowed search capabilities
     * @param specifications mandatory application specifications combined with {@code AND}
     * @param <T> entity type
     * @return validated specification and pageable
     */
    @SafeVarargs
    public final <T> CompiledSearch<T> compile(
            String filter,
            String query,
            Pageable pageable,
            SearchDefinition<T> definition,
            Specification<T>... specifications) {
        return compile(
                SearchProtectionContext.Mode.PAGE, filter, query, pageable, definition, specifications);
    }

    /**
     * Compiles a request intended for a slice query without a count.
     *
     * @param filter optional RSQL filter
     * @param query optional free-text query
     * @param pageable requested page and sort
     * @param definition allowed search capabilities
     * @param specifications mandatory application specifications combined with {@code AND}
     * @param <T> entity type
     * @return validated specification and pageable
     */
    @SafeVarargs
    public final <T> CompiledSearch<T> compileSlice(
            String filter,
            String query,
            Pageable pageable,
            SearchDefinition<T> definition,
            Specification<T>... specifications) {
        return compile(
                SearchProtectionContext.Mode.SLICE, filter, query, pageable, definition, specifications);
    }

    @SafeVarargs
    private final <T> CompiledSearch<T> compile(
            SearchProtectionContext.Mode mode,
            String filter,
            String query,
            Pageable pageable,
            SearchDefinition<T> definition,
            Specification<T>... specifications) {
        Objects.requireNonNull(definition, "definition must not be null");
        rsqlSearchGuard.validateDefinition(definition);
        SearchProtectionContext protection =
                new SearchProtectionContext(definition.effectiveLimits(policy), mode);
        Specification<T> specification =
                specification(filter, query, definition, protection, specifications);
        Pageable validatedPageable =
                searchPageableGuard.pageable(pageable, definition, protection);
        if (SearchSpecificationSorting.requiresCriteriaSorting(pageable, definition)) {
            specification = SearchSpecificationSorting.apply(
                    specification,
                    pageable.getSort(),
                    definition);
            validatedPageable = SearchSpecificationSorting.withoutSort(validatedPageable);
        }
        protection.completeRequest();
        return new CompiledSearch<>(specification, validatedPageable);
    }

    @SafeVarargs
    private final <T> Specification<T> specification(
            String filter,
            String query,
            SearchDefinition<T> definition,
            SearchProtectionContext protection,
            Specification<T>... specifications) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(specifications, "specifications must not be null");

        List<Specification<T>> merged = new ArrayList<>(specifications.length + 2);
        for (Specification<T> specification : specifications) {
            merged.add(Objects.requireNonNull(
                    specification,
                "specifications must not contain null values"));
        }
        merged.add(filterSpecification(filter, definition, protection));
        merged.add(searchQueryGuard.specification(query, definition, protection));
        return Specification.allOf(merged);
    }

    private <T> Specification<T> filterSpecification(
            String filter,
            SearchDefinition<T> definition,
            SearchProtectionContext protection) {
        if (!StringUtils.hasText(filter)) {
            protection.completeFilter();
            return Specification.unrestricted();
        }
        return rsqlSearchGuard.specification(filter, definition, protection);
    }

    static final class SearchQueryGuard {
        private final SearchPolicy policy;

        SearchQueryGuard() {
            this(SearchPolicy.defaults());
        }

        SearchQueryGuard(SearchPolicy policy) {
            this.policy = Objects.requireNonNull(policy, "policy must not be null");
        }

        <T> Specification<T> specification(String query, SearchDefinition<T> definition) {
            Objects.requireNonNull(definition, "definition must not be null");
            SearchProtectionContext protection = new SearchProtectionContext(
                    effectivePolicy(definition), SearchProtectionContext.Mode.PAGE);
            return specification(query, definition, protection);
        }

        <T> Specification<T> specification(
                String query,
                SearchDefinition<T> definition,
                SearchProtectionContext protection) {
            Objects.requireNonNull(definition, "definition must not be null");
            Objects.requireNonNull(protection, "protection must not be null");
            if (!StringUtils.hasText(query)) {
                protection.completeQuery();
                return Specification.unrestricted();
            }
            SearchPolicy.Query limits = protection.policy().query();
            if (!limits.enabled()) {
                throw rulesForbidden();
            }
            protection.recordQuery(query);
            SearchQuery<T> searchQuery = definition.query();
            if (!searchQuery.enabled()
                    || (limits.requireValidator() && !searchQuery.hasRules())) {
                throw rulesForbidden();
            }
            List<RuleViolation> violations = searchQuery.violations(query).stream()
                    .map(violation -> violation.prefixed("query"))
                    .toList();
            if (!violations.isEmpty()) {
                throw rulesForbidden(violations);
            }
            protection.completeQuery();
            try {
                Specification<T> specification = searchQuery.toSpecification(query);
                if (specification == null) {
                    throw rulesForbidden();
                }
                return guardDeferredFailures(specification);
            } catch (SearchQueryValidationException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new SearchQueryValidationException(
                        SearchQueryValidationException.QUERY_RULES_FORBIDDEN,
                        "Search query could not be compiled to a JPA Specification.",
                        exception);
            }
        }

        private SearchQueryValidationException rulesForbidden() {
            return new SearchQueryValidationException(
                    SearchQueryValidationException.QUERY_RULES_FORBIDDEN,
                    "Search query uses values that are not allowed.");
        }

        private SearchQueryValidationException rulesForbidden(List<RuleViolation> violations) {
            return new SearchQueryValidationException(
                    SearchQueryValidationException.QUERY_RULES_FORBIDDEN,
                    "Search query failed validation with %d violation(s).".formatted(violations.size()),
                    violations);
        }

        private SearchPolicy effectivePolicy(SearchDefinition<?> definition) {
            return definition.effectiveLimits(policy);
        }

        private static <T> Specification<T> guardDeferredFailures(Specification<T> specification) {
            return (root, query, criteriaBuilder) -> {
                try {
                    return specification.toPredicate(root, query, criteriaBuilder);
                } catch (SearchQueryValidationException exception) {
                    throw exception;
                } catch (RuntimeException exception) {
                    throw new SearchQueryValidationException(
                            SearchQueryValidationException.QUERY_RULES_FORBIDDEN,
                            "Search query could not be compiled to a JPA Predicate.",
                            exception);
                }
            };
        }
    }

    static final class SearchPageableGuard {
        private static final String DEFINITION_MUST_NOT_BE_NULL = "definition must not be null";

        private final SearchPolicy policy;

        SearchPageableGuard() {
            this(SearchPolicy.defaults());
        }

        SearchPageableGuard(SearchPolicy policy) {
            this.policy = Objects.requireNonNull(policy, "policy must not be null");
        }

        Pageable pageable(Pageable pageable, SearchDefinition<?> definition) {
            Objects.requireNonNull(pageable, "pageable must not be null");
            Objects.requireNonNull(definition, DEFINITION_MUST_NOT_BE_NULL);
            SearchPolicy effectivePolicy = effectivePolicy(definition);
            return pageable(
                    pageable,
                    definition,
                    new SearchProtectionContext(effectivePolicy, SearchProtectionContext.Mode.PAGE));
        }

        Pageable pageable(
                Pageable pageable,
                SearchDefinition<?> definition,
                SearchProtectionContext protection) {
            Objects.requireNonNull(pageable, "pageable must not be null");
            Objects.requireNonNull(definition, DEFINITION_MUST_NOT_BE_NULL);
            Objects.requireNonNull(protection, "protection must not be null");
            SearchPolicy effectivePolicy = protection.policy();

            Sort sort = sort(pageable.getSort(), definition, effectivePolicy, protection);
            if (pageable.isUnpaged()) {
                if (!effectivePolicy.paging().allowUnpaged()) {
                    throw pageLimitExceeded();
                }
                protection.recordPaging(pageable);
                Pageable validated = PageRequest.of(0, effectivePolicy.paging().defaultUnpagedSize(), sort);
                protection.recordPaging(validated);
                protection.completePageable();
                return validated;
            }
            if (!acceptsPageable(pageable, effectivePolicy.paging())) {
                throw pageLimitExceeded();
            }
            if (!definition.paging().enabled()) {
                throw pageRulesForbidden();
            }
            List<RuleViolation> violations = new ArrayList<>();
            definition.paging().pageViolations(pageable.getPageNumber()).stream()
                    .map(violation -> violation.prefixed("page"))
                    .forEach(violations::add);
            definition.paging().sizeViolations(pageable.getPageSize()).stream()
                    .map(violation -> violation.prefixed("size"))
                    .forEach(violations::add);
            if (!violations.isEmpty()) {
                throw pageRulesForbidden(violations);
            }
            Pageable validated = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            protection.recordPaging(validated);
            protection.completePageable();
            return validated;
        }

        Sort sort(Sort sort, SearchDefinition<?> definition) {
            Objects.requireNonNull(sort, "sort must not be null");
            Objects.requireNonNull(definition, DEFINITION_MUST_NOT_BE_NULL);
            SearchProtectionContext protection = new SearchProtectionContext(
                    effectivePolicy(definition), SearchProtectionContext.Mode.PAGE);
            Sort validated = sort(sort, definition, protection.policy(), protection);
            protection.completePageable();
            return validated;
        }

        private Sort sort(
                Sort sort,
                SearchDefinition<?> definition,
                SearchPolicy effectivePolicy,
                SearchProtectionContext protection) {
            if (sort.isUnsorted()) {
                return Sort.unsorted();
            }

            List<Sort.Order> sourceOrders = sort.toList();
            if (sourceOrders.size() > effectivePolicy.sorting().maxOrders()) {
                throw sortLimitExceeded();
            }
            Set<String> selectors = new LinkedHashSet<>();
            Set<String> paths = new LinkedHashSet<>();
            List<Sort.Order> orders = new ArrayList<>();
            for (Sort.Order order : sourceOrders) {
                if (!selectors.add(order.getProperty())) {
                    throw sortLimitExceeded();
                }
                SearchField<?> field =
                        definition.field(order.getProperty()).orElseThrow(this::sortRulesForbidden);
                SearchSorting sorting = field.sorting();
                if (!sorting.accepts(order.getDirection())) {
                    throw sortRulesForbidden();
                }
                if (!sorting.acceptsIgnoreCase(order.isIgnoreCase())
                        || !sorting.acceptsNullHandling(order.getNullHandling())) {
                    throw sortLimitExceeded();
                }
                if (!paths.add(sorting.path())) {
                    throw sortLimitExceeded();
                }
                protection.recordSort(sorting, order);
                orders.add(order.withProperty(sorting.path()));
            }
            return Sort.by(orders);
        }

        private boolean acceptsPageable(Pageable pageable, SearchPolicy.Paging limits) {
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            long offset = (long) page * size;
            return page >= limits.minPage()
                    && page <= limits.maxPage()
                    && size >= limits.minSize()
                    && size <= limits.maxSize()
                    && offset <= limits.maxOffset();
        }

        private SearchPolicy effectivePolicy(SearchDefinition<?> definition) {
            return definition.effectiveLimits(policy);
        }

        private SearchPageableValidationException sortRulesForbidden() {
            return new SearchPageableValidationException(
                    SearchPageableValidationException.SORT_RULES_FORBIDDEN,
                    "Pageable sort uses fields or directions that are not allowed.");
        }

        private SearchPageableValidationException pageRulesForbidden() {
            return new SearchPageableValidationException(
                    SearchPageableValidationException.PAGE_RULES_FORBIDDEN,
                    "Pageable page or size uses values that are not allowed.");
        }

        private SearchPageableValidationException pageRulesForbidden(List<RuleViolation> violations) {
            return new SearchPageableValidationException(
                    SearchPageableValidationException.PAGE_RULES_FORBIDDEN,
                    "Pageable page or size failed validation with %d violation(s)."
                            .formatted(violations.size()),
                    violations);
        }

        private SearchPageableValidationException sortLimitExceeded() {
            return new SearchPageableValidationException(
                    SearchPageableValidationException.SORT_LIMIT_EXCEEDED,
                    "Pageable sort exceeds configured safety limits.");
        }

        private SearchPageableValidationException pageLimitExceeded() {
            return new SearchPageableValidationException(
                    SearchPageableValidationException.PAGE_LIMIT_EXCEEDED,
                    "Pageable page, size, offset, or unpaged mode exceeds configured safety limits.");
        }
    }

    static final class SearchSpecificationSorting {
        private SearchSpecificationSorting() {
        }

        static boolean requiresCriteriaSorting(
                Pageable source,
                SearchDefinition<?> definition) {
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(definition, "definition must not be null");
            return source.getSort().stream()
                    .map(order -> definition.field(order.getProperty()).orElse(null))
                    .filter(Objects::nonNull)
                    .anyMatch(field -> field.subtype().isPresent());
        }

        static Pageable withoutSort(Pageable pageable) {
            Objects.requireNonNull(pageable, "pageable must not be null");
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.unsorted());
        }

        static <T> Specification<T> apply(
                Specification<T> specification,
                Sort sourceSort,
                SearchDefinition<T> definition) {
            Objects.requireNonNull(specification, "specification must not be null");
            Objects.requireNonNull(sourceSort, "sourceSort must not be null");
            Objects.requireNonNull(definition, "definition must not be null");
            List<Sort.Order> requestedOrders = sourceSort.toList();
            return (root, query, criteriaBuilder) -> {
                Predicate predicate = specification.toPredicate(root, query, criteriaBuilder);
                if (!isCountQuery(query.getResultType())) {
                    query.orderBy(requestedOrders.stream()
                            .flatMap(order -> criteriaOrders(order, definition, root, criteriaBuilder).stream())
                            .toList());
                }
                return predicate;
            };
        }

        private static boolean isCountQuery(Class<?> resultType) {
            return Long.class.equals(resultType) || long.class.equals(resultType);
        }

        private static List<Order> criteriaOrders(
                Sort.Order order,
                SearchDefinition<?> definition,
                Root<?> root,
                CriteriaBuilder criteriaBuilder) {
            SearchField<?> field = definition.field(order.getProperty()).orElseThrow();
            From<?, ?> pathRoot = field.subtype()
                    .<From<?, ?>>map(subtype -> treat(criteriaBuilder, root, subtype))
                    .orElse(root);
            Expression<?> expression = path(pathRoot, field.sorting().path());
            if (order.isIgnoreCase()) {
                expression = criteriaBuilder.lower(expression.as(String.class));
            }

            List<Order> orders = new ArrayList<>(2);
            if (order.getNullHandling() != Sort.NullHandling.NATIVE) {
                int nullRank = order.getNullHandling() == Sort.NullHandling.NULLS_FIRST ? 0 : 1;
                int valueRank = 1 - nullRank;
                Expression<Integer> nullOrdering = criteriaBuilder.<Integer>selectCase()
                        .when(criteriaBuilder.isNull(expression), nullRank)
                        .otherwise(valueRank);
                orders.add(criteriaBuilder.asc(nullOrdering));
            }
            orders.add(order.isAscending()
                    ? criteriaBuilder.asc(expression)
                    : criteriaBuilder.desc(expression));
            return orders;
        }

        private static Path<?> path(Path<?> root, String path) {
            Path<?> current = root;
            for (String segment : SearchPath.segments(path)) {
                current = current.get(segment);
            }
            return current;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static From<?, ?> treat(
                CriteriaBuilder criteriaBuilder,
                Root<?> root,
                Class<?> subtype) {
            return criteriaBuilder.treat((Root) root, (Class) subtype);
        }
    }
}
