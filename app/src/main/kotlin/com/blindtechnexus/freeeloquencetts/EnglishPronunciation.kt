package com.blindtechnexus.freeeloquencetts

object EnglishPronunciation {

	private val SHARED: Map<String, String> = mapOf(
		"colonel" to "kernel",
		"worcestershire" to "wuster",
		"worcester" to "wuster",
		"leicester" to "lester",
		"gloucester" to "gloster",
		"edinburgh" to "edinbruh",
		"thames" to "temz",
		"greenwich" to "grenitch",
		"norwich" to "norridge",
		"cholmondeley" to "chumlee",
		"featherstonehaugh" to "faneshaw",
		"quinoa" to "keenwah",
		"epitome" to "epittomee",
		"hyperbole" to "hypurbolee",
		"awry" to "uhry",
		"genre" to "zhahnruh",
		"facade" to "fuhsahd",
		"naive" to "nyeev",
		"debris" to "duhbree",
		"rendezvous" to "rahndayvoo",
		"croissant" to "kwahsahn",
		"entrepreneur" to "ahntruhpruhnur",
		"bourgeois" to "boorzhwah",
		"potpourri" to "pohpooree",
		"pharaoh" to "fairroh",
		"choir" to "kwyr",
		"quay" to "kee",
		"gauge" to "gayj",
		"draught" to "draft",
		"plough" to "plow",
		"trough" to "trof",
		"cough" to "kof",
		"thorough" to "thurruh",
		"borough" to "burruh",
		"hiccough" to "hikkup",
		"bough" to "bow",
		"drought" to "drowt",
		"wrought" to "rawt",
		"height" to "hyt",
		"sleight" to "slyt",
		"freight" to "frayt",
		"weight" to "wayt",
		"eight" to "ayt",
		"foreign" to "forrun",
		"sovereign" to "sovrun",
		"campaign" to "campayn",
		"sign" to "syn",
		"align" to "uhlyn",
		"design" to "duhzyn",
		"assign" to "uhssyn",
		"gnome" to "nohm",
		"gnaw" to "naw",
		"knowledge" to "nolij",
		"acknowledge" to "uknolij",
		"knight" to "nyt",
		"knife" to "nyf",
		"know" to "noh",
		"knee" to "nee",
		"knob" to "nob",
		"knock" to "nok",
		"knot" to "not",
		"knuckle" to "nukkul",
		"pneumonia" to "noomohnyuh",
		"psychology" to "sykolujee",
		"psychiatrist" to "suhkyutrist",
		"pseudonym" to "sooduhnim",
		"ptarmigan" to "tarmigun",
		"mnemonic" to "nuhmonik",
		"receipt" to "rusheet",
		"deceit" to "duseet",
		"conceive" to "kuhnseev",
		"debt" to "det",
		"doubt" to "dowt",
		"subtle" to "suttul",
		"thumb" to "thum",
		"numb" to "num",
		"plumber" to "plummer",
		"lamb" to "lam",
		"climb" to "klym",
		"comb" to "kohm",
		"bomb" to "bom",
		"tomb" to "toom",
		"womb" to "woom",
		"limb" to "lim",
		"crumb" to "crum",
		"dumb" to "dum",
		"wednesday" to "wenzday",
		"february" to "febrooeree",
		"library" to "lybreree",
		"comfortable" to "kumftuhbul",
		"vegetable" to "vejtuhbul",
		"chocolate" to "choklut",
		"temperature" to "temprutchur",
		"interesting" to "intrusting",
		"business" to "biznus",
		"biscuit" to "biskit",
		"circuit" to "surkit",
		"guess" to "gess",
		"guest" to "gest",
		"guitar" to "gitar",
		"guard" to "gard",
		"guarantee" to "garuntee",
		"catalogue" to "catalog",
		"dialogue" to "dyalog",
		"plague" to "playg",
		"vague" to "vayg",
		"tongue" to "tung",
		"mosque" to "mosk",
		"cheque" to "chek",
		"queue" to "kyoo",
		"island" to "ylund",
		"aisle" to "yl",
		"isle" to "yl",
		"corps" to "kor",
		"coup" to "koo",
		"soup" to "soop",
		"suite" to "sweet",
		"colleague" to "kolleeg",
		"salmon" to "sammun",
		"almond" to "ahmund",
		"yolk" to "yohk",
		"folk" to "fohk",
		"calm" to "kahm",
		"palm" to "pahm",
		"psalm" to "sahm",
		"half" to "haaf",
		"calf" to "kaaf",
		"chalk" to "chawk",
		"walk" to "wawk",
		"talk" to "tawk",
		"could" to "kood",
		"would" to "wood",
		"should" to "shood",
		"ubuntu" to "ooboonoo",
		"youtube" to "yootoob",
		"whatsapp" to "wotsap",
		"facebook" to "faysbook",
		"wikipedia" to "wikipedeeya",
		"wifi" to "wyfy",
		"json" to "jayson",
		"kathmandu" to "katmandoo",
		"pokhara" to "pokhruh",
		"bengaluru" to "bengalooroo",
		"mumbai" to "mumbuy",
		"delhi" to "dellee",
		"hindi" to "hindee",
		"nepal" to "nuhpaul",
		"devanagari" to "dayvuhnahguree",
		"krishna" to "krishnuh",
		"shiva" to "shivuh",
		"sita" to "seeta",
		"buddha" to "booduh",
		"everest" to "evurust",
		"himalaya" to "himuhlayuh",
		"gorkha" to "gorkha",
		"sherpa" to "shurpuh",
		"diwali" to "divalee",
		"dashain" to "dushain",
		"tihar" to "tihar",
		"holi" to "holee",
		"namaste" to "nuhmustay",
		"rupees" to "roopees",
		"java" to "jahvuh",
		"javascript" to "jahvuhscript"
	)

