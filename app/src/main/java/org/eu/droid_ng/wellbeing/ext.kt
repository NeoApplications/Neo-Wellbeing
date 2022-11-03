package org.eu.droid_ng.wellbeing

fun String.Companion.join(delimiter: String, strings: Iterable<CharSequence?>): String {
	return java.lang.String.join(delimiter, strings)
}

fun String.Companion.join(delimiter: String, strings: Array<String?>): String {
	return String.join(delimiter, strings.toList())
}
