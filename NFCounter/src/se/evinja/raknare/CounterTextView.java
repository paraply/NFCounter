package se.evinja.raknare;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

/***
 * COUNTERTEXTVIEW
 * Ett eget objekt f�r countersiffran. Ut�kar vanliga textviewens funktionalitet.
 * Anv�nder typsnitt fr�n mappen assets/fonts
 *
 */


public class CounterTextView extends TextView {
	 
	public CounterTextView(Context context) {
		super(context);
		 initiera(context);
	}
	
	public CounterTextView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    initiera(context);
	}

	public CounterTextView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    initiera(context);
	}
	
	/***
	 *	Loads the font from assets/font 
	 */
	private void initiera(Context context){
		Typeface fonten = Typeface.createFromAsset(context.getAssets(),  "fonts/DejaVuSansMono.ttf"); //TODO ta bort bokst�ver fr�n typsnittet f�r att spara lite space
		super.setTypeface( fonten); 
		justeraSize();
	}
	
	@Override
	protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter){
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		if (lengthBefore != lengthAfter){
			justeraSize();
		}
		super.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in)); //lite fade
	}
	
	
	public void flashIn() {
		super.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in)); //lite fade
	}
	
	public void flashOut() {
		super.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out)); //lite fade
	}
	
	/***
	 * F�rs�k att justera textstorleken efter sk�rmen
	 */
	private void justeraSize(){
		super.setTextSize(TypedValue.COMPLEX_UNIT_DIP,200 - super.getText().length() * 20); 
	}
	

	



}
