package com.coolapps.savemyapps;

/*
 * Copyright 2011 Franco Sabadini - fsabadi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at	
 * 
 * http://www.apache.org/licenses/LICENSE-2.0	
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;


public class SaveMyApps extends ListActivity {
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the view assigned to this activity
        this.setContentView(R.layout.apps_list);
        // Set the adapter to fill the list
		this.setListAdapter(new AppsListAdapter(this, this.getAllApps()));
	}
	
	private ArrayList<AppInfo> getAllApps() {
		// Get the list of applications installed on the device
        ArrayList<AppInfo> allApps = getInstalledApps(false);
        //TODO: get the saved apps also and merge it with the installed apps
        return allApps;
	}
	
	// TODO: This function will display a context menu with the options to save, 
	// unsave or install app
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		//Object o = this.getListAdapter().getItem(position);
		//String keyword = o.toString();
		//Toast.makeText(this, "You selected: " + keyword, Toast.LENGTH_LONG)
		//		.show();
	}
	
	public void saveApps(View v) {
		ArrayList<CharSequence> appsToSave = getCheckedApps();
		// TODO: verify if the app is saved in the server and if not save it
	}

	public void unsaveApps(View v) {
		ArrayList<CharSequence> appsToUnsave = getCheckedApps();
		//TODO: verify if the app is saved in the server and if it is unsave it
		System.out.println();
	}
	
	private ArrayList<CharSequence> getCheckedApps() {
		ListView listView = this.getListView();
		ArrayList<CharSequence> checkedApps = new ArrayList<CharSequence>();
		// Loop through all the apps list and get the ones that were checked
		for (int i=0; i<listView.getChildCount(); i++) {
			View appView = listView.getChildAt(i);
			CheckBox appCheckBox = (CheckBox) appView.findViewById(R.id.check);
			if (appCheckBox.isChecked()) {
				CharSequence appName = ((TextView) appView.findViewById(R.id.label)).getText();
				checkedApps.add(appName);
			}
		}
		return checkedApps;
	}
	
	/**
	 * Returns all the apps actually installed on the device, 
	 * with the option to choose the system packages or not.
	 */
	private ArrayList<AppInfo> getInstalledApps(boolean getSysPackages) {
	    ArrayList<AppInfo> installedApps = new ArrayList<AppInfo>();        
	    //List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
	    List<ApplicationInfo> packs = getPackageManager().getInstalledApplications(0);
	    for (int i=0; i<packs.size(); i++) {
	    	ApplicationInfo pack = packs.get(i);
	        /*if ((!getSysPackages) && (pack.versionName == null)) {
	            continue ;
	        }*/
	        //String appName = pack.applicationInfo.loadLabel(getPackageManager()).toString();
	    	String appName = pack.loadLabel(getPackageManager()).toString();
	        //newInfo.pname = p.packageName;
	        //newInfo.versionName = p.versionName;
	        //newInfo.versionCode = p.versionCode;
	        //newInfo.icon = p.applicationInfo.loadIcon(getPackageManager());
	        AppInfo app = new AppInfo(appName, false);
	        installedApps.add(app);
	    }
	    return installedApps; 
	}
	
	// TODO: check if I can use the code down here which has a better performance
/*	
	private ArrayList<CharSequence> getCheckedApps() {
		ListView listView = this.getListView();
		// Get all the apps whose checkbox is checked
		SparseBooleanArray checkedAppsPos = listView.getCheckedItemPositions();
		ArrayList<CharSequence> checkedApps = new ArrayList<CharSequence>();
		// Get all the names of the apps that were checked
		for (int i=0; i<checkedAppsPos.size(); i++) {
			View appView = listView.getChildAt(checkedAppsPos.keyAt(i));
			CharSequence appName = ((TextView) appView.findViewById(R.id.label)).getText();
			checkedApps.add(appName);
		}
		return checkedApps;
	}
*/
}