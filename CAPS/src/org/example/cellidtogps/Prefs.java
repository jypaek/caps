/**
 * Prefs
 * - Android application menu
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
	// option names and default values
	private static final String OPT_GPS = "gps";	
	private static final boolean OPT_GPS_DEF = false;
	private static final String OPT_LOG = "log";
	private static final boolean OPT_LOG_DEF = true;
	private static final String OPT_SAVE_DB = "save_db";
	private static final boolean OPT_SAVE_DB_DEF = false;
	private static final String OPT_LOAD_DB = "load_db";
	private static final boolean OPT_LOAD_DB_DEF = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	// get current value of the music option
	public static boolean getGps(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_GPS, OPT_GPS_DEF);
	}
	
	// get current value of the hint option
	public static boolean getLog(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_LOG, OPT_LOG_DEF);
	}
	
	// get current value of the address lookup option
	public static boolean getSaveDb(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_SAVE_DB, OPT_SAVE_DB_DEF);
	}

	// get current value of the address lookup option
	public static boolean getLoadDb(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OPT_LOAD_DB, OPT_LOAD_DB_DEF);
	}

}
