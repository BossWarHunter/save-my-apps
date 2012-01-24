package com.cooldroidapps.savemyapps;

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

import com.cooldroidapps.savemyapps.R;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

public class AppsListLoader extends AsyncTask<Void, Void, ArrayList<AppInfo>> {

	private SaveMyApps mainActivity;
	private ProgressDialog progressDialog;
	
	public AppsListLoader(SaveMyApps activity) {
		this.mainActivity = activity;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog = new ProgressDialog(mainActivity);
		progressDialog.setMessage(mainActivity.getString(R.string.loading_list_title));
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}
	
	@Override
	protected ArrayList<AppInfo> doInBackground(Void... params) {
        // Create the default list where the app names will be saved 
        // (if it doesn't exists)
		// TODO: do this better
		// TODO change the listExists param by ID		            		
		String listId = mainActivity.getGTasksManager().getListId(SaveMyApps.DEFAULT_LIST_NAME);
		// If the list is not created in teh server
		if (listId == null) {
			mainActivity.getGTasksManager().createTaskList(SaveMyApps.DEFAULT_LIST_NAME);	
		} else {
			mainActivity.DEFAULT_LIST_ID = listId;
		}
		return getAllApps();
	}		
	
	@Override
	protected void onPostExecute(ArrayList<AppInfo> appsList) {
		// Remove the progress dialog from the UI
		progressDialog.dismiss();							
		// Create the list adapter that loads the apps list to the UI
		AppsListAdapter listAdapter = new AppsListAdapter(mainActivity, appsList);
        // Order the apps list alphabetically
        listAdapter.sort(new AppNameComparator());
        // Set the list adapter that loads the apps list to the UI
        mainActivity.setListAdapter(listAdapter);
	}
	
	/**
	 * Return a list of all the installed and saved apps.
	 * */
	private ArrayList<AppInfo> getAllApps() {
		// Get the list of apps installed on the device
		ArrayList<AppInfo> allApps = getInstalledApps();
		// Get the list of apps saved on the server
        ArrayList<AppInfo> savedApps = mainActivity.getGTasksManager().getSavedApps();
        // Merge the 2 list of apps
        int savedPassNum = savedApps.size();
        for (int i=0; i<savedPassNum; i++) {
        	AppInfo savedAppInfo = savedApps.get(i);
        	int appIndex = allApps.indexOf(savedAppInfo);
        	// If the app is already installed
        	if (appIndex != -1) { 
        		AppInfo appInfo = allApps.get(appIndex);
        		appInfo.setId(savedAppInfo.getId());
        		appInfo.setSaved(true);
        	} else { // If the app is not installed
        		allApps.add(savedAppInfo);
        	}
        }
        return allApps;
	}
	
	/**
	 * Returns all the apps actually installed on the device, 
	 * with t)he option to choose the system packages or not.
	 */
	private ArrayList<AppInfo> getInstalledApps() {
		ArrayList<AppInfo> installedApps = new ArrayList<AppInfo>();     
		PackageManager packageManager = mainActivity.getPackageManager();
	    List<ApplicationInfo> appsList = packageManager.getInstalledApplications(0);
	    for (int i=0; i<appsList.size(); i++) {
	    	ApplicationInfo app = appsList.get(i);
	        String appName = app.loadLabel(packageManager).toString();
	        // If the appName is not a system app 
	        // (didn't come pre-installed on the device)
	        if (!isSystemApp(app.flags)) {
	        	AppInfo appInfo = new AppInfo(appName);
		        appInfo.setPackageName(app.packageName);
		        appInfo.setInstalled(true);
		        installedApps.add(appInfo);
	        }
	    }
	    return installedApps; 
	}
	
	private boolean isSystemApp(int appFlags) {
		 return ((appFlags & ApplicationInfo.FLAG_SYSTEM) != 0);
	}

}

