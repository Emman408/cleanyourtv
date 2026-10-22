package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class AppSearchTest {
    private List<Integer> find(Locale locale, String query, String... labels) {
        List<String> keys = Arrays.stream(labels).map(s -> AppSearch.key(s, locale)).collect(Collectors.toList());
        return indices(keys, query, locale);
    }
    private List<Integer> indices(List<String> keys, String query, Locale locale) {
        String[] words = AppSearch.words(query, locale);
        return java.util.stream.IntStream.range(0, keys.size())
                .filter(i -> AppSearch.matches(keys.get(i), words)).boxed().collect(Collectors.toList());
    }
    @Test public void findsAPartialAppNameWithoutChangingTheExistingOrder() {
        assertEquals(Arrays.asList(0, 2), find(Locale.ENGLISH, "net", "Netflix", "YouTube", "NET Radio"));
    }
    @Test public void blankQueryRestoresEveryEntry() {
        assertEquals(Arrays.asList(0, 1, 2), find(Locale.ENGLISH, " \t\n", "Netflix", "YouTube", "AirPlay"));
    }
    @Test public void noMatchesIsAnEmptyList() {
        assertEquals(Collections.emptyList(), find(Locale.ENGLISH, "missing", "Netflix", "YouTube"));
    }
    @Test public void emptyCatalogueRemainsEmpty() {
        assertEquals(Collections.emptyList(), find(Locale.ENGLISH, "net"));
    }
    @Test public void queryWordsMayOccurInEitherOrder() {
        assertEquals(Arrays.asList(0, 2), find(Locale.ENGLISH, "video prime", "Prime Video", "Video", "Video / Prime"));
    }
    @Test public void everyQueryWordMustMatch() {
        assertEquals(Collections.singletonList(0), find(Locale.ENGLISH, "youtube music", "YouTube Music", "YouTube", "Music"));
    }
    @Test public void composedAndDecomposedAccentsMatch() {
        assertEquals(Arrays.asList(0, 1), find(Locale.FRENCH, "cafe", "Café TV", "Cafe\u0301 TV", "Coffee TV"));
        assertEquals(Collections.singletonList(0), find(Locale.FRENCH, "CAFÉ", "Cafe TV", "Tea"));
    }
    @Test public void turkishCaseUsesTheRequestedLocale() {
        Locale tr = Locale.forLanguageTag("tr");
        assertEquals(Collections.singletonList(0), find(tr, "iz", "İzle", "IRMAK"));
        assertEquals(Collections.singletonList(1), find(tr, "ır", "İzle", "IRMAK"));
    }
    @Test public void germanSharpSAndGreekFinalSigmaFoldForSearch() {
        assertEquals(Collections.singletonList(0), find(Locale.GERMAN, "STRASSE", "Straße TV"));
        assertEquals(Collections.singletonList(0), find(Locale.forLanguageTag("el"), "κοσμοσ", "Κόσμος"));
    }
    @Test public void hebrewAndArabicNamesRemainSearchable() {
        assertEquals(Collections.singletonList(1), find(Locale.forLanguageTag("he"), "כאן", "YouTube", "כאן"));
        assertEquals(Collections.singletonList(1), find(Locale.forLanguageTag("ar"), "شاهد", "YouTube", "شَاهِد"));
    }
    @Test public void cjkAndEmojiTextIsPreserved() {
        assertEquals(Collections.singletonList(0), find(Locale.JAPANESE, "テレビ", "テレビ番組", "YouTube"));
        assertEquals(Collections.singletonList(1), find(Locale.ENGLISH, "📺", "YouTube", "TV 📺"));
    }
    @Test public void unicodeWhitespaceAndNullLabelsAreHandled() {
        assertEquals(Collections.singletonList(0), find(Locale.ENGLISH, " prime\u00a0video ", "Prime Video", null));
        assertEquals("", AppSearch.key(null, Locale.ROOT));
    }
    @Test public void inputCatalogAndKeysAreNotMutated() {
        List<String> keys = Arrays.asList("youtube", "netflix");
        assertEquals(Collections.singletonList(1), indices(keys, "NET", Locale.ENGLISH));
        assertEquals(Arrays.asList("youtube", "netflix"), keys);
    }
    @Test public void locatedSelectionOverridesItsPreviousPosition() {
        assertEquals(1, AppSearch.focusIndex(1, 0, 2));
        assertEquals(0, AppSearch.focusIndex(0, 1, 2));
    }
    @Test public void removingTheSelectedAppChoosesAnExistingNeighbour() {
        assertEquals(1, AppSearch.focusIndex(-1, 9, 2));
        assertEquals(0, AppSearch.focusIndex(-1, -1, 2));
    }
    @Test public void emptyResultsHaveNoSelectedCard() {
        assertEquals(-1, AppSearch.focusIndex(-1, 0, 0));
    }
}
