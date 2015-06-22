package se.evinja.raknare;
/***
 * HUVUDKLASS
 * 
 * Laddar in ljuden
 * Hanterar inl�sningen av inst�llningar
 * Hanterar de olika r�knarna
 * Skapar och hanterar drawer och actionbar
 * Hanterar NFC
 * 
 * NFC-kod delvis h�mtad ifr�n http://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278 
 */


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Start extends Activity{
	/************** INST�LLNINGAR **************/
	private SharedPreferences appPrefs;
	private static final String KEY_SELECTED = "selected", KEY_SOUNDS = "do_sounds", KEY_VOL = "do_vol";
	private static final int SETTINGS_KOD = 1;
	private boolean doSounds, doVol;
	/************** NFC **************/
	private NfcAdapter nfc_adaptern;
	private static final String MIME_TEXT_PLAIN = "text/plain";
	/************** R�knare **************/
	private Fragment_Countern rakna;
	private MediaPlayer hiSnd,loSnd,limSnd;
	private Vibrator vibby;
	private int selectedCounter = 0;
	private ArrayList<Counter_Object> allCounters  = new ArrayList<Counter_Object>(); //s�krast initiera s� inte null
	private Databasen db;
	public Counter_Adapter adapter;
	/************** DRAWERN **************/
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle; 	// navdrawertitel
	private CharSequence mTitle; // sparar apptitel

	private enum ValueChangeSources{ //Agera olika beroende p� vilken k�lla det �r som f�r�ndrar v�rdet
		KnappTryck,
		VolymTryck,
		NFC_tagg
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aktivitet_start);
		
		//*** L�s in inst�llningarna s�som volymknapp/spela upp ljud/senast markerad r�knare
		appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		readAppSettings();
		
		//*** Ladda ljuden och initiera vibratorn
		hiSnd = MediaPlayer.create(this, R.raw.hi); 
		loSnd = MediaPlayer.create(this, R.raw.lo);
		limSnd = MediaPlayer.create(this, R.raw.limit);
		vibby = (Vibrator) this.getSystemService(VIBRATOR_SERVICE); //beh�ver permission

		//*** Ladda in r�knarna fr�n databasen och l�gg till den i allCounters-arrayen
		db = new Databasen(this);
		allCounters = db.getAllCounters();
		adapter = new Counter_Adapter(); //adaptern som kr�vs f�r att drawern ska kunna l�sa in v�ran array
		
		//*** Starta upp NFC
		nfc_adaptern = NfcAdapter.getDefaultAdapter(this); 
		if (nfc_adaptern == null) { //kolla om nfc-adapter finns
			meddela(getString(R.string.nfc_not_exists));
		}else{
			if (!nfc_adaptern.isEnabled()) {
				meddela(getString(R.string.nfc_disabled));
			}
		}
		
		//*** Kolla om vi har en tidigare instans
		if (savedInstanceState != null){ //om s� �r fallet
			selectedCounter = savedInstanceState.getInt(KEY_SELECTED); //markerad r�knare fr�n tidigare state
		}else{ //�r en nystart
			handleIntent(getIntent()); // Kolla om vi har startat fr�n Intet av tagg
			selectedCounter = appPrefs.getInt(KEY_SELECTED, 0); //l�s in markerad r�knare fr�n sharedpreferences
		}
		
		rakna = new Fragment_Countern(); //skapa sj�lva r�knarfragmentet
		getFragmentManager().beginTransaction().replace(R.id.huvud_frame, rakna).commit(); //ers�tt huvud_frame med v�rat counterfragment. F�r inte till det bra genom att l�gga till fragmentet direkt p� layouten.
		getFragmentManager().executePendingTransactions(); //fixar s� att fragmentet f�rdigladdas innan vi g�r saker med det
		

		}catch (Exception e){
			meddela(R.string.error + "onCreate: " + e.getMessage());
		}
	}

	/***
	 *  Initierar drawern och actionbaren. Kallas fr�n fragmentsLoaded
	 */
	private void initDrawer(){ 
		try{
		mTitle = mDrawerTitle = getTitle(); //titel kommer anv�ndas av drawer

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
		mDrawerList.setOnItemClickListener(new DrawerClickListener());
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, 	//ikon f�r drawermenyn
				R.string.app_name, 		// nav drawer �ppen - description
				R.string.app_name 		// nav drawer st�ngd - description
				) {
			
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle); //titeln s�tt som aktiva r�knaren om det finns n�gon 
				invalidateOptionsMenu(); //kallar onPrepareOptionsMenu() som visar actionbar-ikoner
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle); //titeln s�tts som programmets namn 
				invalidateOptionsMenu(); //kallar onPrepareOptionsMenu() som d�ljer actionbar-ikoner
			}
		};
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerList.setAdapter(adapter);
		
		}catch (Exception e){
			meddela(R.string.error + "initDrawer: " + e.getMessage());
		}
	}

	/***
	 * Kallas fr�n Fragment_Countern n�r den har laddats f�rdogt
	 */
	public void fragmentsLoaded(){ 
		if (allCounters.size() == 0){ //skapa en ny counter om det inte finns n�gon, med defaultv�rden
			counter_Skapa(getString(R.string.new_counter), 0, 0, 1000, 1, 1, 0, 0, 0);
		}
		initDrawer(); //initiera drawern
		selectDrawer(selectedCounter); //v�lj senaste markerad counter eller den f�rsta
	}

	/***
	 * N�r instansen sparas
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY_SELECTED, selectedCounter);
	}

	/***
	 * L�ser in programmets inst�llningar
	 * Kallas fr�n onCreate och n�r settings-aktiviteten st�ngs
	 */
	private void readAppSettings(){ 
		selectedCounter = appPrefs.getInt(KEY_SELECTED, 0); 
		doSounds = appPrefs.getBoolean(KEY_SOUNDS, true);
		doVol = appPrefs.getBoolean(KEY_VOL, true);
	}

	/***
	 * Kallad n�r settingsaktivitet f�rdig
	 */
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data){
		super.onActivityResult(reqCode, resCode, data);
		if (reqCode == SETTINGS_KOD){
			readAppSettings(); //uppdatera ifall inst�llningarna har �ndrats
		}
	}

	/***
	 * Kallas n�r programet startas upp igen
	 */
	@Override
	protected void onResume() {
		super.onResume();
		setupForegroundDispatch(this, nfc_adaptern); //Aktivitet m�ste vara i foreground -annars IllegalStateException
	}

	/***
	 * Kallas n�r programmet st�ngs ner
	 */
	@Override
	protected void onPause() {
		stopForegroundDispatch(this, nfc_adaptern); // M�ste vara f�re onPause. Annars IllegalArgumentException
		super.onPause();
		counter_Spara(); //spara nuvarande counter
		//*** Spara inst�llningarna
		SharedPreferences.Editor editor = appPrefs.edit();
		editor.putInt(KEY_SELECTED, selectedCounter);
		editor.putBoolean(KEY_SOUNDS, doSounds);
		editor.commit();
	}


	/***
	 * Inflate:ar menyn
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) { 
		getMenuInflater().inflate(R.menu.huvudmeny, menu);
		return true;
	}

	/***
	 * Om inst�lld p� att reagera p� volymknapparna
	 * hantera detta h�r
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (doVol){
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
				dekrement(ValueChangeSources.VolymTryck);
				return true;
			}
			else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
				inkrement(ValueChangeSources.VolymTryck);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/***
	 * M�ste ha onkeyup annars �ndras volymen samtidigt som den r�knar
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		if (doVol){
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
				return true;
			}
			else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
				return true;
			}
		}

		return super.onKeyUp(keyCode, event);

	}

	/***
	 * Hantera klick i actionbaren
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		
		int id = item.getItemId();
		switch (id) { 
		case R.id.action_settings:  				/**** INST�LLNINGAR ****/
			Intent intent = new Intent(this, Installningar.class);
			startActivityForResult(intent, SETTINGS_KOD); //starta inst�llningsaktivitet
			return true;

		case R.id.action_ny: 						/**** NY ****/
			counter_NyDialog();
			return true;


		case R.id.action_radera: 					/**** RADERA ****/
			counter_radera();
			invalidateOptionsMenu(); //g�r n�gon nytta?
			return true;


		case R.id.action_nolla: 					/***** NOLLA ****/
			counter_nolla();
			return true;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	/***
	 * hj�lpgrej f�r att visa meddelanden
	 */
	public void meddela(String msg){ 
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	/*********************** DRAWER ****************/

	/***
	 * V�xla mellan apptitel och countertitel
	 */
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}
	
	/***
	 * V�ljer item i drawern efter parametern position och st�nger den sedan
	 */
	public void selectDrawer(int position){
		try{
		if ((allCounters.size() > 0) && (position <= allCounters.size())){ //F�r s�kerhets skull
			mDrawerList.setItemChecked(position, true);
			mDrawerList.setSelection(position);
			setTitle(allCounters.get(position).name);
			mDrawerLayout.closeDrawer(mDrawerList);
			selectedCounter = position;
			rakna.displayValue( allCounters.get(selectedCounter).value);
		}else{
			meddela("Position non-existing in selectDrawer: " + position);
		}
		}catch (Exception e){
			meddela(R.string.error + "selectDrawer: " + e.getMessage());
		}
	}
	
	/***
	 * Lyssnar efter klick i drawern 
	 */
	private class DrawerClickListener implements
	ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			counter_Spara();
			selectDrawer(position);
			rakna.displayValue(allCounters.get(position).value);
		}
	}

	/***
	 * Efter create
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState(); //synkronisera toggle efter onRestoreInstanceState
	}
	
	/***
	 * Efter att konfigurationen har �ndrats
	 * Skicka ny information till drawer toggle
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}


	/*********************** NFC ****************/ 
	
	/***
	 * Hantera NFC-intent. Kallas fr�n onCreate och onNewIntent
	 */
	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) { //tag av typen NDEF = det vi vill ha
			String type = intent.getType();
			if (MIME_TEXT_PLAIN.equals(type)) { // m�ste vara av av mime-typen plain/text, inte location, url eller n� annat gojs
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				new NdefReaderTask().execute(tag); // l�t v�ran inre klass fixa l�sandet av taggen
			} else {
				Toast.makeText(this, "Wrong MIME-type " + type, Toast.LENGTH_LONG).show();
			}
		}
	}

	/***
	 *	Inre klass som l�ser in NFC-taggen
	 */
	private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
		@Override
		protected String doInBackground(Tag... params) {
			Tag tag = params[0];
			Ndef ndef = Ndef.get(tag);
			if (ndef == null) { // NDEF is not supported by this Tag. 
				meddela("NDEF not supported");
				return null;
			}

			NdefMessage ndefMessage = ndef.getCachedNdefMessage();
			NdefRecord[] records = ndefMessage.getRecords();

			for (NdefRecord ndefRecord : records) {
				if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
					try {
						return readText(ndefRecord);
					} catch (UnsupportedEncodingException e) {
						System.out.println(e.getMessage());
						meddela("Unsupported encoding");
					}
				}
			}

			return null;
		}

		/***
		 *	L�ser in texten 
		 */
		private String readText(NdefRecord record) throws UnsupportedEncodingException {
			byte[] payload = record.getPayload();
			String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";  // Get the Text Encoding
			int languageCodeLength = payload[0] & 0063; // Get the Language Code
			return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding); // Get the Text
		}
		
		/***
		 * N�r taggen har l�sts f�rdig
		 * Kalla p� hanteraNFCStrang
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				hanteraNFCStrang(result);
			}
		}
	}

	/***
	 *	Hantera str�ngen p� nfc-taggen 
	 */
	private void hanteraNFCStrang(String tagg){
		if (tagg.equals("plussa") ){
			inkrement(ValueChangeSources.NFC_tagg);
		}else if (tagg.equals("minusa")){
			dekrement(ValueChangeSources.NFC_tagg);
		}
	}

	
	/***
	 * Ny tagg l�sts
	 */
	@Override
	protected void onNewIntent(Intent intent) { // Kallas n�r tag �r kopplad till enheten
		handleIntent(intent);
	}

	/**
	 * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
	 * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][]{};


		filters[0] = new IntentFilter(); // Samma filter som i manifest
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check mime type.");
		}

		adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
	}

	/**
	 * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
	 * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}


	/*********************** COUNTERS ****************/
	
	/***
	 *	inkrement fr�n layout 
	 */
	public void inkrement(View v) {
		inkrement(ValueChangeSources.KnappTryck);
	}

	/***
	 *	inkrement fr�n alla k�llor 
	 */
	public void inkrement(ValueChangeSources v){
		if (selectedCounter != -1){
			Counter_Object c = allCounters.get(selectedCounter);

			if (v == ValueChangeSources.KnappTryck){
				vibby.vibrate(45);
			}
			if ((c.value + c.incby) <= c.max){
				c.value = c.value + c.incby;
				rakna.displayValue(c.value);
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet g�r det
					hiSnd.start();
				}
				adapter.notifyDataSetChanged();
			}else{
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet g�r det
					limSnd.start();
				}
				rakna.flash();
			}
		}
	}

	/***
	 *	dekrement fr�n layout 
	 */
	public void dekrement(View v) {
		dekrement(ValueChangeSources.KnappTryck);
	}

	/***
	 *	dekrement fr�n alla k�llor 
	 */
	public void dekrement(ValueChangeSources v){
		if (selectedCounter != -1){
			Counter_Object c = allCounters.get(selectedCounter);

			if (v == ValueChangeSources.KnappTryck){
				vibby.vibrate(45);
			}
			if ((c.value-c.decby) >= c.min){
				c.value = c.value - c.decby;
				rakna.displayValue(c.value);
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet g�r det
					loSnd.start();
				}
				adapter.notifyDataSetChanged();
			}else{
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet g�r det
					limSnd.start();
				}
				rakna.flash();
			}
		}
	}


	/***
	 * Skapa en ny counter
	 */
	private void counter_Skapa(String name,int value,int min_value,int max_value,int inc_by,int dec_by,int nfc_inc,int nfc_dec, int theme){
		if (allCounters.size() == 0){ //om g�r fr�n 0 till 1 antal r�knare
			rakna.hideAddNewText();
		}else{
			counter_Spara();
		}
		Counter_Object ny = db.addCounter(new Counter_Object(name, value, min_value, max_value, inc_by, dec_by, nfc_inc, nfc_dec,theme), false);
		if (ny.id != -1 ){
		allCounters.add(ny);
		}
	}

	/***
	 * Spara nuvarande counter
	 */
	public void counter_Spara(){
		if (selectedCounter != -1){
			db.addCounter(allCounters.get(selectedCounter), true); //update = true
		}
	}
	
	/***
	 * S�tt nuvarande (om finns n�gon) counter till sitt minimumv�rde
	 */
	private void counter_nolla(){
		if (selectedCounter != -1){
			allCounters.get(selectedCounter).value = allCounters.get(selectedCounter).min;
			rakna.displayValue(allCounters.get(selectedCounter).min);
		}
	}

	/***
	 * Radera aktiv counter (om det finns n�gon)
	 */
	private void counter_radera(){
		if (allCounters.size() != 0) {
			db.deleteCounter(allCounters.get(selectedCounter)); 
			allCounters.remove(selectedCounter);
			if (allCounters.size() == 0){ //inga r�knare kvar
				rakna.showAddNewText();
				selectedCounter = -1;
			}else{
				if (selectedCounter >= allCounters.size()){
					selectedCounter = allCounters.size() -1;
				}
				rakna.displayValue(allCounters.get(selectedCounter).value);
				selectDrawer(selectedCounter);
			}
		}else{
			getActionBar().setTitle(getTitle());
			mTitle = getTitle();
		}
	}
	
	/***
	 * Dialog f�r att skapa ny counter
	 * TODO fixa mer f�lt och NFC-skrivare
	 */
	private void counter_NyDialog(){
		final EditText input = new EditText(this);
		input.setHint(getString(R.string.new_counter_hinta));
		input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) }); //Max femton tecken l�ngd p� namn
		
		new AlertDialog.Builder(this)
		.setTitle(getString(R.string.new_counter_dialog))
		.setView(input)
		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() { 
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText(); 
				counter_Spara();
				counter_Skapa(value.length() == 0 ? getString(R.string.new_counter) + " " + (allCounters.size() + 1) : value.toString(), 0, 0, 1000, 1, 1, 0, 0, 0);
				selectDrawer(allCounters.size() -1);
				rakna.displayValue(allCounters.get(selectedCounter).value);
			}
		}).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() { 
			public void onClick(DialogInterface dialog, int whichButton) {} // G�r ingenting
		}).show();
	}

	/***
	 *	Speciell adapter f�r att kunna hantera en arraylist med Counter_Object
	 *	Fyller in namnet/v�rdet p� r�knaren och ikon f�r varje item i drawern
	 */
	class Counter_Adapter extends BaseAdapter {
		@Override
		public int getCount() {
			return allCounters.size();
		}

		@Override
		public Object getItem(int position) {		
			return allCounters.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
				convertView = mInflater.inflate(R.layout.drawer_list_item, null);
			}
			//TODO olika ikoner f�r olika themes
			//			ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);
			TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
			TextView txtCount = (TextView) convertView.findViewById(R.id.counter);

			//			imgIcon.setImageResource( themeIcons.getResourceId(allCounters.get(position).theme, -1));    
			//			themeIcons.recycle();
			txtTitle.setText(allCounters.get(position).name);
			txtCount.setText(allCounters.get(position).getFormattedAmount());

			return convertView;
		}

	}

}
