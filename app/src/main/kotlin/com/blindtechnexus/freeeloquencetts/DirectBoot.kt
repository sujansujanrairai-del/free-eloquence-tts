package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.os.Build
import java.io.File

object DirectBoot {

	@Volatile
	private var storage: Context? = null

	@Volatile
	private var forApp: Context? = null

	@Volatile
	private var dictionaryDir: File? = null

	fun storage(context: Context): Context {
		val app = context.applicationContext
		storage?.let { if (forApp === app) return it }
		return synchronized(this) {
			storage?.let { if (forApp === app) return it }
			val made = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) app else {
				app.createDeviceProtectedStorageContext() ?: app
			}

			if (made !== app) made.moveSharedPreferencesFrom(app, prefsName(app))
			storage = made
			forApp = app
			made
		}
	}

	fun dictionaries(context: Context): File {
		val app = context.applicationContext
		dictionaryDir?.let { if (forApp === app) return it }
		return synchronized(this) {
			val into = File(storage(context).filesDir, DICTIONARIES)
			dictionaryDir = into
			into
		}
	}

	fun dictionariesReady(context: Context): File {
		val into = dictionaries(context)
		if (!into.isDirectory) into.mkdirs()
		return into
	}

	fun forget() {
		synchronized(this) {
			storage = null
			forApp = null
			dictionaryDir = null
		}
	}

	private fun prefsName(app: Context) = "${app.packageName}_preferences"

	const val DICTIONARIES = "dictionaries"
}
