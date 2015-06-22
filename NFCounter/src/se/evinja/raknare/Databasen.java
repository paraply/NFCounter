package se.evinja.raknare;

/***
 *	DATABASHJÄLPARE
 *	Skapar databas om ingen tidigare existerar
 *	Vid uppgradering tas den gamla bort och en ny skapas
 *
 *	Hanterar att läsa in alla, lägga till, ta bort, uppdatera olika counters
 */


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class Databasen extends SQLiteOpenHelper {
	private static final int DB_version = 5; //om ändras så raderas databasen och en ny skapas
	private static final String DB_namn = "raknaren";
	
	
	private static final String TABELL_raknare = "Counters";
	public static final String 	COLUMN_id = "_id", 
								COLUMN_name = "counter_name", 
								COLUMN_value = "current_value",
								COLUMN_minValue = "min_value", 
								COLUMN_maxValue = "max_value",
								COLUMN_incBy = "inc_by", 
								COLUMN_decBy = "dec_by",
								COLUMN_NFC_inc = "nfc_inc", 
								COLUMN_NFC_dec = "nfc_dec",
								COLUMN_Theme ="theme";
		
	private Context context;

	public Databasen(Context context) {
		super(context, DB_namn, null, DB_version);
		this.context = context;
	}
	
	/***
	 * Skapar tabell om den inte finns redan
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		try{
			db.execSQL("CREATE TABLE " + TABELL_raknare + "(" + COLUMN_id + " integer primary key autoincrement, counter_name text, current_value integer," //TODO orka använda konstanterna + varchar(15)?
					+ "min_value integer, max_value integer, inc_by integer, dec_by integer,"
					+ "nfc_inc integer, nfc_dec integer, theme integer)"
					);

		}catch (Exception e){
			Toast toast = Toast.makeText(context, "Error createing table: " + e.getMessage() ,Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/***
	 * Raderar databasen om versionen inte 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { //om databasen förändrats = ny version måste vi börja om från början
		db.execSQL("DROP TABLE IF EXISTS " + TABELL_raknare);
		onCreate(db);
	}

	/***
	 * Lägger till en nytt counter eller uppdaterar om befintlig om isAnUpdate är satt som true
	 * Vid uppdatering används id:t i counter-objektet för att matcha raden som sql ska uppdatera. 
	 */
	public Counter_Object addCounter(Counter_Object from, boolean isAnUpdate) {
		try{
			ContentValues values = new ContentValues();
			values.put(COLUMN_name, from.name);
			values.put(COLUMN_value, from.value);
			values.put(COLUMN_minValue, from.min);
			values.put(COLUMN_maxValue, from.max);
			values.put(COLUMN_incBy, from.incby);
			values.put(COLUMN_decBy, from.decby);
			values.put(COLUMN_NFC_inc, from.nfc_inc);
			values.put(COLUMN_NFC_dec, from.nfc_dec);
			values.put(COLUMN_Theme, from.theme);
		
		SQLiteDatabase db = this.getWritableDatabase();
		if (isAnUpdate){
			db.update(TABELL_raknare, values, COLUMN_id + " = ?", new String[] { String.valueOf(from.id)});
			return null;
		}else{
			long resultat = db.insert(TABELL_raknare, null, values);
			from.id = (int)resultat; //obs -1 om misslyckats
			db.close();
			return from;
		}
		

		} catch (Exception e){
			Toast toast = Toast.makeText(context, "Error add new: " + e.getMessage() ,Toast.LENGTH_LONG);
			toast.show();
			return null;
		}
		
	}
	
	/***
	 * Returnerar en lista med alla counter-objekt 
	 */
	public ArrayList<Counter_Object> getAllCounters() {
		try{
		ArrayList<Counter_Object> alla = new ArrayList<Counter_Object>();
	       String query = "SELECT  * FROM " + TABELL_raknare;
	       SQLiteDatabase db = this.getWritableDatabase();
	       Cursor cursor = db.rawQuery(query, null);

	       Counter_Object nyC = null;
	       if (cursor.moveToFirst()) {
	           do {
	        	   
	        	   nyC = new Counter_Object(
	        			   cursor.getString(cursor.getColumnIndex(COLUMN_name)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_value)), 
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_minValue)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_maxValue)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_incBy)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_decBy)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_NFC_inc)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_NFC_dec)),
	        			   cursor.getInt(cursor.getColumnIndex(COLUMN_Theme)));
	               alla.add(nyC);
	           } while (cursor.moveToNext());
	       }
	 
	       return alla;
		} catch (Exception e){
			Toast toast = Toast.makeText(context, "Error get counters: " + e.getMessage() ,Toast.LENGTH_LONG);
			toast.show();
			return null;
		}
	   }
	
	/*
	 * Raderar en specifik counter
	 * Matchar id:t i sql-förfrågan för att veta vilken som ska uppdateras
	 */
    public void deleteCounter(Counter_Object raderas) {
    	try{
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABELL_raknare, COLUMN_id + " = ?", new String[] { String.valueOf(raderas.id)});
        //db.close();
		Toast toast = Toast.makeText(context, "DELETED " + raderas.name ,Toast.LENGTH_LONG); //TODO @string
		toast.show();
		} catch (Exception e){
			Toast toast = Toast.makeText(context, "Error deleting: " + e.getMessage() ,Toast.LENGTH_LONG);
			toast.show();
		}
    }
}
