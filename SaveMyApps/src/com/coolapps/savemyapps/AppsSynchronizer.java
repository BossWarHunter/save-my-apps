package com.coolapps.savemyapps;

import java.util.ArrayList;

import com.google.api.services.tasks.model.Task;

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
		listAdapter.updateSavedState(appsInfo[0], true);		
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
				Task task = new Task();
				task.setTitle(appInfo.getName());
				//TODO saveTask may return null, handle this
				Task savedTask = gTasksManager.insertTask(SaveMyApps.DEFAULT_LIST_ID, task);
				// Set the ID given by the tasks service (if the state is true)
				appInfo.setId(savedTask.getId());
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
				publishProgress(appInfo);
			}
		}
	}

}
