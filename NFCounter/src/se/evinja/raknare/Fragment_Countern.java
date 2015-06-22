package se.evinja.raknare;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/***
 *	RÄKNARFRAGMENTET
 *	Hanterar själva counter-texten
 *	Döljer counter-texten och knapparna när det inte finns några räknare
 *	Blinkar när countern har nått sin gräns 
 */

public class Fragment_Countern extends Fragment {
	private Button decBtn, incBtn;
	private TextView noCountersText;
	private CounterTextView counter_text;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		View myView = inflater.inflate(R.layout.fragment_countern, container, false);
		noCountersText = (TextView)  myView.findViewById(R.id.no_counters_text);
		decBtn = (Button) myView.findViewById(R.id.button_dec);
		incBtn = (Button) myView.findViewById(R.id.button_inc);
		counter_text = (CounterTextView) myView.findViewById(R.id.countertextview);

		((Start) getActivity()).fragmentsLoaded(); //kalla på fragmentsLoaded i Start när vi är färdiga. Bästa sättet att slippa nullpointerexceptions.
		return myView;
	}


	//när det inte finns några räknare visa en text
	public void showAddNewText(){
		noCountersText.setVisibility(View.VISIBLE); //visa lägg-till-ny-text
		decBtn.setVisibility(View.INVISIBLE); //och göm knapparna
		incBtn.setVisibility(View.INVISIBLE);
		counter_text.setVisibility(View.INVISIBLE);
	}

	//dölj texten när det lagts till en ny räknare
	public void hideAddNewText(){
		noCountersText.setVisibility(View.INVISIBLE); //visa lägg-till-ny-text
		decBtn.setVisibility(View.VISIBLE); //och göm knapparna
		incBtn.setVisibility(View.VISIBLE);
		counter_text.setVisibility(View.VISIBLE);
	}

	//visa ett värde
	public void displayValue(int newValue){
		if (counter_text != null){
			counter_text.setText(Integer.toString(newValue));
		}else{
			System.err.println("counter_text är null");
		}
	}

	//blinka när countern har nått sin gräns
	public void flash() { //TODO fixa annan bättre effekt, denna hänger kvar emellanåt
		final int saveColor = counter_text.getCurrentTextColor();
//		counter_text.setTextColor(getResources().getColor(R.color.countern_flasha));
		counter_text.flashOut();

		Handler handler = new Handler(); //kör run efter bestämd tid
		handler.postDelayed(new Runnable() {  
			public void run() { 
				counter_text.flashIn();
				counter_text.setTextColor(saveColor);
			} 
		}, 500); // tid = 500 millisekunder

	}
}
