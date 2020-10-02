import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;


public class BrokerProxyService {

	public static String toSimpleDateFormat(GregorianCalendar calendar){
	    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
	    fmt.setCalendar(calendar); 
	    String dateFormatted = fmt.format(calendar.getTime());
	    return dateFormatted;
	}

	public static String getCurrentDate() {
	    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
	    String dateFormatted = fmt.format(new Date());
	    return dateFormatted;
	}

	public static String toSimpleDateFormat(Date date){
	    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	    String dateFormatted = fmt.format(date);
	    return dateFormatted;
	}

	public static Properties parsePropertiesString(String s) throws IOException {
	    // grr at load() returning void rather than the Properties object
	    // so this takes 3 lines instead of "return new Properties().load(...);"
	    final Properties p = new Properties();
	    p.load(new StringReader(s));
	    return p;
	}

	public static String getStringValue(Object v) {
		return v != null ? v.toString() : "";
	}

}