	private val AMERICAN: Map<String, String> = mapOf(
		"schedule" to "skedjool",
		"either" to "eether",
		"neither" to "neether",
		"tomato" to "tuhmaytoh",
		"vase" to "vayss",
		"route" to "rowt",
		"advertisement" to "advurtyzmunt",
		"privacy" to "pryvusee",
		"lieutenant" to "lootenant",
		"zebra" to "zeebruh",
		"oregano" to "orehgunoh",
		"garage" to "guhrahzh",
		"missile" to "missul",
		"fertile" to "furtul",
		"docile" to "dossul",
		"hostile" to "hostul",
		"fragile" to "frajul",
		"agile" to "ajul",
		"clerk" to "klurk",
		"leisure" to "leezhur",
		"vitamin" to "vytumun",
		"dynasty" to "dynustee",
		"process" to "prossess",
		"data" to "daytuh",
		"patent" to "patent",
		"forehead" to "forhed",
		"mobile" to "mohbl"
	)

	private val BRITISH: Map<String, String> = mapOf(
		"schedule" to "shedyool",
		"either" to "eyether",
		"neither" to "neyether",
		"tomato" to "tuhmahtoh",
		"vase" to "vaahz",
		"route" to "root",
		"advertisement" to "advurtismunt",
		"privacy" to "privusee",
		"lieutenant" to "leftenant",
		"zebra" to "zebruh",
		"oregano" to "orrugahnoh",
		"garage" to "garrahzh",
		"missile" to "missyle",
		"fertile" to "furtyle",
		"docile" to "dosyle",
		"hostile" to "hostyle",
		"fragile" to "frajyle",
		"agile" to "ajyle",
		"clerk" to "klark",
		"leisure" to "lezhur",
		"vitamin" to "vitumun",
		"dynasty" to "dinustee",
		"process" to "prohssess",
		"data" to "dahtuh",
		"patent" to "paytent",
		"forehead" to "forrid",
		"mobile" to "mohbile"
	)

	fun apply(text: String, british: Boolean): String {
		if (text.isEmpty()) return text
		val dialect = if (british) BRITISH else AMERICAN
		val out = StringBuilder(text.length + 16)
		var at = 0
		var changed = false
		while (at < text.length) {
			val c = text[at]
			if (c == '`') {
				val end = EvvEngine.annotationEnd(text, at)
				out.append(text, at, end)
				at = end
				continue
			}
			if (!isLetter(c)) {
				out.append(c)
				at++
				continue
			}
			var end = at
			while (end < text.length && (isLetter(text[end]) || text[end] == '-' || text[end] == '\'')) end++
			val word = text.substring(at, end)
			val key = word.lowercase()
			val said = dialect[key] ?: SHARED[key]
			if (said != null && key != said) {
				out.append(said)
				changed = true
			} else {
				out.append(word)
			}
			at = end
		}
		return if (changed) out.toString() else text
	}

	private fun isLetter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'
}
