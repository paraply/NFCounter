package se.evinja.raknare;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;


/***
 * 	INSTÄLLNINGSKLASS
 *	Skapar ett preferencefragment och 
 * 	läser in inställningar ifrån xml-filen settings
 */
public class Installningar extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new Globala()).commit();
	}


	public class Globala extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);
		}
	}

	
}

