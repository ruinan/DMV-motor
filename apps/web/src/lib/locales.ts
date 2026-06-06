import type { Locale } from "@/lib/dictionaries";

/**
 * Client-safe language list for the {@link LanguageSelect} dropdown.
 *
 * dictionaries.ts is `server-only`, so a client component can't read its locale
 * list at runtime — this mirrors it. Adding a language is three coordinated edits:
 * a `messages/<code>.json`, a `dictionaries.ts` entry (that's what extends the
 * `Locale` type, so this list won't type-check until the dictionary exists), and
 * a row here. The selector renders whatever is listed, so it already scales past
 * two languages — that's the whole point of using a dropdown over a 2-way toggle.
 *
 * `nativeName` is the language's name in its own language (endonym), never
 * translated — the convention for language pickers so a speaker always recognises
 * their language regardless of the current UI locale.
 */
export type LanguageOption = { code: Locale; nativeName: string };

export const LANGUAGE_OPTIONS: LanguageOption[] = [
  { code: "en", nativeName: "English" },
  { code: "zh", nativeName: "中文" },
];
