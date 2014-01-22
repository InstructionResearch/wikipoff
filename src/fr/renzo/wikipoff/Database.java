package fr.renzo.wikipoff;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

public class Database   {
	private static final String TAG = "Database";
	public File seldatabasefile;
	private Context context;
	public SQLiteDatabase sqlh;
	private long maxId;
	
	public Database(Context context, File databasefile) throws DatabaseException {
	        this.context=context;
	        this.seldatabasefile = databasefile;
	        String error=checkDatabaseHealth();
	        if ( !error.equals("")) {
	        	Log.e(TAG,"Error: "+error);
	        	throw (new DatabaseException(error));
	        }
			try {
				SQLiteDatabase sqlh = SQLiteDatabase.openDatabase(this.seldatabasefile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS );
				this.sqlh=sqlh;
			} catch (SQLiteCantOpenDatabaseException e) {
    			Toast.makeText(context, "Problem opening database '"+databasefile+"'"+e.getMessage(), Toast.LENGTH_LONG).show();
			} 
			this.maxId=getMaxId();
	}
	
	public String checkDatabaseHealth(){
		String error="";
		String p = seldatabasefile.getAbsolutePath();
		if (seldatabasefile==null) {
			return "I need a database file....";
		}
		if (!seldatabasefile.exists()) {
			return "Unable to find '"+p+"'";
		}
		if (seldatabasefile.length()==0) {
			return "Database file '"+p+"' is an empty file";
		} 
//		if (seldatabasefile.length()<=100000000) { //TODO
//			return "Database file '"+p+"' is less than 100Mb. This is not normal";
//		} 
		return error;
	}
	
	public Cursor myRawQuery(String query) throws DatabaseException {
		return myRawQuery(query,new String[0]);
	}
	public Cursor myRawQuery(String query, String param1) throws DatabaseException {
		return myRawQuery(query,new String[]{param1});
	}
	
	public Cursor myRawQuery(String query, String[] objects ) throws DatabaseException {
		Log.d(TAG,"SQL: "+query);
		for (int i = 0; i < objects.length; i++) {
			Log.d(TAG,"SQL: "+objects[i]);
		}
		Cursor c=null;
		try {
			c = this.sqlh.rawQuery(query, objects);		
			
		} catch (SQLiteException e) {
			if (c!= null){
				c.close();
			}
			throw new DatabaseException(e.getMessage());
		}
		return c;
	}
	
	public String getArticleTitleFromId(int id) throws DatabaseException{
		String res="";

		Cursor cursor;
		cursor = myRawQuery("SELECT title FROM articles WHERE _id = "+id);
	
		if (cursor.getCount()==1) {
			cursor.moveToFirst();
			res = cursor.getString(0);
			
		} else if (cursor.getCount()==0) {
			res = "No article not found with id '"+id+"'";
		} else {
			res = "Found "+cursor.getCount()+" articles with id "+id+"!!!!";
		}
		cursor.close();
		return res;
	}

	public Cursor getAllTitles() throws DatabaseException  {
		return  myRawQuery("SELECT _id,title FROM articles ORDER BY title LIMIT 10 ");

	}

	public Cursor getAllTitles(String query) throws DatabaseException {
		return myRawQuery("SELECT _id,title FROM articles WHERE title LIKE '%?%' ORDER BY title LIMIT 10",query);		
	}
	
	public int getMaxId() throws DatabaseException{
		Cursor c= myRawQuery("SELECT MAX(_id) FROM articles");
		if (c.moveToFirst()){
			return c.getInt(0);
		}
		c.close();
		return 0;
	}
	
	public List<String> getRandomTitles(int nb) throws DatabaseException {
		long[] rnd_ids = new long[nb];
		for (int i = 0; i < rnd_ids.length; i++) {
			rnd_ids[i]=0;
		}
		long max = this.maxId;
		
		int idx=0;
		while (rnd_ids[nb-1]==0) {
			boolean found=false;
			long ir = Math.round(Math.random()*max) + 1;
			for (int i = 0; i < idx+1; i++) {
				if (rnd_ids[i]==ir){
					found=true;
					break;
				}
			}
			if (!found){
				rnd_ids[idx]=ir;
				idx+=1;
			}
		}
		
		ArrayList<String> res = new ArrayList<String>();
		String q="("+rnd_ids[0];
		for (int i = 1; i < rnd_ids.length; i++) {
			q = q+", "+String.valueOf(rnd_ids[i]);
		}
		q = q +" )";
		
		Cursor c = myRawQuery("SELECT title FROM articles WHERE _id IN "+q);
		if (c.moveToFirst()) {
            do {
                String t = c.getString(0);
                res.add(t);
            } while (c.moveToNext());
            
        } else {
        	Log.d(TAG,"What");
        }
		c.close();
		return res;
	}

