package com.example.facedoor.util;

import android.app.ProgressDialog;

public class ProgressShow {

	public static void show(ProgressDialog proDialog, String msg){
		proDialog.setMessage(msg);
		proDialog.show();
	}

	public static void stop(ProgressDialog proDialog){
		proDialog.dismiss();
	}
}
