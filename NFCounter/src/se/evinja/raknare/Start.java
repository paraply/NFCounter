package se.evinja.raknare;
/***
 * HUVUDKLASS
 * 
 * Laddar in ljuden
 * Hanterar inläsningen av inställningar
 * Hanterar de olika räknarna
 * Skapar och hanterar drawer och actionbar
 * Hanterar NFC
 * 
 * NFC-kod delvis hämtad ifrån http://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278 
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
	/************** INSTÄLLNINGAR **************/
	private SharedPreferences appPrefs;
	private static final String KEY_SELECTED = "selected", KEY_SOUNDS = "do_sounds", KEY_VOL = "do_vol";
	private static final int SETTINGS_KOD = 1;
	private boolean doSounds, doVol;
	/************** NFC **************/
	private NfcAdapter nfc_adaptern;
	private static final String MIME_TEXT_PLAIN = "text/plain";
	/************** Räknare **************/
	private Fragment_Countern rakna;
	private MediaPlayer hiSnd,loSnd,limSnd;
	private Vibrator vibby;
	private int selectedCounter = 0;
	private ArrayList<Counter_Object> allCounters  = new ArrayList<Counter_Object>(); //säkrast initiera så inte null
	private Databasen db;
	public Counter_Adapter adapter;
	/************** DRAWERN **************/
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle; 	// navdrawertitel
	private CharSequence mTitle; // sparar apptitel

	private enum ValueChangeSources{ //Agera olika beroende på vilken källa det är som förändrar värdet
		KnappTryck,
		VolymTryck,
		NFC_tagg
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aktivitet_start);
		
		//*** Läs in inställningarna såsom volymknapp/spela upp ljud/senast markerad räknare
		appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		readAppSettings();
		
		//*** Ladda ljuden och initiera vibratorn
		hiSnd = MediaPlayer.create(this, R.raw.hi); 
		loSnd = MediaPlayer.create(this, R.raw.lo);
		limSnd = MediaPlayer.create(this, R.raw.limit);
		vibby = (Vibrator) this.getSystemService(VIBRATOR_SERVICE); //behöver permission

		//*** Ladda in räknarna från databasen och lägg till den i allCounters-arrayen
		db = new Databasen(this);
		allCounters = db.getAllCounters();
		adapter = new Counter_Adapter(); //adaptern som krävs för att drawern ska kunna läsa in våran array
		
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
		if (savedInstanceState != null){ //om så är fallet
			selectedCounter = savedInstanceState.getInt(KEY_SELECTED); //markerad räknare från tidigare state
		}else{ //är en nystart
			handleIntent(getIntent()); // Kolla om vi har startat från Intet av tagg
			selectedCounter = appPrefs.getInt(KEY_SELECTED, 0); //läs in markerad räknare från sharedpreferences
		}
		
		rakna = new Fragment_Countern(); //skapa själva räknarfragmentet
		getFragmentManager().beginTransaction().replace(R.id.huvud_frame, rakna).commit(); //ersätt huvud_frame med vårat counterfragment. Får inte till det bra genom att lägga till fragmentet direkt på layouten.
		getFragmentManager().executePendingTransactions(); //fixar så att fragmentet färdigladdas innan vi gör saker med det
		

		}catch (Exception e){
			meddela(R.string.error + "onCreate: " + e.getMessage());
		}
	}

	/***
	 *  Initierar drawern och actionbaren. Kallas från fragmentsLoaded
	 */
	private void initDrawer(){ 
		try{
		mTitle = mDrawerTitle = getTitle(); //titel kommer användas av drawer

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
		mDrawerList.setOnItemClickListener(new DrawerClickListener());
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, 	//ikon för drawermenyn
				R.string.app_name, 		// nav drawer öppen - description
				R.string.app_name 		// nav drawer stängd - description
				) {
			
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle); //titeln sätt som aktiva räknaren om det finns någon 
				invalidateOptionsMenu(); //kallar onPrepareOptionsMenu() som visar actionbar-ikoner
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle); //titeln sätts som programmets namn 
				invalidateOptionsMenu(); //kallar onPrepareOptionsMenu() som döljer actionbar-ikoner
			}
		};
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerList.setAdapter(adapter);
		
		}catch (Exception e){
			meddela(R.string.error + "initDrawer: " + e.getMessage());
		}
	}

	/***
	 * Kallas från Fragment_Countern när den har laddats färdogt
	 */
	public void fragmentsLoaded(){ 
		if (allCounters.size() == 0){ //skapa en ny counter om det inte finns någon, med defaultvärden
			counter_Skapa(getString(R.string.new_counter), 0, 0, 1000, 1, 1, 0, 0, 0);
		}
		initDrawer(); //initiera drawern
		selectDrawer(selectedCounter); //välj senaste markerad counter eller den första
	}

	/***
	 * När instansen sparas
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY_SELECTED, selectedCounter);
	}

	/***
	 * Läser in programmets inställningar
	 * Kallas från onCreate och när settings-aktiviteten stängs
	 */
	private void readAppSettings(){ 
		selectedCounter = appPrefs.getInt(KEY_SELECTED, 0); 
		doSounds = appPrefs.getBoolean(KEY_SOUNDS, true);
		doVol = appPrefs.getBoolean(KEY_VOL, true);
	}

	/***
	 * Kallad när settingsaktivitet färdig
	 */
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data){
		super.onActivityResult(reqCode, resCode, data);
		if (reqCode == SETTINGS_KOD){
			readAppSettings(); //uppdatera ifall inställningarna har ändrats
		}
	}

	/***
	 * Kallas när programet startas upp igen
	 */
	@Override
	protected void onResume() {
		super.onResume();
		setupForegroundDispatch(this, nfc_adaptern); //Aktivitet måste vara i foreground -annars IllegalStateException
	}

	/***
	 * Kallas när programmet stängs ner
	 */
	@Override
	protected void onPause() {
		stopForegroundDispatch(this, nfc_adaptern); // Måste vara före onPause. Annars IllegalArgumentException
		super.onPause();
		counter_Spara(); //spara nuvarande counter
		//*** Spara inställningarna
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
	 * Om inställd på att reagera på volymknapparna
	 * hantera detta här
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
	 * Måste ha onkeyup annars ändras volymen samtidigt som den räknar
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
		case R.id.action_settings:  				/**** INSTÄLLNINGAR ****/
			Intent intent = new Intent(this, Installningar.class);
			startActivityForResult(intent, SETTINGS_KOD); //starta inställningsaktivitet
			return true;

		case R.id.action_ny: 						/**** NY ****/
			counter_NyDialog();
			return true;


		case R.id.action_radera: 					/**** RADERA ****/
			counter_radera();
			invalidateOptionsMenu(); //gör någon nytta?
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
	 * hjälpgrej för att visa meddelanden
	 */
	public void meddela(String msg){ 
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	/*********************** DRAWER ****************/

	/***
	 * Växla mellan apptitel och countertitel
	 */
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}
	
	/***
	 * Väljer item i drawern efter parametern position och stänger den sedan
	 */
	public void selectDrawer(int position){
		try{
		if ((allCounters.size() > 0) && (position <= allCounters.size())){ //För säkerhets skull
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
	 * Efter att konfigurationen har ändrats
	 * Skicka ny information till drawer toggle
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}


	/*********************** NFC ****************/ 
	
	/***
	 * Hantera NFC-intent. Kallas från onCreate och onNewIntent
	 */
	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) { //tag av typen NDEF = det vi vill ha
			String type = intent.getType();
			if (MIME_TEXT_PLAIN.equals(type)) { // måste vara av av mime-typen plain/text, inte location, url eller nå annat gojs
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				new NdefReaderTask().execute(tag); // låt våran inre klass fixa läsandet av taggen
			} else {
				Toast.makeText(this, "Wrong MIME-type " + type, Toast.LENGTH_LONG).show();
			}
		}
	}

	/***
	 *	Inre klass som läser in NFC-taggen
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
		 *	Läser in texten 
		 */
		private String readText(NdefRecord record) throws UnsupportedEncodingException {
			byte[] payload = record.getPayload();
			String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";  // Get the Text Encoding
			int languageCodeLength = payload[0] & 0063; // Get the Language Code
			return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding); // Get the Text
		}
		
		/***
		 * När taggen har lästs färdig
		 * Kalla på hanteraNFCStrang
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				hanteraNFCStrang(result);
			}
		}
	}

	/***
	 *	Hantera strängen på nfc-taggen 
	 */
	private void hanteraNFCStrang(String tagg){
		if (tagg.equals("plussa") ){
			inkrement(ValueChangeSources.NFC_tagg);
		}else if (tagg.equals("minusa")){
			dekrement(ValueChangeSources.NFC_tagg);
		}
	}

	
	/***
	 * Ny tagg lästs
	 */
	@Override
	protected void onNewIntent(Intent intent) { // Kallas när tag är kopplad till enheten
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
	 *	inkrement från layout 
	 */
	public void inkrement(View v) {
		inkrement(ValueChangeSources.KnappTryck);
	}

	/***
	 *	inkrement från alla källor 
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
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet gör det
					hiSnd.start();
				}
				adapter.notifyDataSetChanged();
			}else{
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet gör det
					limSnd.start();
				}
				rakna.flash();
			}
		}
	}

	/***
	 *	dekrement från layout 
	 */
	public void dekrement(View v) {
		dekrement(ValueChangeSources.KnappTryck);
	}

	/***
	 *	dekrement från alla källor 
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
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet gör det
					loSnd.start();
				}
				adapter.notifyDataSetChanged();
			}else{
				if (doSounds && v != ValueChangeSources.NFC_tagg){ //spela inte ljud vid nfc-tag eftersom systemet gör det
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
		if (allCounters.size() == 0){ //om går från 0 till 1 antal räknare
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
	 * Sätt nuvarande (om finns någon) counter till sitt minimumvärde
	 */
	private void counter_nolla(){
		if (selectedCounter != -1){
			allCounters.get(selectedCounter).value = allCounters.get(selectedCounter).min;
			rakna.displayValue(allCounters.get(selectedCounter).min);
		}
	}

	/***
	 * Radera aktiv counter (om det finns någon)
	 */
	private void counter_radera(){
		if (allCounters.size() != 0) {
			db.deleteCounter(allCounters.get(selectedCounter)); 
			allCounters.remove(selectedCounter);
			if (allCounters.size() == 0){ //inga räknare kvar
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
	 * Dialog för att skapa ny counter
	 * TODO fixa mer fält och NFC-skrivare
	 */
	private void counter_NyDialog(){
		final EditText input = new EditText(this);
		input.setHint(getString(R.string.new_counter_hinta));
		input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) }); //Max femton tecken längd på namn
		
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
			public void onClick(DialogInterface dialog, int whichButton) {} // Gör ingenting
		}).show();
	}

	/***
	 *	Speciell adapter för att kunna hantera en arraylist med Counter_Object
	 *	Fyller in namnet/värdet på räknaren och ikon för varje item i drawern
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
			//TODO olika ikoner för olika themes
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
