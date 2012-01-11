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

import android.os.AsyncTask;
import android.widget.Toast;


public class AppsSynchronizer extends AsyncTask<ArrayList<AppInfo>, AppInfo, Void> {

	public enum SyncType {
		SAVE, UNSAVE
	}
	
	private SaveMyApps mainActivity;
	private SyncType syncType;
	
	public AppsSynchronizer(SaveMyApps activity, SyncType syncType) {
		this.mainActivity = activity;
		this.syncType = syncType;
	}
	
	@Override
	protected void onPreExecute() {
		//TODO use an appropiate duration
		Toast.makeText(mainActivity.getApplicationContext(), 
				R.string.sync_start_message, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected Void doInBackground(ArrayList<AppInfo>... appsLists) {
		if (syncType.equals(SyncType.SAVE)) {
			saveApps(appsLists[0]);
		} else {
			unsaveApps(appsLists[0]);
		}
		return null;
	}		
	
	@Override
	protected void onProgressUpdate(AppInfo... appsInfo) {
		AppsListAdapter listAdapter = (AppsListAdapter) mainActivity.getListAdapter();
		listAdapter.updateSavedState(appsInfo[0]);		
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onPostExecute(Void result) {
		//TODO show different messages wheter the sync was successful or not
		Toast.makeText(mainActivity.getApplicationContext(), 
				R.string.sync_end_message, Toast.LENGTH_SHORT).show();		
	}	
	
	private void saveApps(ArrayList<AppInfo> appsToSave) {
		GTasksManager gTasksManager = mainActivity.gTasksManager;
		for (int i=0; i < appsToSave.size(); i++) {
			AppInfo appInfo = appsToSave.get(i);
			// If the app name is not saved on the server
			if (!appInfo.isSaved()) { 
				//TODO saveTask may return null, handle this
				String savedTaskId = gTasksManager.createTask(SaveMyApps.DEFAULT_LIST_ID, 
						appInfo.getName());
				// Set the ID given by the tasks service (if the state is true)
				appInfo.setId(savedTaskId);
				appInfo.setSaved(true);
				publishProgress(appInfo);
			}
		}		
	}
	
	private void unsaveApps(ArrayList<AppInfo> appsToUnsave) {
		GTasksManager gTasksManager = mainActivity.gTasksManager;
		for (int i=0; i < appsToUnsave.size(); i++) {
			AppInfo appInfo = appsToUnsave.get(i);
			// If the app name is saved on the server
			if (appInfo.isSaved()) { 
				//TODO maybe deleteTask should return a boolean and handle the posibility 
				// of not been able to connect to the server
				gTasksManager.deleteTask(SaveMyApps.DEFAULT_LIST_ID, appInfo.getId());
				appInfo.setSaved(false);
				publishProgress(appInfo);
			}
		}
	}

}