	public List<String> getRandomTitles() throws DatabaseException {
		int nb=context.getResources().getInteger(R.integer.def_random_list_nb);
		return getRandomTitles(nb);
	}

	public Article getArticleFromId(int article_id) {
		if (article_id == 0) {
			return null;
		}
		Cursor c;
		try {
			c = myRawQuery("SELECT title, text FROM articles WHERE _id= "+article_id);
			if (c.moveToFirst()) {
	            String title=c.getString(0);
	            String text = decodeBlob(c.getBlob(1));
	            Article res = new Article(article_id,title,text);
	            return res;           
	        } else {
	        	Log.d(TAG,"No article found for id '"+article_id+"'");
	        }
			c.close();
		} catch (DatabaseException e) {
			e.alertUser(context);
		}
		
		return null;
	}
	
	public Article getArticleFromTitle(String title) {
		return getArticleFromTitle(title, true);
	}
	
	public Article getArticleFromTitle(String title, boolean redirect) {
		Cursor c;
		try {
			c = myRawQuery("SELECT _id,text FROM articles WHERE title= ?",title);
			if (c.moveToFirst()) {
				int article_id = c.getInt(0);
	            String text = decodeBlob(c.getBlob(1));
	            Article res = new Article(article_id,title,text);
	            return res;           
	        } else {
	        	if (!redirect)
	        		return getArticleFromTitle(getRedirectArticleTitle(title),false);
	        	Log.d(TAG,"No article found for title '"+title+"'");
	        }
			c.close();
		} catch (DatabaseException e) {
			e.alertUser(context);
		}
		return null;
	}

	public int getArticleIdFromTitle(String title) {
		return getArticleIdFromTitle(title,true);
	}

	public int getArticleIdFromTitle(String title,boolean redirect) {
		Cursor c;
		String uppertitle=title.substring(0, 1).toUpperCase() + title.substring(1);
		try {
			c = myRawQuery("SELECT _id FROM articles WHERE title= ? or title =?", new String[]{title, uppertitle});
			if (c.moveToFirst()) {
	            int res=c.getInt(0);
	            c.close();
	            return res;
	        } else {
	        	if (!redirect)
	        		return getArticleIdFromTitle(getRedirectArticleTitle(title),false);
	        	Log.d(TAG,"No article found with title '"+title+"'");
		        
	        }
			c.close();
		} catch (DatabaseException e) {
			e.alertUser(context);
		}
		return 0;
	}

	public String getRedirectArticleTitle(String title) {
		String uppertitle=title.substring(0, 1).toUpperCase() + title.substring(1);
		Cursor c;
		try {
			c = myRawQuery("SELECT title_to FROM redirects WHERE title_from= ? or title_from =?", new String[]{title, uppertitle});
			if (c.moveToFirst()) {
	            String res=c.getString(0);
	            return res;
	        } else {
	        	Log.d(TAG,"No article found with title '"+title+"'");
	        }
			c.close();
		} catch (DatabaseException e) {
			e.alertUser(context);
		}
		return "";
	}
	
	public String decodeBlob(byte[]coded) {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			int propertiesSize = 5;
			byte[] properties = new byte[propertiesSize];
			
			InputStream inStream = new ByteArrayInputStream(coded);
			
			OutputStream outStream = new BufferedOutputStream(baos,1024*1024);
			SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();

			if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
				throw new Exception("input .lzma file is too short");
			if (!decoder.SetDecoderProperties(properties))
				throw new Exception("Incorrect stream properties");
			long outSize = 0;
			for (int i = 0; i < 8; i++)
			{
				int v = inStream.read();
				if (v < 0)
					throw new Exception("Can't read stream size");
				outSize |= ((long)v) << (8 * i);
			}
		
			if (!decoder.Code(inStream, outStream, outSize))
				throw new Exception("Error in data stream");
			outStream.flush();
			outStream.close();
			inStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toString();
	}

	public class DatabaseException extends Exception {

		private static final long serialVersionUID = -4015796136387495698L;
		public DatabaseException(String message) {
			super(message);
		}
		
	    public Builder alertUser(Context context){
	        AlertDialog.Builder dialog = new AlertDialog.Builder(context); 
	        dialog.setTitle("Database Error:");
	        dialog.setMessage(this.toString());
	        dialog.setNeutralButton("Ok", null);
	        dialog.create().show();
	        return dialog;
	    }
	}
}