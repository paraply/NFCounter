package se.evinja.raknare;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

/***
 * COUNTERTEXTVIEW
 * Ett eget objekt för countersiffran. Utökar vanliga textviewens funktionalitet.
 * Använder typsnitt från mappen assets/fonts
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
		Typeface fonten = Typeface.createFromAsset(context.getAssets(),  "fonts/DejaVuSansMono.ttf"); //TODO ta bort bokstäver från typsnittet för att spara lite space
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
	 * Försök att justera textstorleken efter skärmen
	 */
	private void justeraSize(){
		super.setTextSize(TypedValue.COMPLEX_UNIT_DIP,200 - super.getText().length() * 20); 
	}
	

	



}
