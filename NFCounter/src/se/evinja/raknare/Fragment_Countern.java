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
 *	R�KNARFRAGMENTET
 *	Hanterar sj�lva counter-texten
 *	D�ljer counter-texten och knapparna n�r det inte finns n�gra r�knare
 *	Blinkar n�r countern har n�tt sin gr�ns 
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

		((Start) getActivity()).fragmentsLoaded(); //kalla p� fragmentsLoaded i Start n�r vi �r f�rdiga. B�sta s�ttet att slippa nullpointerexceptions.
		return myView;
	}


	//n�r det inte finns n�gra r�knare visa en text
	public void showAddNewText(){
		noCountersText.setVisibility(View.VISIBLE); //visa l�gg-till-ny-text
		decBtn.setVisibility(View.INVISIBLE); //och g�m knapparna
		incBtn.setVisibility(View.INVISIBLE);
		counter_text.setVisibility(View.INVISIBLE);
	}

	//d�lj texten n�r det lagts till en ny r�knare
	public void hideAddNewText(){
		noCountersText.setVisibility(View.INVISIBLE); //visa l�gg-till-ny-text
		decBtn.setVisibility(View.VISIBLE); //och g�m knapparna
		incBtn.setVisibility(View.VISIBLE);
		counter_text.setVisibility(View.VISIBLE);
	}

	//visa ett v�rde
	public void displayValue(int newValue){
		if (counter_text != null){
			counter_text.setText(Integer.toString(newValue));
		}else{
			System.err.println("counter_text �r null");
		}
	}

	//blinka n�r countern har n�tt sin gr�ns
	public void flash() { //TODO fixa annan b�ttre effekt, denna h�nger kvar emellan�t
		final int saveColor = counter_text.getCurrentTextColor();
//		counter_text.setTextColor(getResources().getColor(R.color.countern_flasha));
		counter_text.flashOut();

		Handler handler = new Handler(); //k�r run efter best�md tid
		handler.postDelayed(new Runnable() {  
			public void run() { 
				counter_text.flashIn();
				counter_text.setTextColor(saveColor);
			} 
		}, 500); // tid = 500 millisekunder

	}
}
