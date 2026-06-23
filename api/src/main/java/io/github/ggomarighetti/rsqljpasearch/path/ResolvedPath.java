package io.github.ggomarighetti.rsqljpasearch.path;

import java.util.Set;

record ResolvedPath(
        Class<?> type,
        boolean traversesCollection,
        boolean collectionValued,
        int depth,
        Set<String> joinedPaths,
        Set<String> toManyPaths) {
}
