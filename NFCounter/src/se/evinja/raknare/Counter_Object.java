package se.evinja.raknare;

/***
 * 
 * COUNTER OBJECT
 * Används för att spara information om varje counter
 *
 */
public class Counter_Object {
		//Getters and setters are for pussies
		public String name; 
		public int id,value,min,max,incby,decby,nfc_inc,nfc_dec, theme;
		
		public Counter_Object(String name, int value,int min, int max, int incby, int decby, int nfcinc, int nfcdec, int theme){
			this.name = name;
			this.value = value;
			this.min = min;
			this.max = max;
			this.incby = incby;
			this.decby = decby;
			this.nfc_inc = nfcinc;
			this.nfc_dec = nfcdec;
			this.theme = theme;
		}
		
		public String getFormattedAmount(){
			if(value <= 1000000){
				return Integer.toString(value);
			}else{
				return "+999999"; //TODO okej värde?
			}
		}
	}	