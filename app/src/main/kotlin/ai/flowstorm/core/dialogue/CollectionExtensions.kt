package ai.flowstorm.core.dialogue

import ai.flowstorm.core.Input

fun <T> Collection<T>.list(transform: T.() -> String) = map { transform(it) }

fun Map<String, Any>.list(transform: Map.Entry<String, Any>.() -> String) = map { transform(it) }

fun <T> Collection<T>.random(a: Int): Collection<T> = shuffled().take(a)

fun <T> Collection<T>.similarTo(tokens: List<Input.Word>, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        filter { minSimilarity == .0 || transform(it) similarityTo tokens >= minSimilarity }
                .sortedByDescending { transform(it) similarityTo tokens }.take(n)

fun <T> Collection<T>.similarTo(input: Input, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        similarTo(input.words, transform, n, minSimilarity)

fun <T> Collection<T>.similarTo(text: String, transform: T.() -> String, n: Int, minSimilarity: Double = .0) =
        similarTo(text.tokenize(), transform, n, minSimilarity)

fun <T> Collection<T>.similarTo(tokens: List<Input.Word>, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(tokens, transform, 1, minSimilarity).firstOrNull()

fun <T> Collection<T>.similarTo(input: Input, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(input, transform, 1, minSimilarity).firstOrNull()

fun <T> Collection<T>.similarTo(text: String, transform: T.() -> String, minSimilarity: Double = .0) =
        similarTo(text, transform, 1, minSimilarity).firstOrNull()

fun Collection<String>.similarTo(tokens: List<Input.Word>, n: Int, minSimilarity: Double = .0) =
        similarTo(tokens, { this }, n, minSimilarity)

fun Collection<String>.similarTo(input: Input, n: Int, minSimilarity: Double = .0) =
        similarTo(input, { this }, n, minSimilarity)

fun Collection<String>.similarTo(text: String, n: Int, minSimilarity: Double = .0) =
        similarTo(text, { this }, n, minSimilarity)

fun Collection<String>.similarTo(tokens: List<Input.Word>, minSimilarity: Double = .0) =
        similarTo(tokens, { this }, minSimilarity)

fun Collection<String>.similarTo(input: Input, minSimilarity: Double = .0) =
        similarTo(input, { this }, minSimilarity)

fun Collection<String>.similarTo(text: String, minSimilarity: Double = .0) =
        similarTo(text, { this }, minSimilarity)