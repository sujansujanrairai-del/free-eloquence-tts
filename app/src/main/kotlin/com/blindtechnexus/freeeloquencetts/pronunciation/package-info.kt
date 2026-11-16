/**
 * The pronunciation rules for Hindi and Nepali.
 *
 * The engine this app is built on speaks eight Western languages and has no
 * module for either of the two Devanagari ones. What it does have is an English
 * voice that reads English spelling, so the road taken here is the one a
 * human romanisation takes: write the Devanagari down as the English letters
 * that sound like it, and let the voice read those. "नमस्ते" becomes
 * "nuhmusteh"; "नगरने" becomes "nugurneh" and not "naagarna".
 *
 * The package is split by what each file knows:
 *
 * [Devanagari] is the script as data -- every vowel, every consonant, every
 * matra, the nukta letters, the digits, the marks that are not said.
 *
 * [Akshara] and [Syllabifier] cut a word into the syllables the script is
 * written in. This is where Devanagari stops being letters and starts being
 * sounds.
 *
 * [SchwaDeletion] is the rule that makes the difference between a Western
 * reading and an Indian one: the inherent vowel that is not said.
 *
 * [Nasalization] is the anusvara, the chandrabindu and the visarga.
 *
 * [Conjuncts] is the consonant clusters, including the handful that are said
 * as something other than their letters.
 *
 * [HindiLexicon] and [NepaliLexicon] are the words the rules get wrong, written
 * down as they are said.
 *
 * [PronunciationRules] is the order those happen in, and
 * [DevanagariPronunciation] is the door the rest of the app knocks on.
 */
package com.blindtechnexus.freeeloquencetts.pronunciation
