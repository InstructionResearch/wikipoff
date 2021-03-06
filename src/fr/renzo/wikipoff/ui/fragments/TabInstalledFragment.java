package fr.renzo.wikipoff.ui.fragments;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import fr.renzo.wikipoff.R;
import fr.renzo.wikipoff.Wiki;
import fr.renzo.wikipoff.ui.activities.DeleteDatabaseActivity;
import fr.renzo.wikipoff.ui.activities.ManageDatabasesActivity;


public class TabInstalledFragment extends SherlockFragment implements OnItemClickListener {
	private SharedPreferences config;
	private ArrayList<Wiki> installedwikis=new ArrayList<Wiki>();
	private ListView installedwikislistview;
	private ManageDatabasesActivity context;
	private View wholeview;
	@SuppressWarnings("unused")
	private static final String TAG = "TabInstalledFragment";


	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context= (ManageDatabasesActivity) getSherlockActivity();
		config = PreferenceManager.getDefaultSharedPreferences(context);

		wholeview=inflater.inflate(R.layout.fragment_tab_installed,container, false);

		this.installedwikis=context.getInstalledWikis();


		InstalledWikisListViewAdapter adapter = new InstalledWikisListViewAdapter(getActivity(),  this.installedwikis); 

		installedwikislistview= (ListView) wholeview.findViewById(R.id.installedwikislistview);
		installedwikislistview.setAdapter(adapter);
		installedwikislistview.setOnItemClickListener(this);
		installedwikislistview.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Wiki wiki = installedwikis.get(position);

				Intent outputintent = new Intent(context, DeleteDatabaseActivity.class);
				outputintent.putStringArrayListExtra("dbtodelete", wiki.getDBFilesnamesAsList());
				outputintent.putExtra("dbtodeleteposition", position);
				startActivityForResult(outputintent,ManageDatabasesActivity.REQUEST_DELETE_CODE);

				return true;
			}
		});

		return wholeview;

	}


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ManageDatabasesActivity.REQUEST_DELETE_CODE)
		{
			if (resultCode >=0 && resultCode < installedwikis.size()) {
				refreshList(resultCode);
			}
		} 
	}


	public void refreshList(int position) {
		Wiki wiki = installedwikis.get(position);
		String currseldb = config.getString(getString(R.string.config_key_selecteddbfiles), "");
		if (wiki.getFilenamesAsString().equals(currseldb)){
			config.edit().remove(getString(R.string.config_key_selecteddbfiles)).commit();
		}
		config.edit().putBoolean(getString(R.string.config_key_should_update_db), true).commit();
		this.installedwikis.remove(wiki);
		((BaseAdapter) this.installedwikislistview.getAdapter()).notifyDataSetInvalidated();
	}


	public class InstalledWikisListViewAdapter extends BaseAdapter implements OnClickListener {
		private LayoutInflater inflater;
		private ArrayList<Wiki> data;
		@SuppressWarnings("unused")
		private int selectedPosition = 0;
		public InstalledWikisListViewAdapter(Context context, ArrayList<Wiki> data){
			// Caches the LayoutInflater for quicker use
			this.inflater = LayoutInflater.from(context);
			// Sets the events data
			this.data= data;
		}
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			if(position < getCount() && position >= 0 ){
				return position;
			} else {
				return -1;
			}
		}

		@Override
		public void onClick(View view) {
			selectedPosition = (Integer)view.getTag();
			notifyDataSetInvalidated();
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Wiki w = data.get(position);
			if(convertView == null){ // If the View is not cached
				// Inflates the Common View from XML file
				convertView = this.inflater.inflate(R.layout.installed_wiki, parent, false);
			}
			TextView header = (TextView ) convertView.findViewById(R.id.installedwikiheader);
			header.setText(w.getType()+" "+w.getLanglocal());
			TextView bot = (TextView ) convertView.findViewById(R.id.installedwikifooter);
			bot.setText(w.getFilenamesAsString()+" "+w.getLocalizedGendate());
//			RadioButton rb = (RadioButton) convertView.findViewById(R.id.radio);
//			rb.setChecked(w.isSelected());
//			rb.setTag(position);
//			rb.setOnClickListener(this);
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Wiki wiki = installedwikis.get(position);
		String key = context.getString(R.string.config_key_selecteddbfiles);
		ArrayList<String> namelist = wiki.getDBFilesnamesAsList();
		config.edit().putString(key ,TextUtils.join(",", namelist)).commit();
//		RadioButton rb =(RadioButton) view.findViewById(R.id.radio);
		String key2 = context.getString(R.string.config_key_should_update_db);
		config.edit().putBoolean(key2, true).commit();
//		rb.setSelected(true);
		for (int i = 0; i < installedwikis.size(); i++) {
			View ll = (View) installedwikislistview.getChildAt(i);
			RadioButton rbb =(RadioButton) ll.findViewById(R.id.radio);
			if (i!=position) {
				rbb.setChecked(false);
			} else {
				rbb.setChecked(true);
			}
		}
	}
}