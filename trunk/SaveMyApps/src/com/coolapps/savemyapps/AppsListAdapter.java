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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;


public class AppsListAdapter extends ArrayAdapter<AppInfo> {

	private final Activity context;
	private ArrayList<AppInfo> appsList;
	
	/** 
	 * Static to save the reference to the outer class and to avoid access to
	 * any members of the containing class
	 */
	static class ViewHolder {
		public ImageView imageViewInstalled;
		public ImageView imageViewSaved;
		public TextView textView;
		public CheckBox checkBox;
	}
	
	public AppsListAdapter(Activity context, ArrayList<AppInfo> appsList) {
        super(context, R.layout.apps_list_item, appsList);
        this.context = context;
        this.appsList = appsList;
	}
	
	/**
	 * Return a row view of the element in the positions given 
	 * to be showed in a list view.
	 * ViewHolder is used to increase the method performance 
	 * and resource utilization.
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		// Get the info of the current app that is been displayed
		AppInfo app = appsList.get(pos);
		// Recycle existing view if passed as parameter
		// This only works if the base layout for all classes are the same
		View rowView = null;
		if (convertView == null) {
			rowView = createNewRowView(app);
		} else {
			rowView = convertView;
			ViewHolder viewHolder = (ViewHolder) rowView.getTag();
			viewHolder.checkBox.setTag(app);
		}
		// Get the row for a last time and set all the values for it
		ViewHolder viewHolder = (ViewHolder) rowView.getTag();
		viewHolder.textView.setText(app.getName());
		viewHolder.checkBox.setChecked(app.isSelected());
		// If the app is saved on the server then assign it the up_arrow image, 
		// if not assign it the down_arrow image
		if (app.isSaved()) {
			viewHolder.imageViewSaved.setImageResource(R.drawable.up_arrow);
		}
		else {
			viewHolder.imageViewSaved.setImageResource(R.drawable.down_arrow);
		}
		//TODO un-comment once the install feature is added
		/*if (app.isInstalled()) {
			viewHolder.imageViewInstalled.setImageResource(R.drawable.up_arrow);
		}
		else {
			viewHolder.imageViewInstalled.setImageResource(R.drawable.down_arrow);
		}*/
		return rowView;
	}
	
	/**
	 * Create a row view linking the elements of the apps_list_item xml 
	 * to a ViewHolder object.
	 * */
	private View createNewRowView(AppInfo app) {
		// Inflate the xml row view (convert it to java code)
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.apps_list_item, null);
		// ViewHolder will buffer the assess to the individual fields of the row layout
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.textView = (TextView) rowView.findViewById(R.id.label);
		//TODO un-comment once the install feature is added
		//viewHolder.imageViewInstalled = (ImageView) rowView.findViewById(R.id.installed_icon);
		viewHolder.imageViewSaved = (ImageView) rowView.findViewById(R.id.saved_icon);
		viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.checkbox); 
		viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppInfo app = (AppInfo) viewHolder.checkBox.getTag();
				app.setSelected(buttonView.isChecked());
			}
		});
		viewHolder.checkBox.setTag(app);
		rowView.setTag(viewHolder);
		return rowView;
	}
	
	/**
	 * Updates the checkbox state of all the apps on the list. 
	 * 
	 * @param state
	 * */
	public void updateCheckState(boolean state) {
		// Update the state of all the apps on the list
		for (int i=0; i<appsList.size(); i++) {
			appsList.get(i).setSelected(state);
		}
		// Notify the adapter that the state of the checkboxs changed
		notifyDataSetChanged();
	}
	
	/**
	 * Returns all the apps that were chosen by the user 
	 * (the ones who's checkboxs are checked).
	 * */
	public ArrayList<AppInfo> getCheckedApps() {
		ArrayList<AppInfo> checkedApps = new ArrayList<AppInfo>();
		// Loop through all the apps list and get the ones that were checked
		for (int i=0; i<appsList.size(); i++) {
			AppInfo appInfo = appsList.get(i);
			if (appInfo.isSelected()) {
				checkedApps.add(appInfo);
			}
		}
		return checkedApps;
	}
	
	/**
	 * Updates the saved state of an app.
	 * 
	 * @param appToUpdate
	 * @param state
	 * 			True if the app was saved, false if it was removed from the
	 * 			sever.
	 * @param appId
	 * 			The ID that was given to the app by the tasks service (only
	 * 			makes sense when the state is true, otherwise can be null).
	 * */
	public void updateSavedState(AppInfo appToUpdate, boolean state, String appId) {
		// Get the position of the app in appsList
		int appPos = appsList.indexOf(appToUpdate);
		AppInfo appInfo = appsList.get(appPos);
		// Update the saved state of the app
		appInfo.setSaved(state);
		// Set the ID given by the tasks service (if the state is true)
		appInfo.setId(appId);
	}
	
	/**
	 * Updates the installed state of a sub-group of apps.
	 * 
	 * @param apps
	 * @param state
	 * */
	public void updateInstalledState(ArrayList<AppInfo> appsToUpdate, boolean state) {
		// TODO: Implement when the "install apps" option is added 
	}
	
}
