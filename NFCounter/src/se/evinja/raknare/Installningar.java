package se.evinja.raknare;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;


/***
 * 	INST�LLNINGSKLASS
 *	Skapar ett preferencefragment och 
 * 	l�ser in inst�llningar ifr�n xml-filen settings
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

