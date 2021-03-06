package fr.renzo.wikipoff;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class Wiki implements Serializable {

	private static final long serialVersionUID = -4809830901675667519L;
	private static final String TAG = "WIKI";
	
	// Careful when changing those, you might break everything.
	// Blame the poor original programmer
	public static final int WIKINOTINSTALLED = 2;
	public static final int WIKIEQUAL = 0;
	public static final int WIKIOLDER = -1;
	public static final int WIKINEWER = 1;
	
	private String type="";
	private String langcode="";
	private String langenglish="";
	private String langlocal="";
	private String version="";
	public boolean corrupted=false;

	private ArrayList<WikiDBFile> dbfiles=new ArrayList<WikiDBFile>();

	private transient Context context; 
	
	public Wiki (Context context){
		this.context=context;
	}

	public Wiki (Context context,File sqlitefile) throws WikiException {
		WikiDBFile wdbf =new WikiDBFile(sqlitefile);
		this.context=context;
		if (sqlitefile.getName().endsWith(".sqlite")){
			this.dbfiles.add(wdbf);
			SQLiteDatabase sqlh = openDB(sqlitefile);
			Cursor c;
			String k="";
			String v="";
			try {
				c = sqlh.rawQuery("SELECT * FROM metadata", new String[0]);
				if (c.moveToFirst()) {
					do {
						k = c.getString(0);
						v = c.getString(1);

						if (k.equals("lang-code")) {
							setLangcode(v);
						}else if (k.equals("lang")) {
							setLangcode(v);
						} else if (k.equals("type")) {
							setType(v);
						} else if (k.equals("lang-local")) {
							setLanglocal(v);
						} else if (k.equals("lang-english")) {
							setLangenglish(v);
						} else if (k.equals("date")) {
							wdbf.setGendate(v);
						} else if (k.equals("version")) {
							setVersion(v);
						}
					} while (c.moveToNext());
				}
				sqlh.close();
			} catch (SQLiteDatabaseCorruptException e ){
				e.printStackTrace();
				this.corrupted=true;
				//throw new WikiException("Database file : "+sqlitefile.getName()+" is corrupted. Please delete it or wait for transfer to finish!");
			}

		} else {
			Log.d(TAG,"not a sqlite file to load a Wiki from : "+sqlitefile);
		}
		
	}

	public ArrayList<WikiDBFile> getDBFiles(){
		return this.dbfiles;
	}

	public void setDBFiles(ArrayList<WikiDBFile> list){
		this.dbfiles=list;
	}

	public String getType() {
		if (type=="") {
			return this.context.getString(R.string.database_corrupted);
		}
		return type;
	}
	public String toString(){
		return this.type+" "+this.langlocal+" "+this.getDateAsString();
	}



	private String getDateAsString() {
		return this.dbfiles.get(0).getDateAsString();
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLangcode() {
		return langcode;
	}

	public void setLangcode(String code) {
		assert null!=code;
		this.langcode= code;
	}
	public String getLangenglish() {
		return langenglish;
	}
	public void setLangenglish(String langenglish) {
		this.langenglish = langenglish;
	}
	public String getLanglocal() {
		return langlocal;
	}
	public void setLanglocal(String langlocal) {
		this.langlocal = langlocal;
	}
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	private SQLiteDatabase openDB(File sqlitefile) throws SQLiteDatabaseCorruptException {
		SQLiteDatabase sqlh=SQLiteDatabase.openDatabase(sqlitefile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS );
		return sqlh;
	}

	public boolean isMissing() throws WikiException {
		SharedPreferences config= PreferenceManager.getDefaultSharedPreferences(context);
		String storage = config.getString(context.getString(R.string.config_key_storage), StorageUtils.getDefaultStorage());

		File rootDbDir= new File(storage,context.getString(R.string.DBDir));
		ArrayList<File> allpaths = new ArrayList<File>(Arrays.asList(rootDbDir.listFiles()));
		ArrayList<String> allnames = new ArrayList<String>();
		for(File i : allpaths) {
			allnames.add(i.getName());
		}
		return (! allnames.containsAll(getDBFilesnamesAsList()));
	}

	public boolean isSelected() {
		String key =context.getString(R.string.config_key_selecteddbfiles);
		SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(context);
		ArrayList<String> sel_db=new ArrayList<String>(Arrays.asList(config.getString(key, "").split(",")));
		if (sel_db.isEmpty())
			return false;
		if (sel_db.equals(getDBFilesnamesAsList())) {
			return true;
		} else {
			return false;
		}
	}

	public String getLocalizedGendate() {
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
		WikiDBFile file = this.dbfiles.get(0);
		if (file == null) {
			return "CORRUPT DB";
		} else {
			return dateFormat.format(file.getDate());
		}
	}

	public ArrayList<String> getDBFilesnamesAsList() {
		ArrayList<String> res = new ArrayList<String>();
		for (WikiDBFile wdbf : this.dbfiles) {
			res.add(wdbf.getFilename());
		}
		return res;
	}
	public ArrayList<File> getDBFilesasFilesList(){
		ArrayList<File> res = new ArrayList<File>();
		for (WikiDBFile wdbf : this.dbfiles) {
			res.add(new File(wdbf.getFilename()));
		}
		return res;
	}

	public String getSizeReadable(boolean si) {
		long size=0;
		for (WikiDBFile wdbf : this.dbfiles) {
			size+=wdbf.getSize();
		}
		int unit = si ? 1000 : 1024;
		if (size < unit) {
			return Long.toString(size) + " B";
		}
		int exp = (int) (Math.log(size) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);
	}

	public void addDBFile(File file){
		dbfiles.add(new WikiDBFile(file));
	}

	public String getGendateAsString() {
		return this.dbfiles.get(0).getGendate();
	}
	public Date getGendateAsDate() {
		return this.dbfiles.get(0).getDate();
	}
	public String getFilenamesAsString() {
		return TextUtils.join("+", getDBFilesnamesAsList());
	}
	
	public int compareWithWiki(Wiki w_tocheck) {
		Log.d(TAG,"Comparing this: "+this.toString()+" with to_check: "+w_tocheck.toString());
		if (this.getType().equals(w_tocheck.getType()) && this.getLangcode().equals(w_tocheck.getLangcode()) ){
			// w_tocheck is the same wikicode (lang + type)
			int res=this.getGendateAsDate().compareTo(w_tocheck.getGendateAsDate());
			return res;
		}
		return WIKINOTINSTALLED;
	}

	
}